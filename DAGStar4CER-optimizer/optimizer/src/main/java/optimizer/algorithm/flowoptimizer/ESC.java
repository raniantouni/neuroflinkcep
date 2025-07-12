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
 * Algorithm ESC: Creates the plan space by enumerating all possible
 * states using counting and number base switch.
 * <p>[Single-threaded version]
 * <p>
 * Supports two actions: changePlatform and changeSite.
 * For O operators, P platforms and S sites, the possible plans are:
 * (P * S) ^ O
 * <p>
 * We consider each operator as a vector that can take P*S values and
 * construct an index into the O vectors as a O-digit, base-(P*S) number.
 * Each digit of this number is an index into one of the vectors.
 *
 * <p>Example:</p>
 * For 4 operators and 4 possible values (e.g., 2 changePlatform and
 * 2 changeSite actions), we count from 0 to 4^4=256, and convert each
 * number to 4 base-4 digits, and use these digits as indices into
 * the operator vectors to get a plan. In this example, the possible
 * actions are [P1|S1, P1|S2, P2|S1, P2|S2].
 * <p>
 * Hence, the number 164 maps to 2210, which represents the plan:
 * O1(P2S1) -> O2(P2S1) -> O3(P1S2) -> O4(P1S1)
 */
public class ESC {

    private static final int MESSAGE_SIZE = FlowOptimizer.MESSAGE_SIZE;

    /**
     * Creates the plan space by enumerating all possible states
     * using counting and number base switch.
     *
     * <p>[Single-threaded version]</p>
     *
     * <p>Supports two actions: changePlatform and changeSite.</p>
     *
     * @param flow           - the initial flow
     * @param costEstimation
     * @param executor
     * @param timeout
     * @return
     */
    public static Graph createPlanSpaceWithCounting2Dim(Graph flow, Map<Integer, AvailablePlatform> platformMapping,
                                                        Map<Integer, Site> siteMapping, CostEstimatorIface costEstimation, ExecutorService executor, int timeout) {
        //Get the sizes
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;

        // initializations
        int operators = flow.getVertices().size();
        int possiblePlans = (int) Math.pow((platforms * sites), operators);
        int srcBase = 10;
        int trgBase = platforms * sites;
        List<Tuple<Integer, Integer>> actions = new ArrayList<>();
        ConcurrentHashMap<Integer, Integer> actionsApplied = new ConcurrentHashMap<>();

        // get all actions (skip P0, S0)
        for (int i = 0; i < platforms; i++) {
            for (int j = 0; j < sites; j++) {
                actions.add(new Tuple<>((i + 1), (j + 1)));
            }
        }

        // print header
        System.out.println("\n--------\n[" + Thread.currentThread().getStackTrace()[1].getMethodName() + "]");
        System.out.println("#operators: " + operators + " , #platforms: " + platforms + " , #sites: " + sites);
        System.out.println("possible #plans: (" + platforms + " * " + sites + ") ^ " + operators + " = " + possiblePlans);
        System.out.println("possible actions: " + actions + "\n");

        // start-time
        Instant t1 = Instant.now();

        AtomicReference<Integer> minCostRef = new AtomicReference<>();
        AtomicReference<Graph> minPlanRef = new AtomicReference<>();

        Future<?> task = executor.submit(() -> {
            // set bestPlan <- initial flow
            Graph minPlan;
            flow.updateCost(costEstimation);
            minPlanRef.set(flow);
            int minCost = flow.getCost();
            minCostRef.set(minCost);

            int vis = 0;
            for (int i = 0; i < possiblePlans; i++) {

                // identify action
                String no = Integer.toString(Integer.parseInt("" + i, srcBase), trgBase);
                no = String.format("%1$" + operators + "s", no).replace(' ', '0');
                char[] n = no.toCharArray();
                vis++;

                // apply action
                int c = 0;
                Graph g = new Graph(flow);
                for (Vertex v : g.getVertices()) {

                    // get action
                    int a = Integer.parseInt(n[c++] + "");
                    Tuple<Integer, Integer> act = actions.get(a);
                    int p = act._1;
                    int s = act._2;

                    if (v.getPlatform() != p) {
                        v.setPlatform(p);
                    }
                    if (v.getSite() != s) {
                        v.setSite(s);
                    }

                    // update graph
                    g.updateVertex(v);
                    flow.updateCost(costEstimation);
                    int cst = g.getCost();

                    // keep stats for actions applied
                    actionsApplied.compute(p, (k, val) -> (val == null) ? 1 : val + 1);
                    actionsApplied.compute(s, (k, val) -> (val == null) ? 1 : val + 1);

                    // update min plan
                    if (cst < minCost) {
                        minPlan = g;
                        minCost = g.getGraphCost();
                        minCostRef.set(minCost);
                        minPlanRef.set(minPlan);
                    }
                }

                // show progress
                if ((vis % MESSAGE_SIZE) == 0) {
                    System.out.println("vis: " + vis);
                }

            }
        });

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

        // end-time
        long t = Duration.between(t1, Instant.now()).toMillis();

        // print stats
        System.out.println("time: " + t);
        System.out.println("min cost: " + minCostRef.get());
        System.out.println("min plan signature:" + minPlanRef.get().getSignature());
        System.out.println("#actions applied: " + actionsApplied);
        System.out.println("min plan:\n" + minPlanRef.get());
        return minPlanRef.get();
    }
}
