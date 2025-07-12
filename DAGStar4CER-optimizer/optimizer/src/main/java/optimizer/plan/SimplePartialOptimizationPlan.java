package optimizer.plan;

import core.structs.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SimplePartialOptimizationPlan implements OptimizationPlan {
    //Operator-Implementations
    private final LinkedHashMap<String, Tuple<String, String>> operatorImplementations;

    //G cost of ending nodes
    private final Map<String, Integer> endingNodeCost;

    //Ending nodes of this plan
    private final Set<String> endingNodes;

    //Cost of this solution
    private final int gCost;
    private final int hCost;
    public SimplePartialOptimizationPlan(LinkedHashMap<String, Tuple<String, String>> operatorImplementations,
                                         Map<String, Integer> ENCosts,
                                         Set<String> endingNodes,
                                         int gCost,
                                         int hCost) {
        this.operatorImplementations = operatorImplementations;
        this.endingNodes = endingNodes;
        this.endingNodeCost = ENCosts;
        this.gCost = gCost;
        this.hCost = hCost;
    }

    public Set<String> getEndingNodes() {
        return endingNodes;
    }

    public int getOperatorCount() {
        return this.operatorImplementations.size();
    }

    @Override
    public LinkedHashMap<String, Tuple<String, String>> getOperatorsAndImplementations() {
        return operatorImplementations;
    }

    public Map<String, Integer> getEndingNodeCost() {
        return endingNodeCost;
    }

    public boolean containsOperator(String operator) {
        return this.operatorImplementations.containsKey(operator);
    }

    public boolean containsAllOperators(Set<String> operators) {
        return this.operatorImplementations.keySet().containsAll(operators);
    }

    @Override
    public int realCost() {
        return this.gCost;
    }

    public int heuristicCost() {
        return this.hCost;
    }

    @Override
    public int totalCost() {
        return this.gCost + this.hCost;
    }

    public void removeOperator(String operator) {
        this.operatorImplementations.remove(operator);
        this.endingNodeCost.remove(operator);
    }

    public String getStringOfOperators() {
        StringBuilder sb = new StringBuilder();
        for (String op : this.operatorImplementations.keySet()) {
            sb.append(op).append("->");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(OptimizationPlan o) {
        return this.totalCost() - o.totalCost();
    }

    @Override
    public String toString() {
        return "SimplePartialOptimizationPlan{" +
                "operatorImplementations=" + operatorImplementations +
                ", endingNodeCost=" + endingNodeCost +
                ", endingNodes=" + endingNodes +
                ", gCost=" + gCost +
                ", hCost=" + hCost +
                '}';
    }
}
