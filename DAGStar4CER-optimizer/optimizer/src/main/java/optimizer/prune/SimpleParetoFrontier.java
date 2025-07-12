package optimizer.prune;

import core.skyline.bbs.BBS;
import core.structs.rtree.Entry;
import core.structs.rtree.RTree;
import core.structs.rtree.geometry.Point;
import optimizer.cost.CostEstimator;
import optimizer.plan.HeuristicPlan;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleParetoFrontier implements ParetoFrontier<HeuristicPlan> {
    private final CostEstimator costEstimator;
    private RTree<Object, Point> rTree;
    private boolean dirty;
    private List<HeuristicPlan> frontier;

    public SimpleParetoFrontier(CostEstimator costEstimator) {
        this.costEstimator = costEstimator;
        this.rTree = RTree.star().create();
        this.dirty = false;
        this.frontier = new ArrayList<>();
    }

    public Point makePoint(HeuristicPlan plan) {
        return new Point(
                this.costEstimator.getPlanPlatformCost(plan.getOperatorsAndImplementations()),
                plan.migrationCostSoFar()
        );
    }

    @Override
    public void offerPlan(HeuristicPlan plan) {
        Point point = makePoint(plan);
        this.rTree = this.rTree.add(plan, point);
        this.dirty = true;
    }

    @Override
    public void offerBatchPlans(Collection<HeuristicPlan> plans) {
        this.rTree = this.rTree.add(plans.stream()
                .map(plan -> new Entry<>((Object) plan, makePoint(plan)))
                .iterator());
        this.dirty = true;
    }

    /**
     * Lazy evaluation of the Pareto Frontier.
     *
     * @return A list of {@link HeuristicPlan} in the Pareto Frontier.
     */
    @Override
    public List<HeuristicPlan> getFrontier() {
        if (this.dirty) {
            this.frontier = new BBS(this.rTree).execute().stream()
                    .map(entry -> (HeuristicPlan) entry.value())
                    .collect(Collectors.toList());
            this.dirty = false;
        }
        return this.frontier;
    }

    @Override
    public void clear() {
        this.rTree = RTree.star().create();
        this.dirty = false;
        this.frontier = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleParetoFrontier that = (SimpleParetoFrontier) o;
        return rTree.equals(that.rTree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rTree);
    }

    @Override
    public String toString() {
        return "SimpleParetoFrontier{" +
                "frontier=" + this.getFrontier() +
                '}';
    }
}
