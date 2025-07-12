package optimizer.algorithm;

import core.exception.OptimizerException;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.structs.BoundedPriorityQueue;
import core.structs.Tuple;
import core.utils.FileUtils;
import optimizer.OptimizationRequestStatisticsBundle;
import optimizer.OptimizationResourcesBundle;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;
import optimizer.prune.PlanPruner;
import optimizer.prune.BBSPlanPruner;
import java.util.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ParallelHeuristicSearchAlgorithm implements GraphTraversalAlgorithm {
    private int timeoutMS;
    private Logger logger;
    private ExecutorService executorService;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private Timer timer;
    private Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph;
    private ArrayList<pHSTask> tasks;
    private CountDownLatch latch;
    private OptimizationPlan rootPlan;

    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan, ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        this.rootPlan = rootPlan;
        this.executorService = executorService;
        //Resources
        final OptimizationRequest optimizationRequest = bundle.getWorkflow();
        final INFORENetwork inforeNetwork = bundle.getNetwork();
        final INFOREDictionary newInforeDictionary = bundle.getNewDictionary();
        final OptimizationRequestStatisticsBundle stats = bundle.getStatisticsBundle();
        final int workers = bundle.getThreads();
        final int timeoutMS = bundle.getTimeout();

        //RM operator resources
        final Map<String, String> classKeyMapping = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        final Map<String, Map<String, List<String>>> operatorImplementations = FileUtils.getOperatorImplementations(FileUtils.getOperatorGraph(optimizationRequest), inforeNetwork, newInforeDictionary, classKeyMapping);   // Operator -> (Site -> Platforms)

        this.validPlans = validPlans;
        this.logger = logger;

        //Plan graph
        this.planGraph = new ConcurrentHashMap<>();
        this.planGraph.put(rootPlan, new ConcurrentLinkedQueue<>());
        stats.addExploredPlans(1);
        stats.addCreatedPlans(1);
        stats.addExploredDimensions(rootPlan.getOperatorsAndImplementations().size());

        //Plan pruning details
        PlanPruner planPruner = new BBSPlanPruner(costEstimator,rootPlan);
        logger.fine(String.format("Pruner set to %s and timeout set to %d ms.", "bbs", timeoutMS));

        //Plan queue, accessed by all workers concurrently
        //Plans are polled, extended as many times as possible and enqueued back to this queue
        BlockingQueue<Object> planQueue = new LinkedBlockingQueue<>();
        planQueue.offer(rootPlan);

        //Global set of explored plans
        Set<OptimizationPlan> globalSet = ConcurrentHashMap.newKeySet();
        globalSet.add(rootPlan);

        //Create thread pool and spawn workers
        this.executorService = Executors.newFixedThreadPool(workers);
        this.timeoutMS = bundle.getTimeout();

        //For timer tasks
        this.timer = new Timer();

        //Construct the tasks
        this.tasks = new ArrayList<>();
        this.latch = new CountDownLatch(workers);
        for (int i = 0; i < workers; i++) {
            pHSTask task = new pHSTask(i, planGraph, operatorImplementations, planQueue, globalSet, costEstimator, stats, logger, planPruner, rootPlan, latch);
            task.registerTimerTask(timer);
            tasks.add(task);
        }
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        setup(bundle, bundle.getPlanQueue(), bundle.getRootPlan(), bundle.getExecutorService(), bundle.getCostEstimator(), bundle.getLogger());
    }

    @Override
    public void doWork() {
        //Schedule all tasks at once and wait for them to complete/timeout
        try {
            for (Future<String> task : executorService.invokeAll(tasks, timeoutMS, TimeUnit.MILLISECONDS)) {
                try {
                    logger.fine(String.format("Task completed successfully with [%s]", task.get()));
                } catch (InterruptedException e) {
                    logger.fine("Task was interrupted.");
                } catch (CancellationException e) {
                    logger.fine("Task was cancelled.");
                } catch (ExecutionException e) {
                    logger.severe(String.format("Executor encountered the following error: [%s]", e.toString()));
                }
            }
        } catch (InterruptedException e) {
            logger.fine("Executor interrupted.");
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }

        //Wait for all tasks to EXIT
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.severe("CDL Interrupted");
        }

        //Collect plans
        Comparator<? super OptimizationPlan> comparator = validPlans.comparator();
        for (int i = 0; i < this.validPlans.maxSize(); i++) {
            OptimizationPlan cur_min = Collections.max(planGraph.keySet(), comparator);
            this.validPlans.offer(cur_min);
            planGraph.remove(cur_min);
        }
    }

    @Override
    public void teardown() {
        logger.fine("Teardown");

        //Cancel timer tasks and queue
        timer.cancel();
        timer.purge();
    }


    @Override
    public List<String> aliases() {
        return Collections.singletonList("parallel_exhaustive_pruning");
    }

    private static class pHSTask implements Callable<String> {
        //Used to stop the worker when the queue empties
        private static final Object POISON_PILL = new Object();

        private final int workerID;
        private final Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph;
        private final Set<OptimizationPlan> globalSet;
        private final CostEstimator costEstimator;
        private final OptimizationRequestStatisticsBundle statisticsBundle;
        private final Logger logger;
        private final PlanPruner planPruner;
        private OptimizationPlan rootPlan;
        private final CountDownLatch latch;
        private final BlockingQueue<Object> planQueue;
        private final Map<String, Map<String, List<String>>> operatorImplementations;

        //Stats for this worker
        private int plansExplored;
        private int plansGenerated;
        private int graphCollisions;
        private int setCollisions;
        private int costDims;

        public pHSTask(int workerID,
                       Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph,
                       Map<String, Map<String, List<String>>> operatorImplementations,
                       BlockingQueue<Object> planQueue,
                       Set<OptimizationPlan> globalSet,
                       CostEstimator costEstimator,
                       OptimizationRequestStatisticsBundle statisticsBundle,
                       Logger logger,
                       PlanPruner planPruner,
                       OptimizationPlan rootPlan,
                       CountDownLatch latch) {
            this.workerID = workerID;
            this.planGraph = planGraph;
            this.operatorImplementations = operatorImplementations;
            this.planQueue = planQueue;
            this.globalSet = globalSet;
            this.costEstimator = costEstimator;
            this.statisticsBundle = statisticsBundle;
            this.logger = logger;
            this.planPruner = planPruner;
            this.rootPlan = rootPlan;
            this.latch = latch;
            this.plansExplored = 0;
            this.plansGenerated = 0;
            this.graphCollisions = 0;
            this.setCollisions = 0;
            this.costDims = 0;
        }

        public void registerTimerTask(Timer timer) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.fine(String.format("Worker %s explored %d plans so far.", workerID, plansExplored));
                }
            }, 5000, 10000);
        }

        @Override
        public String call() {
            //Thread uptime
            final Instant startInstant = Instant.now();
            final int rootCost = rootPlan.realCost();

            //Keep processing the plan graph as long as there are available actions and the executor service hasn't interrupted this thread (via a shutdownAll/get method)
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    //Wait for a plan to be made available and check if it's a plan or a poison pill
                    final Object dequeuedObject = this.planQueue.take();
                    if (dequeuedObject == POISON_PILL) {
                        this.planQueue.offer(POISON_PILL);
                        logger.fine(String.format("Worker %s poisoned..", workerID));
                        break;
                    }

                    //Safely cast to a plan object now
                    final SimpleOptimizationPlan parent = (SimpleOptimizationPlan) dequeuedObject;
                    final int parentMigrationCostSoFar = parent.realCost();

                    //Traverse the parent plan operator by operator and expand it
                    for (final String operator : parent.getOperators()) {

                        //Parent implementation for this operator
                        final Tuple<String, String> parentImplementation = parent.getOperatorsAndImplementations().get(operator);

                        //All available implementations for this operator
                        final Map<String, List<String>> implementationMap = this.operatorImplementations.get(operator);
                        this.costDims += implementationMap.size();

                        //Enumerate all possible combinations of site and platform
                        for (final String candidateSite : implementationMap.keySet()) {
                            for (final String candidatePlatform : implementationMap.get(candidateSite)) {

                                //Perform that action to the parent plan and create a new plan (deep copy)
                                final LinkedHashMap<String, Tuple<String, String>> candidateImplementations = new LinkedHashMap<>(parent.getImplementationMap());
                                Tuple<String, String> selCandidateImpl = new Tuple<>(candidateSite, candidatePlatform);
                                candidateImplementations.put(operator, selCandidateImpl);
                                final int real_cost = this.costEstimator.getPlanTotalCost(candidateImplementations);
                                final int migration_cost_so_far = getMigrationCostSoFar(this.costEstimator, this.rootPlan.getOperatorsAndImplementations(), candidateImplementations);
                                final int total_cost = real_cost - rootCost + migration_cost_so_far;
                                final SimpleOptimizationPlan candidatePlan = new SimpleOptimizationPlan(candidateImplementations, total_cost, real_cost);
                                this.plansGenerated++;

                                // Check for set collisions
                                if (this.globalSet.contains(candidatePlan)) {
                                    this.setCollisions++;
                                    continue;
                                }

                                //Check if plan belongs in the pareto frontier
                                if (this.planPruner.prune(candidatePlan)) {
                                    continue;
                                }

                                // Attempt to add child to the shared graph i.e check for graph collision
                                if (this.planGraph.putIfAbsent(candidatePlan, new ConcurrentLinkedQueue<>()) == null) {
                                    this.plansExplored++;

                                    //Add child back to the queue
                                    if (!this.planQueue.offer(candidatePlan)) {
                                        throw new IllegalStateException(String.format("Worker %s failed to offer candidate plan to queue", workerID));
                                    }

                                    //Add the new plan to global set. Add this AFTER enqueuing the plan.
                                    this.globalSet.add(candidatePlan);

                                    // Connect new plan with its parent
                                    if (!this.planGraph.get(parent).offer(candidatePlan)) {
                                        //This shouldn't really happen (unless bounded queues are used)
                                        throw new IllegalStateException(String.format("Error adding plan %s as edge.", candidatePlan.toString()));
                                    }
                                } else {
                                    this.graphCollisions++;
                                }
                            }
                        }
                    }

                    //Check if this worker needs to stop and send a poison pill to another worker
                    if (this.planQueue.isEmpty()) {
                        this.planQueue.offer(POISON_PILL);
                        logger.fine(String.format("Worker %s found plan queue empty. Sending poison pill.", workerID));
                    }
                } catch (InterruptedException e) {
                    logger.fine(String.format("Worker %s interrupted.", workerID));
                }
            }

            //Reached upon the search space is exhausted
            logger.fine(String.format("Worker %s finished after exploring %d plans, %d graph collisions and %d set collisions after %d ms.",
                    this.workerID, this.plansExplored, this.graphCollisions, this.setCollisions, Duration.between(startInstant, Instant.now()).toMillis()));

            //Update stats
            this.statisticsBundle.addCreatedPlans(plansGenerated);
            this.statisticsBundle.addExploredPlans(plansExplored);
            this.statisticsBundle.addGraphCollisions(graphCollisions);
            this.statisticsBundle.addSetCollisions(setCollisions);
            this.statisticsBundle.addPrunedPlans(planPruner.getPrunedPlans());
            this.statisticsBundle.addExploredDimensions(costDims);

            latch.countDown();
            return String.format("ID=%d", this.workerID);
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
}
