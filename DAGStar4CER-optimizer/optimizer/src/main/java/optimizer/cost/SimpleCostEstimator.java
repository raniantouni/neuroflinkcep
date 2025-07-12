package optimizer.cost;

import core.parser.dictionary.INFOREDictionary;
import core.structs.Tuple;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SimpleCostEstimator implements CostEstimator {
    private final Map<String, Set<String>> operatorParents;
    private final INFOREDictionary dictionary;
    private final Map<String, String> classKeyMapping;

    public SimpleCostEstimator(Map<String, Set<String>> operatorParents, INFOREDictionary newInforeDictionary, Map<String, String> classKeyMapping) {
        this.operatorParents = operatorParents;
        this.dictionary = newInforeDictionary;
        this.classKeyMapping = classKeyMapping;
    }

    public int getPlanTotalCost(LinkedHashMap<String, Tuple<String, String>> operatorImplementations) {
        int cost = 0;

        for (String op : operatorImplementations.keySet()) {
            String classKey = classKeyMapping.get(op);
            if (classKey == null) {
                //Handle unknown operators
                continue;
            }
            String curSite = operatorImplementations.get(op)._1;
            String curPlatform = operatorImplementations.get(op)._2;

            int siteMigrationCost = 0;
            int platformMigrationCost = 0;

            for (String parent : this.operatorParents.getOrDefault(op, Collections.emptySet())) {
                String parentSite = operatorImplementations.get(parent)._1;
                String parentPlatform = operatorImplementations.get(parent)._2;
                siteMigrationCost = Math.max(siteMigrationCost, this.dictionary.getSiteMigrationCostForClassKey(classKey, parentSite, curSite));
                platformMigrationCost = Math.max(platformMigrationCost, this.dictionary.getPlatformMigrationCostForClassKey(classKey, parentPlatform, curPlatform));
            }

            cost += this.dictionary.getOperatorCost(classKey);
            cost += this.dictionary.getSiteStaticCostForClassKey(classKey, curSite);
            cost += this.dictionary.getPlatformStaticCostForClassKey(classKey, curPlatform);
            cost += siteMigrationCost;
            cost += platformMigrationCost;
        }
        return cost;
    }

    //Platform related costs (static and migration)
    public int getPlanPlatformCost(LinkedHashMap<String, Tuple<String, String>> operatorImplementations) {
        int cost = 0;
        for (String op : operatorImplementations.keySet()) {
            String classKey = classKeyMapping.get(op);
            if (classKey == null) {
                continue;
            }
            String curPlatform = operatorImplementations.get(op)._2;
            int platformMigrationCost = 0;
            for (String parent : this.operatorParents.getOrDefault(op, Collections.emptySet())) {
                String parentPlatform = operatorImplementations.get(parent)._2;
                platformMigrationCost = Math.max(platformMigrationCost, this.dictionary.getPlatformMigrationCostForClassKey(classKey, parentPlatform, curPlatform));
            }
            cost += this.dictionary.getPlatformStaticCostForClassKey(classKey, curPlatform);
            cost += platformMigrationCost;
        }
        return cost;
    }

    //Static cost of an operator and its implementation
    public int getOperatorAndImplementationCost(String operator, Tuple<String, String> implementation) {
        int cost = 0;
        String classKey = this.classKeyMapping.get(operator);
        if (classKey == null) {
            return 0;
        }
        cost += this.dictionary.getOperatorCost(classKey);
        cost += this.dictionary.getSiteStaticCostForClassKey(classKey, implementation._1);
        cost += this.dictionary.getPlatformStaticCostForClassKey(classKey, implementation._2);
        return cost;
    }

    //Site and platform migration between two implementations
    public int getMigrationCost(String op1, Tuple<String, String> impl1, Tuple<String, String> impl2) {
        int cost = 0;
        String classKey1 = this.classKeyMapping.get(op1);
        if (classKey1 == null) {
            return 0;
        }
        cost += this.dictionary.getSiteMigrationCostForClassKey(classKey1, impl1._1, impl2._1);
        cost += this.dictionary.getPlatformMigrationCostForClassKey(classKey1, impl1._2, impl2._2);
        return cost;
    }

    public int getHeuristicCostForOperator(String operator) {
        String key = classKeyMapping.get(operator);
        if (key == null) {
            return 0;
        }
        return this.dictionary.getOperatorCost(key)
                + this.dictionary.getMinSiteCostForOperator(key)
                + this.dictionary.getMinPlatformCostForOperator(key)
                + this.dictionary.getMinPlatformCostMigrationForOperator(key)
                + this.dictionary.getMinSiteCostMigrationForOperator(key);
    }

    public int getCommunicationCost(String site1, String site2) {
        return 0;
    }

    @Override
    public int getMinCostForOperator(String operator) {
        return 0;
    }
}

