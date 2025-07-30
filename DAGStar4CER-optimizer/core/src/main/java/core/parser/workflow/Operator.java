package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Operator implements Serializable {

    private final static long serialVersionUID = -8770035276029489408L;
    @SerializedName("name")
    @Expose
    @NotEmpty(message = "name must not be empty")
    private String name;

    @SerializedName("classKey")
    @Expose
    @NotEmpty(message = "classKey must not be empty")
    private String classKey;    // Use this to optimize the workflow

    @SerializedName("operatorClass")
    @Expose
    private String operatorClass;
    @SerializedName("isEnabled")
    @Expose
    private boolean isEnabled;
    @SerializedName("inputPortsAndSchemas")
    @Expose
    private List<@Valid InputPortsAndSchema> inputPortsAndSchemas = null;
    @SerializedName("outputPortsAndSchemas")
    @Expose
    private List<@Valid OutputPortsAndSchema> outputPortsAndSchemas = null;
    @SerializedName("parameters")
    @Expose
    private List<@Valid Parameter> parameters = null;
    @SerializedName("hasSubprocesses")
    @Expose
    private boolean hasSubprocesses;
    @SerializedName("numberOfSubprocesses")
    @Expose
    private int numberOfSubprocesses;
    @SerializedName("innerWorkflows")
    @Expose
    private List<@Valid OptimizationRequest> innerWorkflows = null;

    private transient boolean isAuxiliary = false;

    /**
     * No args constructor for use in serialization
     */
    public Operator() {
    }

    //Source and sink constructor
    public Operator(String name) {
        this.name = name;
        this.classKey = "";
        this.operatorClass = "";
    }

    public Operator(String name, String classKey, String operatorClass, boolean isEnabled, List<InputPortsAndSchema> inputPortsAndSchemas, List<OutputPortsAndSchema> outputPortsAndSchemas, List<Parameter> parameters, boolean hasSubprocesses, int numberOfSubprocesses, List<OptimizationRequest> innerWorkflows) {
        super();
        this.name = name;
        this.classKey = classKey;
        this.operatorClass = operatorClass;
        this.isEnabled = isEnabled;
        this.inputPortsAndSchemas = inputPortsAndSchemas;
        this.outputPortsAndSchemas = outputPortsAndSchemas;
        this.parameters = parameters;
        this.hasSubprocesses = hasSubprocesses;
        this.numberOfSubprocesses = numberOfSubprocesses;
        this.innerWorkflows = innerWorkflows;
    }

    public boolean isAuxiliary() {
        return isAuxiliary;
    }

    //Getters, setters and builder methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Operator withName(String name) {
        this.name = name;
        return this;
    }

    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String classKey) {
        this.classKey = classKey;
    }

    public Operator withClassKey(String classKey) {
        this.classKey = classKey;
        return this;
    }

    public String getOperatorClass() {
        return operatorClass;
    }

    public void setOperatorClass(String operatorClass) {
        this.operatorClass = operatorClass;
    }

    public Operator withOperatorClass(String operatorClass) {
        this.operatorClass = operatorClass;
        return this;
    }

    public boolean isIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Operator withIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        return this;
    }

    public List<InputPortsAndSchema> getInputPortsAndSchemas() {
        return inputPortsAndSchemas;
    }

    public void setInputPortsAndSchemas(List<InputPortsAndSchema> inputPortsAndSchemas) {
        this.inputPortsAndSchemas = inputPortsAndSchemas;
    }

    public Operator withInputPortsAndSchemas(List<InputPortsAndSchema> inputPortsAndSchemas) {
        this.inputPortsAndSchemas = inputPortsAndSchemas;
        return this;
    }

    public List<OutputPortsAndSchema> getOutputPortsAndSchemas() {
        return outputPortsAndSchemas;
    }

    public void setOutputPortsAndSchemas(List<OutputPortsAndSchema> outputPortsAndSchemas) {
        this.outputPortsAndSchemas = outputPortsAndSchemas;
    }

    public Operator withOutputPortsAndSchemas(List<OutputPortsAndSchema> outputPortsAndSchemas) {
        this.outputPortsAndSchemas = outputPortsAndSchemas;
        return this;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Operator withParameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public boolean isHasSubprocesses() {
        return hasSubprocesses;
    }

    public void setHasSubprocesses(boolean hasSubprocesses) {
        this.hasSubprocesses = hasSubprocesses;
    }

    public Operator withHasSubprocesses(boolean hasSubprocesses) {
        this.hasSubprocesses = hasSubprocesses;
        return this;
    }

    public Operator withAuxiliary(boolean isAuxiliary) {
        this.isAuxiliary = isAuxiliary;
        return this;
    }

    public int getNumberOfSubprocesses() {
        return numberOfSubprocesses;
    }

    public void setNumberOfSubprocesses(int numberOfSubprocesses) {
        this.numberOfSubprocesses = numberOfSubprocesses;
    }

    public Operator withNumberOfSubprocesses(int numberOfSubprocesses) {
        this.numberOfSubprocesses = numberOfSubprocesses;
        return this;
    }

    public List<OptimizationRequest> getInnerWorkflows() {
        return innerWorkflows;
    }

    public void setInnerWorkflows(List<OptimizationRequest> innerWorkflows) {
        this.innerWorkflows = innerWorkflows;
    }

    public Operator withInnerWorkflows(List<OptimizationRequest> innerWorkflows) {
        this.innerWorkflows = innerWorkflows;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operator operator = (Operator) o;
        return this.getName().equals(operator.getName());
//        return isEnabled == operator.isEnabled &&
//                hasSubprocesses == operator.hasSubprocesses &&
//                numberOfSubprocesses == operator.numberOfSubprocesses &&
//                Objects.equals(name, operator.name) &&
//                Objects.equals(classKey, operator.classKey) &&
//                Objects.equals(operatorClass, operator.operatorClass) &&
//                Objects.equals(inputPortsAndSchemas, operator.inputPortsAndSchemas) &&
//                Objects.equals(outputPortsAndSchemas, operator.outputPortsAndSchemas) &&
//                Objects.equals(parameters, operator.parameters) &&
//                Objects.equals(innerWorkflows, operator.innerWorkflows);
    }

    @Override
    public int hashCode() {
        //return Objects.hash(name, classKey, operatorClass, isEnabled, inputPortsAndSchemas, outputPortsAndSchemas, parameters, hasSubprocesses, numberOfSubprocesses, innerWorkflows);
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
