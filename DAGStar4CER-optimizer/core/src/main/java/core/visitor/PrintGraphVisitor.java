package core.visitor;

import core.graph.Vertex;
import core.visitor.GraphVisitor;

import java.util.Iterator;

public class PrintGraphVisitor implements GraphVisitor<String> {
    private int totalCalls;

    public PrintGraphVisitor() {
        this.totalCalls = 0;
    }

    @Override
    public void visit(Vertex<String> node) {
        System.out.println("Visiting: " + node.toString());
    }

    @Override
    public Iterable<String> resultIter() {
        return () -> new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                return null;
            }
        };
    }

    @Override
    public int totalCalls() {
        return this.totalCalls;
    }
}
