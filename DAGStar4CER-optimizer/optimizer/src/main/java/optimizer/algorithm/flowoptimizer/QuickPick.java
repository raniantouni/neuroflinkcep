package optimizer.algorithm.flowoptimizer;

import core.parser.network.AvailablePlatform;
import core.parser.network.Site;
import core.structs.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Algorithm QuickPick: Computes a random sample of the possible plans and returns the best solution amongst them.
 */
public class QuickPick {

    private static final int MESSAGE_SIZE = FlowOptimizer.MESSAGE_SIZE;

    public static Graph createPlanSpaceWithRandomPlans(Graph flow, Map<Integer, AvailablePlatform> platformMapping,Map<Integer, Site> siteMapping, CostEstimatorIface costEstimation,int inpSampleSize, ExecutorService executor, int timeout) {
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

        // set bestPlan <- initial flow

        flow.updateCost(costEstimation);

        // get all actions (skip P0, S0)
        for (int i = 0; i < platforms; i++) {
            for (int j = 0; j < sites; j++) {
                actions.add(new Tuple<>((i + 1), (j + 1)));
            }
        }

        // if the search space is smaller than the sample size, enumerate them all
        final int sampleSize = Math.min(inpSampleSize, possiblePlans);

        // print header
        System.out.println("\n--------\n[" + Thread.currentThread().getStackTrace()[1].getMethodName() + "]");
        System.out.println("#operators: " + operators + " , #platforms: " + platforms + " , #sites: " + sites);
        System.out.println("possible #plans: (" + platforms + " * " + sites + ") ^ " + operators + " = " + possiblePlans);
        System.out.println("possible actions: " + actions);
        System.out.println("sample size: " + sampleSize + "\n");

        // start-time
        Instant t1 = Instant.now();
        AtomicReference<Long> tsample = new AtomicReference<>();
        AtomicReference<Integer> minCostRef = new AtomicReference<>();
        AtomicReference<Graph> minPlanRef = new AtomicReference<>();

        Future<?> task = executor.submit(() -> {
            int minCost = flow.getCost();
            Graph minPlan;

            // create a sample of possiblePlans (limit possiblePlans to max-int to make it tractable)
            Instant t2 = Instant.now();
            List<Integer> sample = new ArrayList<>(sampleSize);
            for (int k = 0; k < sampleSize; k++) {
                int x = (int) (Math.random() * possiblePlans) + 1;
                if (!sample.contains(x)) sample.add(x);
                else k--;
            }
            tsample.set(Duration.between(t2, Instant.now()).toMillis());

            // keep stats for action frequency
            HashMap<Integer, Integer> dig = new HashMap<>();
            for (int k = 0; k < actions.size(); k++) dig.put(k, 0);

            int vis = 0;
            for (int i : sample) {
                // identify action
                String no = Integer.toString(Integer.parseInt("" + i, srcBase), trgBase);
                no = String.format("%1$" + operators + "s", no).replace(' ', '0');
                char[] n = no.toCharArray();
                vis++;

                // keep stats for action frequency
                for (char c : n) dig.compute(Integer.parseInt(String.valueOf(c)), (ky, vl) -> vl + 1);

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
                    g.updateCost(costEstimation);
                    g.updateVertex(v);
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
                //System.out.println(i + " -> " + no + "  : " + g.getSignF() + "\n");
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
        System.out.println("time: " + t + " (" + tsample.get() + ")");
        System.out.println("min cost: " + minCostRef.get());
        System.out.println("min plan signature:" + minPlanRef.get().getSignature());
        System.out.println("#actions applied: " + actionsApplied);
        System.out.println("min plan:\n" + minPlanRef.get());
        return minPlanRef.get();
    }

}
