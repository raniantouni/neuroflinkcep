package com.rapidminer.extension.streaming.flink.RegexToAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PatternSplitter {

    public static List<List<String>> generateSplitsFromNodes(List<Node> flatEvents) {
        // 0) First merge look-arounds into their preceding atom:
        List<Node> events = mergeLookarounds(flatEvents);

        int n = events.size();
        // 1) find indices of all QuantifierNode
        List<Integer> quantIdxs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (events.get(i) instanceof QuantifierNode) {
                quantIdxs.add(i);
            }
        }

        // 2) formatter: Node -> exact original regex snippet
        Function<Node, String> toPattern = new Function<Node, String>() {
            @Override
            public String apply(Node node) {
                if (node instanceof EventTypeNode) {
                    String v = ((EventTypeNode) node).getValue();
                    return v.length() > 1 ? "\"" + v + "\"" : v;
                }
                if (node instanceof QuantifierNode) {
                    QuantifierNode q = (QuantifierNode) node;
                    String core = apply(q.getChild());
                    switch (q.getType()) {
                        case KL:        return core + "*";
                        case ONEORMORE: return core + "+";
                        case ZEROORONE: return core + "?";
                        case TIMES:
                            int min = q.getMin(), max = q.getMax();
                            if (min == max)   return core + "{" + min + "}";
                            else if (max < 0) return core + "{" + min + ",}";
                            else              return core + "{" + min + "," + max + "}";
                    }
                }
                if (node instanceof OrNode) {
                    return "[" +
                            ((OrNode) node).getChildren().stream()
                                    .map(this::apply)
                                    .collect(Collectors.joining())
                            + "]";
                }
                if (node instanceof CompositeAtomNode) {
                    // CompositeAtomNode holds several child-nodes already merged
                    return ((CompositeAtomNode)node).toString();
                }
                // look-around GroupNodes will have been merged already,
                // so any remaining GroupNode here is a normal capturing or non-capturing group:
                if (node instanceof GroupNode) {
                    return "(" +
                            ((GroupNode)node).getChildren().stream()
                                    .map(this::apply)
                                    .collect(Collectors.joining())
                            + ")";
                }
                // fallback
                return node.toString();
            }
        };

        // 3) build all valid splits
        List<List<String>> all = new ArrayList<>();
        for (int q : quantIdxs) {
            for (int i = 0; i <= q; i++) {
                for (int j = q; j < n; j++) {
                    List<String> parts = new ArrayList<>(3);

                    if (i > 0) {
                        parts.add(join(events.subList(0, i), toPattern));
                    }
                    parts.add(join(events.subList(i, j + 1), toPattern));
                    if (j + 1 < n) {
                        parts.add(join(events.subList(j + 1, n), toPattern));
                    }

                    if (parts.size() <= 3) {
                        all.add(parts);
                    }
                }
            }
        }

        return all;
    }

    /**
     * Walk the flat list and, whenever you see a look-around GroupNode,
     * merge it into the CompositeAtomNode together with its preceding atom.
     */
    private static List<Node> mergeLookarounds(List<Node> flat) {
        List<Node> merged = new ArrayList<>();
        for (Node n : flat) {
            if (n instanceof GroupNode
                    && ((GroupNode)n).getLookAroundType() != LookAroundType.NONE) {
                // pull off the previous atom if any
                if (!merged.isEmpty()) {
                    Node prev = merged.remove(merged.size() - 1);
                    merged.add(new CompositeAtomNode(prev, n));
                } else {
                    // no preceding atom: just wrap the look-around itself
                    merged.add(new CompositeAtomNode(n));
                }
            } else {
                merged.add(n);
            }
        }
        return merged;
    }
    private static String join(List<Node> list, Function<Node, String> f) {
        return list.stream()
                .map(f)
                .collect(Collectors.joining());
    }

    public static class CompositeAtomNode extends Node {
        private final List<Node> children;

        public CompositeAtomNode(Node... nodes) {
            this.children = Arrays.asList(nodes);
        }

        @Override
        public String toString() {
            return children.stream()
                    .map(Node::toString)
                    .collect(Collectors.joining());
        }
    }
}
