package optimizer.prune;


import optimizer.plan.SimpleOptimizationPlan;

import java.util.Collection;

public class DummyPlanPruner implements PlanPruner {

    @Override
    public boolean prune(SimpleOptimizationPlan plan) {
        return false;
    }

    @Override
    public void pruneList(Collection<SimpleOptimizationPlan> collection) {
    }

    @Override
    public int getPrunedPlans() {
        return 0;
    }
}
