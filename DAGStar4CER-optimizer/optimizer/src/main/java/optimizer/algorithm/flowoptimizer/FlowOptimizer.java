package optimizer.algorithm.flowoptimizer;

import core.exception.OptimizerException;
import core.graph.ThreadSafeDAG;
import core.iterable.DAGIterable;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.AvailablePlatform;
import core.parser.network.INFORENetwork;
import core.parser.network.Site;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.BoundedPriorityQueue;
import core.structs.Tuple;
import core.utils.FileUtils;
import core.utils.GraphUtils;
import optimizer.OptimizationResourcesBundle;
import optimizer.algorithm.GraphTraversalAlgorithm;
import optimizer.cost.CostEstimator;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Driver for testing plan space generation algorithms.
 */
public class FlowOptimizer implements GraphTraversalAlgorithm {
    public static final int MESSAGE_SIZE = 500_000;
    private static final boolean deterministicStartPos = false;
    private final Map<Integer, Integer> staticOperatorCost;     //<operatorId,cost>
    private final Map<Integer, Map<Integer, Integer>> staticPlatformCost;   //<operatorId,<platform,cost>>
    private final Map<Integer, Map<Integer, Integer>> staticSiteCost;       //<operatorId,<site,cost>>
    private final Map<Integer, Map<Integer, Map<Integer, Integer>>> platformMigrationCost;  //<operatorId,<platform_from,<platform_to,cost>>>
    private final Map<Integer, Map<Integer, Map<Integer, Integer>>> siteMigrationCost;      //<operatorId,<platform_from,<platform_to,cost>>>
    private final String algoName;
    private final List<Integer> platformSeeds;
    private final List<Integer> siteSeeds;
    private OptimizationResourcesBundle bundle;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private Graph root;
    private final CostEstimatorIface costEstimation = this::getGraphTotalCost;
    private ExecutorService executor;
    private int timeout;
    private Map<Integer, String> idToOperatorMapping;
    private Map<Integer, Site> siteMapping;
    private Map<Integer, AvailablePlatform> platformMapping;


    public FlowOptimizer(String algoName, List<Integer> platformSeeds, List<Integer> siteSeeds) {
        this.platformSeeds = platformSeeds;
        this.siteSeeds = siteSeeds;
        this.staticPlatformCost = new HashMap<>();
        this.staticSiteCost = new HashMap<>();
        this.staticOperatorCost = new HashMap<>();
        this.platformMigrationCost = new HashMap<>();
        this.siteMigrationCost = new HashMap<>();
        this.algoName = algoName;
        this.idToOperatorMapping = new HashMap<>();
        this.siteMapping = new HashMap<>();
        this.platformMapping = new HashMap<>();
    }

    public FlowOptimizer(String algoName) {
        this.platformSeeds = null;
        this.siteSeeds = null;
        this.staticPlatformCost = new HashMap<>();
        this.staticSiteCost = new HashMap<>();
        this.staticOperatorCost = new HashMap<>();
        this.platformMigrationCost = new HashMap<>();
        this.siteMigrationCost = new HashMap<>();
        this.algoName = algoName;
        this.idToOperatorMapping = new HashMap<>();
        this.siteMapping = new HashMap<>();
        this.platformMapping = new HashMap<>();
    }

    //Implement the cost iface as a separate private function to ensure proper initialization
    private int getGraphTotalCost(Graph flow) {
        int real_cost = getGraphRealCost(flow);
        if (root == null || real_cost == Integer.MAX_VALUE) {
            return real_cost;
        }
        return real_cost - root.getCost() + getGraphMigrationCostSoFar(root, flow);
    }

    private int getGraphRealCost(Graph flow) {
        //Cost is temporarily cast to long in order to avoid int overflow due to INT_MAX values
        long real_cost = 0;

        Map<Integer, Integer> siteMigMap = new HashMap<>();
        Map<Integer, Integer> platMigMap = new HashMap<>();

        for (Vertex vtx : flow.getVertices()) {
            int id = vtx.getOperatorId();
            int curSite = vtx.getSite();
            int curPlatform = vtx.getPlatform();
            for (Vertex childVtx : vtx.getAdjVertices()) {
                int cid = childVtx.getOperatorId();
                int childSite = childVtx.getSite();
                int childPlatform = childVtx.getPlatform();
                int siteMigration = this.siteMigrationCost.get(id).get(curSite).get(childSite);
                siteMigMap.compute(cid, (k, v) -> v == null ? siteMigration : Math.max(v, siteMigration));
                int platformMigration = this.platformMigrationCost.get(id).get(curPlatform).get(childPlatform);
                platMigMap.compute(cid, (k, v) -> v == null ? platformMigration : Math.max(v, platformMigration));
            }
            real_cost += this.staticOperatorCost.get(id);
            real_cost += this.staticSiteCost.get(id).get(curSite);
            real_cost += this.staticPlatformCost.get(id).get(curPlatform);
        }

        real_cost += siteMigMap.values().stream().reduce(Integer::sum).orElse(0);
        real_cost += platMigMap.values().stream().reduce(Integer::sum).orElse(0);
        return (int) Math.min(Integer.MAX_VALUE, real_cost);
    }

    private int getGraphMigrationCostSoFar(Graph R, Graph G) {
        int migration_cost_so_far = 0;
        for (Vertex vtx : G.getVertices()) {
            int id = vtx.getOperatorId();
            Vertex rootVtx = R.getVertex(id);
            migration_cost_so_far += this.platformMigrationCost.get(id).get(rootVtx.getPlatform()).get(vtx.getPlatform());
            migration_cost_so_far += this.siteMigrationCost.get(id).get(rootVtx.getSite()).get(vtx.getSite());
        }
        return migration_cost_so_far;
    }

    /**
     * Invokes the various plan space generation algorithms.
     */
    public void createPlanSpace(Map<Integer, AvailablePlatform> platformMapping,
                                Map<Integer, Site> siteMapping,
                                Graph flow,
                                int threads) {
        //
        // show flow details
        //
        flow.updateCost(costEstimation);
        this.root = flow;
        System.out.println("\n-- Initial flow --");
        System.out.println("\n> Flow structure: \n" + flow);              // print flow structure
        System.out.println("> Flow cost: " + flow.getCost());             // print flow cost

        Graph bestPlan = null;
        switch (this.algoName) {
            case "e-gsp":
                GSProgressive greedyProgressiveAlgorithm = new GSProgressive(platformMapping, siteMapping, costEstimation, executor, timeout);
                bestPlan = greedyProgressiveAlgorithm.createPlanSpaceGSProgressive(new Graph(flow));
                // try with greedy keeping history, single-threaded - O(V^3)
                break;
            case "e-gsg":
                // try with greedy based on local optima, single-threaded - O(V^2)
                // TODO DEMO
                GSGreedyLO greedyLocalOptimaAlgorithm = new GSGreedyLO(platformMapping, siteMapping, costEstimation, executor, timeout);
                bestPlan = greedyLocalOptimaAlgorithm.createPlanSpaceGSGreedyLO(new Graph(flow), root);
                break;
            case "e-qp":
                // try with random sampling, single-threaded
                // TODO DEMO
                bestPlan = QuickPick.createPlanSpaceWithRandomPlans(new Graph(flow), platformMapping, siteMapping, costEstimation, 1000, executor, timeout);
                break;
            case "e-escp":
                // try with counting, multi-threaded
                // TODO DEMO
                bestPlan = ESCp.createPlanSpaceWithCounting2DimMultThrd(new Graph(flow), platformMapping, siteMapping, costEstimation, threads, executor, timeout);   // [ok version]
                break;
            case "e-esc":
                // try with counting, single-threaded
//                bestPlan = ESC.createPlanSpaceWithCounting2Dim(new Graph(flow), platformMapping, siteMapping, costEstimation, executor, timeout);   // [ok version]
                break;
            case "e-esq":
                // try with a queue to keep intermediate plans
                // TODO DEMO
                bestPlan = ESQ.createPlanSpaceWithQueue(new Graph(flow), platformMapping, siteMapping, true, costEstimation, executor, timeout);  // [ok version, but slow logic]
                break;
            case "e-esq2":
                // try with a queue to keep intermediate plans
                // TODO DEMO
                bestPlan = ESQ2.createPlanSpaceWithQueue(new Graph(flow), platformMapping, siteMapping, true, costEstimation, executor, timeout);  // [somewhat exhaustive version]
                break;
            default:
                throw new IllegalStateException("Default case in switch case.");
        }

        //Map Flow optimizer plans to the standard @{OptimizationPlan}
        if (bestPlan != null) {
            int total_cost = getGraphTotalCost(bestPlan);
            int real_cost = getGraphRealCost(bestPlan);
            LinkedHashMap<String, Tuple<String, String>> plan = new LinkedHashMap<>();
            for (Vertex v : bestPlan.getVertices()) {
                String site = this.siteMapping.get(v.getSite()).getSiteName();
                String platform = this.platformMapping.get(v.getPlatform()).getPlatformName();
                plan.put(this.idToOperatorMapping.get(v.getOperatorId()), new Tuple<>(site, platform));
            }
            validPlans.offer(new SimpleOptimizationPlan(plan, total_cost, real_cost));
        }
    }

    //Injected from the Optimizer interface
    @Override
    public void setup(OptimizationResourcesBundle bundle, BoundedPriorityQueue<OptimizationPlan> validPlans, OptimizationPlan rootPlan,
                      ExecutorService executorService, CostEstimator costEstimator, Logger logger) throws OptimizerException {
        this.bundle = bundle;
        this.validPlans = validPlans;
        this.executor = executorService;
        this.timeout = bundle.getTimeout();
    }

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        setup(bundle, bundle.getPlanQueue(), bundle.getRootPlan(), bundle.getExecutorService(), bundle.getCostEstimator(), bundle.getLogger());
    }

    @Override
    public void doWork() {
        Instant start = Instant.now();

        // Unpack
        final OptimizationRequest optimizationRequest = bundle.getWorkflow();
        final INFORENetwork inforeNetwork = bundle.getNetwork();
        final ThreadSafeDAG<Operator> operatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        final Set<AvailablePlatform> ogPlatforms = inforeNetwork.getPlatforms();
        final Set<Site> ogSites = inforeNetwork.getSites();
        final int threads = bundle.getThreads();

        //Fast check to make sure nothing went wrong during parsing
        if (ogPlatforms.isEmpty() || ogSites.isEmpty()) {
            throw new IllegalStateException("Need at least one platform and site");
        }

        // Mapping
        int idx = 1;
        for (AvailablePlatform platform : ogPlatforms.stream()
                .sorted(Comparator.comparing(AvailablePlatform::getPlatformName))
                .collect(Collectors.toCollection(LinkedHashSet::new))) {
            platformMapping.put(idx++, platform);
        }
        AvailablePlatform DEFAULT_PLATFORM = new AvailablePlatform("UNKNOWN_PLATFORM");
        platformMapping.put(0, DEFAULT_PLATFORM);

        idx = 1;
        for (Site site : ogSites.stream()
                .sorted(Comparator.comparing(Site::getSiteName))
                .collect(Collectors.toCollection(LinkedHashSet::new))) {
            siteMapping.put(idx++, site);
        }
        Site DEFAULT_SITE = new Site("UNKNOWN_SITE", Collections.emptyList());
        siteMapping.put(0, DEFAULT_SITE);

        final Map<String, Integer> operatorToIdMapping = new HashMap<>();//Temp
        idx = 1;
        for (core.graph.Vertex<Operator> vertex : new DAGIterable<>(operatorGraph)) {
            idToOperatorMapping.put(idx, vertex.getData().getName());
            operatorToIdMapping.put(vertex.getData().getName(), idx);
            idx++;
        }
        String DEFAULT_OPERATOR = "UNKNOWN_OPERATOR";
        idToOperatorMapping.put(0, DEFAULT_OPERATOR);
        operatorToIdMapping.put(DEFAULT_OPERATOR, 0);
        final Map<String, Set<String>> operatorParents = GraphUtils.getOperatorParentMap(operatorGraph);

        //Insert the operator IDs in a Graph
        final Map<Integer, List<Integer>> childMapping = new HashMap<>();
        for (String vertexName : idToOperatorMapping.values()) {
            int vid = operatorToIdMapping.get(vertexName);
            childMapping.put(vid, new ArrayList<>());
        }
        childMapping.put(0, Collections.emptyList());
        for (String vertexName : idToOperatorMapping.values()) {
            if (vertexName.equals(DEFAULT_OPERATOR)) {
                continue;
            }
            int vertexId = operatorToIdMapping.get(vertexName);
            for (String parentVertexName : operatorParents.get(vertexName)) {
                int parentVertexId = operatorToIdMapping.get(parentVertexName);
                childMapping.get(parentVertexId).add(vertexId);
            }
        }

        final Map<String, Integer> classKeyToVertexId = new HashMap<>();
        final Map<Integer, String> vertexIdToClassKeyId = new HashMap<>();
        for (core.graph.Vertex<Operator> vertex : operatorGraph.getVertices()) {
            int vid = operatorToIdMapping.get(vertex.getData().getName());
            Operator vertexData = vertex.getData();
            classKeyToVertexId.putIfAbsent(vertexData.getClassKey(), vid);
            vertexIdToClassKeyId.put(vid, vertexData.getClassKey());
        }

        //Translate legacy Graph
        Graph flow = new Graph();
        final Map<Integer, Vertex> vertexIdToVertex = new HashMap<>();//Temp
        int opCnt = 0;
        for (Map.Entry<Integer, String> entry : idToOperatorMapping.entrySet()) {
            int vertexId = entry.getKey();
            if (vertexId == 0) {
                continue;
            }
            int starting_site = deterministicStartPos ? 1 : 1 + siteSeeds.get(opCnt);
            int starting_platform = deterministicStartPos ? 1 : 1 + platformSeeds.get(opCnt);
            opCnt++;

            Vertex newVtx = new Vertex(vertexId, starting_platform, starting_site);
            flow.addVertex(newVtx);
            vertexIdToVertex.put(vertexId, newVtx);
        }
        for (Vertex v : flow.getVertices()) {
            for (int nbrId : childMapping.get(v.getOperatorId())) {
                v.addAdj(vertexIdToVertex.get(nbrId));
            }
        }

        //Populate the cost estimation structures
        INFOREDictionary dictionary = bundle.getNewDictionary();
        for (Vertex v : flow.getVertices()) {
            int id = v.getOperatorId();
            String classKey = vertexIdToClassKeyId.get(id);
            if (classKey == null) {
                this.staticOperatorCost.put(id, Integer.MAX_VALUE);
            } else {
                this.staticOperatorCost.put(id, dictionary.getOperatorCost(classKey));
            }
        }

        //Enumerate all class keys (i.e operator implementations)
        for (int vertexId : vertexIdToVertex.keySet()) {
            String classKey = vertexIdToClassKeyId.get(vertexId);

            //Platform static cost
            Map<Integer, Integer> platformMap = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> otherPlatformMap = new HashMap<>();
            for (int platform : platformMapping.keySet()) {
                String fromPlatformName = null;
                if (platform == 0 || classKey == null) {
                    platformMap.put(platform, Integer.MAX_VALUE);
                } else {
                    fromPlatformName = platformMapping.get(platform).getPlatformName();
                    int platformCost = dictionary.getPlatformStaticCostForClassKey(classKey, fromPlatformName);
                    platformMap.put(platform, platformCost);
                }
                Map<Integer, Integer> migrationPlatformMap = new HashMap<>();
                for (int other_platform : platformMapping.keySet()) {
                    if (other_platform == 0 || platform == 0 || classKey == null) {
                        migrationPlatformMap.put(other_platform, Integer.MAX_VALUE);
                    } else {
                        String toPlatformName = platformMapping.get(other_platform).getPlatformName();
                        int otherPlatformCost = dictionary.getPlatformMigrationCostForClassKey(classKey, fromPlatformName, toPlatformName);
                        migrationPlatformMap.put(other_platform, otherPlatformCost);
                    }
                }
                otherPlatformMap.put(platform, migrationPlatformMap);
            }
            this.staticPlatformCost.put(vertexId, platformMap);
            this.platformMigrationCost.put(vertexId, otherPlatformMap);

            //Site static cost
            Map<Integer, Integer> siteMap = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> otherSiteMap = new HashMap<>();
            for (int site : siteMapping.keySet()) {
                String fromSiteName = null;
                if (site == 0 || classKey == null) {
                    siteMap.put(site, Integer.MAX_VALUE);
                } else {
                    fromSiteName = siteMapping.get(site).getSiteName();
                    int siteCost = dictionary.getSiteStaticCostForClassKey(classKey, fromSiteName);
                    siteMap.put(site, siteCost);
                }
                Map<Integer, Integer> migrationSiteMap = new HashMap<>();
                for (int other_site : siteMapping.keySet()) {
                    if (other_site == 0 || site == 0 || classKey == null) {
                        migrationSiteMap.put(other_site, Integer.MAX_VALUE);
                    } else {
                        String toSiteName = siteMapping.get(other_site).getSiteName();
                        int otherSiteCost = dictionary.getSiteMigrationCostForClassKey(classKey, fromSiteName, toSiteName);
                        migrationSiteMap.put(other_site, otherSiteCost);
                    }
                }
                otherSiteMap.put(site, migrationSiteMap);
            }
            this.staticSiteCost.put(vertexId, siteMap);
            this.siteMigrationCost.put(vertexId, otherSiteMap);
        }

        System.out.println("***Costs***");
        System.out.println("Operator static costs: " + this.staticOperatorCost);
        System.out.println("Platform static costs per operator: " + this.staticPlatformCost);
        System.out.println("Site static costs per operator: " + this.staticSiteCost);
        System.out.println("Platform migration costs per operator: " + this.platformMigrationCost);
        System.out.println("Site migration costs per operator: " + this.siteMigrationCost);
        System.out.printf("Parser duration: %d ms%n", Duration.between(start, Instant.now()).toMillis());

        //plan space generation algos
        createPlanSpace(platformMapping, siteMapping, flow, threads);
    }

    @Override
    public void teardown() {
        System.out.println("FlowOptimizer Teardown");
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("exp");
    }
}
