package com.rapidminer.extension.streaming.RegexToAST;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupNode extends Node {
    List<Node> children = new ArrayList<>();
    LookAroundType lookAroundType;

    public GroupNode(LookAroundType lookAroundType) {
        this.lookAroundType = lookAroundType;
    }

    public LookAroundType getLookAroundType() {
        return lookAroundType;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node newChild) {
        children.add(newChild);
    }

    @Override
    public String toString() {
        String inner = children.stream()
                .map(Node::toString)
                .collect(Collectors.joining());
        switch (lookAroundType) {
            case POSITIVELOOKAHEAD:
                return "(?=" + inner + ")";
            case NEGATIVELOOKAHEAD:
                return "(?!" + inner + ")";
            default:
                return "(" + inner + ")";
        }
    }
}
