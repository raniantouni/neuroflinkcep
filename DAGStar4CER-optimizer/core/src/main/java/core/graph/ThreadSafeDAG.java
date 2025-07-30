package core.graph;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /**
     * Returns all vertices in a topologically-sorted order (parents appear before
     * their children).  Runs in O(|V| + |E|) time.  If the graph is no longer a
     * DAG (i.e., a cycle was introduced concurrently) an {@link IllegalStateException}
     * is thrown.
     *
     * @return immutable list containing the vertices in topological order
     */
    public List<Vertex<T>> getTopSortedVertices() {

        /* ---------- 1.  Snapshot current in-degrees ---------- */
        Map<Vertex<T>, Integer> inDegree = new HashMap<>();
        for (Vertex<T> v : vertexMap.keySet()) {
            inDegree.put(v, 0);                     // initialise
        }
        for (Queue<Edge<T>> edges : vertexMap.values()) {
            for (Edge<T> e : edges) {
                inDegree.merge(e.getSecond(), 1, Integer::sum);
            }
        }

        /* ---------- 2.  Collect sources (in-degree == 0) ----- */
        Deque<Vertex<T>> queue = new ArrayDeque<>();
        for (Map.Entry<Vertex<T>, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        /* ---------- 3.  Kahn’s algorithm --------------------- */
        List<Vertex<T>> result = new ArrayList<>(inDegree.size());
        while (!queue.isEmpty()) {
            Vertex<T> v = queue.removeFirst();
            result.add(v);

            // Each outgoing edge "removes" one prerequisite from its child
            for (Edge<T> edge : vertexMap.getOrDefault(v, new ConcurrentLinkedQueue<>())) {
                Vertex<T> child = edge.getSecond();
                int newDeg = inDegree.computeIfPresent(child, (k, d) -> d - 1);
                if (newDeg == 0) {
                    queue.add(child);
                }
            }
        }

        /* ---------- 4.  Cycle-check -------------------------- */
        if (result.size() != inDegree.size()) {
            throw new IllegalStateException("Graph contains a cycle; topological sort impossible.");
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Computes a unique signature for the current state of this DAG.
     * <p>
     * The algorithm:
     * <ol>
     *   <li>Create a canonical textual representation consisting of
     *       every vertex and every directed edge, both sorted
     *       lexicographically so the order is deterministic.</li>
     *   <li>Hash that string with SHA-256.</li>
     * </ol>
     * The resulting 64-hex-character string is effectively unique:
     * any change to the vertex/edge set yields a different hash with
     * overwhelming probability.
     *
     * @return 64-character lowercase hexadecimal SHA-256 hash
     * @throws RuntimeException if the JVM lacks SHA-256 (highly unlikely)
     */
    public String computeSignature() {

        /* ---------- 1.  Canonicalise vertices ---------- */
        List<String> vertexStrings = this.vertexMap.keySet()
                .stream()
                .map(Object::toString)          // rely on Vertex<T>.toString()
                .sorted()
                .collect(Collectors.toList());

        /* ---------- 2.  Canonicalise edges ------------- */
        List<String> edgeStrings = new ArrayList<>();
        for (Map.Entry<Vertex<T>, Queue<Edge<T>>> entry : vertexMap.entrySet()) {
            Vertex<T> parent = entry.getKey();
            for (Edge<T> edge : entry.getValue()) {
                // "parent->child"
                edgeStrings.add(parent.toString() + "->" + edge.getSecond().toString());
            }
        }
        Collections.sort(edgeStrings);

        /* ---------- 3.  Build canonical text ----------- */
        String canonical =
                String.join("|", vertexStrings) + "#" + String.join("|", edgeStrings);

        /* ---------- 4.  Hash with SHA-256 -------------- */
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(canonical.getBytes(StandardCharsets.UTF_8));

            // Convert to lowercase hex
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // Extremely unlikely on any modern JVM
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
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
