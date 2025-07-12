package optimizer.cost;

import core.structs.Tuple;

import java.util.LinkedHashMap;

public interface CostEstimator {
    int getPlanTotalCost(LinkedHashMap<String, Tuple<String, String>> implementationMap);

    int getHeuristicCostForOperator(String operator);

    int getMigrationCost(String prevOp, Tuple<String, String> prevImpl, Tuple<String, String> newImpl);

    int getOperatorAndImplementationCost(String newOp, Tuple<String, String> newImpl);

    int getPlanPlatformCost(LinkedHashMap<String, Tuple<String, String>> implementationMap);

    int getCommunicationCost(String site1, String site2);

    int getMinCostForOperator(String operator);
}
