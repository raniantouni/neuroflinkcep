package core.graph;


public class Edge<T> {
    private final Vertex<T> first, second;

    public Edge(Vertex<T> first, Vertex<T> second) {
        this.first = first;
        this.second = second;
    }

    public Edge(Edge<T> copy) {
        this.first = new Vertex<>(copy.getFirst());
        this.second = new Vertex<>(copy.getSecond());
    }

    public Vertex<T> getFirst() {
        return this.first;
    }

    public Vertex<T> getSecond() {
        return this.second;
    }

    public Edge<T> reversed() {
        return new Edge<>(second, first);
    }

    public int nodePosition(Vertex<T> a) {
        if (this.first.equals(a)) {
            return 1;
        } else if (this.second.equals(a)) {
            return 2;
        }
        return 0;
    }

    public boolean contains(Vertex<T> a) {
        return this.first.equals(a) || this.second.equals(a);
    }

    public boolean equalsIgnoreDirection(Edge<T> edge) {
        return (this.first.equals(edge.getFirst()) && this.second.equals(edge.getSecond())) ||
                (this.second.equals(edge.getFirst()) && this.first.equals(edge.getSecond()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge<T> edge = (Edge<T>) o;

        if (!first.equals(edge.first)) return false;
        return second.equals(edge.second);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EdgeImplementation{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
