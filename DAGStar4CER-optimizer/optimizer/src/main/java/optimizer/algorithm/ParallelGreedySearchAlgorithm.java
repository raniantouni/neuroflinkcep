package optimizer.algorithm;

import core.exception.OptimizerException;
import core.graph.ThreadSafeDAG;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.BoundedPriorityQueue;
import core.structs.Triple;
import core.structs.Tuple;
import core.utils.FileUtils;
import optimizer.OptimizationRequestStatisticsBundle;
import optimizer.OptimizationResourcesBundle;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;
import java.util.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ParallelGreedySearchAlgorithm implements GraphTraversalAlgorithm {
    private int timeoutMS;
    private Logger logger;
    private ExecutorService executorService;
    private Timer timer;
    private ArrayList<pGSTask> tasks;
    private CountDownLatch latch;

    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan,
                      ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {

        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        INFORENetwork inforeNetwork = bundle.getNetwork();
        ThreadSafeDAG<Operator> operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        INFOREDictionary newInforeDictionary = bundle.getNewDictionary();

        //Return if the graph is empty
        if (operatorGraph.isEmpty()) {
            throw new OptimizerException("Empty graph provided.");
        }
        this.timeoutMS = bundle.getTimeout();
        this.executorService = executorService;
        OptimizationRequestStatisticsBundle stats = bundle.getStatisticsBundle();
        final Map<String, String> classKeyMapping = FileUtils.getOpNameToClassKeyMapping(optimizationRequest);

        //Number of plans this algorithm needs to produce
        this.logger = logger;
        int workers = bundle.getThreads();

        //Global set of explored plans
        Set<OptimizationPlan> globalSet = ConcurrentHashMap.newKeySet();
        globalSet.add(rootPlan);

        //All actions <operator,site,platform>
        final Queue<Triple<String, String, String>> operatorActionsQueue = new ConcurrentLinkedQueue<>();

        final Map<String, Operator> operatorNames = new HashMap<>();
        optimizationRequest.getOperators().forEach(op -> operatorNames.put(op.getName(), op));
        Map<String, Map<String, List<String>>> operatorImplementations = FileUtils.getOperatorImplementations(FileUtils.getOperatorGraph(optimizationRequest), inforeNetwork, newInforeDictionary, classKeyMapping);   // Operator -> (Site -> Platforms)

        //Build actions collections
        for (String operator : operatorNames.keySet()) {

            //Operator implementations. Site -> Available platforms
            Map<String, List<String>> implementations = operatorImplementations.get(operator);

            String rootSite = rootPlan.getOperatorsAndImplementations().get(operator)._1;
            String rootPlatform = rootPlan.getOperatorsAndImplementations().get(operator)._2;

            //Change site actions
            int actionsCnt = 0;
            for (String site : implementations.keySet()) {
                for (String platform : implementations.get(site)) {
                    if (site.equals(rootSite) && platform.equals(rootPlatform)) {
                        continue;   //Skip the root implementation's for this operator
                    }
                    operatorActionsQueue.add(new Triple<>(operator, site, platform));
                    actionsCnt++;
                }
            }

            //Log the number of available actions for each operator
            if (actionsCnt == 0) {
                logger.warning(String.format("Operator %s has an empty action implementation list.", operator));
            }
        }
        logger.fine(String.format("A total of %d action-operator pairs will be distributed to workers.", operatorActionsQueue.size()));

        //For timer tasks
        this.timer = new Timer();

        //Construct the tasks
        this.tasks = new ArrayList<>();
        this.latch = new CountDownLatch(workers);
        for (int i = 0; i < workers; i++) {
            pGSTask task = new pGSTask(i, operatorActionsQueue, costEstimator, logger, rootPlan, latch, validPlans, stats);
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

        //Plans are collected INSIDE the tasks
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
        return Collections.singletonList("parallel_greedy");
    }

    private static class pGSTask implements Callable<String> {
        private final int workerID;
        private final Queue<Triple<String, String, String>> operatorActions;

        private final CostEstimator costEstimator;
        private final Logger logger;
        private final OptimizationPlan rootPlan;
        private final CountDownLatch latch;
        private final OptimizationRequestStatisticsBundle stats;
        private final int graphCollisions;
        private final BoundedPriorityQueue<OptimizationPlan> validPlans;

        //Stats for this worker
        private int plansExplored;
        private int plansGenerated;
        private int costDims;

        public pGSTask(int workerID,
                       Queue<Triple<String, String, String>> operatorActions,
                       CostEstimator costEstimator,
                       Logger logger,
                       OptimizationPlan rootPlan,
                       CountDownLatch latch,
                       BoundedPriorityQueue<OptimizationPlan> validPlans,
                       OptimizationRequestStatisticsBundle stats) {
            this.workerID = workerID;
            this.operatorActions = operatorActions;
            this.costEstimator = costEstimator;
            this.logger = logger;
            this.rootPlan = rootPlan;
            this.latch = latch;
            this.validPlans = validPlans;
            this.stats = stats;
            this.plansExplored = 0;
            this.plansGenerated = 0;
            this.graphCollisions = 0;
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

        @Override
        public String call() {
            //Thread uptime
            Instant startInstant = Instant.now();
            final int rootCost = rootPlan.realCost();

            //Retrieve all actions
            logger.fine(String.format("Worker %s is waiting for plans.", workerID));

            //Stats for this input plan
            int acceptedPlans = 0;
            int rejectedPlans = 0;
            int newConnections = 0;

            //Traverse all operator-actions
            while (!Thread.currentThread().isInterrupted()) {

                //For each possible action
                final Triple<String, String, String> triple = this.operatorActions.poll();
                if (triple == null) {
                    logger.fine(String.format("Worker %s found an empty queue.", workerID));
                    break;
                }

                //Unpack the triple
                final String operator = triple._1;
                final String candidateSite = triple._2;
                final String candidatePlatform = triple._3;

                // Generate a plan by performing an action
                final LinkedHashMap<String, Tuple<String, String>> candidateImplementations = new LinkedHashMap<>(rootPlan.getOperatorsAndImplementations());
                Tuple<String, String> sekCandidateImpl = new Tuple<>(candidateSite, candidatePlatform);
                candidateImplementations.put(operator, sekCandidateImpl);
                final int real_cost = this.costEstimator.getPlanTotalCost(candidateImplementations);
                final int migration_cost_so_far = getMigrationCostSoFar(this.costEstimator, this.rootPlan.getOperatorsAndImplementations(), candidateImplementations);
                final int total_cost = real_cost - rootCost + migration_cost_so_far;
                final SimpleOptimizationPlan newPlan = new SimpleOptimizationPlan(candidateImplementations, total_cost, real_cost);
                this.costDims++;
                this.plansGenerated++;

                //Offer plan
                this.validPlans.offer(newPlan);

                this.plansExplored++;
                acceptedPlans++;
            }

            //Log the number of processed plans in this batch
            logger.fine(String.format("Worker %s processed %d plans (%d this batch) with %d collisions (%d this batch), %d new connections in %d ms.",
                    workerID, plansExplored, acceptedPlans, graphCollisions, rejectedPlans, newConnections, Duration.between(startInstant, Instant.now()).toMillis()));

            //Update stats
            this.stats.addCreatedPlans(plansGenerated);
            this.stats.addExploredPlans(plansExplored);
            this.stats.addGraphCollisions(graphCollisions);
            this.stats.addExploredDimensions(costDims);

            latch.countDown();
            return String.format("ID=%d", this.workerID);
        }
    }
}
