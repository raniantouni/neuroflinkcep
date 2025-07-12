package core.parser.workflow;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.persistence.ElementCollection;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

public class OptimizationRequest implements Serializable {

    private final static long serialVersionUID = -1871235623182336168L;

    @SerializedName("workflowName")
    @Expose
    @javax.persistence.Id
    @NotEmpty(message = "workflowName must not be empty")
    private String workflowName;

    @SerializedName("enclosingOperatorName")
    @Expose
    @NotEmpty(message = "enclosingOperatorName must not be empty")
    private String enclosingOperatorName;

    @SerializedName("innerSourcesPortsAndSchemas")
    @Expose
    @NotNull(message = "InnerSourcesPortsAndSchema field must be present (can be empty)")
    @ElementCollection
    private List<@Valid InnerSourcesPortsAndSchema> innerSourcesPortsAndSchemas = null;

    @SerializedName("innerSinksPortsAndSchemas")
    @Expose
    @NotNull(message = "innerSinksPortsAndSchemas field must be present (can be empty)")
    @ElementCollection
    private List<@Valid InnerSinksPortsAndSchema> innerSinksPortsAndSchemas = null;

    @SerializedName("operatorConnections")
    @Expose
    @NotNull(message = "operatorConnections field must be present (can be empty)")
    @ElementCollection
    private List<@Valid OperatorConnection> operatorConnections = null;

    @SerializedName("operators")
    @Expose
    @NotEmpty(message = "operators must not be empty")
    @ElementCollection
    private List<@Valid Operator> operators = null;

    @SerializedName("placementSites")
    @Expose
    @NotNull(message = "placementSites field must be present (can be empty)")
    @ElementCollection
    private List<@Valid PlacementSite> placementSites = null;

    @SerializedName("optimizationParameters")
    @Expose
    @NotNull(message = "A list of optimization parameters is missing (use JSON map 'OptimizationParameters').")
    private @Valid OptimizationParameters optimizationParameters;

    /**
     * No args constructor for use in serialization
     */
    public OptimizationRequest() {
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getEnclosingOperatorName() {
        return enclosingOperatorName;
    }

    public List<InnerSourcesPortsAndSchema> getInnerSourcesPortsAndSchemas() {
        return innerSourcesPortsAndSchemas;
    }

    public List<InnerSinksPortsAndSchema> getInnerSinksPortsAndSchemas() {
        return innerSinksPortsAndSchemas;
    }

    public List<OperatorConnection> getOperatorConnections() {
        return operatorConnections;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public List<PlacementSite> getPlacementSites() {
        return placementSites;
    }

    public OptimizationParameters getOptimizationParameters() {
        return optimizationParameters;
    }

    public void setPlacementSites(List<PlacementSite> placementSites) {
        this.placementSites = placementSites;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public void setEnclosingOperatorName(String enclosingOperatorName) {
        this.enclosingOperatorName = enclosingOperatorName;
    }

    public void setInnerSourcesPortsAndSchemas(List<InnerSourcesPortsAndSchema> innerSourcesPortsAndSchemas) {
        this.innerSourcesPortsAndSchemas = innerSourcesPortsAndSchemas;
    }

    public void setInnerSinksPortsAndSchemas(List<InnerSinksPortsAndSchema> innerSinksPortsAndSchemas) {
        this.innerSinksPortsAndSchemas = innerSinksPortsAndSchemas;
    }

    public void setOperatorConnections(List<OperatorConnection> operatorConnections) {
        this.operatorConnections = operatorConnections;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    public void setOptimizationParameters(OptimizationParameters optimizationParameters) {
        this.optimizationParameters = optimizationParameters;
    }
}
