package optimizer.algorithm;

import com.google.common.collect.Sets;
import core.exception.OptimizerException;
import core.graph.ThreadSafeDAG;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.BoundedPriorityQueue;
import core.structs.Tuple;
import core.utils.FileUtils;
import core.utils.GraphUtils;
import optimizer.OptimizationRequestStatisticsBundle;
import optimizer.OptimizationResourcesBundle;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;
import optimizer.prune.BBSPlanPruner;
import optimizer.prune.PlanPruner;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GreedySearchAlgorithm implements GraphTraversalAlgorithm {
    private int createdPlans = 0;
    private int exploredPlans = 0;
    private int costDims = 0;
    private INFORENetwork inforeNetwork;
    private INFOREDictionary dictionary;
    private Map<String, String> opNamesToClassKeysMap;
    private ThreadSafeDAG<Operator> operatorGraph;
    private LinkedHashMap<String, List<Tuple<String, String>>> operatorImplementations;
    private Map<String, Set<String>> operatorParents;
    private CostEstimator costEstimator;
    private int timeoutMS;
    private OptimizationRequestStatisticsBundle stats;
    private Logger logger;
    private ExecutorService executorService;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private OptimizationPlan rootPlan;


    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan, ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        this.inforeNetwork = bundle.getNetwork();
        this.dictionary = bundle.getNewDictionary();
        this.opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        this.operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        //Return if the graph is empty
        if (operatorGraph.isEmpty()) {
            throw new OptimizerException("Empty graph provided.");
        }

        this.validPlans = validPlans;
        this.rootPlan = rootPlan;

        //Operators in topological order with their available implementations
        this.operatorImplementations = FileUtils.getOperatorImplementationsAsTuples(operatorGraph, inforeNetwork, dictionary, opNamesToClassKeysMap);

        this.operatorParents = GraphUtils.getOperatorParentMap(operatorGraph);
        this.timeoutMS = bundle.getTimeout();
        this.costEstimator = costEstimator;
        this.executorService = executorService;
        this.stats = bundle.getStatisticsBundle();

        //Number of plans this algorithm needs to produce
        this.logger = logger;
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        setup(bundle, bundle.getPlanQueue(), bundle.getRootPlan(), bundle.getExecutorService(), bundle.getCostEstimator(), bundle.getLogger());
    }

    @Override
    public void doWork() {
        logger.info("Greedy search algorithm started! Searching...");
        //Select the best plan
        Future<?> task = executorService.submit(() -> {
            //Operator implementation (placement)
            final LinkedHashMap<String, Tuple<String, String>> selectedOperatorImplementations = new LinkedHashMap<>();

            //Root total cost
            // Uncomment this for an implementation that considers the root plan
            int rootCost = rootPlan.realCost();

            //Root op impl
            LinkedHashMap<String, Tuple<String, String>> rootImpl = rootPlan.getOperatorsAndImplementations();

            //Last operator cost
            int lastOpCost = Integer.MAX_VALUE;

            //Cost of partial plan so far
            int planCostSoFar = Integer.MAX_VALUE;

            //Real plan cost
            int realCost = 0;

            //Traverse the graph in topological order
            for (String operator : operatorImplementations.keySet()) {

                //Check if the optimizer service canceled this task
                //TODO consider patching up a plan
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException();
                }

                //Parent operator
                Set<String> parents = operatorParents.get(operator);

                // Enumerate all implementations of this operator for each parent implementation.
                // Parents have only one implementation
                // Contains tuples of site-platform
                Tuple<String, String> selectedImpl = new Tuple<>("UNKNOWN", "UNKNOWN");

                //Handle root operators (no parents)
                int selectedImplCost = Integer.MAX_VALUE;
                if (parents.isEmpty()) {
                    for (Tuple<String, String> candidateImplementation : operatorImplementations.get(operator)) {
                        this.costDims++;
                        int candidateImplCost = costEstimator.getOperatorAndImplementationCost(operator, candidateImplementation);
                        if (candidateImplCost < selectedImplCost) {
                            selectedImpl = candidateImplementation;
                            LinkedHashMap<String, Tuple<String, String>> newImpl = new LinkedHashMap<>();
                            newImpl.put(operator, candidateImplementation);
//                            selectedImplCost = candidateImplCost - rootCost + getMigrationCostSoFar(costEstimator, rootImpl, newImpl, operator);
                            selectedImplCost = candidateImplCost;
                            planCostSoFar = candidateImplCost;
                            realCost = candidateImplCost;
                        }
                    }
                } else {
                    for (String parent : parents) {
                        Tuple<String, String> parentImplementation = selectedOperatorImplementations.get(parent);
                        for (Tuple<String, String> candidateImplementation : operatorImplementations.get(operator)) {
                            this.costDims++;
                            int operatorStaticCosts = costEstimator.getOperatorAndImplementationCost(operator, candidateImplementation);
                            // Uncomment this for an implementation that considers the root plan
//                            int operatorMigrationCosts = costEstimator.getMigrationCost(operator, parentImplementation, candidateImplementation);
//                            int candidateImplCost = operatorStaticCosts + operatorMigrationCosts;

                            // If the above line is uncommented, the following line should be commented
                            int candidateImplCost = operatorStaticCosts;
                            if (candidateImplCost < selectedImplCost) {
                                selectedImpl = candidateImplementation;
                                // Uncomment this for an implementation that considers the root plan
//                                int migrationCost = getMigrationCostSoFar(costEstimator, rootImpl, selectedOperatorImplementations, operator);
//                                selectedImplCost = candidateImplCost - rootCost + migrationCost;
//                                planCostSoFar += candidateImplCost + migrationCost;

                                // If the above line is uncommented, the following line should be commented
                                selectedImplCost = candidateImplCost;
                                planCostSoFar += candidateImplCost;
                                realCost += candidateImplCost;
                            }
                        }
                    }
                }

                //Update partial plan values
                lastOpCost = planCostSoFar - rootCost;
                selectedOperatorImplementations.put(operator, selectedImpl);
            }

            //Construct the plan from selected operator placements
            SimpleOptimizationPlan newPlan = new SimpleOptimizationPlan(selectedOperatorImplementations, lastOpCost, realCost);
            logger.info(String.format("Produced plan: [%s]", newPlan));

            this.exploredPlans++;
            this.createdPlans++;
            this.validPlans.offer(newPlan);
        });

        try {
            task.get(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.fine("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            logger.fine("Algorithm timed out.");
        } catch (ExecutionException e) {
            logger.severe(String.format("Executor encountered the following error: [%s]", e));
        }

        //Update stats
        stats.addCreatedPlans(this.createdPlans);
        stats.addExploredPlans(this.exploredPlans);
        stats.addExploredDimensions(this.costDims);
    }

    private int getMigrationCostSoFar(CostEstimator costEstimator,
                                      LinkedHashMap<String, Tuple<String, String>> R,
                                      LinkedHashMap<String, Tuple<String, String>> G,
                                      String curOp) {
        LinkedHashMap<String, Tuple<String, String>> H = new LinkedHashMap<>();
        for (String operator : R.keySet()) {
            if (R.get(operator).equals(G.get(operator))) {
                H.put(operator, G.get(operator));
            }
            if (operator.equals(curOp)) {
                break;
            }
        }
        LinkedHashMap<String, Tuple<String, String>> D1 = new LinkedHashMap<>();
        for (String operator : R.keySet()) {
            if (!R.get(operator).equals(H.get(operator))) {
                D1.put(operator, R.get(operator));
            }
        }
        LinkedHashMap<String, Tuple<String, String>> D2 = new LinkedHashMap<>();
        for (String operator : G.keySet()) {
            if (!G.get(operator).equals(H.get(operator))) {
                D2.put(operator, G.get(operator));
            }
        }
        int migration_cost_so_far = 0;
        for (String operator : Sets.intersection(D1.keySet(), D2.keySet())) {
            Tuple<String, String> D1_Placement = D1.get(operator);
            Tuple<String, String> newPlacement = D2.get(operator);
            if (!D1_Placement.equals(newPlacement)) {
                migration_cost_so_far += costEstimator.getMigrationCost(operator, D1_Placement, newPlacement);
            }
        }
        return migration_cost_so_far;
    }


    @Override
    public void teardown() {
        logger.fine("Teardown");
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("greedy", "GreedyAlgorithm");
    }
}
