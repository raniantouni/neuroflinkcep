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
import optimizer.plan.SimplePartialOptimizationPlan;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AStarSearchAlgorithm implements GraphTraversalAlgorithm {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AStarSearchAlgorithm.class);

    /**
     * Enum representing different strategies for aggregating costs
     * when calculating the total cost across multiple operators.
     */
    public enum AggregationStrategy {
        MAX,        // Takes the maximum cost path (critical path)
        SUM,        // Sums all operator costs
    }

    public static String VIRTUAL_START = "__START_OR";
    public static String VIRTUAL_END = "__END_OR";

    private ThreadSafeDAG<Operator> operatorGraph;
    private LinkedHashMap<String, List<Tuple<String, String>>> operatorImplementations;
    private Map<String, Set<String>> operatorChildren;
    private Map<String, Set<String>> operatorParents;
    private CostEstimator costEstimator;
    private int timeout;
    private OptimizationRequestStatisticsBundle stats;
    private Logger logger;
    private ExecutorService executorService;
    private ScheduledExecutorService statisticsExecutorService;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private OptimizationPlan rootPlan;

    private final AggregationStrategy aggregationStrategy;
    private final boolean enableStatisticsOutput;
    private int planCount = 0;
    private int planCost = 0;
    private int planOpsCount = 0;

    public AStarSearchAlgorithm(AggregationStrategy aggregationStrategy, boolean enableStatisticsOutput) {
        this.aggregationStrategy = aggregationStrategy;
        this.enableStatisticsOutput = enableStatisticsOutput;
        this.statisticsExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Method to schedule the thread that outputs the statistics
     * each 1s
     * @param startTime The time in ms when the method was called
     */
    private void scheduleStatistics(long startTime) {
        this.statisticsExecutorService.scheduleAtFixedRate(() -> {
            System.out.print("\rCompleted tasks: " + this.planCount +
                    " | " + "Task count: " + this.planCost +
                    " | " + "Operators count: " + this.planOpsCount +
                    " | " + "Running time: " + (System.currentTimeMillis() - startTime) + " ms");
            System.out.flush();  // Ensure output is flushed to the console
        }, 1L, 500L, TimeUnit.MILLISECONDS);
    }


    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan,
                      ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        INFORENetwork inforeNetwork = bundle.getNetwork();
        INFOREDictionary dictionary = bundle.getNewDictionary();
        Map<String, String> opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        this.operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        //Return if the graph is empty
        if (operatorGraph.isEmpty()) {
            throw new OptimizerException("Empty graph provided.");
        }

        this.validPlans = validPlans;
        this.rootPlan = rootPlan;

        //Operators in topological order with their available implementations
        this.operatorImplementations = FileUtils.getOperatorImplementationsAsTuples(operatorGraph, inforeNetwork, dictionary, opNamesToClassKeysMap);

        this.operatorChildren = GraphUtils.getOperatorChildrenMap(operatorGraph);
        this.operatorParents = GraphUtils.getOperatorParentMap(operatorGraph);
        this.timeout = bundle.getTimeout();
        this.costEstimator = costEstimator;
        this.executorService = executorService;
        this.stats = bundle.getStatisticsBundle();
        this.logger = logger;
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        INFORENetwork inforeNetwork = bundle.getNetwork();
        INFOREDictionary dictionary = bundle.getNewDictionary();
        Map<String, String> opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        this.operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        //Return if the graph is empty
        if (operatorGraph.isEmpty()) {
            throw new OptimizerException("Empty graph provided.");
        }

        this.validPlans = bundle.getPlanQueue();
        this.rootPlan = bundle.getRootPlan();

        //Operators in topological order with their available implementations
        this.operatorImplementations = FileUtils.getOperatorImplementationsAsTuples(operatorGraph, inforeNetwork, dictionary, opNamesToClassKeysMap);
        this.operatorChildren = GraphUtils.getOperatorChildrenMap(operatorGraph);
        this.operatorParents = GraphUtils.getOperatorParentMap(operatorGraph);
        this.timeout = bundle.getTimeout();
        this.costEstimator = bundle.getCostEstimator();
        this.executorService = bundle.getExecutorService();
        this.stats = bundle.getStatisticsBundle();
        this.logger = bundle.getLogger();
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("DAG*", "DAGStar", "dag*", "DagStar", "DAG Star", "DAG *");
    }

    @Override
    public void doWork() {
        logger.info("Starting A* search algorithm with aggregation strategy: " + aggregationStrategy);
        logger.info("Searching...");

//        if (enableStatisticsOutput) {
//            long startTime = System.currentTimeMillis();
//            scheduleStatistics(startTime);
//        }

        // Plan queue for partial plans, comparator is set to min cost
        final PriorityQueue<SimplePartialOptimizationPlan> partialPlanQueue = new PriorityQueue<>();

        // Run the task that will explore the graph and produce the best plan
        // The best plan is stored in the validPlans queue
        Future<?> task = executorService.submit(() -> explore(operatorChildren, partialPlanQueue, operatorParents, operatorImplementations, costEstimator, validPlans, operatorGraph, stats));

        try {
            task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.fine("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            logger.fine("Algorithm timed out.");
        } catch (ExecutionException e) {
            logger.fine(String.format("Executor encountered the following error: [%s]", e));
        }

        // If there wasn't time to produce a complete plan just take the best partial plan and
        // add a random implementation for the rest of the operators
        if (validPlans.isEmpty()) {
            logger.fine("Worker did not produce any complete plans. Attempting to patch one now.");
            final SimplePartialOptimizationPlan suboptimalPlan = partialPlanQueue.poll();

            //If the partialPlanQueue is empty (potentially due to a very low timeout value) generate a preset random one
            if (suboptimalPlan == null) {
                logger.fine("Giving up and producing a random plan and returning the starting plan.");
            } else {
                // Skip the first operator since it's the START_OR one. There is also never an END_OR operator in such plans
                operatorImplementations.keySet().stream()
                        .filter(op -> !suboptimalPlan.containsOperator(op))
                        .forEach(op -> suboptimalPlan.getOperatorsAndImplementations().put(op, operatorImplementations.get(op).iterator().next()));

                //Get a complete operator implementation map
                LinkedHashMap<String, Tuple<String, String>> patchedImplementations = suboptimalPlan.getOperatorsAndImplementations().entrySet().stream()
                        .skip(1)    //Skip the START_OR operator
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (o, o2) -> {
                                    throw new IllegalStateException("");
                                }, LinkedHashMap::new));

                final int bestPlanCost = costEstimator.getPlanTotalCost(patchedImplementations);
                SimpleOptimizationPlan candidatePlan = new SimpleOptimizationPlan(patchedImplementations, bestPlanCost, 0);
                validPlans.offer(candidatePlan);
                stats.addCreatedPlans(1);
                stats.addExploredPlans(1);
                logger.fine("Succeeded in patching a partial plan.");
            }
        }
    }

    @Override
    public void teardown() {
        if (enableStatisticsOutput) { // Shutdown the statistics executor only when enableStatistics is true
            statisticsExecutorService.shutdownNow();
        }
        logger.fine("Teardown");
    }

    private void explore(Map<String, Set<String>> operatorChildrenOriginal,
                         PriorityQueue<SimplePartialOptimizationPlan> partialPlanQueue,
                         Map<String, Set<String>> operatorParentsOriginal,
                         LinkedHashMap<String, List<Tuple<String, String>>> operatorImplementations,
                         CostEstimator costEstimator,
                         BoundedPriorityQueue<OptimizationPlan> bestPlanHeap,
                         ThreadSafeDAG<Operator> operatorGraph,
                         OptimizationRequestStatisticsBundle stats) {

        //Use copies of operator parents/children
        final Map<String, Set<String>> operatorParents = new HashMap<>(operatorParentsOriginal);
        final Map<String, Set<String>> operatorChildren = new HashMap<>(operatorChildrenOriginal);

        //Start by adding a START_OR and END_OR operator to the existing workflow
        final List<String> sourceOperatorNames = operatorGraph.getParentsAndChildren().entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(entry -> entry.getKey().getData().getName())
                .collect(Collectors.toList());
        final List<String> sinkOperatorNames = operatorChildren.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        sourceOperatorNames.forEach(op -> operatorParents.put(op, Collections.singleton(VIRTUAL_START)));
        sinkOperatorNames.forEach(op -> operatorChildren.put(op, Collections.singleton(VIRTUAL_END)));
        operatorChildren.put(VIRTUAL_START, Sets.newHashSet(sourceOperatorNames));
        operatorChildren.put(VIRTUAL_END, Collections.emptySet());
        operatorParents.put(VIRTUAL_END, Sets.newHashSet(sinkOperatorNames));
        operatorParents.put(VIRTUAL_START, Collections.emptySet());

        //Start by calculating the heuristic cost of each operator
        final Map<String, Integer> heuristics = calculateHeuristics(costEstimator, operatorChildren, operatorParents, VIRTUAL_END, VIRTUAL_START);

        // Print the heuristic per operator
        heuristics.forEach((op, heuristic) -> System.out.printf("Operator: %s, Heuristic: %d%n", op, heuristic));

        //Enqueue the first plan, dummy operator implementation for both START_OR and END_OR will be used
        //Ending nodes is a set that contains operators INSIDE the partial solution but with at least one successor OUTSIDE the partial solution
        final LinkedHashMap<String, Tuple<String, String>> startingPlacement = new LinkedHashMap<>();
        startingPlacement.put(VIRTUAL_START, new Tuple<>("UNKNOWN", "UNKNOWN"));
        final int virtualStartRealCost = 0;
        final int virtualStartHeuristicCost = heuristics.get(VIRTUAL_START);
        final Map<String, Integer> startOrEndingNodeCosts = new HashMap<>();
        startOrEndingNodeCosts.put(VIRTUAL_START, 0);
        final SimplePartialOptimizationPlan initialSolution = new SimplePartialOptimizationPlan(startingPlacement, startOrEndingNodeCosts, Sets.newHashSet(VIRTUAL_START), virtualStartRealCost, virtualStartHeuristicCost);
        partialPlanQueue.add(initialSolution);
        stats.addExploredDimensions(1);


        //Process all graph vertices
        long startTime = System.currentTimeMillis();
        while (!partialPlanQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {

            //Poll the partial solution with the min cost
            final SimplePartialOptimizationPlan currentPlan = partialPlanQueue.poll();
//            System.out.println();
//            System.out.println(currentPlan.getStringOfOperators() + " | Cost: " + currentPlan.totalCost());
//            System.out.print("Press Enter to continue...");
//            try {
//                System.in.read();        // waits until the user presses Enter
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            this.planCount++;
            this.stats.addExploredPlans(1);
            this.planCost = currentPlan.totalCost();
            this.planOpsCount = currentPlan.getOperatorCount();

            // If the current plan contains the virtual end operator, then it is a complete plan
            // Remove the virtual operators, convert it to a complete plan and add it to the best plans, and end the loop
            if (currentPlan.containsOperator(VIRTUAL_END)) {
                currentPlan.removeOperator(VIRTUAL_START);
                currentPlan.removeOperator(VIRTUAL_END);
                long endTime = System.currentTimeMillis();
                OptimizationPlan completePlan = new SimpleOptimizationPlan(currentPlan.getOperatorsAndImplementations(), currentPlan.totalCost(), currentPlan.totalCost());
                System.out.println("-------------------------- ALERT --------------------------");
                System.out.println("Found a complete plan with cost: " + completePlan.totalCost());
                System.out.println("Time taken: " + (endTime - startTime) + " ms");
                System.out.println("-----------------------------------------------------------");
                System.out.println();
                bestPlanHeap.offer(completePlan);
                break;
            }

            // Select the next operator to be added to the partial plans
            final Tuple<String, Set<String>> newOperatorAndParentsTuple = selectOpToAdd(currentPlan, operatorChildren, operatorParents);
            final String newOperator = newOperatorAndParentsTuple._1;
            final Set<String> newOperatorParents = newOperatorAndParentsTuple._2;

            // If new operator is the virtualEnd operator then, just add the operator and put the plan back into the queue.
            if (newOperator.equals(VIRTUAL_END)) {
                currentPlan.getOperatorsAndImplementations().put(VIRTUAL_END, new Tuple<>("UNKNOWN", "UNKNOWN"));
                partialPlanQueue.offer(currentPlan);
                continue;
            }

            // If the new operator is not the virtual end operator then, create all possible plans for the new operator
            // First, retrieve the operator implementations
            final List<Tuple<String, String>> availableImplementations = operatorImplementations.get(newOperator);

//            System.out.println("Creating " + availableImplementations.size() + " new plans for operator " + newOperator);
            //Construct and enqueue a new solution for each new implementation
            for (Tuple<String, String> availableImplementation : availableImplementations) {
                this.stats.addCreatedPlans(1);
                String newOperatorSite = availableImplementation._1;

                //Deep copy the implementations of the parent solution
                final LinkedHashMap<String, Tuple<String, String>> newImplementations = new LinkedHashMap<>(currentPlan.getOperatorsAndImplementations());
                newImplementations.put(newOperator, availableImplementation);

                //Deep copy the cost map of ending nodes and calculate the cost of the new operator
                final Map<String, Integer> newCostMap = new HashMap<>(currentPlan.getEndingNodeCost());

                int newOperatorRealCost;
                if (aggregationStrategy.equals(AggregationStrategy.SUM)) {
                    int sumOfParentRealCosts = 0;
                    int sumOfParentCommunicationCosts = 0;
                    for (String parent : newOperatorParents) {
                        String parentSite = newImplementations.get(parent)._1;
                        sumOfParentRealCosts += newCostMap.get(parent); //Real cost of the parent operator
                        sumOfParentCommunicationCosts += costEstimator.getCommunicationCost(parentSite, newOperatorSite);
                    }
                    newOperatorRealCost = costEstimator.getOperatorAndImplementationCost(newOperator, availableImplementation) + sumOfParentRealCosts + sumOfParentCommunicationCosts;
                } else {
                    int maxRealPlusCommCost = -1;
                    for (String parent : newOperatorParents) {
                        String parentSite = newImplementations.get(parent)._1;
                        int parentRealCost = newCostMap.get(parent);
                        int parentCommCost = costEstimator.getCommunicationCost(parentSite, newOperatorSite);
                        maxRealPlusCommCost = Math.max(maxRealPlusCommCost, parentRealCost + parentCommCost);
                    }
                    newOperatorRealCost = costEstimator.getOperatorAndImplementationCost(newOperator, availableImplementation) + maxRealPlusCommCost;
                }
                newCostMap.put(newOperator, newOperatorRealCost);

                // Maintain the ending nodes set
                // First, copy the ending nodes set from the current plan
                final Set<String> newEndingNodes = new HashSet<>(currentPlan.getEndingNodes());
                // Add the newly added operator to the ending nodes set
                newEndingNodes.add(newOperator);
                // Get the parents of the new operator (old ending nodes)
                final Set<String> toRemove = operatorParents.get(newOperator).stream()
                        .filter(en -> newEndingNodes.containsAll(operatorChildren.get(en)))
                        .collect(Collectors.toSet());
                // Remove the parents from the ending nodes set and the cost map
                newEndingNodes.removeAll(toRemove);
                toRemove.forEach(newCostMap::remove);

                // Now, we need to calculate the estimated cost of the plan in order to insert it to the queue
                // According to errikos' code: f(g) = max real cost of ending nodes + min heuristic cost of ending nodes
                // First, calculate the total gCost as the max real cost of the ending nodes
                int gCost = newCostMap.values().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElseThrow(() -> new IllegalStateException("Node cost map is empty, cannot calculate max real cost!"));

                // Now, calculate the hCost as the min heuristic cost of the ending nodes
                int hCost = newEndingNodes.stream()
                        .map(heuristics::get)
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElseThrow(() -> new IllegalStateException("Node heuristic cost map is empty, cannot calculate min heuristic cost!"));

//                System.out.println("Inserting new plan with " + newImplementations.size() + " with cost: " + " | gCost: " + gCost + " | hCost: " + hCost);

                //Add the new solution to the PQ
                partialPlanQueue.offer(new SimplePartialOptimizationPlan(newImplementations, newCostMap, newEndingNodes, gCost, hCost));
            }
        }

        //Thread will now exit
        logger.fine("Worker finished.");
    }

    /**
     * Selects an operator that is currently not in the polled solution but all
     * of its incoming operators are in the polled solution. If there are multiple
     * operators that satisfy these conditions the one is chosen randomly.
     *
     * @return The selected operator and all of its parent operators or
     * null with an empty if no operator was found.
     */
    private Tuple<String, Set<String>> selectOpToAdd(SimplePartialOptimizationPlan currentPlan,
                                                     Map<String, Set<String>> children,
                                                     Map<String, Set<String>> parents) {

        // Select an operator from the children of the ending nodes set
        for (String endingNode : currentPlan.getEndingNodes()) {

            // Iff a child has all of its incoming operators satisfied then select it
            for (String candidateOperator : children.get(endingNode)) {

                // Dependencies of this candidate
                Set<String> dependencies = parents.get(candidateOperator);

                // Check if this operator can be expanded and iff so commit to this one
                if (currentPlan.containsAllOperators(dependencies)) {
                    // Also check that the candidate operator is not already in the plan
                    if (currentPlan.containsOperator(candidateOperator)) {
                        continue;
                    }
                    return new Tuple<>(candidateOperator, parents.get(candidateOperator));
                }
            }
        }

        //Should only happen on dummy operator END_OR
        logger.severe(String.format("Failed to select an operator from ending nodes set [%s]", currentPlan.getEndingNodes()));
        return new Tuple<>(null, Collections.emptySet());
    }


    private int calculateHeuristicForOp(String operator,
                                        Map<String, Set<String>> children,
                                        CostEstimator costEstimator,
                                        HashMap<String, Integer> currentHeuristics,
                                        String virtualEndNode,
                                        String virtualStartNode) {
        if (operator.equals(virtualStartNode) || operator.equals(virtualEndNode)) {
            return 0;
        }

        int maxHeuristicOfChildren = -1;
        for (String child : children.get(operator)) {
            int childHeuristic = currentHeuristics.get(child);
            int childMinCost = costEstimator.getMinCostForOperator(child);
            int totalHeuristic = childMinCost + childHeuristic;
            maxHeuristicOfChildren = Math.max(maxHeuristicOfChildren, totalHeuristic);
        }
        return maxHeuristicOfChildren;
    }


    /**
     * BFS procedure to calculate the heuristic cost of each operator.
     * It stars from the end of the workflow (sink side) and performs a
     * BFS to get to the start of the workflow.
     * @param costEstimator The cost estimator
     * @param virtualEndNode The virtual end node
     * @param virtualStartNode The virtual start node
     * @return A map of operators and their heuristic costs
     */
    private Map<String, Integer> calculateHeuristics(CostEstimator costEstimator,
                                                     Map<String, Set<String>> children,
                                                     Map<String, Set<String>> parents,
                                                     String virtualEndNode,
                                                     String virtualStartNode) {
        HashMap<String, Integer> heuristics = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(virtualEndNode);

        this.logger.info("Calculating heuristics for all operators...");

        while (!queue.isEmpty()) {
            String currentOperator = queue.poll();
            if (visited.contains(currentOperator)) continue;

            visited.add(currentOperator);

            // Calculate the heuristic for the current operator
            int heuristic = calculateHeuristicForOp(currentOperator, children, costEstimator, heuristics, virtualEndNode, virtualStartNode);
            heuristics.put(currentOperator, heuristic);

            // Add all parents of the current operator to the queue
            for (String parentOperator : parents.get(currentOperator)) {
                if (!visited.contains(parentOperator)) {
                    // Need to check if all children of the parent operator are visited
                    // Then, and only then I can add it to the queue
                    boolean allChildrenOfParentVisited = true;
                    for (String childOperator : children.get(parentOperator)) {
                        if (!visited.contains(childOperator)) {
                            allChildrenOfParentVisited = false;
                            break;
                        }
                    }

                    if (allChildrenOfParentVisited) {
                        queue.add(parentOperator);
                    }
                }
            }
        }

        return heuristics;
    }
}