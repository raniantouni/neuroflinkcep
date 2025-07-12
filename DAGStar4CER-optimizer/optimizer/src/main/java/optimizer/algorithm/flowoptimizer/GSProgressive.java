package optimizer.algorithm.flowoptimizer;

import core.parser.network.AvailablePlatform;
import core.parser.network.Site;
import core.structs.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Algorithm GSProgressive: Find the best action across the entire flow and repeat until all assignments are completed.
 * <p>(1) Start with the initial flow</p>
 * <p>(2) Find the action that produces the lowest flow cost</p>
 * <p>(3) Update the flow graph with the action and repeat</p>
 */
public class GSProgressive {
    // Remove static fields to make the class reentrant
    private final CostEstimatorIface costEstimation;
    private final Map<Integer, AvailablePlatform> platformMapping;
    private final Map<Integer, Site> siteMapping;
    private final List<Tuple<Integer, Integer>> actions;
    private final ExecutorService executorService;
    private final int timeout;

    // Statistics tracking (thread-local or instance specific)
    private int visitCount = 0;

    /**
     * Constructor to initialize algorithm parameters
     *
     * @param platformMapping - number of changePlatform actions
     * @param siteMapping - number of changeSite actions
     * @param costEstimation - the cost estimator
     * @param executorService - executor service for parallel execution
     * @param timeout - timeout in milliseconds
     */
    public GSProgressive(
            Map<Integer, AvailablePlatform> platformMapping,
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
     * Build the list of possible actions based on platform and site mappings
     *
     * @return list of possible actions
     */
    private List<Tuple<Integer, Integer>> buildActionsList() {
        List<Tuple<Integer, Integer>> actionsList = new ArrayList<>();
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;

        // get all actions (skip P0, S0)
        for (int i = 0; i < platforms; i++) {
            for (int j = 0; j < sites; j++) {
                actionsList.add(new Tuple<>((i + 1), (j + 1)));
            }
        }

        return actionsList;
    }

    /**
     * Driver for the algorithm, including initialization and generation of the proposed plan.
     *
     * @param flow - the initial flow
     * @return - the best plan
     */
    public Graph createPlanSpaceGSProgressive(Graph flow) {
        //Get the sizes
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;
        int operators = flow.getVertices().size();
        int possiblePlans = (int) Math.pow((platforms * sites), operators);

        // Reset visit count for this run
        this.visitCount = 0;

        // print header
        System.out.println("\n--------\n[GSProgressive.createPlanSpaceGSProgressive]");
        System.out.println("#operators: " + operators + " , #platforms: " + platforms + " , #sites: " + sites);
        System.out.println("possible #plans: (" + platforms + " * " + sites + ") ^ " + operators + " = " + possiblePlans);
        System.out.println("possible actions: " + actions + "\n");

        // start-time
        Instant t1 = Instant.now();

        //Run the task
        AtomicReference<Graph> bestPlanRef = new AtomicReference<>();
        Future<?> task = executorService.submit(() -> checkAction(new Graph(flow), bestPlanRef));

        //Wait for task to complete or a timeout has reached
        try {
            task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            System.out.println("Algorithm timed out.");
        } catch (ExecutionException e) {
            System.out.printf("Executor encountered the following error: [%s]%n", e);
        }

        if (bestPlanRef.get() == null) {
            throw new IllegalStateException("Failed to produce any valid plans.");
        }
        Graph bestPlan = bestPlanRef.get();

        // end-time
        long t = Duration.between(t1, Instant.now()).toMillis();

        // print stats
        System.out.println("time: " + t + " , vis:" + visitCount);
        System.out.println("min cost: " + bestPlan.getGraphCost());
        System.out.println("min plan signature: " + bestPlan.getSignature());
        System.out.println("min plan:\n" + bestPlan);
        return bestPlan;
    }

    /**
     * Identifies the cheapest action for a flow, updates the flow, and repeat.
     *
     * @param flow - an input graph
     * @param bestPlan - reference to store the best plan found
     */
    private void checkAction(Graph flow, AtomicReference<Graph> bestPlan) {
        Graph g = new Graph(flow);
        Graph gs;    // keep intermediate states

        List<Vertex> vertexList = new ArrayList<>(g.getVertices());  // Create a copy to avoid concurrent modification

        // until we test all vertices
        while (!vertexList.isEmpty()) {
            int minCost = Integer.MAX_VALUE;   // minCost per episode
            gs = new Graph(g);                // intermediate flow with history of past finds

            Vertex minV = null;
            Tuple<Integer, Integer> minA = null;

            // for each vertex
            for (Vertex v : vertexList) {
                // try all actions
                for (Tuple<Integer, Integer> a : actions) {
                    visitCount++; // counts the plans visited

                    // Create a temporary vertex to test this action
                    Vertex tempV = new Vertex(v);

                    // get action
                    int s = a._2;
                    int p = a._1;

                    // update vertex
                    if (tempV.getPlatform() != p) {
                        tempV.setPlatform(p);
                    }
                    if (tempV.getSite() != s) {
                        tempV.setSite(s);
                    }

                    // Create a temporary graph to evaluate this action
                    Graph tempGraph = new Graph(g);
                    tempGraph.updateVertex(tempV);
                    tempGraph.updateCost(costEstimation);
                    int cst = tempGraph.getCost();

                    // keeps the cheapest action a per vertex v
                    if (cst < minCost) {
                        minCost = cst;
                        minV = v;
                        minA = new Tuple<>(p, s);
                    }
                }
            }

            if (minA == null) {
                throw new IllegalStateException("No valid platform-site combination was found.");
            }

            // update intermediate state
            g = new Graph(gs);
            Vertex u = g.getVertex(minV.getOperatorId());
            u.setPlatform(minA._1);
            u.setSite(minA._2);
            g.updateVertex(u);
            g.updateCost(costEstimation);
            bestPlan.set(g);

            // remove visited vertex
            vertexList.remove(minV);
        }
    }
}