package core.visitor;



import core.graph.DirectedAcyclicGraph;
import core.graph.Vertex;

/**
 * Traverses a {@link DirectedAcyclicGraph} and performs an action on each visited vertex.
 *
 * @param <T> The vertex data type.
 */
public interface GraphVisitor<T> {
    void visit(Vertex<T> vertex);

    Iterable<T> resultIter();

    int totalCalls();
}
