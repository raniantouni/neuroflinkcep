package core.iterable;


import core.graph.DirectedAcyclicGraph;
import core.graph.Vertex;

/**
 * Algorithms that traverse a {@link DirectedAcyclicGraph} without modifying any vertices or edges.
 *
 * @param <T> The type encapsulated by the vertices.
 */
public interface GraphIterable<T> extends Iterable<Vertex<T>> {
}
