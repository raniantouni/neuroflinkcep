package com.rapidminer.extension.streaming.RegexToAST;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrNode extends Node {
    private boolean inRange;
    private final List<EventTypeNode> children;
    private final List<RangeNode> rangeChildren;


    public OrNode() {
        this.children = new ArrayList<>();
        this.rangeChildren = new ArrayList<>();
        inRange = false;
    }

    public void addChild(EventTypeNode child) {
        children.add(child);
    }

    public void addRangeChild(RangeNode child) { rangeChildren.add(child); }

    public List<RangeNode> getRangeChildren() {
        return rangeChildren;
    }

    public List<EventTypeNode> getChildren() {
        return children;
    }

    public boolean isInRange() {
        return inRange;
    }

    public void setInRange(boolean inRange) {
        this.inRange = inRange;
    }

    @Override
    public String toString() {
        String inside = Stream.concat(
                // quote each literal child
                children.stream().map(EventTypeNode::toString),
                // explicitly format start–end for ranges
                rangeChildren.stream()
                        .map(r -> r.getStart() + "-" + r.getEnd())
        ).collect(Collectors.joining());
        return "[" + inside + "]";
    }
}
