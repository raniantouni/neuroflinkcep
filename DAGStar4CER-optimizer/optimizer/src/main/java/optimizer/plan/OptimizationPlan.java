package optimizer.plan;

import core.structs.Tuple;

import java.util.LinkedHashMap;

public interface OptimizationPlan extends Comparable<OptimizationPlan> {
    LinkedHashMap<String, Tuple<String, String>> getOperatorsAndImplementations();

    //New cost model
    int totalCost();

    //Old cost model
    int realCost();
}
