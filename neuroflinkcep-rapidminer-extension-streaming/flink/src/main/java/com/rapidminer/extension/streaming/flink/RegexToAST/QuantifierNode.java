package com.rapidminer.extension.streaming.flink.RegexToAST;

public class QuantifierNode extends Node{
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
}
