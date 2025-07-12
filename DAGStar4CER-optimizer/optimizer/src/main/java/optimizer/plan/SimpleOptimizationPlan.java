package optimizer.plan;

import core.structs.Tuple;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Plan for op-X algorithms.
 */
public class SimpleOptimizationPlan implements OptimizationPlan {
    //Preserve topological order by using a linked map
    //Operator name -> <Site name,Platform name>
    private final LinkedHashMap<String, Tuple<String, String>> implementationMap;   //TODO make it an enum

    // Cost estimation of this plan, includes the migration cost, total plan cost and root cost components.
    private final int totalCost;

    // Plan migration cost, computed incrementally
    private final int realCost;

    public SimpleOptimizationPlan(LinkedHashMap<String, Tuple<String, String>> implMap, int totalCost, int realCost) {
        this.implementationMap = implMap;
        this.totalCost = totalCost;
        this.realCost = realCost;
    }

    public Set<String> getOperators() {
        return this.implementationMap.keySet();
    }

    @Override
    public LinkedHashMap<String, Tuple<String, String>> getOperatorsAndImplementations() {
        return this.implementationMap;
    }

    @Override
    public int totalCost() {
        return totalCost;
    }

    @Override
    public int realCost() {
        return realCost;
    }

    @Override
    public String toString() {
        return "SimpleOptimizationPlan{" +
                "implementationMap=" + implementationMap +
                ", totalCost=" + totalCost +
                ", realCost=" + realCost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        SimpleOptimizationPlan that = (SimpleOptimizationPlan) o;

        return implementationMap.equals(that.implementationMap);
    }

    public LinkedHashMap<String, Tuple<String, String>> getImplementationMap() {
        return implementationMap;
    }

    @Override
    public int hashCode() {
        return implementationMap.hashCode();
    }

    @Override
    public int compareTo(OptimizationPlan o) {
        return this.totalCost - o.totalCost();
    }
}
