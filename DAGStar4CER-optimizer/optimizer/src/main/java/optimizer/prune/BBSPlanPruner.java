package optimizer.prune;

import core.skyline.bbs.BBS;
import core.structs.Tuple;
import core.structs.rtree.Entry;
import core.structs.rtree.RTree;
import core.structs.rtree.geometry.Point;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class BBSPlanPruner implements PlanPruner {
    private final CostEstimator costEstimator;
    private final OptimizationPlan rootPlan;
    private volatile int prunedPlansCount;
    private RTree<Object, Point> rTree;

    public BBSPlanPruner(CostEstimator costEstimator, OptimizationPlan rootPlan) {
        this.costEstimator = costEstimator;
        this.rTree = RTree.star().create();
        this.rootPlan = rootPlan;
    }

    public Point makePoint(SimpleOptimizationPlan plan) {
        return new Point(
                getMigrationCostSoFar(costEstimator, rootPlan.getOperatorsAndImplementations(), plan.getImplementationMap()),
                this.costEstimator.getPlanPlatformCost(plan.getImplementationMap())
        );
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
    public synchronized boolean prune(SimpleOptimizationPlan plan) {
        this.rTree = this.rTree.add(plan, makePoint(plan));
        final BBS bbs = new BBS(this.rTree);
        List<Entry<Object, Point>> skylineEntries = bbs.execute();
        for (Entry<Object, Point> skylineEntry : skylineEntries) {
            if (skylineEntry.value().equals(plan)) {
                return false;
            }
        }
        prunedPlansCount++;
        return true;
    }

    @Override
    public synchronized void pruneList(Collection<SimpleOptimizationPlan> collection) {
        List<SimpleOptimizationPlan> toRemove = new ArrayList<>();
        for (SimpleOptimizationPlan planVertex : collection) {
            if (prune(planVertex)) {
                toRemove.add(planVertex);
            }
        }
        collection.removeAll(toRemove);
    }

    @Override
    public synchronized int getPrunedPlans() {
        return this.prunedPlansCount;
    }
}
