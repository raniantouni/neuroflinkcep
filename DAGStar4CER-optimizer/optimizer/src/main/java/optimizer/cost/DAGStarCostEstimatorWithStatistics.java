package optimizer.cost;

import core.structs.Tuple;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class DAGStarCostEstimatorWithStatistics implements CostEstimator {

    private final HashMap<String, HashMap<String, Double>> operatorCosts;
    private final HashMap<String, Double> communicationCosts;
    public static final int COST_MULTIPLIER = 10_000_000;

    public DAGStarCostEstimatorWithStatistics(HashMap<String, HashMap<String, Double>> operatorCosts,
                                HashMap<String, Double> communicationCosts) {
        this.operatorCosts = operatorCosts;
        this.communicationCosts = communicationCosts;
    }

    @Override
    public int getPlanTotalCost(LinkedHashMap<String, Tuple<String, String>> implementationMap) {
        return 0;
    }

    @Override
    public int getHeuristicCostForOperator(String operator) {
        return 0;
    }

    @Override
    public int getMigrationCost(String prevOp, Tuple<String, String> prevImpl, Tuple<String, String> newImpl) {
        return 0;
    }

    @Override
    public int getOperatorAndImplementationCost(String newOp, Tuple<String, String> newImpl) {
        String site = newImpl._1;
        return (int) (operatorCosts.get(newOp).get(site) * COST_MULTIPLIER);
    }

    @Override
    public int getPlanPlatformCost(LinkedHashMap<String, Tuple<String, String>> implementationMap) {
        return 0;
    }


    @Override
    public int getCommunicationCost(String site1, String site2) {
        String key = site1 + ":" + site2;
        return (int) (communicationCosts.getOrDefault(key, 0.0) * COST_MULTIPLIER);
    }

    @Override
    public int getMinCostForOperator(String operator) {
        if (!operatorCosts.containsKey(operator)) {
            return 0;
        }

        return (int) (operatorCosts.get(operator).values().stream()
                .min(Double::compareTo)
                .get() * COST_MULTIPLIER);
    }
}
