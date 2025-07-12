package spark;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "action",
        "appResource",
        "clientSparkVersion",
        "appArgs",
        "environmentVariables",
        "mainClass",
        "sparkProperties"
})
public class SparkRequestData implements Serializable {

    private final static long serialVersionUID = 7555460285855962501L;
    @JsonProperty("action")
    private String action;
    @JsonProperty("appResource")
    private String appResource;
    @JsonProperty("clientSparkVersion")
    private String clientSparkVersion;
    @JsonProperty("appArgs")
    private List<String> appArgs = null;
    @JsonProperty("environmentVariables")
    private EnvironmentVariables environmentVariables;
    @JsonProperty("mainClass")
    private String mainClass;
    @JsonProperty("sparkProperties")
    private SparkProperties sparkProperties;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("action")
    public String getAction() {
        return action;
    }

    @JsonProperty("action")
    public void setAction(String action) {
        this.action = action;
    }

    public SparkRequestData withAction(String action) {
        this.action = action;
        return this;
    }

    @JsonProperty("appResource")
    public String getAppResource() {
        return appResource;
    }

    @JsonProperty("appResource")
    public void setAppResource(String appResource) {
        this.appResource = appResource;
    }

    public SparkRequestData withAppResource(String appResource) {
        this.appResource = appResource;
        return this;
    }

    @JsonProperty("clientSparkVersion")
    public String getClientSparkVersion() {
        return clientSparkVersion;
    }

    @JsonProperty("clientSparkVersion")
    public void setClientSparkVersion(String clientSparkVersion) {
        this.clientSparkVersion = clientSparkVersion;
    }

    public SparkRequestData withClientSparkVersion(String clientSparkVersion) {
        this.clientSparkVersion = clientSparkVersion;
        return this;
    }

    @JsonProperty("appArgs")
    public List<String> getAppArgs() {
        return appArgs;
    }

    @JsonProperty("appArgs")
    public void setAppArgs(List<String> appArgs) {
        this.appArgs = appArgs;
    }

    public SparkRequestData withAppArgs(List<String> appArgs) {
        this.appArgs = appArgs;
        return this;
    }

    @JsonProperty("environmentVariables")
    public EnvironmentVariables getEnvironmentVariables() {
        return environmentVariables;
    }

    @JsonProperty("environmentVariables")
    public void setEnvironmentVariables(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public SparkRequestData withEnvironmentVariables(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        return this;
    }

    @JsonProperty("mainClass")
    public String getMainClass() {
        return mainClass;
    }

    @JsonProperty("mainClass")
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public SparkRequestData withMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    @JsonProperty("sparkProperties")
    public SparkProperties getSparkProperties() {
        return sparkProperties;
    }

    @JsonProperty("sparkProperties")
    public void setSparkProperties(SparkProperties sparkProperties) {
        this.sparkProperties = sparkProperties;
    }

    public SparkRequestData withSparkProperties(SparkProperties sparkProperties) {
        this.sparkProperties = sparkProperties;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public SparkRequestData withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("action", action).append("appResource", appResource).append("clientSparkVersion", clientSparkVersion).append("appArgs", appArgs).append("environmentVariables", environmentVariables).append("mainClass", mainClass).append("sparkProperties", sparkProperties).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SparkRequestData) == false) {
            return false;
        }
        SparkRequestData rhs = ((SparkRequestData) other);
        return new EqualsBuilder().append(clientSparkVersion, rhs.clientSparkVersion).append(additionalProperties, rhs.additionalProperties).append(mainClass, rhs.mainClass).append(appArgs, rhs.appArgs).append(appResource, rhs.appResource).append(action, rhs.action).append(sparkProperties, rhs.sparkProperties).append(environmentVariables, rhs.environmentVariables).isEquals();
    }

}
