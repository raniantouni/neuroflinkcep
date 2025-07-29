package com.rapidminer.extension.streaming.flink.translate;

import com.google.common.collect.Lists;
import com.rapidminer.extension.streaming.flink.MobileFraudDetectionModel.MobileEventsExtraction;
import com.rapidminer.extension.streaming.flink.MobileFraudDetectionModel.MobileFraudDetectionModel;
import com.rapidminer.extension.streaming.flink.RegexToAST.*;
import com.rapidminer.extension.streaming.flink.RobotStatusPredictionModel.RobotBatchProcessor;
import com.rapidminer.extension.streaming.flink.RobotStatusPredictionModel.RobotGoalPredictionModel;
import com.rapidminer.extension.streaming.utility.graph.transform.CEP;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FlinkCEPTranslator {
    DataStream<JSONObject> stream;

    public FlinkCEPTranslator(DataStream<JSONObject> stream) throws PatternSyntaxException {
        this.stream = stream;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FlinkCEPTranslator.class);
    public static int uniqueId = 0;

    public DataStream<JSONObject> translate(CEP cep) {

        String patternName = cep.getPatternName();
        String key = cep.getKey();

        Boolean isModelIncluded = cep.getModelIncluded();
        Integer modelInput = cep.getLength();
        String modelDirectory = cep.getDirectory();
        Boolean recognizeAggEvents = false;

        Boolean useLoadedModel = cep.getUseLoadedModel();
        Boolean enableKeyBy = cep.getEnableKeyBy();

        String modelName = cep.getModelName();
        String keyName = cep.getKeyName();

        String regex = cep.getRegex();
        Long timeWindow = cep.getTimeWindow();
        RegexAST parser = new RegexAST(regex);
        Node ast = parser.parse();
        Map<String, Map<String, String>> predicates = cep.getParsedPredicates();
        CEP.SelectionStrategy selectionStrategy = cep.getSelectionStrategy();
        CEP.ConsumptionPolicy consumptionPolicy = cep.getConsumptionPolicy();

        DataStream<JSONObject> preCepStream;
        if (isModelIncluded && useLoadedModel) {
            if (modelName.equals("Telecom Scenario")) {
                LOG.info(" -------------------- Using existing loaded model: " + modelName);
                preCepStream = stream
                        .flatMap(new MobileEventsExtraction("/mobileFraudDetectionModel", recognizeAggEvents));

                LOG.info("CURRENT PATTERN -----------------");

            }
            else {
                RobotGoalPredictionModel rModel = new RobotGoalPredictionModel("/model");
                preCepStream = stream
                        .keyBy(obj -> Integer.parseInt(obj.getString(keyName)))
                        .process(new RobotBatchProcessor(rModel));
            }
        } else preCepStream = stream;

        AfterMatchSkipStrategy consumptionStrategy = getConsumptionStrategy(consumptionPolicy);
        DataStream<JSONObject> timestampedStream = preCepStream.assignTimestampsAndWatermarks(new AssignerWithPunctuatedWatermarks<JSONObject>() {
            @Override
            public long extractTimestamp(JSONObject jsonObject, long l) {

//                return (long) jsonObject.get("timestamp");
                Object ts = jsonObject.get("timestamp");
                if (ts instanceof Number) {
                    // Works for Integer, Long, Double, etc.
                    return ((Number) ts).longValue();
                } else if (ts instanceof String) {
                    try {
                        return Long.parseLong((String) ts);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid timestamp format: " + ts, e);
                    }
                } else {
                    throw new RuntimeException("Unsupported timestamp type: " + ts.getClass());
                }
            }

            @Nullable
            @Override
            public Watermark checkAndGetNextWatermark(JSONObject jsonObject, long l) {
                return new Watermark(l);
            }

        });
        LOG.info("AST " + RegexAST.toStringTree(ast));
        Pattern<JSONObject, JSONObject> pattern = convertToFlinkCEP(ast, key, selectionStrategy, consumptionStrategy, true, predicates);

        pattern = pattern.within(Time.seconds(timeWindow));
        PatternStream<JSONObject> patternStream;
        if (enableKeyBy) {
            patternStream = org.apache.flink.cep.CEP.pattern(
                    timestampedStream.keyBy(obj -> obj.getString(keyName)),
                    pattern
            );
        } else {
            patternStream = org.apache.flink.cep.CEP.pattern(
                    timestampedStream,
                    pattern
            );
        }
//        PatternStream<JSONObject> patternStream = org.apache.flink.cep.CEP.pattern(timestampedStream.keyBy(obj -> obj.get("robotID")), pattern);
        return patternStream.select((PatternSelectFunction<JSONObject, JSONObject>) match -> {
            JSONArray eventsArray = new JSONArray();

            for (List<JSONObject> events : match.values()) {
                for (JSONObject event : events) {
                    if (event.has("matches")) {
                        JSONArray matches = event.getJSONArray("matches");
                        matches.forEach(eventsArray::put);
                        continue;
                    }
                    eventsArray.put(event);
                }
            }

            JSONObject res = new JSONObject();
            if (match.size() == 1 && match.values().iterator().next().size() == 1) {
                res = new JSONObject(match.values().iterator().next().get(0).toString()); // clone the original event
            } else {
                res = new JSONObject();
                res.put("event", patternName);
            }
            res.put("matches", eventsArray);
            return res;
        }).name(patternName);

    }

    private static Pattern<JSONObject, JSONObject> convertToFlinkCEP(Node node,
                                                                     String key,
                                                                     CEP.SelectionStrategy selectionStrategy,
                                                                     AfterMatchSkipStrategy consumptionPolicy,
                                                                     boolean isLast, Map<String, Map<String, String>> predicates) {
        if (node instanceof EventTypeNode) {
            return convertEventTypeNode((EventTypeNode) node, key, consumptionPolicy, isLast, predicates);
        } else if (node instanceof BackReferenceGroup) {
            return convertBackReferenceGroup((BackReferenceGroup) node, key, isLast, predicates);
        } else if (node instanceof OrNode) {
            return convertOrNode((OrNode) node, key, isLast, predicates);
        } else if (node instanceof QuantifierNode) {
            return convertQuantifierNode((QuantifierNode) node, key, selectionStrategy, consumptionPolicy, isLast, predicates);
        } else if (node instanceof GroupNode) {
            return convertGroupNode((GroupNode) node, key, selectionStrategy, consumptionPolicy, isLast, predicates);
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
        }
    }

    private static Pattern<JSONObject, JSONObject> convertEventTypeNode(EventTypeNode node, String key, AfterMatchSkipStrategy consumptionPolicy, boolean isLast, Map<String, Map<String, String>> predicates) {
        String eventType = String.valueOf(node.getValue());
        String label = isLast ? "Last" : "pattern" + uniqueId++;
        SimpleCondition<JSONObject> condition = new SimpleCondition<JSONObject>() {
            @Override
            public boolean filter(JSONObject obj) throws Exception {
                System.out.println("key: " + key + " eventType: " + eventType + " jsonObject " + obj);
                boolean mainCondition = obj.get(key).toString().startsWith(eventType);
                boolean predicateCondition = checkPredicates(obj, predicates);
                return mainCondition && predicateCondition;
            }
        };
        if (uniqueId == 1) {
            if (consumptionPolicy == null) return Pattern.<JSONObject>begin(label).where(condition);

            return Pattern.<JSONObject>begin(label, consumptionPolicy).where(condition);


        }
        return Pattern.<JSONObject>begin(label).where(condition);
    }

    private static Pattern<JSONObject, JSONObject> convertBackReferenceGroup(BackReferenceGroup node, String key, boolean isLast, Map<String, Map<String, String>> predicates) {
        int groupNum = node.getGroupNumber();
        String label = isLast ? "Last" : "pattern" + uniqueId++;
        String referencedLabel = "pattern" + (groupNum - 1);
        IterativeCondition<JSONObject> condition = new IterativeCondition<JSONObject>() {
            @Override
            public boolean filter(JSONObject jsonObject, Context<JSONObject> context) throws Exception {
                List<JSONObject> previousMatches = Lists.newArrayList(context.getEventsForPattern(referencedLabel));
                if (previousMatches != null) {
                    for (JSONObject event : previousMatches) {
                        if (jsonObject.get(key).equals(event.get(key))) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
        return Pattern.<JSONObject>begin(label).where(condition);
    }

    private static Pattern<JSONObject, JSONObject> convertOrNode(OrNode node, String key, boolean isLast, Map<String, Map<String, String>> predicates) {

        List<String> possibleValues = node.getChildren().stream()
                .map(child -> String.valueOf(child.getValue()))
                .collect(Collectors.toList());

        // 2) Extract the ranges (start-end) from RangeNode children
        //    and store them in a list of pairs or something similar.
        List<RangeNode> rangeChildren = node.getRangeChildren();  // or however you access them
        List<char[]> ranges = new ArrayList<>();
        for (RangeNode rangeNode : rangeChildren) {
            ranges.add(new char[]{rangeNode.getStart(), rangeNode.getEnd()});
        }
        Set<String> possibleSet = new HashSet<>(possibleValues);

        // 4) Define your condition referencing only these sets/lists
        SimpleCondition<JSONObject> condition = new SimpleCondition<JSONObject>() {
            @Override
            public boolean filter(JSONObject obj) throws Exception {
                // Use the local sets/lists, which *are* serializable
                String value = obj.getString(key);
                // Check if value is in the 'possibleSet'
                if (possibleSet.contains(value)) {
                    return true;
                }
                // Check if value is in any of the ranges
                if (!value.isEmpty()) {
                    char c = value.charAt(0);
                    for (char[] range : ranges) {
                        char start = range[0];
                        char end = range[1];
                        if (c >= start && c <= end) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };

        String label = isLast ? "Last" : "pattern" + uniqueId++;
        return Pattern.<JSONObject>begin(label).where(condition);
    }

    private static Pattern<JSONObject, JSONObject> convertQuantifierNode(
            QuantifierNode node,
            String key,
            CEP.SelectionStrategy selectionStrategy,
            AfterMatchSkipStrategy consumptionPolicy,
            boolean isLast,
            Map<String, Map<String, String>> predicates
    ) {
        // build the underlying child pattern
        Pattern<JSONObject, JSONObject> child = convertToFlinkCEP(
                node.getChild(), key, selectionStrategy, consumptionPolicy, isLast, predicates
        );

        Pattern<JSONObject, JSONObject> quantified;
        switch (node.getType()) {
            case ZEROORONE:
                // “?” → zero or one
                quantified = child.times(0,1);
                break;

            case ONEORMORE:
                // “+” → one or more
                quantified = child.oneOrMore();
                break;

            case KL:
                // “*” → zero or more
                quantified = child.oneOrMore().optional();
                break;

            case TIMES:
                int min = node.getMin();
                int max = node.getMax();
                if (max < 0) {
                    // {m,}
                    quantified = child.timesOrMore(min);
                } else {
                    // {m,n}
                    quantified = child.times(min, max);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown quantifier " + node.getType());
        }
        // apply STRICT / RELAXED / NON_DETERMINISTIC
        return applySelectionStrategy(quantified, selectionStrategy);
    }

    private static Pattern<JSONObject, JSONObject> convertGroupNode(GroupNode node,
                                                                    String key,
                                                                    CEP.SelectionStrategy selectionStrategy,
                                                                    AfterMatchSkipStrategy consumptionPolicy,
                                                                    boolean isLast, Map<String, Map<String, String>> predicates) {
        List<Node> children = node.getChildren();
        boolean isOptional = false;
        if (children.isEmpty()) {
            throw new IllegalArgumentException("GroupNode has no children");
        }
        Pattern<JSONObject, JSONObject> pattern = null;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            boolean childIsLast = (i == children.size() - 1) && isLast;
            Pattern<JSONObject, JSONObject> childPattern = convertToFlinkCEP(child, key, selectionStrategy, consumptionPolicy, childIsLast, predicates);
            if (childPattern.getQuantifier().hasProperty(Quantifier.QuantifierProperty.OPTIONAL)) isOptional = true;
            if (pattern == null) {
                pattern = isOptional ? childPattern.optional() : childPattern;
            } else {
                switch (selectionStrategy) {
                    case STRICT:
                        if (node.getLookAroundType() == LookAroundType.NEGATIVELOOKAHEAD) {
                            pattern = pattern.notNext(childPattern.getName());
                            break;
                        }
                        pattern = isOptional ? pattern.next(childPattern).optional() : pattern.next(childPattern);
                        break;
                    case RELAXED:
                        if (node.getLookAroundType() == LookAroundType.NEGATIVELOOKAHEAD) {
                            pattern = pattern.notFollowedBy(childPattern.getName()).where(childPattern.getCondition());
                            break;
                        }
                        pattern = isOptional ? pattern.followedBy(childPattern).optional() : pattern.followedBy(childPattern);
                        break;
                    case NON_DETERMINISTIC:
                        pattern = isOptional ? pattern.followedByAny(childPattern).optional() : pattern.followedByAny(childPattern);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid Selection Strategy: " + selectionStrategy.name());
                }
            }
        }
        return pattern;
    }

    private static Pattern<JSONObject, JSONObject> applySelectionStrategy(Pattern<JSONObject, JSONObject> pattern, CEP.SelectionStrategy selectionStrategy) {

        switch (selectionStrategy) {
            case STRICT:
                return pattern.consecutive();
            case RELAXED:
                return pattern.allowCombinations();
            case NON_DETERMINISTIC:
                //default is non deterministic
                return pattern;
            default:
                throw new IllegalArgumentException("Invalid Selection Strategy: " + selectionStrategy.name());
        }
    }

    private AfterMatchSkipStrategy getConsumptionStrategy(CEP.ConsumptionPolicy consumptionPolicy) {
        switch (consumptionPolicy) {
            case SKIP_TO_NEXT:
                return AfterMatchSkipStrategy.skipToNext();
            case NO_SKIP:
                return AfterMatchSkipStrategy.noSkip();
            case SKIP_TO_LAST:
                return AfterMatchSkipStrategy.skipToLast("Last");
            case SKIP_PAST_LAST_EVENT:
                return AfterMatchSkipStrategy.skipPastLastEvent();
            case NONE:
                return null;
            default:
                throw new IllegalArgumentException("Invalid Consumption Policy: " + consumptionPolicy.name());
        }
    }


    public FilterFunction<JSONObject> buildPredicate(Map<String, Map<String, String>> predicates) {
        return obj -> {
            for (Map.Entry<String, Map<String, String>> attributeEntry : predicates.entrySet()) {
                String key = attributeEntry.getKey();
                Map<String, String> conditions = attributeEntry.getValue();

                for (Map.Entry<String, String> condEntry : conditions.entrySet()) {
                    String condition = condEntry.getKey().toLowerCase();  // case-insensitive
                    String value = condEntry.getValue();

                    if (!evaluateCondition(obj, key, condition, value)) {
                        return false; // If any condition fails, reject the object
                    }
                }
            }
            return true; // All conditions passed
        };
    }

    private static boolean evaluateCondition(JSONObject obj, String key, String condition, String value) {
        if (!obj.has(key)) return false;

        switch (condition) {
            case "equal":
                return StringUtils.equals(obj.get(key).toString(), value);
            case "not equal":
                return !StringUtils.equals(obj.get(key).toString(), value);
            case "greater than":
                return obj.getDouble(key) > Double.parseDouble(value);
            case "greater than or equal":
                return obj.getDouble(key) >= Double.parseDouble(value);
            case "less than":
                return obj.getDouble(key) < Double.parseDouble(value);
            case "less than or equal":
                return obj.getDouble(key) <= Double.parseDouble(value);
            default:
                throw new IllegalArgumentException("Unsupported condition: " + condition);
        }
    }

    private static boolean checkPredicates(JSONObject obj, Map<String, Map<String, String>> predicates)
    {
        if (predicates == null) return true;
        for (Map.Entry<String, Map<String, String>> attributeEntry : predicates.entrySet()) {
            String key = attributeEntry.getKey();
            Map<String, String> conditions = attributeEntry.getValue();

            for (Map.Entry<String, String> condEntry : conditions.entrySet()) {
                String condition = condEntry.getKey().toLowerCase();  // case-insensitive
                String value = condEntry.getValue();

                if (!evaluateCondition(obj, key, condition, value)) {
                    return false; // If any condition fails, reject the object
                }
            }
        }
        return true;
    }
}
