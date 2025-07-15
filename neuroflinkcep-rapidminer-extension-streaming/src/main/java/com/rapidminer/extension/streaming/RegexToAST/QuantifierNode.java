package com.rapidminer.extension.streaming.RegexToAST;

public class QuantifierNode extends Node {
    private Node child;
    private QuantifierType type;
    private int min,max;

    public QuantifierNode(Node child, QuantifierType type, int min, int max) {
        this.child = child;
        this.type = type;
        this.min = min;
        this.max = max;
    }

    public Node getChild() {
        return child;
    }

    public void setChild(Node child) {
        this.child = child;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public QuantifierType getType() {
        return type;
    }

    public void setType(QuantifierType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        String core = child.toString();
        switch (type) {
            case KL:        return core + "*";
            case ONEORMORE: return core + "+";
            case ZEROORONE: return core + "?";
            case TIMES:
                if (min == max)   return core + "{" + min + "}";
                else if (max < 0) return core + "{" + min + ",}";
                else              return core + "{" + min + "," + max + "}";
        }
        throw new IllegalStateException();
    }
}
