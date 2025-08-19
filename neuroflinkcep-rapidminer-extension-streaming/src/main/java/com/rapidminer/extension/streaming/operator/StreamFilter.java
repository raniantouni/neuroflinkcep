/**
 * RapidMiner Streaming Extension
 * <p>
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.FilterTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.*;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;


/**
 * Filter operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamFilter extends AbstractStreamTransformOperator {
    private static final Logger logger = LogService.getRoot();
    private static final String NeuroFlinkCEP = "NeuroFlinkCEP_filtering_mode";
    private static final String PARAMETER_KEY = "key";
    private static final String PARAMETER_PREDICATES = "predicates";
    private static final String PARAMETER_OPERATOR = "operator";

    private static final String PARAMETER_VALUE = "value";

    private static final Map<String, FilterTransformer.Operator> OPERATOR_MAP = buildOperatorMap();

    public StreamFilter(OperatorDescription description) {
        super(description);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        ParameterType neuroFlinkCEPMode = new ParameterTypeBoolean(
                NeuroFlinkCEP,
                "Enabled when is about early filtering mode",
                false

        );
        types.add(neuroFlinkCEPMode);

        ParameterCondition neuroFlinkCEPCondition = new BooleanParameterCondition(
                this,
                NeuroFlinkCEP,
                true,
                true
        );
        ParameterCondition NOTneuroFlinkCEPCondition = new BooleanParameterCondition(
                this,
                NeuroFlinkCEP,
                true,
                false
        );

        ParameterType predicates = new ParameterTypeEnumeration(
                PARAMETER_PREDICATES,
                "Defines a list of predicates to apply filtering logic on attributes of the incoming ExampleSet.",
                new ParameterTypeTupel(
                        "Predicate",  // Name for each predicate entry
                        "A predicate defines a condition that will be applied to a specific attribute.",

                        // Attribute name to which the predicate will be applied
                        new ParameterTypeString(
                                "attribute_name",
                                "The name of the attribute from the incoming data on which the predicate will be applied.",
                                true
                        ),

                        // Enumeration of conditions for the attribute
                        new ParameterTypeEnumeration(
                                "conditions",
                                "A list of condition rules that apply to the specified attribute (e.g., per platform).",

                                // Each condition: operator + value
                                new ParameterTypeTupel(
                                        "condition",
                                        "A condition specifying the comparison operator and the target value.",

                                        new ParameterTypeCategory(
                                                "operator",
                                                "The comparison operator used to evaluate the condition.",
                                                new String[]{
                                                        "greater than",
                                                        "greater than or equal",
                                                        "less than",
                                                        "less than or equal",
                                                        "equal",
                                                        "not equal"
                                                },
                                                0
                                        ),

                                        new ParameterTypeString(
                                                "value",
                                                "The value to be compared against the attribute using the specified operator."
                                        )
                                )
                        )
                )
        );
        predicates.registerDependencyCondition(neuroFlinkCEPCondition);
        types.add(predicates);

        ParameterType key = new ParameterTypeString(
                PARAMETER_KEY,
                "Key of the value to be evaluated in the filter predicate.",
                true);
        key.registerDependencyCondition(NOTneuroFlinkCEPCondition);
        types.add(key);

        ParameterType value = new ParameterTypeString(
                PARAMETER_VALUE,
                "Right-value (operand) of the filter predicate.",
                true);
        value.registerDependencyCondition(NOTneuroFlinkCEPCondition);
        types.add(value);

        ParameterType function = new ParameterTypeCategory(
                PARAMETER_OPERATOR,
                "Operator to be used when evaluating the filter predicate.",
                OPERATOR_MAP.keySet().toArray(new String[]{}),
                0);
        function.registerDependencyCondition(NOTneuroFlinkCEPCondition);
        types.add(function);
        return types;
    }

    @Override
    public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
        List<String> rawPredicates = ParameterTypeEnumeration.transformString2List(
                getParameterAsString(PARAMETER_PREDICATES));

        if (!isParameterSet(PARAMETER_KEY)) setParameter(PARAMETER_KEY, "");

        if (!isParameterSet(PARAMETER_VALUE)) setParameter(PARAMETER_VALUE, "");


        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String line : rawPredicates) {
            String[] tupel = ParameterTypeTupel.transformString2Tupel(line);
            String attributeName = tupel[0];
            List<String> predicatesPerAttribute = ParameterTypeEnumeration.transformString2List(tupel[1]);
            Map<String, String> predicateMap = new HashMap<>();
            for (String predicate : predicatesPerAttribute) {
                String[] predicateTupel = ParameterTypeTupel.transformString2Tupel(predicate);

                predicateMap.put(predicateTupel[0], predicateTupel[1]);
                LOGGER.info("Adding " + attributeName + " to " + predicateTupel[0] + " of " + predicateTupel[1]);
            }

            result.put(attributeName, predicateMap);
        }




        return new FilterTransformer.Builder(graph)
                .withKey(getParameterAsString(PARAMETER_KEY))
                .withOperator(OPERATOR_MAP.get(getParameterAsString(PARAMETER_OPERATOR)))
                .withValue(getParameterAsString(PARAMETER_VALUE))
                .withParent(inData.getLastNode())
                .build();
    }

    /**
     * @return newly built mapping between operator names and actual operator objects (enums)
     */
    private static Map<String, FilterTransformer.Operator> buildOperatorMap() {
        return new ImmutableMap.Builder<String, FilterTransformer.Operator>()
                .put("Equal to", FilterTransformer.Operator.EQUAL)
                .put("Not equal to", FilterTransformer.Operator.NOT_EQUAL)
                .put("Greater than", FilterTransformer.Operator.GREATER_THAN)
                .put("Greater than or equal to", FilterTransformer.Operator.GREATER_THAN_OR_EQUAL)
                .put("Less than", FilterTransformer.Operator.LESS_THAN)
                .put("Less than or equal to", FilterTransformer.Operator.LESS_THAN_OR_EQUAL)
                .put("String starts with", FilterTransformer.Operator.STARTS_WITH)
                .build();
    }

}