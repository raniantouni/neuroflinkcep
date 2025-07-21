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
    private final Random random = new Random(7); // Fixed seed for reproducibility

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

    private int getRandomOffset() {
        return this.random.nextInt(200); // Random offset for cost estimation
    }

    @Override
    public int getOperatorAndImplementationCost(String newOp, Tuple<String, String> newImpl) {

        String opName = newOp;
        if (newOp.startsWith("Neuro"))
            opName = newOp.split("_")[0]; // Handle neuroflinkcep operators by taking the first part of the name

        String classKey = classKeyMapping.get(opName);
        if (classKey == null) {
            throw new IllegalStateException("Class key not found for operator: " + opName);
        }

        // Get the site static cost for the class key
        String site = newImpl._1;
        int siteCost = dictionary.getSiteStaticCostForClassKey(classKey, site);

        // Get the platform static cost for the class key
        String platform = newImpl._2;
        int platformCost = dictionary.getPlatformStaticCostForClassKey(classKey, platform);

        return siteCost + platformCost + getRandomOffset();
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

        String opName = operator;
        if (operator.startsWith("Neuro"))
            opName = operator.split("_")[0]; // Handle neuroflinkcep operators by taking the first part of the name

        String classKey = classKeyMapping.get(opName);
        if (classKey == null)
            throw new IllegalStateException("Class key not found for operator: " + opName);

        // Get the minimum site cost for the operator
        return dictionary.getMinSiteCostForOperator(classKey);
    }
}
