package com.rapidminer.extension.streaming.flink.RegexToAST;

import java.util.ArrayList;
import java.util.List;

public class GroupNode extends Node{
    List<Node> children = new ArrayList<>();
    LookAroundType lookAroundType;

    public GroupNode(LookAroundType lookAroundType) {
        this.lookAroundType = lookAroundType;
    }

    public LookAroundType getLookAroundType() {
        return lookAroundType;
    }

    public List<Node> getChildren()
    {
        return children;
    }

    public void addChild(Node newChild){
        children.add(newChild);
    }
}
