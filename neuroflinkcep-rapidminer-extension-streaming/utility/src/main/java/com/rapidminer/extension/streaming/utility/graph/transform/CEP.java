package com.rapidminer.extension.streaming.utility.graph.transform;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CEP extends Transformer{

    public enum SelectionStrategy { STRICT, RELAXED, NON_DETERMINISTIC }
    public enum ConsumptionPolicy { NONE, NO_SKIP, SKIP_TO_NEXT, SKIP_TO_LAST, SKIP_PAST_LAST_EVENT }

    private static final Logger LOGGER = LoggerFactory.getLogger(CEP.class);
    private StreamProducer parent;
    private StreamConsumer child;
    private String directory;
    private Integer length;
    private Boolean isModelIncluded;
    private String patternName;
    private String regex;
    private String key;
    private Long timeWindow;
    private SelectionStrategy selectionStrategy;
    private ConsumptionPolicy consumptionPolicy;
    private Map<String, Map<String, String>> parsedPredicates;

    private Boolean enableKeyBy;
    private String keyName;
    private Boolean useLoadedModel;
    private String modelName;


    CEP() {
        super(-1);
    }

    CEP(CEP.Builder builder) {
        super(builder.graph.getNewId());
        this.parent = builder.parent;
        this.patternName = builder.patternName;
        this.regex = builder.regex;
        this.key = builder.key;
        this.timeWindow = builder.timeWindow;
        this.selectionStrategy = builder.selectionStrategy;
        this.consumptionPolicy = builder.consumptionPolicy;
        this.parsedPredicates = builder.parsedPredicates;
        this.directory = builder.directory;
        this.length = builder.length;
        this.isModelIncluded = builder.isModelIncluded;

        this.enableKeyBy = builder.enableKeyBy;
        this.keyName = builder.keyName;
        this.useLoadedModel = builder.useLoadedModel;
        this.modelName = builder.modelName;

    }

    public Map<String, Map<String, String>> getParsedPredicates() {
        return parsedPredicates;
    }

    public Boolean getEnableKeyBy() {
        return enableKeyBy;
    }

    public String getKeyName() {
        return keyName;
    }

    public Boolean getUseLoadedModel() {
        return useLoadedModel;
    }

    public String getModelName() {
        return modelName;
    }

    public void setParsedPredicates(Map<String, Map<String, String>> parsedPredicates) {
        this.parsedPredicates = parsedPredicates;
    }

    public String getDirectory() {
        return directory;
    }

    public Integer getLength() {
        return length;
    }

    public Boolean getModelIncluded() {
        return isModelIncluded;
    }

    public SelectionStrategy getSelectionStrategy() {
        return selectionStrategy;
    }

    public ConsumptionPolicy getConsumptionPolicy() {
        return consumptionPolicy;
    }

    public Long getTimeWindow() { return timeWindow; }

    public String getRegex() {
        return regex;
    }

    public String getKey() {
        return key;
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public StreamProducer getParent() {
        return parent;
    }

    public StreamConsumer getChild() {
        return child;
    }

    @Override
    public void registerChild(StreamConsumer child) {
        this.child  = child;
    }

    @Override
    public void accept(StreamGraphNodeVisitor visitor) {
        visitor.visit(this);
    }

    public static class Builder {

        private final StreamGraph graph;

        private Boolean isModelIncluded;
        private String patternName;
        private String regex;
        private String key;
        private Long timeWindow;
        private Integer length;
        private String directory;
        private StreamProducer parent;
        private SelectionStrategy selectionStrategy;
        private ConsumptionPolicy consumptionPolicy;
        private Map<String, Map<String, String>> parsedPredicates;

        private Boolean enableKeyBy;
        private String keyName;
        private Boolean useLoadedModel;
        private String modelName;

        public Builder(StreamGraph graph) {
            this.graph = graph;
        }

        public CEP build() {
            CEP node = new CEP(this);

            if (node.parent != null) {
                node.parent.registerChild(node);
            }

            return node;
        }

        public CEP.Builder withRegex(String regex) {
            this.regex = regex;
            return this;
        }

        public CEP.Builder withPredicates(Map<String, Map<String, String>> parsedPredicates) {
            this.parsedPredicates = parsedPredicates;
            return this;
        }

        public CEP.Builder withKey(String key){
            this.key = key;
            return this;
        }

        public CEP.Builder withTimeWindow(Long timeWindow){
            this.timeWindow = timeWindow;
            return this;
        }
        public CEP.Builder withPatternName(String patternName){
            this.patternName = patternName;
            return this;
        }
        public CEP.Builder withSelectionStrategy(SelectionStrategy selectionStrategy){
            this.selectionStrategy = selectionStrategy;
            return this;
        }
        public CEP.Builder withConsumptionPolicy(ConsumptionPolicy consumptionPolicy){
            this.consumptionPolicy = consumptionPolicy;
            return this;
        }

        public CEP.Builder withModelDirectory(String directory) {
            this.directory = directory;
            return this;
        }
        public CEP.Builder withModelInputLength(int length) {
            this.length = length;
            return this;
        }
        public CEP.Builder withParent(StreamProducer parent) {
            this.parent = parent;
            return this;
        }

        public CEP.Builder withIncludeModelFlag (boolean isModelIncluded){
            this.isModelIncluded = isModelIncluded;
            return this;
        }

        public CEP.Builder withEnableKeyBy(Boolean enableKeyBy) {
            this.enableKeyBy = enableKeyBy;
            return this;
        }
        public CEP.Builder withKeyName(String keyName){
            this.keyName = keyName;
            return this;
        }
        public CEP.Builder withUseLoadedModel(boolean useLoadedModel){
            this.useLoadedModel = useLoadedModel;
            return this;
        }
        public CEP.Builder withModelName(String modelName){
            this.modelName = modelName;
            return this;
        }

    }

}
