package com.rapidminer.extension.streaming.flink.RegexToAST;

import com.rapidminer.extension.streaming.flink.translate.FlinkCEPTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexAST {
    private final String regex;
    private int pos = 0;
    private static final Logger LOG = LoggerFactory.getLogger(RegexAST.class);

    public RegexAST(String regex) {
        this.regex = regex;
    }

    public Node parse() {
        return parseExpression(LookAroundType.NONE);
    }

    private Node parseExpression(LookAroundType type) {
        GroupNode group = new GroupNode(type);

        while (pos < regex.length()) {
            char ch = regex.charAt(pos++);

            switch (ch) {
                case '(':
                    // Check if it's a lookaround or non-capturing group.
                    if (pos < regex.length() && regex.charAt(pos) == '?') {
                        pos++; // Skip '?'
                        if (pos < regex.length()) {
                            char next = regex.charAt(pos);
                            if (next == '=') {
                                // Positive lookahead (?=...)
                                pos++; // Skip '='
                                group.addChild(parseGroup(LookAroundType.POSITIVELOOKAHEAD));
                            } else if (next == '!') {
                                // Negative lookahead (?!...)
                                pos++; // Skip '!'
                                group.addChild(parseGroup(LookAroundType.NEGATIVELOOKAHEAD));
                            } else {
                                // Other constructs like non-capturing groups (?:...)
                                // For now, just parse as a normal group.
                                group.addChild(parseGroup(LookAroundType.NONE));
                            }
                        }
                    } else {
                        group.addChild(parseGroup(LookAroundType.NONE));
                    }
                    break;
                case ')':
                    return group;
                case '*':
                    group.addChild(new QuantifierNode(group.children.remove(group.children.size() - 1), QuantifierType.KL, 0, -1));
                    break;
                case '+':
                    group.addChild(new QuantifierNode(group.children.remove(group.children.size() - 1), QuantifierType.ONEORMORE, 1, -1));
                    break;
                case '?':
                    group.addChild(new QuantifierNode(group.children.remove(group.children.size() - 1), QuantifierType.ZEROORONE, 0, 1));
                    break;
                case '\\':
                    // Handle backreference or escaped literal.
                    if (pos < regex.length()) {
                        char next = regex.charAt(pos++);
                        if (Character.isDigit(next)) {
                            // Create a BackreferenceNode for \1, \2, etc.
                            group.addChild(new BackReferenceGroup(Character.getNumericValue(next)));
                        } else {
                            // If not a digit, treat it as a literal character.
                            group.addChild(new EventTypeNode(next));
                        }
                    }
                    break;
                case '{':
                    String tmp = regex.substring(pos);
                    // Split on `}` to get the part inside `{}` and then further split on `,`.
                    String nums = tmp.split("}")[0];
                    nums.contains(",");
                    String[] parts = nums.split(",");

                    int min, max;
                    // Case {n}
                    if (!nums.contains(",") && parts.length == 1) {
                        min = Integer.parseInt(parts[0].trim());
                        max = min;  // Exact repetitions
                    }
                    // Case {,m}
                    else if (parts[0].isEmpty()) {
                        min = 0;
                        max = Integer.parseInt(parts[1].trim());

                    }
                    // Case {n,}
                    else if (parts.length == 1) {
                        min = Integer.parseInt(parts[0].trim());
                        max = -1;  // Unlimited

                    }
                    // Case {n,m}
                    else {
                        min = Integer.parseInt(parts[0].trim());
                        max = Integer.parseInt(parts[1].trim());
                    }
                    pos += nums.length() + 1;
                    group.addChild(new QuantifierNode(group.children.remove(group.children.size() - 1), QuantifierType.TIMES, min, max));
                    break;
                case '[':
                    group.addChild(parseBracketExpression());
                    break;
                default:
                    group.addChild(new EventTypeNode(ch));
                    break;
            }
        }
        return group;
    }

    private Node parseGroup(LookAroundType type) {
        return parseExpression(type);
    }

    private Node parseBracketExpression() {
        OrNode orNode = new OrNode();

        while (pos < regex.length()) {
            char ch = regex.charAt(pos++);
            if (ch == ']') {
                break;
            }
            if (pos < regex.length()            // still in bounds
                    && regex.charAt(pos) == '-'     // next char is '-'
                    && (pos + 1) < regex.length()   // there's at least one more char after '-'
                    && regex.charAt(pos + 1) != ']') {

                pos++; // skip the '-'
                char endRange = regex.charAt(pos++);
                LOG.info("START END {}",ch);// read the end char
                // For [C-L], endRange will be 'L'
                orNode.setInRange(true);
                orNode.addRangeChild(new RangeNode(ch, endRange));
            } else {
                LOG.info("----------------- EP {}",ch);// read the end char
                orNode.addChild(new EventTypeNode(ch));
            }
        }
        return orNode;
    }


    // ===============================
    // AST printing methods below
    // ===============================

    /**
     * Returns a string representing the entire AST of the given Node
     * in a 'tree-like' structure with lines and branches.
     */
    public static String toStringTree(Node root) {
        // Start recursion at top-level
        return toStringTree(root, "", true);
    }

    /**
     * Internal recursive method that:
     *  1) Builds a label for the current node (depending on its class).
     *  2) Collects child nodes (depending on the type).
     *  3) Recurses on each child with updated prefix.
     */
    private static String toStringTree(Node node, String prefix, boolean isTail) {
        if (node == null) {
            return prefix + (isTail ? "└── " : "├── ") + "null\n";
        }

        // 1) Determine the name/label for this node
        String nodeLabel = getNodeLabel(node);

        // Build the current line
        StringBuilder sb = new StringBuilder();
        sb.append(prefix)
                .append(isTail ? "└── " : "├── ")
                .append(nodeLabel)
                .append("\n");

        // 2) Get children of this node
        List<Node> children = getChildren(node);

        // 3) Recurse on each child
        for (int i = 0; i < children.size(); i++) {
            boolean lastChild = (i == children.size() - 1);
            // If this node is the last child, we add "    " to prefix,
            // otherwise we add "│   " to indicate more siblings exist.
            sb.append(toStringTree(
                    children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    lastChild
            ));
        }
        return sb.toString();
    }

    /**
     * Builds a human-readable label for a single node,
     * checking its class via instanceof.
     */
    private static String getNodeLabel(Node node) {
        if (node instanceof EventTypeNode) {
            EventTypeNode e = (EventTypeNode) node;
            return "EventTypeNode('" + e.getValue() + "')";
        }
        if (node instanceof GroupNode) {
            GroupNode g = (GroupNode) node;
            return "GroupNode(" + g.getLookAroundType() + ")";
        }
        if (node instanceof QuantifierNode) {
            QuantifierNode q = (QuantifierNode) node;
            return "QuantifierNode(" + q.getType() + ", min=" + q.getMin() + ", max=" + q.getMax() + ")";
        }
        if (node instanceof BackReferenceGroup) {
            BackReferenceGroup b = (BackReferenceGroup) node;
            return "BackReference(\\"
                    + b.getGroupNumber() + ")";
        }
        if (node instanceof OrNode) {
            return "OrNode";
        }
        if (node instanceof RangeNode) {
            RangeNode r = (RangeNode) node;
            return "RangeNode(" + r.getStart() + "-" + r.getEnd() + ")";
        }
        // fallback
        return node.getClass().getSimpleName();
    }

    /**
     * Returns all direct children of the given node, using instanceof checks.
     */
    private static List<Node> getChildren(Node node) {
        List<Node> result = new ArrayList<>();

        if (node instanceof GroupNode) {
            GroupNode g = (GroupNode) node;
            result.addAll(g.getChildren()); // whatever you called it
        }
        else if (node instanceof QuantifierNode) {
            QuantifierNode q = (QuantifierNode) node;
            // There's typically a single child node
            result.add(q.getChild());
        }
        else if (node instanceof OrNode) {
            OrNode o = (OrNode) node;
            result.addAll(o.getChildren()); // includes range children
        }
        // RangeNode, EventTypeNode, BackReferenceGroup usually have no children
        // If you have more node types, add them here.

        return result;
    }
}
