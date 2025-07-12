package optimizer.algorithm.flowoptimizer;

import core.parser.network.AvailablePlatform;
import core.parser.network.Site;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Algorithm ESQ: Creates the plan space by applying an action
 * to an operator of a plan, stores the plan into a queue,
 * and repeats until there is no more plans in the queue.
 * <p>
 * The initial plan affects the plan generation. If its operators
 * start with a Site or Platform different from those applied
 * to the plan, then the #visited_plans increases, as the
 * Site and Platform of the initial plan counts towards
 * the actions applied. E.g., if a plan O1 -> O2 -> O3 -> O4
 * starts with all four operators at P0 and S0, and the
 * actions tested include platforms P1, P2 and sites S1, S2,
 * then the #visited_plans is (3*3)^4 = 6561 instead of
 * (2*2)^4 = 256.
 * <p>
 * The [fixRoot] flag enables a fix by jumping ahead with
 * a single hop to an initial plan containing vertices on
 * the desired Sites and Platforms.
 */
public class ESQ2 {

    private static final int MESSAGE_SIZE = FlowOptimizer.MESSAGE_SIZE;
    //Cache the available actions
    private static final OptimizerAction[] allAction = OptimizerAction.values();
    // Global variables used in createPlanSpaceWithQueue
    //
    static ConcurrentLinkedQueue<Graph> planSpace_X = new ConcurrentLinkedQueue<Graph>();
    static ConcurrentLinkedQueue<String> visitedSigns_X = new ConcurrentLinkedQueue<String>();
    static ConcurrentHashMap<List<Integer>, Integer> actionsApplied_X = new ConcurrentHashMap<>();
    static int minCost_X = Integer.MAX_VALUE;
    static Graph minPlan_X = new Graph();
    static int cnt_vis_X = 0;
    static int cnt_expl_X = 0;
    private static CostEstimatorIface costEstimation;

    /**
     * Creates the plan space by applying an action to an operator
     * of a plan, stores the plan into a queue, and repeats until there is
     * no more plans in the queue.
     * <p>
     * The number of actions is given as a parameter to the algorithm.
     *
     * @param flow           - the initial flow
     * @param fixRoot        - enables action assignment to the initial flow
     * @param costEstimation
     * @param executor
     * @param timeout
     * @return
     */
    public static Graph createPlanSpaceWithQueue(Graph flow, Map<Integer, AvailablePlatform> platformMapping, Map<Integer, Site> siteMapping,
                                                 boolean fixRoot, CostEstimatorIface costEstimation, ExecutorService executor, int timeout) {
        ESQ2.costEstimation = costEstimation;
        //Get the sizes
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;

        // get all actions
        final List<Integer> platformActions = new ArrayList<>();
        final List<Integer> siteActions = new ArrayList<>();
        final List<List<Integer>> actions = new ArrayList<>();
        for (int i = 0; i < platforms; i++) {
            platformActions.add(i + 1);
        }
        for (int j = 0; j < sites; j++) {
            siteActions.add(j + 1);
        }
        for (int i = 0; i < platforms; i++) {
            for (int j = 0; j < sites; j++) {
                for (OptimizerAction action : OptimizerAction.values()) {
                    actions.add(List.of(i + 1, j + 1, action.ordinal()));
                }
            }
        }

        // if the flow contains S or P other than those in actions,
        // assign flow vertices into S and P included in actions;
        // this is a one-hop generation of a valid initial flow
        if ((fixRoot) && (platforms * sites > 0)) {
            int p1 = 1;
            int s1 = 1;
            for (Vertex v : flow.getVertices()) {
                boolean upd = false;
                if (!platformActions.contains(v.getPlatform())) {
                    v.setPlatform(p1);
                    upd = true;
                }
                if (!siteActions.contains(v.getSite())) {
                    v.setSite(s1);
                    upd = true;
                }
                if (upd) flow.updateVertex(v);
            }
        }

        // initialization
        planSpace_X = new ConcurrentLinkedQueue<>();
        planSpace_X.add(flow);
        visitedSigns_X = new ConcurrentLinkedQueue<>();
        visitedSigns_X.add(flow.getSignature());
        actionsApplied_X = new ConcurrentHashMap<>();
        minPlan_X = flow;
        flow.updateCost(costEstimation);
        minCost_X = flow.getCost();
        cnt_vis_X = 0;
        cnt_expl_X = 0;

        // compute the size of the plan space (for the header)
        int operators = flow.getVertices().size();
        int possiblePlans = (int) Math.pow((platforms * sites), operators);

        // print header
        System.out.println("\n--------\n[" + Thread.currentThread().getStackTrace()[1].getMethodName() + "]");
        System.out.println("#operators: " + operators + " , #platforms: " + platforms + " , #sites: " + sites);
        System.out.println("possible #plans: (" + platforms + " * " + sites + ") ^ " + operators + " = " + possiblePlans);
        System.out.println("possible actions: " + platformActions.size() + siteActions.size() + "\n");

        // start-time
        Instant ts = Instant.now();

        Future<?> task = executor.submit(() -> {

            // create plan space
            while (!planSpace_X.isEmpty()) {

                // get a plan from the space
                Graph g = new Graph(planSpace_X.poll());

                // apply action to Graph's vertices
                for (Vertex v : g.getVertices()) {

                    for (List<Integer> e : actions) {

                        // apply an action to a plan
                        genPlanWorkerForQueue(g, v, e);
                    }
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
        long t = Duration.between(ts, Instant.now()).toMillis();

        // print stats
        System.out.println("time: " + t + " , vis:" + visitedSigns_X.size() + "]");
        System.out.println("min cost: " + minCost_X);
        System.out.println("min plan signature:" + minPlan_X.getSignature());
        System.out.println("#actions applied: " + actionsApplied_X);
        System.out.println("min plan:\n" + minPlan_X);
        return minPlan_X;
    }

    /**
     * Applies an action to a vertex of a flow.
     * <p>
     * [Worker function for {@code createPlanSpaceWithQueue()}]
     *
     * @param g A flow
     * @param v A vertex of the flow
     * @param e An action triple, (site,platform,optimizer_action)
     */
    public static void genPlanWorkerForQueue(Graph g, Vertex v, List<Integer> e) {
        //Unpack
        final int candidatePlatform = e.get(0);
        final int candidateSite = e.get(1);
        final OptimizerAction candidateAction = allAction[e.get(2)];

        // get the position of v in the list of graph vertices
        int vrtCnt = g.getVertices().indexOf(v);    //TODO replace with operator name?

        // look ahead, before applying the action (only for Platform)
        //char[] graphSignature = g.getSign().toCharArray();
        //char[] actionName = e.name().toCharArray();
        //graphSignature[vrtCnt] = actionName[1];  // replace P bit in the signature
        //String sgn = String.valueOf(graphSignature);

        // look-ahead filter:
        // finds the flow signature before applying an action and
        // if it is already in a visited list, it skips the action
        // 1. get the flow signature, assuming we have applied the action e
        // 2. check whether this signature corresponds to a flow already processed
        // 3. if so, skip this <vertex, action> combination and return
        // it assumes Px|Sx syntax --modify as needed for more action types
        String[] graphSignature = g.getSignature().split("\\|");
        switch (candidateAction) {
            case CHANGE_SITE:
                graphSignature[vrtCnt] = v.getPlatform() + String.valueOf(candidateSite);
                break;
            case CHANGE_PLATFORM:
                graphSignature[vrtCnt] = String.valueOf(candidatePlatform) + v.getSite();
                break;
            default:
                throw new IllegalStateException("Default case");
        }
//        String sgn = String.join("|", graphSignature);
        //if (visitedSigns_X.contains(sgn)) return;

        // apply an action
        g = new Graph(g);
        PerformAction act = new PerformAction(v, g, candidateSite, candidatePlatform, candidateAction);
        cnt_expl_X++;

        // compute cost
        g.updateCost(costEstimation);
        int c = g.getCost();
        if (c < minCost_X) {
            minCost_X = c;
            minPlan_X = g;
        }

        // keep stats for actions applied
        actionsApplied_X.compute(e, (k, val) -> (val == null) ? 1 : val + 1);   //TODO performance concerns

        // add the new flow in the queue (if it isn't there)
        if (!planSpace_X.contains(g)) {
            planSpace_X.add(g);
            cnt_vis_X++;
        }

        // show progress
        if ((cnt_vis_X % MESSAGE_SIZE) == 0) {
            System.out.println("vis: " + cnt_vis_X + " , expl: " + cnt_expl_X + " , mcost: " + minCost_X + " , queue-size: " + planSpace_X.size());
        }

        // add the flow in the visited list
        visitedSigns_X.add(g.getSignature());
    }


}
