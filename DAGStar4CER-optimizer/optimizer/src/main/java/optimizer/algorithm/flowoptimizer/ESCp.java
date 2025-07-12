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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Algorithm ESCp: Creates the plan space by enumerating all possible
 * states using counting and number base switch.
 * <p>[Multi-threaded version]
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
public class ESCp {

    private static final int MESSAGE_SIZE = FlowOptimizer.MESSAGE_SIZE;

    // Global variables used in createPlanSpaceWithCounting2DimMultThrd
    //
    static AtomicReference<String> minPlan_mt = new AtomicReference<>();
    static AtomicInteger minCost_mt = new AtomicInteger(Integer.MAX_VALUE);
    static AtomicInteger vis_mt = new AtomicInteger();
    static ConcurrentHashMap<Integer, Integer> actionsApplied_mt = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Integer> threadsCnt = new ConcurrentHashMap<>();


    /**
     * Creates the plan space by enumerating all possible states
     * using counting and number base switch.
     *
     * <p>[Multi-threaded version]
     */
    public static Graph createPlanSpaceWithCounting2DimMultThrd(Graph rootFlow, Map<Integer, AvailablePlatform> platformMapping, Map<Integer, Site> siteMapping,
                                                                CostEstimatorIface costEstimation, int numThreads, ExecutorService executor, int timeout) {
        //Get the sizes
        int platforms = platformMapping.size() - 1;
        int sites = siteMapping.size() - 1;

        // initializations
        int operators = rootFlow.getVertices().size();
        int possiblePlans = (int) Math.pow((platforms * sites), operators);
        int srcBase = 10;
        int trgBase = platforms * sites;
        actionsApplied_mt = new ConcurrentHashMap<>();
        threadsCnt = new ConcurrentHashMap<>();

        // set bestPlan <- initial flow
        minPlan_mt.set(rootFlow.getSignature());
        rootFlow.updateCost(costEstimation);
        int gcst = rootFlow.getCost();
        minCost_mt.set(gcst);

        // get all actions (skip P0, S0)
        List<Tuple<Integer, Integer>> actions = new ArrayList<>();
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
        AtomicReference<Instant> t2 = new AtomicReference<>();
        AtomicReference<Boolean> timedOutRef = new AtomicReference<>(true);

        Future<?> task = executor.submit(() -> {
            // specify a thread pool
            ThreadPoolExecutor tpool = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

            // run the workers
            vis_mt.set(0);
            boolean timed_out = false;
            int fcnt = 0;
            for (int i = 0; i < possiblePlans; i++) {
                GenPlanWorker wrk = new GenPlanWorker(rootFlow, actions, srcBase, trgBase, operators, costEstimation, i);
                tpool.submit(wrk);
                if (fcnt++ % 100 == 0) {
                    if (Duration.between(t1, Instant.now()).toMillis() > timeout) {
                        timed_out = true;
                        timedOutRef.set(true);
                        break;
                    }
                }
            }

            if (timed_out) {
                tpool.shutdownNow();
            } else {
                long diff1 = Duration.between(t1, Instant.now()).toMillis();
                // shutdown
                tpool.shutdown();
                try {
                    timed_out = !tpool.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
            t2.set(Instant.now());
            timedOutRef.set(timed_out);

            // create the solution graph
            int pos = 0;
            String[] act = minPlan_mt.get().split("\\|");
            for (Vertex v : rootFlow.getVertices()) {
                char[] a = act[pos++].toCharArray();
                int p = Integer.parseInt(String.valueOf(a[0]));
                int s = Integer.parseInt(String.valueOf(a[1]));
                v.setPlatform(p);
                v.setSite(s);
                rootFlow.updateVertex(v);
            }
            // recompute the graph cost
            rootFlow.updateCost(costEstimation);
            timedOutRef.set(false);
        });

        //Wait for task to complete or a timeout has reached
        try {
            task.get(timeout + 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Executor service awaitTermination was interrupted.");
        } catch (TimeoutException e) {
            System.out.println("Algorithm timed out.");
        } catch (ExecutionException e) {
            System.out.printf("Executor encountered the following error: [%s]%n", e);
        }
        if (timedOutRef.get()) {
            System.out.println("Algorithm timed out.");
        }

        // end-time
        long t = Duration.between(t1, Instant.now()).toMillis();
        long tgg = Duration.between(t2.get(), Instant.now()).toMillis();

        // print stats
        System.out.println("time: " + t + " (" + tgg + ") , vis:" + vis_mt + " , #threads:" + numThreads);
        System.out.println("min cost: " + minCost_mt);
        System.out.println("min plan signature: " + minPlan_mt);
        System.out.println("#actions applied: " + actionsApplied_mt);
        System.out.println("thread load: " + threadsCnt);
        System.out.println("min plan: \n" + rootFlow);
        return rootFlow;
    }


    /**
     * Implements a flow for a given plan description.
     * <p>Worker for {@code createPlanSpaceWithCounting2DimMultThrd()}
     */
    static class GenPlanWorker implements Runnable {

        private final Graph flow;
        private final List<Tuple<Integer, Integer>> actions;
        private final int srcBase;
        private final int trgBase;
        private final int operators;
        private final CostEstimatorIface costEstimation;
        private final int position;

        public GenPlanWorker(Graph flow, List<Tuple<Integer, Integer>> actions, int srcBase, int trgBase, int operators, CostEstimatorIface costEstimation, int position) {
            this.flow = flow;
            this.actions = actions;
            this.srcBase = srcBase;
            this.trgBase = trgBase;
            this.operators = operators;
            this.costEstimation = costEstimation;
            this.position = position;
        }

        public String getName() {
            return Thread.currentThread().getName();
        }

        @Override
        public void run() {

            // keep stats for threads running
            threadsCnt.compute(getName(), (k, val) -> (val == null) ? 0 : val + 1);

            // identify action
            String no = Integer.toString(Integer.parseInt("" + position, srcBase), trgBase);
            no = String.format("%1$" + operators + "s", no).replace(' ', '0');
            char[] n = no.toCharArray();

            // apply action
            int c = 0;
            Graph g = new Graph(flow);
            for (Vertex v : g.getVertices()) {

                // get action
                int a = Integer.parseInt(n[c++] + "");
                Tuple<Integer, Integer> act = actions.get(a);
                int p = act._1;
                int s = act._2;

                // update vertex
                if (v.getPlatform() != (p)) {
                    v.setPlatform(p);
                }

                if (v.getSite() != (s)) {
                    v.setSite(s);
                }

                // update graph
                g.updateVertex(v);
                g.updateCost(costEstimation);
                int gcst = g.getCost();

                // keep stats for actions applied
                actionsApplied_mt.compute(p, (k, val) -> (val == null) ? 1 : val + 1);
                actionsApplied_mt.compute(s, (k, val) -> (val == null) ? 1 : val + 1);

                // update min plan
                int mcst;
                do {
                    mcst = minCost_mt.get();
                    if (mcst <= gcst) {
                        break;
                    } else {
                        minPlan_mt.set(g.getSignature());
                    }
                } while (!minCost_mt.compareAndSet(mcst, gcst));
            }

            // show progress
            int vis = vis_mt.incrementAndGet(); //.getAndIncrement();
            if ((vis % MESSAGE_SIZE) == 0) {
                System.out.println("vis: " + vis);
            }
        }

    }

}
