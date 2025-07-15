package com.rapidminer.extension.streaming.RegexToAST;

import com.rapidminer.tools.LogService;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PatternSplitter {

    public static final Logger logger = LogService.getRoot();
    public static List<List<String>> generateAllSplits(List<Node> flatEvents) {
        List<List<String>> separate = generateSplits(flatEvents, false);
        List<List<String>> merged   = generateSplits(flatEvents, true);

        LinkedHashSet<List<String>> seen = new LinkedHashSet<>();
        seen.addAll(separate);
        seen.addAll(merged);
        logger.info("Seen: " + seen.toString());
        return new ArrayList<>(seen);
    }

    private static List<List<String>> generateSplits(
            List<Node> flatEvents,
            boolean mergeAdjacentLiterals
    ) {
        // 1) always merge look‐arounds & back‐refs
        List<Node> events = mergeLookaroundsAndBackrefs(flatEvents);

        // 2) optionally also glue adjacent literals
        if (mergeAdjacentLiterals) {
            events = mergeAdjacentLiterals(events);
        }

        // 3) find all quantifier positions
        List<Integer> quantIdxs = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Node n = events.get(i);
            if (n instanceof QuantifierNode
                    || (n instanceof CompositeAtomNode && containsQuantifier(n)))
            {
                quantIdxs.add(i);
            }
        }

        List<List<String>> out = new ArrayList<>();
        int n = events.size();
        Function<Node,String> fmt = toPattern();

        // 4) if no quantifiers & separate‐mode → just AB→[A,B]
        if (!mergeAdjacentLiterals && quantIdxs.isEmpty()) {
            for (int k = 1; k < n; k++) {
                out.add(Arrays.asList(
                        join(events.subList(0, k), fmt),
                        join(events.subList(k, n), fmt)
                ));
            }
            return out;
        }

        // 5) otherwise build all prefix–middle–suffix around each quantifier
        for (int q : quantIdxs) {
            // prevent splitting a backref‐atom cluster
            int clusterStart = q;
            while (clusterStart > 0) {
                Node prev = events.get(clusterStart - 1);
                if (prev instanceof CompositeAtomNode
                        && containsBackReference(prev))
                {
                    clusterStart--;
                } else {
                    break;
                }
            }

            for (int i = 0; i <= clusterStart; i++) {
                for (int j = q; j < n; j++) {
                    List<String> parts = new ArrayList<>(3);
                    if (i > 0) {
                        parts.add(join(events.subList(0, i), fmt));
                    }
                    parts.add(join(events.subList(i, j + 1), fmt));
                    if (j + 1 < n) {
                        parts.add(join(events.subList(j + 1, n), fmt));
                    }
                    if (parts.size() >= 2 && parts.size() <= 3) {
                        out.add(parts);
                    }
                }
            }
        }

        return out;
    }

    /** String‐formatter for Nodes → exact regex snippet */
    private static Function<Node,String> toPattern() {
        return new Function<Node,String>() {
            @Override public String apply(Node node) {
                if (node instanceof EventTypeNode) {
                    String v = ((EventTypeNode)node).getValue();
                    return v.length() > 1 ? "\"" + v + "\"" : v;
                }
                if (node instanceof BackReferenceGroup) {
                    return "\\" + ((BackReferenceGroup)node).getGroupNumber();
                }
                if (node instanceof QuantifierNode) {
                    QuantifierNode q = (QuantifierNode)node;
                    String core = apply(q.getChild());
                    switch(q.getType()) {
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
                            ((OrNode)node).getChildren().stream()
                                    .map(this::apply)
                                    .collect(Collectors.joining())
                            + "]";
                }
                if (node instanceof CompositeAtomNode) {
                    return node.toString();
                }
                if (node instanceof GroupNode) {
                    return "(" +
                            ((GroupNode)node).getChildren().stream()
                                    .map(this::apply)
                                    .collect(Collectors.joining())
                            + ")";
                }
                return node.toString();
            }
        };
    }

    private static boolean containsQuantifier(Node node) {
        if (node instanceof QuantifierNode) return true;
        if (node instanceof CompositeAtomNode) {
            for (Node c : ((CompositeAtomNode)node).children)
                if (containsQuantifier(c)) return true;
        }
        if (node instanceof GroupNode) {
            for (Node c : ((GroupNode)node).getChildren())
                if (containsQuantifier(c)) return true;
        }
        return false;
    }

    private static boolean containsBackReference(Node node) {
        if (node instanceof BackReferenceGroup) return true;
        if (node instanceof CompositeAtomNode) {
            for (Node c : ((CompositeAtomNode)node).children)
                if (containsBackReference(c)) return true;
        }
        return false;
    }

    private static List<Node> mergeLookaroundsAndBackrefs(List<Node> flat) {
        List<Node> merged = new ArrayList<>();
        for (Node n : flat) {
            if ((n instanceof GroupNode
                    && ((GroupNode)n).getLookAroundType() != LookAroundType.NONE)
                    || n instanceof BackReferenceGroup
                    || (n instanceof QuantifierNode
                    && ((QuantifierNode)n).getChild() instanceof BackReferenceGroup))
            {
                mergePrev(merged, n);
            } else {
                merged.add(n);
            }
        }
        return merged;
    }

    private static void mergePrev(List<Node> out, Node n) {
        if (!out.isEmpty()) {
            Node prev = out.remove(out.size()-1);
            out.add(new CompositeAtomNode(prev, n));
        } else {
            out.add(new CompositeAtomNode(n));
        }
    }

    private static List<Node> mergeAdjacentLiterals(List<Node> in) {
        List<Node> merged = new ArrayList<>();
        for (Node n : in) {
            if (n instanceof EventTypeNode && !merged.isEmpty()) {
                Node p = merged.get(merged.size()-1);
                if (p instanceof EventTypeNode) {
                    merged.remove(merged.size()-1);
                    merged.add(new CompositeAtomNode(p, n));
                    continue;
                }
            }
            merged.add(n);
        }
        return merged;
    }

    private static String join(List<Node> list, Function<Node,String> f) {
        return list.stream().map(f).collect(Collectors.joining());
    }

    public static class CompositeAtomNode extends Node {
        final List<Node> children;
        public CompositeAtomNode(Node... nodes) {
            this.children = Arrays.asList(nodes);
        }
        @Override public String toString() {
            return children.stream()
                    .map(Node::toString)
                    .collect(Collectors.joining());
        }
    }
}
