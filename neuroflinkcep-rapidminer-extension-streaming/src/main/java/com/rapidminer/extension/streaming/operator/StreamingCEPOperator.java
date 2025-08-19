package com.rapidminer.extension.streaming.operator;

import com.google.common.collect.ImmutableMap;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.CEP;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.*;
import com.rapidminer.parameter.conditions.AndParameterCondition;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import java.util.*;
import java.util.logging.Logger;

public class StreamingCEPOperator extends AbstractStreamTransformOperator {
    private static final Logger LOGGER = LogService.getRoot();
    private static final String PARAMETER_PATTERN_NAME = "Pattern_Name";
    private static final String PARAMETER_EVENT_KEY = "Event_Key";
    private static final String PARAMETER_REGEX = "Regular_Expression";
    private static final String PARAMETER_TIME_WINDOW = "Time_Window";

    private static final String PARAMETER_KEY_BY_FLAG = "Enable_key_by";
    private static final String PARAMETER_KEY_BY_ATTRIBUTE = "Key_name";

    private static final String PARAMETER_KNOWN_MODEL_FLAG = "Use_loaded_model";
    private static final String PARAMETER_MODEL_NAME = "Model_name";

    private static final String PARAMETER_SELECTION_STRATEGY = "Selection_Strategy";
    private static final String PARAMETER_CONSUMPTION_POLICY = "Consumption_Policy";
    private static final Map<String, CEP.SelectionStrategy> SELECTION_STRATEGY_OPTIONS = buildSelectionStrategyMap();
    private static final Map<String, CEP.ConsumptionPolicy> CONSUMPTION_POLICY_OPTIONS = buildConsumptionPolicyMap();
    private static final String PARAMETER_DIRECTORY_PATH = "Directory_Path";
    private static final String PARAMETER_PREDICATES = "Predicates_List";
    private static final String PARAMETER_DECOMPOSITIONS = "decompositions";
    private static final String PARAMETER_TELECOM_EVENTS = "Recognise_telecom_events";

    public StreamingCEPOperator(OperatorDescription description) {
        super(description);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType name = new ParameterTypeString(
                PARAMETER_PATTERN_NAME,
                "Name of the pattern",
                false);
        types.add(name);

        ParameterType key = new ParameterTypeString(
                PARAMETER_EVENT_KEY,
                "Key of the event",
                true);
        types.add(key);
        ParameterType regex = new ParameterTypeString(
                PARAMETER_REGEX,
                "Regex that describes the pattern",
                false);
        types.add(regex);

        ParameterType enableKeyBy = new ParameterTypeBoolean(
                PARAMETER_KEY_BY_FLAG,
                "Apply CEP in keyed stream.",
                false
        );
        types.add(enableKeyBy);

        ParameterCondition keyByCondition = new BooleanParameterCondition(
                this,
                PARAMETER_KEY_BY_FLAG,
                true,
                true
        );

        ParameterType keyField = new ParameterTypeString(
                PARAMETER_KEY_BY_ATTRIBUTE,
                "Attribute name thay the key by will be applied.",
                true
        );
        keyField.registerDependencyCondition(keyByCondition);
        types.add(keyField);

        ParameterType window = new ParameterTypeLong(
                PARAMETER_TIME_WINDOW,
                "Time Window in seconds",
                0,
                Long.MAX_VALUE,
                false);
        types.add(window);

        ParameterType selectionStrategy = new ParameterTypeCategory(
                PARAMETER_SELECTION_STRATEGY,
                "Choose Selection Strategy",
                SELECTION_STRATEGY_OPTIONS.keySet().toArray(new String[]{}),
                1
        );
        types.add(selectionStrategy);

        ParameterType consumptionPolicy = new ParameterTypeCategory(
                PARAMETER_CONSUMPTION_POLICY,
                "Choose Consumption Policy",
                CONSUMPTION_POLICY_OPTIONS.keySet().toArray(new String[]{}),
                0
        );
        types.add(consumptionPolicy);
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
                                                        "equal"
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

        types.add(predicates);

        ParameterTypeBoolean isModelIncluded = new ParameterTypeBoolean(
                "Include_Model",
                "If this field is true the model for prediction is enabled",
                true
        );
        types.add(isModelIncluded);
        ParameterCondition modelCondition = new BooleanParameterCondition(
                this,
                "Include_Model",
                true,
                true
        );
        ParameterType knownModelFlag = new ParameterTypeBoolean(
                PARAMETER_KNOWN_MODEL_FLAG,
                "Use one of the already loaded models {Robotic Sceanrio, Telecom Scenario}",
                true
        );
        knownModelFlag.registerDependencyCondition(modelCondition);
        types.add(knownModelFlag);

        ParameterCondition knownModelCondition = new BooleanParameterCondition(
                this,
                PARAMETER_KNOWN_MODEL_FLAG,
                true,
                true
        );

        ParameterCondition notKnownModelCondition = new BooleanParameterCondition(
                this,
                PARAMETER_KNOWN_MODEL_FLAG,
                true,
                false
        );

        ParameterCondition includeModelAndNotKnownModelCondition = new AndParameterCondition(
                this,
                true,
                notKnownModelCondition,
                modelCondition
        );

        ParameterType modelNames = new ParameterTypeCategory(
                PARAMETER_MODEL_NAME,
                "Choose one of the already loaded models {Robotic Sceanrio, Telecom Scenario}.",
                new String[]{"Robotic Scenario", "Telecom Scenario"},
                0
        );
        modelNames.registerDependencyCondition(knownModelCondition);
        types.add(modelNames);

        ParameterCondition telecomScenario = new EqualTypeCondition(this, PARAMETER_MODEL_NAME, new String[]{"Telecom Scenario"}, true);

        ParameterType generateEvents = new ParameterTypeBoolean(PARAMETER_TELECOM_EVENTS,
                "Recognize simple events type { E, F, G }",
                false);
        generateEvents.registerDependencyCondition(telecomScenario);
        types.add(generateEvents);

        ParameterTypeDirectory directoryParameter = new ParameterTypeDirectory(
                PARAMETER_DIRECTORY_PATH,
                "The directory path where the model is stored.",
                true
        );
        directoryParameter.registerDependencyCondition(notKnownModelCondition);
        types.add(directoryParameter);

        ParameterTypeInt modelInputSize = new ParameterTypeInt(
                "Input_length",
                "Input length of the trained model.",
                1,
                Integer.MAX_VALUE,
                true
        );
        modelInputSize.registerDependencyCondition(notKnownModelCondition);
        types.add(modelInputSize);

        // -------------------------  hidden decision parameters, after optimizer are not hidden -----------------------
        ParameterType earlyFiltering = new ParameterTypeBoolean("Early Filtering",
                "Early filtering flag. In Strict contiguity is always set to false even if the user selected true.",
                false
        );

        earlyFiltering.setHidden(true);
        types.add(earlyFiltering);

        ParameterType reordering = new ParameterTypeBoolean("Reordering",
                "Reordering flag. In Strict contiguity is always set to false even if the user selected true.",
                false
        );
        reordering.setHidden(true);
        types.add(reordering);

        ParameterType pushingPredicates = new ParameterTypeBoolean("Pushing Predicates upstream",
                "Pushing predicates upstream flag. In Strict contiguity and robotic scenario cases, is always set to false even if the user selected true.",
                false
        );
        pushingPredicates.setHidden(true);
        types.add(pushingPredicates);

        ParameterType r = new ParameterTypeList(
          "splits",
                "d",
                new ParameterTypeString("d","D"));
        r.setHidden(true);
        types.add(r);


        ParameterType splits = new ParameterTypeString(PARAMETER_DECOMPOSITIONS, "splits", true);
        splits.setHidden(true);
        types.add(splits);



        return types;
    }

    @Override
    protected Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
        List<String> rawPredicates = ParameterTypeEnumeration.transformString2List(
                getParameterAsString(PARAMETER_PREDICATES));

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
        String directoryPath = "";
        if (isParameterSet(PARAMETER_DIRECTORY_PATH)) directoryPath = getParameterAsString(PARAMETER_DIRECTORY_PATH);

        int modelInputLength = 0;
        if (isParameterSet("Input_length")) modelInputLength = getParameterAsInt("Input_length");

        boolean keyByFlag = false;
        if (isParameterSet(PARAMETER_KEY_BY_FLAG)) {
            LOGGER.info("key is set to " + keyByFlag);
            keyByFlag = getParameterAsBoolean(PARAMETER_KEY_BY_FLAG);
        }

        String keyName = "";
        if (isParameterSet(PARAMETER_KEY_BY_ATTRIBUTE)) keyName = getParameterAsString(PARAMETER_KEY_BY_ATTRIBUTE);

        boolean useLoadedModel = false;
        if (isParameterSet(PARAMETER_KNOWN_MODEL_FLAG)) useLoadedModel = getParameterAsBoolean(PARAMETER_KNOWN_MODEL_FLAG);

        String modelName = "";
        if (isParameterSet(PARAMETER_MODEL_NAME)) modelName = getParameterAsString(PARAMETER_MODEL_NAME);

        return new CEP.Builder(graph)
                .withPatternName(getParameterAsString(PARAMETER_PATTERN_NAME))
                .withKey(getParameterAsString(PARAMETER_EVENT_KEY))
                .withRegex(getParameterAsString(PARAMETER_REGEX))
                .withTimeWindow(getParameterAsLong(PARAMETER_TIME_WINDOW))
                .withSelectionStrategy(SELECTION_STRATEGY_OPTIONS.get(getParameterAsString(PARAMETER_SELECTION_STRATEGY)))
                .withConsumptionPolicy(CONSUMPTION_POLICY_OPTIONS.get(getParameterAsString(PARAMETER_CONSUMPTION_POLICY)))
                .withPredicates(result)
                .withParent(inData.getLastNode())
                .withModelDirectory(directoryPath)
                .withModelInputLength(modelInputLength)
                .withIncludeModelFlag(getParameterAsBoolean("Include_Model"))
                .withEnableKeyBy(keyByFlag)
                .withKeyName(keyName)
                .withUseLoadedModel(useLoadedModel)
                .withModelName(modelName)
                .withEarlyFiltering(getParameterAsBoolean("Early Filtering"))
                .withPushingPredicates(getParameterAsBoolean("Pushing Predicates Upstream"))
                .withReordering(getParameterAsBoolean("Reordering"))
                .build();
    }

    private static Map<String, CEP.SelectionStrategy> buildSelectionStrategyMap() {
        return new ImmutableMap.Builder<String, CEP.SelectionStrategy>()
                .put("Strict", CEP.SelectionStrategy.STRICT)
                .put("Relaxed", CEP.SelectionStrategy.RELAXED)
                .put("Non-Deterministic", CEP.SelectionStrategy.NON_DETERMINISTIC)
                .build();
    }

    private static Map<String, CEP.ConsumptionPolicy> buildConsumptionPolicyMap() {
        return new ImmutableMap.Builder<String, CEP.ConsumptionPolicy>()
                .put("None", CEP.ConsumptionPolicy.NONE)
                .put("No Skip", CEP.ConsumptionPolicy.NO_SKIP)
                .put("Skip To Next", CEP.ConsumptionPolicy.SKIP_TO_NEXT)
                .put("Skip To Last", CEP.ConsumptionPolicy.SKIP_TO_LAST)
                .put("Skip Past Last Event", CEP.ConsumptionPolicy.SKIP_PAST_LAST_EVENT)
                .build();
    }
}
