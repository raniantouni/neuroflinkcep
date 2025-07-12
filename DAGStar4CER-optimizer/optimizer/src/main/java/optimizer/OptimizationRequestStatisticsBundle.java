package optimizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizationRequestStatisticsBundle {
    private final Map<String, AtomicInteger> stats;

    private final String CREATED_PLANS = "Created Plans";
    private final String EXPLORED_PLANS = "Explored Plans";
    private final String EXPLORED_DIMS = "Explored Dimensions";
    private final String SET_COLLISIONS = "Graph collisions";
    private final String GRAPH_COLLISIONS = "Set collisions";
    private final String PRUNED_PLANS = "Pruned plans";
    private long cost;
    private int totalThreads;
    private long setupDuration;
    private long execDuration;
    private String algorithmName;
    private String workflow;
    private String dictionary;
    private String network;

    public OptimizationRequestStatisticsBundle() {
        this.stats = new HashMap<>();
        this.stats.put(CREATED_PLANS, new AtomicInteger(0));
        this.stats.put(EXPLORED_PLANS, new AtomicInteger(0));
        this.stats.put(EXPLORED_DIMS, new AtomicInteger(0));
        this.stats.put(SET_COLLISIONS, new AtomicInteger(0));
        this.stats.put(GRAPH_COLLISIONS, new AtomicInteger(0));
        this.stats.put(PRUNED_PLANS, new AtomicInteger(0));
        this.algorithmName = "";
        this.workflow = "";
        this.dictionary = "";
        this.network = "";
        this.totalThreads = 1;
        this.setupDuration = -1L;
        this.execDuration = -1L;
        this.cost = -1L;
    }

    public static String[] getCSVHeaders() {
        return new String[]{
                "Created Plans", "Explored Plans", "Explored Cost Dims", "Set collisions", "Graph collisions", "Pruned plans",
                "Algorithm", "Dictionary", "Network", "File", "Threads", "Setup Duration (ms)", "Execution Duration (ms)", "Cost"
        };
    }

    public int addStat(String k, int v) {
        if (!this.stats.containsKey(k)) {
            this.stats.put(k, new AtomicInteger(0));
        }
        return this.stats.get(k).addAndGet(v);
    }

    public int addCreatedPlans(int val) {
        return this.stats.get(CREATED_PLANS).addAndGet(val);
    }

    public int addExploredPlans(int val) {
        return this.stats.get(EXPLORED_PLANS).addAndGet(val);
    }

    public int addExploredDimensions(int val) {
        return this.stats.get(EXPLORED_DIMS).addAndGet(val);
    }

    public int addSetCollisions(int val) {
        return this.stats.get(SET_COLLISIONS).addAndGet(val);
    }

    public int addGraphCollisions(int val) {
        return this.stats.get(GRAPH_COLLISIONS).addAndGet(val);
    }

    public int addPrunedPlans(int val) {
        return this.stats.get(PRUNED_PLANS).addAndGet(val);
    }

    @Override
    public String toString() {
        return "OptimizationRequestStatisticsBundle{" +
                "stats=" + stats +
                ", totalThreads=" + totalThreads +
                ", setupDuration=" + setupDuration +
                ", execDuration=" + execDuration +
                ", cost=" + cost +
                ", algorithmName='" + algorithmName + '\'' +
                ", workflow='" + workflow + '\'' +
                ", dictionary='" + dictionary + '\'' +
                ", network='" + network + '\'' +
                '}';
    }

    public String[] toCSVEntry() {
        return new String[]{
                String.valueOf(this.stats.get(CREATED_PLANS)),
                String.valueOf(this.stats.get(EXPLORED_PLANS)),
                String.valueOf(this.stats.get(EXPLORED_DIMS)),
                String.valueOf(this.stats.get(SET_COLLISIONS)),
                String.valueOf(this.stats.get(GRAPH_COLLISIONS)),
                String.valueOf(this.stats.get(PRUNED_PLANS)),
                algorithmName,
                dictionary,
                network,
                workflow,
                String.valueOf(totalThreads),
                String.valueOf(setupDuration),
                String.valueOf(execDuration),
                String.valueOf(cost)
        };
    }

    public Map<String, AtomicInteger> getStats() {
        return stats;
    }

    public int getTotalThreads() {
        return totalThreads;
    }

    public void setTotalThreads(int totalThreads) {
        this.totalThreads = totalThreads;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public long getSetupDuration() {
        return setupDuration;
    }

    public void setSetupDuration(long setupDuration) {
        this.setupDuration = setupDuration;
    }

    public long getExecDuration() {
        return execDuration;
    }

    public void setExecDuration(long execDuration) {
        this.execDuration = execDuration;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public void setAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getDictionary() {
        return dictionary;
    }

    public void setDictionary(String dictionary) {
        this.dictionary = dictionary;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }
}
