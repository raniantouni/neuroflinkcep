package optimizer.algorithm.flowoptimizer;

public final class FlowGenerator {

    //Emulate the op-ES root plan
    public static Graph getWF1() {
        Graph flow = new Graph();
        Vertex v1 = new Vertex(1, 3, 1);
        Vertex v2 = new Vertex(2, 2, 1);
        Vertex v3 = new Vertex(3, 1, 1);
        Vertex v4 = new Vertex(4, 1, 1);
        v1.addAdj(v2);
        v2.addAdj(v3);
        v2.addAdj(v4);
        v3.addAdj(v4);
        flow.addVertex(v1);
        flow.addVertex(v2);
        flow.addVertex(v3);
        flow.addVertex(v4);
        return flow;
    }

    //Optimal for custom1 workflow
    public static Graph getWF2() {
        Graph flow = new Graph();
        Vertex v1 = new Vertex(1, 2, 1);
        Vertex v2 = new Vertex(2, 2, 1);
        Vertex v3 = new Vertex(3, 2, 1);
        Vertex v4 = new Vertex(4, 1, 1);
        v1.addAdj(v2);
        v2.addAdj(v3);
        v2.addAdj(v4);
        v3.addAdj(v4);
        flow.addVertex(v1);
        flow.addVertex(v2);
        flow.addVertex(v3);
        flow.addVertex(v4);
        return flow;
    }

    public static Graph getWF3() {
        Graph flow = new Graph();
        Vertex v1 = new Vertex(1, 2, 1);
        Vertex v2 = new Vertex(2, 1, 1);
        Vertex v3 = new Vertex(3, 1, 1);
        Vertex v4 = new Vertex(4, 1, 1);
        v1.addAdj(v2);
        v2.addAdj(v3);
        v2.addAdj(v4);
        v3.addAdj(v4);
        flow.addVertex(v1);
        flow.addVertex(v2);
        flow.addVertex(v3);
        flow.addVertex(v4);
        return flow;
    }
}
