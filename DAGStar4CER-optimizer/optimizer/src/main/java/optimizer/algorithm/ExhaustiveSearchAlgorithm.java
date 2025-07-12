package optimizer.algorithm;

import core.exception.OptimizerException;
import core.graph.ThreadSafeDAG;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.BoundedPriorityQueue;
import core.structs.Tuple;
import core.utils.FileUtils;
import optimizer.OptimizationRequestStatisticsBundle;
import optimizer.OptimizationResourcesBundle;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;

import java.util.logging.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Traverses the operator graph and selects the best {@link } for each operator.
 * More specifically, each operator implementation is selected by recursively (re)computing all available
 * paths in the ...
 */
public class ExhaustiveSearchAlgorithm implements GraphTraversalAlgorithm {
    private int createdPlans = 0;
    private int exploredPlans = 0;
    private int costDims = 0;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private Logger logger;
    private LinkedHashMap<String, List<Tuple<String, String>>> operatorImplementations;
    private CostEstimator costEstimator;
    private int timeoutMS;
    private ExecutorService executorService;
    private OptimizationRequestStatisticsBundle stats;
    private OptimizationPlan rootPlan;


    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan,
                      ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
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
        this.timeoutMS = bundle.getTimeout();
        this.costEstimator = costEstimator;
        this.executorService = executorService;
        this.stats = bundle.getStatisticsBundle();
        this.logger = logger;
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        setup(bundle, bundle.getPlanQueue(), bundle.getRootPlan(), bundle.getExecutorService(), bundle.getCostEstimator(), bundle.getLogger());
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("exhaustive", "ExhaustiveAlgorithm");
    }

    //Helper method
    private void exploreAllCombinations(Map<String, List<Tuple<String, String>>> operatorImplementations, CostEstimator costEstimator) {
        recurse(operatorImplementations, new LinkedList<>(operatorImplementations.keySet()).listIterator(), new LinkedHashMap<>(), costEstimator);
    }

    @Override
    public void doWork() {
        //Run the task
        Future<?> task = executorService.submit(() -> exploreAllCombinations(operatorImplementations, costEstimator));

        //Wait for task to complete or a timeout has reached
        try {
            task.get(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.fine("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            logger.fine("Algorithm timed out.");
        } catch (ExecutionException e) {
            logger.severe(String.format("Executor encountered the following error: [%s]", e));
        }

        stats.addCreatedPlans(this.createdPlans);
        stats.addExploredPlans(this.exploredPlans);
        stats.addExploredDimensions(this.costDims);

       // System.out.println(stats);
    }

    @Override
    public void teardown() {
        logger.fine("Teardown");
    }

    // helper method to do the recursion
    private void recurse(Map<String, List<Tuple<String, String>>> implMap,
                         ListIterator<String> iterator,
                         LinkedHashMap<String, Tuple<String, String>> current,
                         CostEstimator costEstimator) {
        // we're at a leaf node in the recursion tree, add solution to list
        if (!iterator.hasNext() && !Thread.currentThread().isInterrupted()) {

            //Operator implementations of this solutions
            LinkedHashMap<String, Tuple<String, String>> candidatePlanImpls = new LinkedHashMap<>();
            for (String operator : current.keySet()) {
                candidatePlanImpls.put(operator, current.get(operator));
            }

            //Offer plan to heap
            int real_cost = costEstimator.getPlanTotalCost(candidatePlanImpls);
            int migration_cost_so_far = getMigrationCostSoFar(costEstimator, rootPlan.getOperatorsAndImplementations(), candidatePlanImpls);
            int total_cost = real_cost - rootPlan.realCost() + migration_cost_so_far;
            SimpleOptimizationPlan newPlan = new SimpleOptimizationPlan(candidatePlanImpls, total_cost, real_cost);
            this.validPlans.offer(newPlan);
            this.createdPlans++;
            this.exploredPlans++;
        } else {
            //Advance the operator iterator, offer its implementations, recurse and roll back the iterator
            String operator = iterator.next();
            for (Tuple<String, String> implementation : implMap.get(operator)) {
                current.put(operator, implementation);
                this.costDims++;
                recurse(implMap, iterator, current, costEstimator);
                current.remove(operator);
            }
            iterator.previous();
        }
    }

    private int getMigrationCostSoFar(CostEstimator costEstimator,
                                      LinkedHashMap<String, Tuple<String, String>> R,
                                      LinkedHashMap<String, Tuple<String, String>> G) {
        int migration_cost_so_far = 0;
        for (String operator : R.keySet()) {
            Tuple<String, String> rootPlacement = R.get(operator);
            Tuple<String, String> newPlacement = G.get(operator);
            if (!rootPlacement.equals(newPlacement)) {
                migration_cost_so_far += costEstimator.getMigrationCost(operator, rootPlacement, newPlacement);
            }
        }
        return migration_cost_so_far;
    }
}
