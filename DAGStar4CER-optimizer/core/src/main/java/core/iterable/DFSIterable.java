package core.iterable;


import core.graph.ThreadSafeDAG;
import core.graph.Vertex;

import java.util.*;

public class DFSIterable<T> implements GraphIterable<T> {
    private final Queue<Vertex<T>> queue;

    public DFSIterable(ThreadSafeDAG<T> graph) {
        this.queue = new LinkedList<>();
        Stack<Vertex<T>> stack = new Stack<>();
        Set<Vertex<T>> visited = new HashSet<>();
        stack.push(graph.getRoot());
        while (!stack.isEmpty()) {
            Vertex<T> current = stack.pop();
            visited.add(current);
            queue.add(current);
            for (Vertex<T> dest : graph.getChildren(current)) {
                if (!visited.contains(dest)) {
                    stack.push(dest);
                }
            }
        }
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
                return queue.poll();
            }
        };
    }
}

