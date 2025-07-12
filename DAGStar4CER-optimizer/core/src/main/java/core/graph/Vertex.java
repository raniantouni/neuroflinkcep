package core.graph;

import core.visitor.GraphVisitor;

public class Vertex<T> {
    private final T data;

    public Vertex(T data) {
        this.data = data;
    }

    //Shallow copy
    public Vertex(Vertex<T> copy) {
        this.data = copy.getData();
    }

    public void accept(GraphVisitor<T> visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex<T> that = (Vertex<T>) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "AbstractVertex{" +
                "data=" + data +
                '}';
    }

    public T getData() {
        return data;
    }
}