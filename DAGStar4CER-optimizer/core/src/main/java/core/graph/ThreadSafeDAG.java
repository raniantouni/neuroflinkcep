package core.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Thread safe implementation of a DAG using a {@link ConcurrentHashMap} for keys and
 * a {@link ConcurrentLinkedQueue} for values.
 *
 * @param <T> The vertex type, i.e. the objects stored in this graph.
 */
public class ThreadSafeDAG<T> implements DirectedAcyclicGraph<T> {

    //Mapping from vertices to outgoing edges
    private final Map<Vertex<T>, Queue<Edge<T>>> vertexMap;
    private final Vertex<T> root;

    public ThreadSafeDAG(Vertex<T> root) {
        this.vertexMap = new ConcurrentHashMap<>();
        this.root = root;
        this.addVertex(root);
    }

    public ThreadSafeDAG(Collection<Vertex<T>> vertices, Collection<Edge<T>> edges) {
        this.vertexMap = new ConcurrentHashMap<>();
        this.root = !vertices.isEmpty() ? vertices.iterator().next() : null;
        for (Vertex<T> vertex : vertices) {
            this.addVertex(vertex);
        }
        for (Edge<T> edge : edges) {
            this.addEdge(edge);
        }
    }

    //Copy constructors
    public ThreadSafeDAG(final ThreadSafeDAG<T> copy) {
        this.root = copy.root;
        this.vertexMap = new ConcurrentHashMap<>();
        for (Vertex<T> vertex : copy.getVertices()) {
            this.addVertex(vertex);
        }
        for (Edge<T> edge : copy.getEdges()) {
            this.addEdge(edge);
        }
    }
    @Override
    public boolean addVertex(Vertex<T> node) {
        return this.vertexMap.putIfAbsent(node, new ConcurrentLinkedQueue<>()) == null;
    }

    @Override
    public boolean addEdge(Edge<T> edge) {
        return this.vertexMap.get(edge.getFirst()).add(edge);
    }

    @Override
    public boolean containsVertex(Vertex<T> a) {
        return this.vertexMap.containsKey(a);
    }

    @Override
    public boolean containsEdge(Edge<T> e) {
        return this.vertexMap.values().stream()
                .flatMap(Queue::stream)
                .anyMatch(edge -> edge.equals(e));
    }

    @Override
    public int totalVertices() {
        return this.vertexMap.size();
    }

    // Slow!! Note that queue.size() is an O(n) operation. This effectively traverses the entire graph.
    @Override
    public int totalEdges() {
        return this.vertexMap.values().stream().mapToInt(Collection::size).sum();
    }


    @Override
    public List<Vertex<T>> getChildren(Vertex<T> v) {
        return this.vertexMap.getOrDefault(v, new ConcurrentLinkedQueue<>()).stream()
                .map(Edge::getSecond)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Vertex<T>, Set<Vertex<T>>> getParentsAndChildren() {
        Map<Vertex<T>, Set<Vertex<T>>> parentChildren = new HashMap<>();
        for (Vertex<T> parent : this.vertexMap.keySet()) {
            parentChildren.putIfAbsent(parent, new HashSet<>());
            for (Vertex<T> child : this.vertexMap.get(parent).stream().map(Edge::getSecond).collect(Collectors.toList())) {
                parentChildren.putIfAbsent(child, new HashSet<>());
                parentChildren.get(child).add(parent);
            }
        }
        return parentChildren;
    }

    @Override
    public List<Vertex<T>> getNeighbours(Vertex<T> v) {
        return this.getChildren(v);   //It's a DAG
    }

    @Override
    public int inDegree(Vertex<T> vertex) {
        return (int) this.vertexMap.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .filter(edge -> edge.getSecond().equals(vertex))
                .count();
    }

    @Override
    public int outDegree(Vertex<T> vertex) {
        return this.vertexMap.get(vertex).size();
    }

    @Override
    public Vertex<T> getRoot() {
        return this.root;
    }

    @Override
    public Set<Vertex<T>> getVertices() {
        return this.vertexMap.keySet();
    }

    public Iterator<Vertex<T>> getVerticesIterator() {
        return this.vertexMap.keySet().iterator();
    }

    private Iterator<Edge<T>> getEdgesIterator() {
        return this.vertexMap.values().stream()
                .flatMap(Queue::stream)
                .distinct()
                .iterator();
    }


    @Override
    public List<Edge<T>> getEdges() {
        return this.vertexMap.values().stream()
                .flatMap(Queue::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Queue<Edge<T>> edgesOf(Vertex<T> vtx) {
        return this.vertexMap.get(vtx);
    }

    @Override
    public boolean isEmpty() {
        return this.vertexMap.isEmpty();
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadSafeDAG<T> that = (ThreadSafeDAG<T>) o;
        if (!this.vertexMap.keySet().equals(that.vertexMap.keySet())) {
            return false;
        }
        for (Vertex<T> that_key : that.vertexMap.keySet()) {
            Queue<Edge<T>> this_edges = this.vertexMap.get(that_key);
            Queue<Edge<T>> that_edges = that.vertexMap.get(that_key);
            if (!this_edges.containsAll(that_edges)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Vertex<T> vertex : this.vertexMap.keySet()) {
            result = 31 * result + vertex.hashCode();
        }
        for (Edge<T> edge : this.vertexMap.values().stream().flatMap(Queue::stream).collect(Collectors.toSet())) {
            result = 31 * result + edge.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        for (Vertex<T> v : vertexMap.keySet()) {
            response.append("Vertex: ").append(v.toString()).append(" ")
                    .append("Edges: ").append(getChildren(v).toString()).append("\n");
        }
        return response.toString();
    }
}
