package optimizer;

import core.graph.ThreadSafeDAG;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.BoundedPriorityQueue;
import core.structs.Tuple;
import core.utils.CSVUtils;
import core.utils.FileUtils;
import core.utils.GraphUtils;
import core.utils.JSONSingleton;
import optimizer.algorithm.*;
import optimizer.algorithm.flowoptimizer.FlowOptimizer;
import optimizer.cost.CostEstimator;
import optimizer.cost.DAGStarCostEstimator;
import optimizer.cost.DAGStarCostEstimatorWithStatistics;
import optimizer.cost.SimpleCostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class StandaloneRunner {
    private static final Random random = new Random(0);

    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("Args: workflowPath,dictionaryPath,networkPath,algorithmName,timeout,threads");
            return;
        }

        //Parse input args
        String workflowPath = args[0];
        String dictionaryPath = args[1];
        String networkPath = args[2];
        String algorithmName = args[3];
        int timeout = Integer.parseInt(args[4]);
        int threads = Integer.parseInt(args[5]);

        try {
            //Get resources
            OptimizationResourcesBundle bundle = getBundle(dictionaryPath, networkPath, workflowPath, threads, timeout);

            //Extract and load
            final OptimizationRequest optimizationRequest = bundle.getWorkflow();
            final INFORENetwork inforeNetwork = bundle.getNetwork();
            final INFOREDictionary dictionary = bundle.getNewDictionary();
            final Map<String, String> opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
            final ThreadSafeDAG<Operator> operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);

            final Map<String, Set<String>> operatorParents = GraphUtils.getOperatorParentMap(operatorGraph);

            //Cost estimator
            CostEstimator costEstimator = new SimpleCostEstimator(operatorParents, dictionary, opNamesToClassKeysMap);

            if (algorithmName.equals("dagstar")) {
//                HashMap<String, HashMap<String, Double>> operatorCosts = getOperatorCosts();
//                HashMap<String, Double> communicationCosts = getCommunicationCosts();
//                costEstimator = new DAGStarCostEstimatorWithStatistics(operatorCosts, communicationCosts);
                costEstimator = new DAGStarCostEstimator(dictionary, opNamesToClassKeysMap);
            }

            //Root plan
            final List<Integer> platformSeeds = random
                    .ints(0, inforeNetwork.getPlatforms().size())
                    .limit(operatorGraph.getVertices().size())
                    .boxed()
                    .collect(Collectors.toList());
            final List<Integer> siteSeeds = random
                    .ints(0, inforeNetwork.getSites().size())
                    .limit(operatorGraph.getVertices().size())
                    .boxed()
                    .collect(Collectors.toList());

            final LinkedHashMap<String, Tuple<String, String>> rootPlanImpls = FileUtils.generateStartingOperatorImplementationsWithSeeds(operatorGraph,
                    platformSeeds, siteSeeds,
                    FileUtils.getOperatorImplementations(operatorGraph, inforeNetwork, dictionary, opNamesToClassKeysMap));
            final int rootCost = costEstimator.getPlanTotalCost(rootPlanImpls);
            final SimpleOptimizationPlan rootPlan = new SimpleOptimizationPlan(rootPlanImpls, 0, rootCost);
            final Logger logger = Logger.getLogger(StandaloneRunner.class.getName());
            logger.fine(String.format("Root plan [%s].", rootPlan));

            //Select the algorithm
            final BoundedPriorityQueue<OptimizationPlan> validPlans;
            final GraphTraversalAlgorithm gta;
            final ExecutorService executorService = Executors.newFixedThreadPool(bundle.getThreads());

            //Comparators
            Comparator<OptimizationPlan> costFormula = Comparator.comparingInt(o -> -o.totalCost());

            //Algo specific params
            switch (algorithmName) {
                case "op-ES":
                    gta = new ExhaustiveSearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, false);
                    break;
                case "dagstar":
                    gta = new AStarSearchAlgorithm(AStarSearchAlgorithm.AggregationStrategy.MAX, true);
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, false);
                    break;
                case "op-GS":
                    gta = new GreedySearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, false);
                    break;
                case "op-HS":
                    gta = new HeuristicSearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, false);
                    break;
                case "p-ES":
                    gta = new ParallelExhaustiveSearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, true);
                    break;
                case "p-GS":
                    gta = new ParallelGreedySearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, true);
                    break;
                case "p-HS":
                    gta = new ParallelHeuristicSearchAlgorithm();
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, true);
                    break;
                case "e-gsp":
                case "e-esq":
                case "e-esq2":
                case "e-gsg":
                case "e-qp":
                case "e-escp":
                case "e-esc":
                    gta = new FlowOptimizer(algorithmName, platformSeeds, siteSeeds);
                    validPlans = new BoundedPriorityQueue<>(costFormula, 1, true);
                    break;
                default:
                    throw new IllegalStateException("Supported algorithms are: [op-ES,op-A*,op-GS,p-ES,p-GS,p-HS]");
            }

            //Execute the algorithm
            Instant startInstant = Instant.now();
            logger.info("Initial implementations (starting plan): " + rootPlan);
            gta.setup(bundle, validPlans, rootPlan, executorService, costEstimator, logger);
            bundle.getStatisticsBundle().setSetupDuration(Duration.between(startInstant, Instant.now()).toMillis());

            gta.doWork();
            bundle.getStatisticsBundle().setExecDuration(Duration.between(startInstant, Instant.now()).toMillis());

            gta.teardown();

            //Check if any plans were produced
            if (validPlans.isEmpty()) {
                logger.warning("Optimizer failed to produce any valid plans.");
            } else {
                //Log the best plan without considering the root plan
                OptimizationPlan bestPlan = validPlans.peek();
                int totalCost = bestPlan.totalCost();
                int realCost = bestPlan.realCost();

                if (costEstimator instanceof DAGStarCostEstimatorWithStatistics) {
                    double cost = (double) totalCost / DAGStarCostEstimatorWithStatistics.COST_MULTIPLIER;
                    logger.info(String.format("------------- DAG* result: [%f] ------------- ", cost));
                }

                logger.info(String.format("WITHOUT ROOT:\tTotal cost: [%d],\tmigration cost: [%d],\treal cost: [%d],\troot cost: [%d],\tPlacements: [%s]",
                        totalCost, totalCost - realCost + rootCost, realCost, rootCost, bestPlan.getOperatorsAndImplementations()));
            }

            //Offer the root plan as a candidate plan
            validPlans.offer(rootPlan);

            //Examine the best plan
            OptimizationPlan bestPlan = validPlans.first();
            int totalCost = bestPlan.totalCost();
            int realCost = bestPlan.realCost();
            logger.info(String.format("WITH ROOT:\t\tTotal cost: [%d],\tmigration cost: [%d],\treal cost: [%d],\troot cost: [%d],\tPlacements: [%s]",
                    totalCost, totalCost - realCost + rootCost, realCost, rootCost, bestPlan.getOperatorsAndImplementations()));

            //Log results
            bundle.getStatisticsBundle().setCost(bestPlan.totalCost());
            bundle.getStatisticsBundle().setAlgorithm(algorithmName);
            bundle.setNumOfPlans(new AtomicInteger(1));
            bundle.getStatisticsBundle().setTotalThreads(threads);

            //Result
            System.out.println(Arrays.toString(OptimizationRequestStatisticsBundle.getCSVHeaders()));

            //Print to stdout
            System.out.println(CSVUtils.convertToCSV(bundle.getStatisticsBundle().toCSVEntry()));

            //Executor service shutdown/cleanup
            executorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        logger.severe("Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, Double> getCommunicationCosts() {
        String commCostsPath = "/Users/dbanelas/Developer/CREXDATA/rapidminer-crexdata-optimizer/crexdata-optimizer/input/topologies/dag-star-topology/exp_7/network_7_1_pair_lat.txt";
        HashMap<String, Double> commCosts = new HashMap<>();
        Logger.getLogger(StandaloneRunner.class.getName()).info("Loading pair latencies from " + commCostsPath + "...");
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(commCostsPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                commCosts.put(parts[0], Double.parseDouble(parts[1]));
            }
            Logger.getLogger(StandaloneRunner.class.getName()).info("Loaded " + commCosts.size() + " pair latencies.");

        } catch(FileNotFoundException e){
            System.out.println("Pair latencies file " + commCostsPath + " not found.");
        } catch (IOException e) {
            System.out.println("Unexpected error while reading pair latencies file " + commCostsPath + ".");
        }
        return commCosts;
    }

    private static HashMap<String, HashMap<String, Double>> getOperatorCosts() {
        String costsPath = "/Users/dbanelas/Developer/CREXDATA/rapidminer-crexdata-optimizer/crexdata-optimizer/input/topologies/dag-star-topology/datasets/pred_xlsx/pred_7_avg.xlsx";

        HashMap<String, HashMap<String, Double>> operatorDeviceCost = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(costsPath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Get device names from the header row (first row)
            Row headerRow = sheet.getRow(0);
            Map<Integer, String> deviceColumns = new HashMap<>();

            // Skip the first cell (empty corner cell)
            for (int i = 1; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String deviceName = cell.getStringCellValue().trim();
                    deviceColumns.put(i, deviceName);
                }
            }

            // Process each row (starting from row 1)
            for (int rowNum = 1; rowNum < sheet.getLastRowNum() + 1; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                // Get operator name from first column
                Cell operatorCell = row.getCell(0);
                if (operatorCell == null) continue;

                String operatorName = operatorCell.getStringCellValue().trim();
                operatorDeviceCost.put(operatorName, new HashMap<>());

                // Process each device column
                for (int colNum : deviceColumns.keySet()) {
                    Cell valueCell = row.getCell(colNum);
                    if (valueCell == null) continue;

                    String deviceName = deviceColumns.get(colNum);
                    Double value = valueCell.getNumericCellValue();

                    operatorDeviceCost.get(operatorName).put(deviceName, value);

                }
            }
        } catch (IOException e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while parsing Excel: " + e.getMessage());
        }

        return operatorDeviceCost;
    }

    //The optimizer bundle is a list of resources necessary for algorithms to run
    private static OptimizationResourcesBundle getBundle(String dictionaryPath, String networkPath, String workflow, int threads, int timeout) throws IOException {
        OptimizationResourcesBundle bundle = OptimizationResourcesBundle.builder()
                .withNetwork(JSONSingleton.fromJson(Files.readString(Paths.get(networkPath)), INFORENetwork.class))
                .withNewDictionary(new INFOREDictionary(Files.readString(Paths.get(dictionaryPath))))
                .withWorkflow(JSONSingleton.fromJson(Files.readString(Paths.get(workflow)), OptimizationRequest.class))
                .withThreads(threads)
                .withTimeout(timeout)
                .build();

        bundle.getStatisticsBundle().setWorkflow(workflow);
        bundle.getStatisticsBundle().setDictionary(dictionaryPath);
        bundle.getStatisticsBundle().setNetwork(networkPath);
        return bundle;
    }

}
