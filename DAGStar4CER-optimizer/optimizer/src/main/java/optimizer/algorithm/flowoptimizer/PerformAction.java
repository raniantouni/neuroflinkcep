package optimizer.algorithm.flowoptimizer;

/**
 * {@code Action}: Encodes action types that can be applied to a vertex of a flow.
 * Currrently, it supports three types: {@code Site}, {@code int}, and {@code OperatorType}.
 * <p>
 * Note: {@code Action} does not produces a new {@code Graph}. The graph update uses a shallow copy.
 */
public class PerformAction {

    //Local caches (care with scope)
    private Vertex vertex;
    private Graph graph;

    /**
     * Applies an action to a vertex, updates the vertex, and updates the graph.
     *
     * @param v             The vertex to apply the action to.
     * @param graph         The graph of the vertex.
     * @param candidateSite
     */
    PerformAction(Vertex v, Graph graph, int candidateSite, int candidatePlatform, OptimizerAction pickedAction) {
        if (v == null) return;
        this.vertex = new Vertex(v);
        switch (pickedAction) {
            case CHANGE_SITE:
                this.vertex.setSite(candidateSite);
                break;
            case CHANGE_PLATFORM:
                this.vertex.setPlatform(candidatePlatform);
                break;
            default:
                throw new IllegalStateException("Default case");
        }
        this.graph = graph;
        this.graph.updateVertex(this.vertex);
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex v) {
        this.vertex = new Vertex(v);
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = new Graph(graph);
    }
}
