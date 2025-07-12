package core.utils;


import core.graph.ThreadSafeDAG;
import core.graph.Vertex;
import core.parser.workflow.Operator;

import java.util.*;

public final class GraphUtils {

    public static Map<String, Set<String>> getOperatorParentMap(ThreadSafeDAG<Operator> operatorGraph) {
        Map<String, Set<String>> operatorParents = new HashMap<>();
        for (Vertex<Operator> operatorVertex : operatorGraph.getVertices()) {
            operatorParents.putIfAbsent(operatorVertex.getData().getName(), new HashSet<>());
            String parentName = operatorVertex.getData().getName();
            for (Vertex<Operator> child : operatorGraph.getChildren(operatorVertex)) {
                String childName = child.getData().getName();
                operatorParents.putIfAbsent(childName, new HashSet<>());
                operatorParents.get(childName).add(parentName);
            }
        }
        return operatorParents;
    }

    public static Map<String, Set<String>> getOperatorChildrenMap(ThreadSafeDAG<Operator> operatorGraph) {
        Map<String, Set<String>> parentChildren = new HashMap<>();
        for (Vertex<Operator> operatorVertex : operatorGraph.getVertices()) {
            String opName = operatorVertex.getData().getName();
            parentChildren.putIfAbsent(opName, new HashSet<>());
            for (Vertex<Operator> child : operatorGraph.getChildren(operatorVertex)) {
                parentChildren.get(opName).add(child.getData().getName());
            }
        }
        return parentChildren;
    }
}
