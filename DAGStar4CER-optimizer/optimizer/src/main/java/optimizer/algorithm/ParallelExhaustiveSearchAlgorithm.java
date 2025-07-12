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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ParallelExhaustiveSearchAlgorithm implements GraphTraversalAlgorithm {
    private int timeoutMS;
    private Logger logger;
    private ExecutorService executorService;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private CountDownLatch latch;
    private Timer timer;
    private List<pESTask> tasks;
    private Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph;

    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan, ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        INFORENetwork inforeNetwork = bundle.getNetwork();
        INFOREDictionary dictionary = bundle.getNewDictionary();
        Map<String, String> opNamesToClassKeysMap = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);
        ThreadSafeDAG<Operator> operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        //Return if the graph is empty
        if (operatorGraph.isEmpty()) {
            throw new OptimizerException("Empty graph provided.");
        }

        //Operators in topological order with their available implementations
        LinkedHashMap<String, Map<String, List<String>>> operatorImplementations = FileUtils.getOperatorImplementations(FileUtils.getOperatorGraph(optimizationRequest), inforeNetwork, dictionary, opNamesToClassKeysMap);

        this.timeoutMS = bundle.getTimeout();
        this.executorService = executorService;
        OptimizationRequestStatisticsBundle stats = bundle.getStatisticsBundle();

        //Number of plans this algorithm needs to produce
        this.logger = logger;
        int workers = bundle.getThreads();
        this.validPlans = validPlans;

        //Global set of explored plans
        int initCapacity = 2 << 12;
        final Set<OptimizationPlan> globalSet = ConcurrentHashMap.newKeySet(initCapacity);
        globalSet.add(rootPlan);

        //Plan graph
        float lf = 0.75f;           // ratio between the number of "buckets" in the map and the number of expected elements;
        this.planGraph = new ConcurrentHashMap<>(initCapacity, lf, workers);
        planGraph.put(rootPlan, new ConcurrentLinkedQueue<>());
        stats.addExploredPlans(1);
        stats.addCreatedPlans(1);

        //Plan queue, accessed by all workers concurrently
        //Plans are polled, extended as many times as possible and enqueued back to this queue
        final BlockingQueue<Object> planQueue = new LinkedBlockingQueue<>();
        planQueue.offer(rootPlan);

        //For timer tasks
        this.timer = new Timer();
        System.out.printf("Starting the optimization of %d operators in %d workers.%n", rootPlan.getOperatorsAndImplementations().size(), workers);

        //Construct the tasks
        this.tasks = new ArrayList<>();
        this.latch = new CountDownLatch(workers);
        for (int i = 0; i < workers; i++) {
            pESTask task = new pESTask(i, planGraph, operatorImplementations, planQueue, globalSet, costEstimator, logger, stats, rootPlan, latch);
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
                    System.out.printf("Task completed successfully with [%s]%n", task.get());
                } catch (InterruptedException e) {
                    System.out.println("Task was interrupted.");
                } catch (CancellationException e) {
                    System.out.println("Task was cancelled.");
                } catch (ExecutionException e) {
                    System.out.printf("Executor encountered the following error: [%s]%n", e);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Executor interrupted.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //Wait for all tasks to EXIT
        try {
            this.latch.await();
        } catch (InterruptedException e) {
            System.out.println("CDL Interrupted");
        }

        //Collect plans
        for (int i = 0; i < this.validPlans.maxSize(); i++) {
            OptimizationPlan cur_min = Collections.max(this.planGraph.keySet(), this.validPlans.comparator());
            this.validPlans.offer(cur_min);
            this.planGraph.remove(cur_min);
        }
    }

    @Override
    public void teardown() {
        System.out.println("Teardown");

        //Cancel timer tasks and queue
        timer.cancel();
        timer.purge();
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("parallel_exhaustive");
    }

    private static class pESTask implements Callable<String> {
        //Used to stop the worker when the queue empties
        private static final Object POISON_PILL = new Object();

        private final int workerID;
        private final Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph;
        private final Set<OptimizationPlan> globalSet;
        private final CostEstimator costEstimator;
        private final OptimizationRequestStatisticsBundle statisticsBundle;
        private final OptimizationPlan rootPlan;
        private final CountDownLatch cdl;
        private final BlockingQueue<Object> planQueue;
        private final Map<String, Map<String, List<String>>> operatorImplementations;
        private final Logger logger;

        //Stats for this worker
        private int plansExplored;
        private int plansGenerated;
        private int graphCollisions;
        private int setCollisions;
        private int costDims;

        public pESTask(int workerID,
                       Map<OptimizationPlan, Queue<OptimizationPlan>> planGraph,
                       Map<String, Map<String, List<String>>> operatorImplementations,
                       BlockingQueue<Object> planQueue,
                       Set<OptimizationPlan> globalSet,
                       CostEstimator costEstimator,
                       Logger logger,
                       OptimizationRequestStatisticsBundle statisticsBundle,
                       OptimizationPlan rootPlan,
                       CountDownLatch cdl) {
            this.workerID = workerID;
            this.planGraph = planGraph;
            this.operatorImplementations = operatorImplementations;
            this.planQueue = planQueue;
            this.globalSet = globalSet;
            this.costEstimator = costEstimator;
            this.logger = logger;
            this.statisticsBundle = statisticsBundle;
            this.rootPlan = rootPlan;
            this.cdl = cdl;
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
                    System.out.printf("Worker %s explored %d plans so far.%n", workerID, plansExplored);
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
                        System.out.printf("Worker %s poisoned..%n", workerID);
                        break;
                    }

                    //Safely cast to a plan object now
                    final OptimizationPlan parent = (OptimizationPlan) dequeuedObject;

                    //Traverse the parent plan operator by operator and expand it
                    for (final String operator : parent.getOperatorsAndImplementations().keySet()) {

                        //All available implementations for this operator
                        final Map<String, List<String>> implementationMap = this.operatorImplementations.get(operator);
                        this.costDims += implementationMap.size();

                        //Enumerate all possible combinations of site and platform
                        for (final String candidateSite : implementationMap.keySet()) {
                            for (final String candidatePlatform : implementationMap.get(candidateSite)) {
                                //Candidate implementation
                                Tuple<String, String> selCandidateImpl = new Tuple<>(candidateSite, candidatePlatform);

                                //Perform that action to the parent plan and create a new plan
                                final LinkedHashMap<String, Tuple<String, String>> candidateImplementations = new LinkedHashMap<>(parent.getOperatorsAndImplementations());
                                candidateImplementations.put(operator, selCandidateImpl);
                                final int real_cost = this.costEstimator.getPlanTotalCost(candidateImplementations);
                                final int migration_cost_so_far = getMigrationCostSoFar(this.costEstimator, this.rootPlan.getOperatorsAndImplementations(), candidateImplementations);
                                final int total_cost = real_cost - rootCost + migration_cost_so_far;
                                final OptimizationPlan candidatePlan = new SimpleOptimizationPlan(candidateImplementations, total_cost, real_cost);
                                this.plansGenerated++;

                                // Check for set collisions
                                if (this.globalSet.contains(candidatePlan)) {
                                    this.setCollisions++;
                                    continue;
                                }

                                // Attempt to add child to the shared graph i.e check for graph collision
                                if (this.planGraph.putIfAbsent(candidatePlan, new ConcurrentLinkedQueue<>()) == null) {

                                    //Add child back to the queue
                                    if (!this.planQueue.offer(candidatePlan)) {
                                        throw new IllegalStateException(String.format("Worker %s failed to offer candidate plan to queue", workerID));
                                    }
                                    this.plansExplored++;

                                    // Connect new plan with its parent
                                    if (!this.planGraph.get(parent).offer(candidatePlan)) {
                                        //This shouldn't really happen (unless bounded queues are used)
                                        throw new IllegalStateException(String.format("Error adding plan %s as edge.", candidatePlan.toString()));
                                    }

                                    //Add the new plan to global set. Add this AFTER enqueuing the plan.
                                    this.globalSet.add(candidatePlan);
                                } else {
                                    this.graphCollisions++;
                                }
                            }
                        }
                    }

                    //Check if this worker needs to stop and send a poison pill to another worker
                    if (this.planQueue.isEmpty()) {
                        this.planQueue.offer(POISON_PILL);
                        System.out.printf("Worker %s found plan queue empty. Sending poison pill.%n", workerID);
                    }
                } catch (Exception e) {
                    System.out.printf("Worker %s interrupted.%n", workerID);
                    Thread.currentThread().interrupt();
                }
            }

            //Reached upon the search space is exhausted
            System.out.printf("Worker %s finished after exploring %d plans, %d graph collisions and %d set collisions after %d ms.%n",
                    this.workerID, this.plansExplored, this.graphCollisions, this.setCollisions, Duration.between(startInstant, Instant.now()).toMillis());

            //Update stats
            this.statisticsBundle.addCreatedPlans(plansGenerated);
            this.statisticsBundle.addExploredPlans(plansExplored);
            this.statisticsBundle.addGraphCollisions(graphCollisions);
            this.statisticsBundle.addSetCollisions(setCollisions);
            this.statisticsBundle.addExploredDimensions(costDims);

            cdl.countDown();
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
