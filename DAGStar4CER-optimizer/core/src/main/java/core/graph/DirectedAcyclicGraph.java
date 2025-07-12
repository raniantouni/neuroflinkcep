package core.graph;

import java.util.*;

public interface DirectedAcyclicGraph<T> {

    boolean addVertex(Vertex<T> node);

    boolean addEdge(Edge<T> edge);

    boolean isThreadSafe();

    boolean containsVertex(Vertex<T> a);

    boolean containsEdge(Edge<T> e);

    int totalVertices();

    int totalEdges();

    List<Vertex<T>> getChildren(Vertex<T> v);

    Map<Vertex<T>, Set<Vertex<T>>> getParentsAndChildren();

    List<Vertex<T>> getNeighbours(Vertex<T> v);

    int inDegree(Vertex<T> vertex);

    int outDegree(Vertex<T> vertex);

    Vertex<T> getRoot();

    List<Edge<T>> getEdges();

    Queue<Edge<T>> edgesOf(Vertex<T> vtx);

    Set<Vertex<T>> getVertices();

    boolean isEmpty();
}
