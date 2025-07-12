package optimizer.cost;

import core.parser.dictionary.INFOREDictionary;
import core.structs.Tuple;
import optimizer.algorithm.AStarSearchAlgorithm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class DAGStarCostEstimator implements CostEstimator {

    private final INFOREDictionary dictionary;
    private final Map<String, String> classKeyMapping;

    public DAGStarCostEstimator(INFOREDictionary dictionary, Map<String, String> classKeyMapping) {
        this.dictionary = dictionary;
        this.classKeyMapping = classKeyMapping;
    }

    /**
     * Not implemented!
     */
    @Override
    public int getPlanTotalCost(LinkedHashMap<String, Tuple<String, String>> implementationMap) {
        return 0;
    }

    /**
     * Νot implemented!
     */
    @Override
    public int getHeuristicCostForOperator(String operator) {
        return 0;
    }

    /**
     * Not implemented!
     */
    @Override
    public int getMigrationCost(String prevOp, Tuple<String, String> prevImpl, Tuple<String, String> newImpl) {
        return 0;
    }

    @Override
    public int getOperatorAndImplementationCost(String newOp, Tuple<String, String> newImpl) {
        String classKey = classKeyMapping.get(newOp);
        if (classKey == null) {
            throw new IllegalStateException("Class key not found for operator: " + newOp);
        }

        // Get the site static cost for the class key
        String site = newImpl._1;
        int siteCost = dictionary.getSiteStaticCostForClassKey(classKey, site);

        // Get the platform static cost for the class key
        String platform = newImpl._2;
        int platformCost = dictionary.getPlatformStaticCostForClassKey(classKey, platform);

        return siteCost + platformCost;
    }

    /**
     * Not implemented!
     */
    @Override
    public int getPlanPlatformCost(LinkedHashMap<String, Tuple<String, String>> implementationMap) {
        return 0;
    }


    @Override
    public int getCommunicationCost(String site1, String site2) {
        Random random = new Random(7);
        return random.nextInt(500);
    }

    @Override
    public int getMinCostForOperator(String operator) {
        if (operator.equals(AStarSearchAlgorithm.VIRTUAL_END) || operator.equals(AStarSearchAlgorithm.VIRTUAL_START))
            return 0;

        String classKey = classKeyMapping.get(operator);
        if (classKey == null)
            throw new IllegalStateException("Class key not found for operator: " + operator);

        // Get the minimum site cost for the operator
        return dictionary.getMinSiteCostForOperator(classKey);
    }
}
