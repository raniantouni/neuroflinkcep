package com.rapidminer.extension.streaming.flink.RegexToAST;

public class NegativeLookaheadNode extends Node {
    private final Node child;

    public NegativeLookaheadNode(Node child) {
        this.child = child;
    }

    public Node getChild() {
        return child;
    }

    @Override
    public String toString() {
        return "(?!" + child + ")";
    }
}

