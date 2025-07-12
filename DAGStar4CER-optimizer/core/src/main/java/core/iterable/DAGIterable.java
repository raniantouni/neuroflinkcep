package core.iterable;


import core.graph.ThreadSafeDAG;
import core.graph.Vertex;

import java.util.*;

public class DAGIterable<T> implements GraphIterable<T> {
    private final Deque<Vertex<T>> order;

    public DAGIterable(ThreadSafeDAG<T> g) {
        //DirectedAcyclicGraph null check
        Objects.requireNonNull(g);

        // List where we'll be storing the topological order
        this.order = new ArrayDeque<>();

        // Map which indicates if a node is visited (has been processed by the algorithm)
        Set<Vertex<T>> visited = new HashSet<>();

        // We go through all the nodes
        for (Vertex<T> u : g.getVertices()) {
            if (!visited.contains(u)) {
                topoSortRecursive(g, u, visited, order);
            }
        }
    }

    private void topoSortRecursive(ThreadSafeDAG<T> g,
                                   Vertex<T> n,
                                   Set<Vertex<T>> visited,
                                   Deque<Vertex<T>> order) {
        //Null checking
        Objects.requireNonNull(n);

        // Mark the current node as visited
        visited.add(n);

        // We reuse the algorithm on all adjacent nodes to the current node
        for (Vertex<T> v : g.getNeighbours(n)) {
            if (!visited.contains(v)) {
                topoSortRecursive(g, v, visited, order);
            }
        }

        // Put the current node in the array
        order.addFirst(n);
    }

    @Override
    public Iterator<Vertex<T>> iterator() {

        return new Iterator<Vertex<T>>() {
            @Override
            public boolean hasNext() {
                return !order.isEmpty();
            }

            @Override
            public Vertex<T> next() {
                return order.poll();
            }
        };
    }
}
