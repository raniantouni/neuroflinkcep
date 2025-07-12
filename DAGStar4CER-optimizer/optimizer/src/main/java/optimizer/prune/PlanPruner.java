package optimizer.prune;

import optimizer.plan.SimpleOptimizationPlan;

import java.util.Collection;

//All methods must be ThreadSafe
public interface PlanPruner {

    boolean prune(SimpleOptimizationPlan plan);

    void pruneList(Collection<SimpleOptimizationPlan> plan);

    int getPrunedPlans();
}
