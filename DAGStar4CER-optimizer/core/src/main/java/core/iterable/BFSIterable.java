package core.iterable;


import core.graph.DirectedAcyclicGraph;
import core.graph.Vertex;

import java.util.*;

public class BFSIterable<T> implements GraphIterable<T> {
    private final Set<Vertex<T>> processed;
    private final Queue<Vertex<T>> queue;
    private final DirectedAcyclicGraph<T> graph;

    public BFSIterable(DirectedAcyclicGraph<T> g, Vertex<T> source) {
        this.processed = new HashSet<>();
        this.graph = g;
        this.queue = new LinkedList<>();
        this.queue.add(source);
    }

    public BFSIterable(DirectedAcyclicGraph<T> planGraph) {
        this.graph = planGraph;
        this.queue = new LinkedList<>();
        this.queue.add(graph.getRoot());
        this.processed = new HashSet<>();
        this.processed.add(graph.getRoot());
    }

    @Override
    public Iterator<Vertex<T>> iterator() {
        return new Iterator<Vertex<T>>() {
            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public Vertex<T> next() {
                Vertex<T> currentPlan = queue.poll();
                List<Vertex<T>> successors = graph.getChildren(currentPlan);
                successors.removeIf(processed::contains);
                processed.addAll(successors);
                queue.addAll(successors);
                return currentPlan;
            }
        };
    }
}
