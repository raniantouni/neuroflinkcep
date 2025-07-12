package optimizer.visitor;

import core.graph.Vertex;
import core.visitor.GraphVisitor;

import java.util.Iterator;

public class PrintGraphVisitor<T> implements GraphVisitor<T> {
    private int totalCalls;

    public PrintGraphVisitor() {
        this.totalCalls = 0;
    }

    @Override
    public void visit(Vertex<T> node) {
        System.out.println("Visiting: " + node.toString());
    }

    @Override
    public Iterable<T> resultIter() {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                return null;
            }
        };
    }

    @Override
    public int totalCalls() {
        return this.totalCalls;
    }
}
