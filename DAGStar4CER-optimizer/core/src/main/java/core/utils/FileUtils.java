package core.utils;

import core.graph.Edge;
import core.graph.ThreadSafeDAG;
import core.graph.Vertex;
import core.iterable.DAGIterable;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import core.parser.workflow.OptimizationRequest;
import core.parser.workflow.Operator;
import core.structs.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public final class FileUtils {
    private static final Random RNG = new Random(0);

    public static ThreadSafeDAG<Operator> getOperatorGraph(OptimizationRequest optimizationRequest) {
        Map<String, Vertex<Operator>> operatorMap = new HashMap<>();
        optimizationRequest.getOperators().forEach(op -> operatorMap.put(op.getName(), new Vertex<>(op)));

        List<Edge<Operator>> edges = optimizationRequest.getOperatorConnections().stream()
                .map(conn -> new Edge<>(new Vertex<>(operatorMap.get(conn.getFromOperator())), new Vertex<>(operatorMap.get(conn.getToOperator()))))
                .collect(Collectors.toList());

        return new ThreadSafeDAG<>(operatorMap.values(), edges);
    }

    public static LinkedHashMap<String, Map<String, List<String>>> getOperatorImplementations(ThreadSafeDAG<Operator> operatorGraph,
                                                                                              INFORENetwork network,
                                                                                              INFOREDictionary dictionary,
                                                                                              Map<String, String> classKeyMapping) {
        LinkedHashMap<String, Map<String, List<String>>> opMap = new LinkedHashMap<>();
        for (Vertex<Operator> operatorVtx : new DAGIterable<>(operatorGraph)) {
            String operatorName = operatorVtx.getData().getName();
            //TODO: WHEN WORKING WITH RIOT WORKFLOWS, THERE IS NO CLASS KEY
            //TODO: WHEN WORKING WITH OTHER WORKFLOW OR THE OPTIMIZER SERVICE, WE NEED THE CLASS KEY
//            System.out.println("Getting implementations for operator: " + operatorName + " with class key: " + classKeyMapping.get(operatorName));
            Map<String, List<String>> impls = dictionary.getImplementationsForClassKey(classKeyMapping.get(operatorName), network);
//             Map<String, List<String>> impls = dictionary.getImplementationsForClassKey(operatorName, network); //TESTING ONLY

            opMap.put(operatorName, impls);
        }
        return opMap;
    }

    public static LinkedHashMap<String, List<Tuple<String, String>>> getOperatorImplementationsAsTuples(ThreadSafeDAG<Operator> operatorGraph,
                                                                                                        INFORENetwork network,
                                                                                                        INFOREDictionary dictionary,
                                                                                                        Map<String, String> classKeyMapping) {
        LinkedHashMap<String, List<Tuple<String, String>>> opMap = new LinkedHashMap<>();
        for (Vertex<Operator> operatorVtx : new DAGIterable<>(operatorGraph)) {
            String operatorName = operatorVtx.getData().getName();
            String nameToSearch = operatorName;
             if (operatorName.startsWith("Neuro")) {
                 nameToSearch = operatorName.split("_")[0];
             }
//            System.out.println("Getting implementations for operator: " + operatorName + " with class key: " + classKeyMapping.get(nameToSearch));

            Map<String, List<String>> impls = dictionary.getImplementationsForClassKey(classKeyMapping.get(nameToSearch), network);

            for (String site : impls.keySet()) {
                for (String platform : impls.get(site)) {
                    opMap.putIfAbsent(operatorName, new ArrayList<>());
                    opMap.get(operatorName).add(new Tuple<>(site, platform));
                }
            }
        }
        return opMap;
    }

    //Place everything on the first platform and site that are available, present networks ensure that this is possible.
    public static LinkedHashMap<String, Tuple<String, String>> generateStartingOperatorImplementations(ThreadSafeDAG<Operator> operatorGraph,
                                                                                                       Map<String, Map<String, List<String>>> operatorImplementations) {
        LinkedHashMap<String, Tuple<String, String>> operatorImplementation = new LinkedHashMap<>();
        for (Vertex<Operator> operator : new DAGIterable<>(operatorGraph)) {
            String opName = operator.getData().getName();
            Map<String, List<String>> opSAP = operatorImplementations.get(opName);
            if (opSAP.isEmpty()) {
                throw new IllegalStateException("Empty operator site implementation list");
            }
            List<String> allSites = new ArrayList<>(opSAP.keySet());
            String site = allSites.get(RNG.nextInt(allSites.size()));

            List<String> allPlatforms = new ArrayList<>(opSAP.get(site));
            String platform = allPlatforms.get(RNG.nextInt(allPlatforms.size()));

            operatorImplementation.put(opName, new Tuple<>(site, platform));
        }
        return operatorImplementation;
    }

    public static LinkedHashMap<String, Tuple<String, String>> generateStartingOperatorImplementationsWithSeeds(ThreadSafeDAG<Operator> operatorGraph,
                                                                                                                List<Integer> platformSeeds,
                                                                                                                List<Integer> siteSeeds,
                                                                                                                Map<String, Map<String, List<String>>> operatorImplementations) {
        LinkedHashMap<String, Tuple<String, String>> operatorImplementation = new LinkedHashMap<>();
        int opCnt = 0;
        for (Vertex<Operator> operator : new DAGIterable<>(operatorGraph)) {
            String opName = operator.getData().getName();
            Map<String, List<String>> opSAP = operatorImplementations.get(opName);
            if (opSAP.isEmpty()) {
                throw new IllegalStateException("Empty operator site implementation list");
            }
            List<String> allSites = new ArrayList<>(opSAP.keySet());
            Collections.sort(allSites);
            String site = allSites.get(Math.min(siteSeeds.get(opCnt), allSites.size() - 1));

            List<String> allPlatforms = new ArrayList<>(opSAP.get(site));
            Collections.sort(allPlatforms);
            String platform = allPlatforms.get(Math.min(platformSeeds.get(opCnt), allPlatforms.size() - 1));

            operatorImplementation.put(opName, new Tuple<>(site, platform));
            opCnt++;
        }
        return operatorImplementation;
    }

    public static Map<String, String> getOpNameToClassKeyMapping(OptimizationRequest optimizationRequest) {
        Map<String, String> classNames = new HashMap<>();
        for (Operator operator : optimizationRequest.getOperators()) {
            classNames.put(operator.getName(), operator.getClassKey());
        }
        return classNames;
    }
}
