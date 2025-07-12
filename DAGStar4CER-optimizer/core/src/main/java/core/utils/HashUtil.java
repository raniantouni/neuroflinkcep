package core.utils;

import core.graph.ThreadSafeDAG;
import core.graph.Vertex;
import core.structs.Tuple;

import java.util.LinkedHashMap;

public final class HashUtil {

    public static <T> int planHash(ThreadSafeDAG<T> graph, LinkedHashMap<String, Tuple<String,String>> implMap) {
        int result = 1;
        for (Vertex<T> operatorVertex : graph.getVertices()) {
            result = 31 * result + graph.getChildren(operatorVertex).hashCode();
            String curOp = (String) operatorVertex.getData();
            result = 31 * result + curOp.hashCode();
            Tuple<String,String> curImpl = implMap.get(curOp);
            result = 31 * result + curImpl.hashCode();
        }
        return result;
    }

    public static <T> String planSignature(ThreadSafeDAG<T> graph, LinkedHashMap<String, Tuple<String,String>> implMap) {
        return String.valueOf(planHash(graph, implMap));
    }
}
