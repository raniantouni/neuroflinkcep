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
import optimizer.plan.HeuristicPlan;
import optimizer.plan.OptimizationPlan;
import optimizer.prune.ParetoFrontier;
import optimizer.prune.SimpleParetoFrontier;
import java.util.logging.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HeuristicSearchAlgorithm implements GraphTraversalAlgorithm {

    //Stats
    private int createdPlans = 0;
    private int exploredPlans = 0;
    private int costDims = 0;
    private int timeoutMS;

    private LinkedHashMap<String, List<Tuple<String, String>>> operatorImplementations;
    private Map<String, Set<String>> operatorParents;
    private CostEstimator costEstimator;
    private OptimizationRequestStatisticsBundle stats;
    private Logger logger;
    private ExecutorService executorService;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private OptimizationPlan rootPlan;
    private List<String> operatorSinks;

    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan, ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();

        //Plan
        INFORENetwork inforeNetwork = bundle.getNetwork();
        INFOREDictionary dictionary = bundle.getNewDictionary();
        Map<String, String> opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        ThreadSafeDAG<Operator> operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);

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

        //Calculate sink operators
        this.operatorSinks = opNamesToClassKeysMap.keySet().stream()
                .filter(op -> !this.operatorParents.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet())
                        .contains(op))
                .collect(Collectors.toList());
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        setup(bundle, bundle.getPlanQueue(), bundle.getRootPlan(), bundle.getExecutorService(), bundle.getCostEstimator(), bundle.getLogger());
    }

    @Override
    public void doWork() {
        //Select the best plan
        Future<?> task = executorService.submit(() -> {
            //Root total cost
            final int rootCost = rootPlan.realCost();

            //Root op impl
            final LinkedHashMap<String, Tuple<String, String>> rootImpl = rootPlan.getOperatorsAndImplementations();

            //Create virtual END_OR operator
            final String END_OR = "__END_OR";

            //Partial plan pruner per operator
            final Map<String, ParetoFrontier<HeuristicPlan>> paretoFrontiers = new HashMap<>();

            //Traverse the graph in topological order (remember that the map is linked)
            for (String operator : this.operatorImplementations.keySet()) {

                //Plan pruner of this operator
                SimpleParetoFrontier currentFrontier = new SimpleParetoFrontier(costEstimator);

                //Parent operator
                final Set<String> parents = operatorParents.get(operator);

                //Handle root operators differently
                if (parents.isEmpty()) {

                    //Produce all the best 1-op plans
                    for (Tuple<String, String> candidateImplementation : operatorImplementations.get(operator)) {
                        this.costDims++;
                        final int realCost = costEstimator.getOperatorAndImplementationCost(operator, candidateImplementation);
                        final LinkedHashMap<String, Tuple<String, String>> newImpl = new LinkedHashMap<>();
                        newImpl.put(operator, candidateImplementation);
                        final int migrationCostSoFar = getMigrationCostSoFar(costEstimator, rootImpl, newImpl, operator);
                        final int totalCost = realCost - rootCost + migrationCostSoFar;
                        final HeuristicPlan newPlan = new HeuristicPlan(newImpl, totalCost, migrationCostSoFar, realCost);
                        currentFrontier.offerPlan(newPlan);
                    }
                } else {
                    //Merge the pareto frontiers of the previous operators to the frontier of the new operator (initially empty)
                    Collection<LinkedHashMap<String, Tuple<String, String>>> candidatePlans = mergePlanLists(
                            paretoFrontiers.entrySet().stream()
                                    .filter(kv -> parents.contains(kv.getKey()))
                                    .map(Map.Entry::getValue)
                                    .flatMap(frontier -> frontier.getFrontier().stream().map(HeuristicPlan::getOperatorsAndImplementations))
                                    .iterator()
                    );

                    //Check if something happened
                    if (candidatePlans.isEmpty()) {
                        throw new IllegalStateException("Merging operator implementations produced no results.");
                    }

                    //Iterate over all candidate operator implementations
                    for (LinkedHashMap<String, Tuple<String, String>> candidatePlan : candidatePlans) {

                        //Iterate over all possible candidate operator implementations and extend each candidate plan
                        for (Tuple<String, String> candidateOperatorImpl : this.operatorImplementations.get(operator)) {
                            this.costDims++;

                            //Add the new implementation
                            candidatePlan.put(operator, candidateOperatorImpl);

                            //Calculate all the costs
                            final int realCost = costEstimator.getPlanTotalCost(candidatePlan);
                            final int migrationCostSoFar = getMigrationCostSoFar(costEstimator, rootImpl, candidatePlan, operator);
                            final int totalCost = realCost - rootCost + migrationCostSoFar;

                            //New plan
                            final HeuristicPlan newPlan = new HeuristicPlan(candidatePlan, totalCost, migrationCostSoFar, realCost);
                            currentFrontier.offerPlan(newPlan);
                        }
                    }
                }

                //Save the pareto frontier of the operator
                paretoFrontiers.put(operator, currentFrontier);

                //TODO consider offering previous pareto frontier objects to GC
                // ...

                //Log the best plan
                //logger.fine(String.format("Best plan for operator %s is [%s]",
                //        operator, opFrontier.getFrontier().stream().min(Comparator.comparingInt(HeuristicPlan::totalCost)).toString()));
            }

            //Merge sink operators, similarly to what an END_OR operator would do
            //Since all operators are terminal we can assume that any of them are the plan's final operator
            final String finalOperator = this.operatorSinks.get(0);
            final List<HeuristicPlan> candidates = new ArrayList<>();
            final Collection<LinkedHashMap<String, Tuple<String, String>>> sinkPlans = mergePlanLists(
                    paretoFrontiers.entrySet().stream()
                            .filter(kv -> this.operatorSinks.contains(kv.getKey()))
                            .map(Map.Entry::getValue)
                            .flatMap(frontier -> frontier.getFrontier().stream().map(HeuristicPlan::getOperatorsAndImplementations))
                            .iterator()
            );
            for (LinkedHashMap<String, Tuple<String, String>> candidatePlan : sinkPlans) {
                //Calculate all the costs
                final int realCost = costEstimator.getPlanTotalCost(candidatePlan);
                final int migrationCostSoFar = getMigrationCostSoFar(costEstimator, rootImpl, candidatePlan, finalOperator);
                final int totalCost = realCost - rootCost + migrationCostSoFar;

                //New plan
                final HeuristicPlan newPlan = new HeuristicPlan(candidatePlan, totalCost, migrationCostSoFar, realCost);
                candidates.add(newPlan);
            }

            //Select the best plan
            final OptimizationPlan selPlan = Collections.min(candidates, Comparator.comparingInt(HeuristicPlan::totalCost));
            final OptimizationPlan outputPlan = selPlan == null || selPlan.compareTo(rootPlan) >= 0 ? rootPlan : selPlan;
            logger.fine(String.format("Output plan: [%s]", outputPlan.toString()));

            this.exploredPlans++;
            this.createdPlans++;
            this.validPlans.offer(outputPlan);
        });

        try {
            task.get(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.fine("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            logger.fine("Algorithm timed out.");
        } catch (ExecutionException e) {
            logger.severe(String.format("Executor encountered the following error: [%s]", e.toString()));
        }

        //Update stats
        stats.addCreatedPlans(this.createdPlans);
        stats.addExploredPlans(this.exploredPlans);
        stats.addExploredDimensions(this.costDims);
    }

    /**
     * Plans with shared operators are also handled here.
     * TODO check if topological order is maintained.
     */
    public Collection<LinkedHashMap<String, Tuple<String, String>>> mergePlanLists(Iterator<LinkedHashMap<String, Tuple<String, String>>> inputPlanIterator) {
        //Edge cases
        if (!inputPlanIterator.hasNext()) {
            return new HashSet<>();
        }

        //Plan accumulator, add the first plan before the main loop
        final Set<LinkedHashMap<String, Tuple<String, String>>> planAcc = new HashSet<>();
        planAcc.add(inputPlanIterator.next());

        //Iterate over remaining plans
        while (inputPlanIterator.hasNext()) {

            //Advance iterator and extract the operators
            final LinkedHashMap<String, Tuple<String, String>> nextPlan = inputPlanIterator.next();

            //Evaluate the cross product of both input collections
            final Set<LinkedHashMap<String, Tuple<String, String>>> newAcc = new HashSet<>();

            //Iterate over all available plans
            for (LinkedHashMap<String, Tuple<String, String>> existingPlan : planAcc) {

                //If the input and existing plan have no common operators simply output their union
                if (Sets.intersection(existingPlan.keySet(), nextPlan.keySet()).isEmpty()) {
                    LinkedHashMap<String, Tuple<String, String>> newPlan = new LinkedHashMap<>();
                    for (String operator : Sets.union(existingPlan.keySet(), nextPlan.keySet())) {
                        if (existingPlan.containsKey(operator)) {
                            newPlan.put(operator, existingPlan.get(operator));
                        } else {
                            newPlan.put(operator, nextPlan.get(operator));
                        }
                    }
                    newAcc.add(newPlan);
                } else {
                    //The goal is to merge the input plan with the current existing plan by taking into account
                    //shared operators and creating multiple plans that that cover all possible outcomes
                    //of such merge.

                    //Collection of new plans
                    final List<LinkedHashMap<String, Tuple<String, String>>> newPlans = new ArrayList<>();

                    //Produce new plans by enumerating all operators once
                    for (String operator : Sets.union(existingPlan.keySet(), nextPlan.keySet())) {

                        //New implementations for this operator
                        final List<Tuple<String, String>> newImplementations = new ArrayList<>();

                        //Produce up to 2 new operator implementations (when encountering a shared operator)
                        if (Sets.intersection(existingPlan.keySet(), nextPlan.keySet()).contains(operator)) {
                            newImplementations.add(existingPlan.get(operator));
                            newImplementations.add(nextPlan.get(operator));
                        } else {
                            if (existingPlan.containsKey(operator)) {
                                newImplementations.add(existingPlan.get(operator));
                            } else {
                                newImplementations.add(nextPlan.get(operator));
                            }
                        }
                        //If this is the first entry in the new map just add the new entries
                        if (newPlans.isEmpty()) {
                            for (Tuple<String, String> entry : newImplementations) {
                                LinkedHashMap<String, Tuple<String, String>> newPlan = new LinkedHashMap<>();
                                newPlan.put(operator, entry);
                                newPlans.add(newPlan);
                            }
                        } else {
                            //Some operators have conflicting implementations in input and existing plans
                            //These operators will have to split the current plan into 2 plans
                            //The split will simply duplicate the map and just add each conflicting operator into a separate map
                            if (newImplementations.size() == 1) {
                                for (LinkedHashMap<String, Tuple<String, String>> newPlan : newPlans) {
                                    newPlan.put(operator, newImplementations.get(0));
                                }
                            } else {
                                List<LinkedHashMap<String, Tuple<String, String>>> extraPlans = new ArrayList<>(newPlans);
                                for (int i = 0; i < newPlans.size(); i++) {
                                    newPlans.get(i).put(operator, newImplementations.get(0));
                                    extraPlans.get(i).put(operator, newImplementations.get(1));
                                }
                                newPlans.addAll(extraPlans);
                            }
                        }
                    }

                    //Offer plans to the merged collection
                    newAcc.addAll(newPlans);
                }
            }

            //Offer
            planAcc.clear();
            planAcc.addAll(newAcc);
        }

        //Return the merged plans without duplicates
        return planAcc;
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
        return Arrays.asList("heuristic", "op-HS");
    }
}
