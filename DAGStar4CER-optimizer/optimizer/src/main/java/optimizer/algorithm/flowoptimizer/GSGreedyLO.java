package optimizer.algorithm.flowoptimizer;

import core.parser.network.AvailablePlatform;
import core.parser.network.Site;
import core.structs.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Algorithm ESG: Greedy plan enumeration with local optima.
 * <p>1. Tests all possible actions on each vertex and, after applying each action, computes the graph cost.</p>
 * <p>2. For each vertex, keeps the action that produces the lowest graph cost (local optimum).</p>
 * <p>3. Returns a plan with the cheapest action per vertex.</p>
 * <p>The greedy solution is suboptimal; the decision at each step does not consider the long-term value of its action.</p>
 */
public class GSGreedyLO {

    private final CostEstimatorIface costEstimation;
    private final Map<Integer, AvailablePlatform> platformMapping;
    private final Map<Integer, Site> siteMapping;
    private final ExecutorService executorService;
    private final int timeout;
    private final List<Tuple<Integer, Integer>> actions;

    // Statistics tracking for this instance.
    private final AtomicInteger visitCount = new AtomicInteger(0);

    /**
     * Constructor to initialize algorithm parameters.
     *
     * @param platformMapping  mapping of platforms
     * @param siteMapping      mapping of sites
     * @param costEstimation   cost estimator interface
     * @param executorService  executor for parallel execution
     * @param timeout          timeout in milliseconds
     */
    public GSGreedyLO(Map<Integer, AvailablePlatform> platformMapping,
                      Map<Integer, Site> siteMapping,
                      CostEstimatorIface costEstimation,
                      ExecutorService executorService,
                      int timeout) {
        this.platformMapping = platformMapping;
        this.siteMapping = siteMapping;
        this.costEstimation = costEstimation;
        this.executorService = executorService;
        this.timeout = timeout;
        this.actions = buildActionsList();
    }

    /**
     * Build the list of possible actions based on platform and site mappings.
     *
     * @return list of possible actions
     */
    private List<Tuple<Integer, Integer>> buildActionsList() {
        List<Tuple<Integer, Integer>> actionsList = new ArrayList<>();
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;
        // Get all actions (skip P0, S0)
        for (int i = 0; i < platforms; i++) {
            for (int j = 0; j < sites; j++) {
                actionsList.add(new Tuple<>(i + 1, j + 1));
            }
        }
        return actionsList;
    }

    /**
     * Driver for the greedy algorithm, including initialization and generation of the proposed plan.
     *
     * @param flow the initial flow graph
     * @param root a fallback graph in case the algorithm does not complete in time
     * @return the best plan found
     */
    public Graph createPlanSpaceGSGreedyLO(Graph flow, Graph root) {
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;
        int operators = flow.getVertices().size();
        int possiblePlans = (int) Math.pow((platforms * sites), operators);

        // Reset visit count for this run.
        visitCount.set(0);

        // Print header (or use a logging framework in production)
        System.out.println("\n--------\n[GSGreedyLO.createPlanSpaceGSGreedyLO]");
        System.out.println("#operators: " + operators + " , #platforms: " + platforms + " , #sites: " + sites);
        System.out.println("possible #plans: (" + platforms + " * " + sites + ") ^ " + operators + " = " + possiblePlans);
        System.out.println("possible actions: " + actions + "\n");

        Instant startTime = Instant.now();
        AtomicReference<Graph> bestPlanRef = new AtomicReference<>();

        Future<?> task = executorService.submit(() -> {
            Graph workingGraph = new Graph(flow);
            Collection<Vertex> vertices = new HashSet<>(workingGraph.getVertices());
            // Map to store the best action for each vertex.
            Map<Integer, Tuple<Integer, Integer>> greedyStrategy = new HashMap<>();

            // For each vertex, evaluate all actions.
            for (Vertex v : vertices) {
                int minCost = Integer.MAX_VALUE;
                // Test each action.
                for (Tuple<Integer, Integer> act : actions) {
                    visitCount.incrementAndGet();

                    // Save current values to restore later.
                    int originalPlatform = v.getPlatform();
                    int originalSite = v.getSite();

                    // Apply action
                    v.setPlatform(act._1);
                    v.setSite(act._2);
                    workingGraph.updateVertex(v);
                    workingGraph.updateCost(costEstimation);
                    int currentCost = workingGraph.getCost();

                    // If the current action yields a lower cost, record it.
                    if (currentCost < minCost) {
                        minCost = currentCost;
                        greedyStrategy.put(v.getOperatorId(), act);
                    }

                    // Restore original vertex state before testing the next action.
                    v.setPlatform(originalPlatform);
                    v.setSite(originalSite);
                }
            }

            // Now apply the best action found for each vertex.
            for (Vertex v : vertices) {
                Tuple<Integer, Integer> bestAct = greedyStrategy.get(v.getOperatorId());
                if (bestAct != null) {
                    v.setPlatform(bestAct._1);
                    v.setSite(bestAct._2);
                    workingGraph.updateVertex(v);
                }
            }
            workingGraph.updateCost(costEstimation);
            bestPlanRef.set(workingGraph);
        });

        try {
            task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            System.out.println("Algorithm timed out.");
        } catch (ExecutionException e) {
            System.out.printf("Executor encountered the following error: [%s]%n", e);
        }

        Graph bestPlan = bestPlanRef.get();
        if (bestPlan == null) {
            bestPlan = root;
        }
        long elapsed = Duration.between(startTime, Instant.now()).toMillis();
        System.out.println("time: " + elapsed + " ms , vis:" + visitCount.get());
        System.out.println("min cost: " + bestPlan.getGraphCost());
        System.out.println("min plan signature: " + bestPlan.getSignature());
        System.out.println("min plan:\n" + bestPlan);
        return bestPlan;
    }
}
