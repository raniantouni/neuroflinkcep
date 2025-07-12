package optimizer.plan;

import core.structs.Tuple;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

public class HeuristicPlan implements OptimizationPlan {
    private final LinkedHashMap<String, Tuple<String, String>> plan;
    private final int totalCost;
    private final int migrationCostSoFar;
    private final int realCost;

    public HeuristicPlan(LinkedHashMap<String, Tuple<String, String>> plan, int totalCost, int migrationCostSoFar, int realCost) {
        this.plan = plan;
        this.totalCost = totalCost;
        this.migrationCostSoFar = migrationCostSoFar;
        this.realCost = realCost;
    }

    @Override
    public LinkedHashMap<String, Tuple<String, String>> getOperatorsAndImplementations() {
        return this.plan;
    }

    @Override
    public int totalCost() {
        return this.totalCost;
    }

    @Override
    public int realCost() {
        return this.realCost;
    }

    public int migrationCostSoFar() {
        return this.migrationCostSoFar;
    }

    @Override
    public int compareTo(OptimizationPlan o) {
        return this.totalCost - o.totalCost();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicPlan that = (HeuristicPlan) o;
        return plan.equals(that.plan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plan);
    }

    @Override
    public String toString() {
        return "HeuristicPlan{" +
                "plan=" + plan +
                ", totalCost=" + totalCost +
                ", migrationCostSoFar=" + migrationCostSoFar +
                ", realCost=" + realCost +
                '}';
    }

    public Set<String> getOperators() {
        return this.plan.keySet();
    }
}
