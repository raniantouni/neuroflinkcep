package optimizer.algorithm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.exception.OptimizerException;
import core.graph.Edge;
import core.graph.ThreadSafeDAG;
import core.graph.Vertex;
import core.parser.workflow.Operator;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Parameter;
import core.structs.BoundedPriorityQueue;
import core.utils.FileUtils;
import optimizer.OptimizationResourcesBundle;
import optimizer.plan.OptimizationPlan;
import optimizer.plan.SimpleOptimizationPlan;

import java.util.*;
import java.util.stream.Collectors;

public class DAGStar4CEP implements GraphTraversalAlgorithm {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CEP_OPERATOR_CLASS_KEY = "streaming:cep";
    private ThreadSafeDAG<Operator> originalOperatorGraph;
    private OptimizationResourcesBundle bundle;
    private BoundedPriorityQueue<OptimizationPlan> validPlans;
    private SimpleOptimizationPlan rootPlan;

    @Override
    public void setup(OptimizationResourcesBundle bundle) throws OptimizerException {
        this.bundle = bundle;
        OptimizationRequest optimizationRequest = bundle.getWorkflow();
        this.originalOperatorGraph = FileUtils.getOperatorGraph(optimizationRequest);
        this.validPlans = bundle.getPlanQueue();
        this.rootPlan = bundle.getRootPlan();
    }

    private static boolean isCepOperator(Operator op) {
        return CEP_OPERATOR_CLASS_KEY.equals(op.getClassKey());
    }

    private String getOriginalRegex(Operator cepOp) {
        List<Parameter> params = cepOp.getParameters();
        String originalPattern = null;
        for (Parameter p : params) {
            if (p.getKey().equals("Regular_Expression")) {
                originalPattern = p.getValue();
                break;
            }
        }
        return originalPattern;
    }

    private Operator createCepOperator(String name, boolean isAuxiliary) {
        return new Operator(name)
                .withClassKey(CEP_OPERATOR_CLASS_KEY)
                .withHasSubprocesses(false)  // no sub-processes in this case
                .withNumberOfSubprocesses(0)
                .withAuxiliary(isAuxiliary);  // auxiliary operator for aggregation
    }

    /** Returns the first CEP vertex in the graph, or empty if none. */
    private Optional<Vertex<Operator>> firstValidCepVertex(ThreadSafeDAG<Operator> g) {
        return g.getVertices().stream()
                .filter(v -> (isCepOperator(v.getData()) && !v.getData().isAuxiliary()))
                .findFirst();
    }

    List<ThreadSafeDAG<Operator>> generateDecompositions(ThreadSafeDAG<Operator> g)
            throws OptimizerException, JsonProcessingException {

        /* -----------------------------------------------------------
         * Recursively expand CEP operators one-by-one.
         * -----------------------------------------------------------
         */
        Optional<Vertex<Operator>> cepOpt = firstValidCepVertex(g);

        // Base-case: no CEP left ➜ return this graph as a terminal variant
        if (cepOpt.isEmpty()) {
            return List.of(new ThreadSafeDAG<>(g));
        }

        Vertex<Operator> cepV = cepOpt.get();
        Operator cepOp = cepV.getData();
        System.out.println("Expanding CEP operator: " + cepOp.getName() + " and isAux: " + cepOp.isAuxiliary());
        String originalPattern = getOriginalRegex(cepOp);
        String originalName = cepOp.getName();

        // ---------- read its decomposition list ----------
        List<Parameter> params = cepOp.getParameters();
        if (params == null || params.isEmpty()) {
            throw new OptimizerException("CEP operator '" + cepOp.getName()
                    + "' carries no decomposition parameter");
        }
        String json = params.get(params.size() - 1).getValue();
        List<List<String>> decomps = MAPPER.readValue(json, new TypeReference<>() {});

        // ---------- compute parents & children of this CEP ----------
        Set<Vertex<Operator>> parents = g.getVertices().stream()
                .filter(v -> g.getChildren(v).contains(cepV))
                .collect(Collectors.toSet());

        List<Vertex<Operator>> children = g.getChildren(cepV);

        /* ---------- replace THIS cep with every decomposition alt. ---------- */
        List<ThreadSafeDAG<Operator>> oneStep = new ArrayList<>();

        for (List<String> alt : decomps) {

            /* 1) vertices -------------------------------------------------- */
            LinkedHashSet<Vertex<Operator>> vertices = new LinkedHashSet<>();

            // keep everything except the CEP being expanded
            g.getTopSortedVertices().stream()
                    .filter(v -> !v.equals(cepV))
                    .forEach(vertices::add);

            // create fresh vertices for this decomposition alternative
            List<Vertex<Operator>> newVertices = new ArrayList<>();
            for (String pattern : alt) {
                String newCepOpName = originalName + "_" + pattern;
                Operator op = createCepOperator(newCepOpName, true);
                Vertex<Operator> v = new Vertex<>(op);
                newVertices.add(v);
                vertices.add(v);
            }
            // Also need to add a cep operator that will aggregate the decomposed patterns
            Operator aggrCepOp = createCepOperator(originalName + "_" + originalPattern, true);
            Vertex<Operator> aggrCepV = new Vertex<>(aggrCepOp);
            vertices.add(aggrCepV); // Add the aggregation CEP operator to the vertices of the graph
            // Not the new vertices.

            /* 2) edges ----------------------------------------------------- */
            List<Edge<Operator>> edges = g.getEdges().stream()
                    .filter(e -> !e.getFirst().equals(cepV) && !e.getSecond().equals(cepV))
                    .collect(Collectors.toCollection(ArrayList::new));

            // fan-out: parents ➜ every new vertex
            for (Vertex<Operator> p : parents)
                for (Vertex<Operator> nv : newVertices)
                    edges.add(new Edge<>(p, nv));

            // fan-in: every new vertex ➜ aggregation CEP
            for (Vertex<Operator> nv : newVertices)
                edges.add(new Edge<>(nv, aggrCepV));

            // fan-out: aggregation CEP ➜ children
            for (Vertex<Operator> c : children)
                edges.add(new Edge<>(aggrCepV, c));

            /* 3) assemble the DAG ------------------------------------------ */
            ThreadSafeDAG<Operator> expanded = new ThreadSafeDAG<>(vertices, edges);
            oneStep.add(expanded);
        }

        /* ---------- recurse on each new DAG ------------------------------- */
        List<ThreadSafeDAG<Operator>> result = new ArrayList<>();
        for (ThreadSafeDAG<Operator> dag : oneStep) {
            result.addAll(generateDecompositions(dag));   // further CEPs (if any)
        }
        return result;
    }

    private void printDecompositionsInfo(List<ThreadSafeDAG<Operator>> decompositions) {
        int i = 1;
        for (ThreadSafeDAG<Operator> dag : decompositions) {
            System.out.println("======= DECOMPOSITION #" + i++ + " =======");
            System.out.println("Total operators: " + dag.getVertices().size());
            System.out.println("Signature: " + dag.computeSignature());
            for (Vertex<Operator> v : dag.getTopSortedVertices()) {
                String vertexName = v.getData().getName();

                String children =
                        dag.getChildren(v).stream()
                                .map(child -> child.getData().getName())
                                .collect(Collectors.joining(", "));  // empty string if no children

                System.out.println("Vertex: " + vertexName);
                System.out.println("To: " + children);
                System.out.println();
            }
            System.out.println();   // blank line between decompositions
        }

    }


    @Override
    public void doWork() {

        // ---------- 1. Generate and print the decomposed workflows ----------
        try {

            List<ThreadSafeDAG<Operator>> decomposedGraphs;
            decomposedGraphs = generateDecompositions(originalOperatorGraph);

            decomposedGraphs.sort(Comparator.comparingInt(ThreadSafeDAG::totalVertices));

            int i = 1;
            List<SimpleOptimizationPlan> dagStarResults = new ArrayList<>();
            for (ThreadSafeDAG<Operator> dag : decomposedGraphs) {

                AStarSearchAlgorithm sStar = new AStarSearchAlgorithm(AStarSearchAlgorithm.AggregationStrategy.MAX, false);
                sStar.setup4CEP(this.bundle, dag);
                sStar.doWork();
                System.out.println("After doWork, queue size: " + this.bundle.getPlanQueue().size());
                SimpleOptimizationPlan result = (SimpleOptimizationPlan) this.bundle.getPlanQueue().poll();
                if (result == null) throw new IllegalStateException("No valid plan found for decomposition #" + i);
                dagStarResults.add(new SimpleOptimizationPlan(result));

                if (i == 5) break;

                System.out.println("Best plan cost for decomposition #" + i++ + ": " + result.totalCost());
                System.out.println("Number of operators: " + result.getOperatorsAndImplementations().size());
                // Print the operators and their implementations
                result.getOperatorsAndImplementations().forEach((op, impl) -> {
                    System.out.println("Operator: " + op + ", Implementation: " + impl);
                });

                System.out.println();
                this.bundle.getPlanQueue().clear(); // Clear the queue for the next decomposition
                System.out.println("After clearing, queue size: " + this.bundle.getPlanQueue().size());
                sStar.teardown();
            }
            dagStarResults.sort(Comparator.comparingInt(SimpleOptimizationPlan::totalCost));
            this.bundle.getPlanQueue().offer(dagStarResults.get(0));
        } catch (Exception e) {
            e.printStackTrace();   // quick visibility for any parsing / graph issues
        }

    }

    @Override
    public void teardown() {

    }

    @Override
    public List<String> aliases() {
        return List.of();
    }
}
