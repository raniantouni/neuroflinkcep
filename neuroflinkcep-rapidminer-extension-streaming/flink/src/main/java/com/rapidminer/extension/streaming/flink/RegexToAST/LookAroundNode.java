package com.rapidminer.extension.streaming.flink.RegexToAST;

import java.util.Objects;

public class LookAroundNode extends Node {
    private Node expression;
    private LookAroundType type;


    public LookAroundNode(Node expression,LookAroundType type ) {
        this.expression = expression;
        this.type = type;
    }

    public LookAroundType getType() {
        return type;
    }

    public void setType(LookAroundType type) {
        this.type = type;
    }

    public Node getExpression() {
        return expression;
    }

    public void setExpression(Node expression) {
        this.expression = expression;
    }
}
