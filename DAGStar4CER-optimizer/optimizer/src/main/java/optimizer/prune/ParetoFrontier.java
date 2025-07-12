package optimizer.prune;

import java.util.Collection;
import java.util.List;

public interface ParetoFrontier<T> {

    void offerPlan(T plan);

    void offerBatchPlans(Collection<T> plans);

    List<T> getFrontier();

    void clear();
}
