package spark;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "spark.jars",
        "spark.driver.supervise",
        "spark.executor.memory",
        "spark.driver.memory",
        "spark.submit.deployMode",
        "spark.app.name",
        "spark.master"
})
public class SparkProperties implements Serializable {

    private final static long serialVersionUID = 3492781435431379418L;
    @JsonProperty("spark.jars")
    private String sparkJars;
    @JsonProperty("spark.driver.supervise")
    private String sparkDriverSupervise;
    @JsonProperty("spark.executor.memory")
    private String sparkExecutorMemory;
    @JsonProperty("spark.driver.memory")
    private String sparkDriverMemory;
    @JsonProperty("spark.submit.deployMode")
    private String sparkSubmitDeployMode;
    @JsonProperty("spark.app.name")
    private String sparkAppName;
    @JsonProperty("spark.master")
    private String sparkMaster;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("spark.jars")
    public String getSparkJars() {
        return sparkJars;
    }

    @JsonProperty("spark.jars")
    public void setSparkJars(String sparkJars) {
        this.sparkJars = sparkJars;
    }

    public SparkProperties withSparkJars(String sparkJars) {
        this.sparkJars = sparkJars;
        return this;
    }

    @JsonProperty("spark.driver.supervise")
    public String getSparkDriverSupervise() {
        return sparkDriverSupervise;
    }

    @JsonProperty("spark.driver.supervise")
    public void setSparkDriverSupervise(String sparkDriverSupervise) {
        this.sparkDriverSupervise = sparkDriverSupervise;
    }

    public SparkProperties withSparkDriverSupervise(String sparkDriverSupervise) {
        this.sparkDriverSupervise = sparkDriverSupervise;
        return this;
    }

    @JsonProperty("spark.executor.memory")
    public String getSparkExecutorMemory() {
        return sparkExecutorMemory;
    }

    @JsonProperty("spark.executor.memory")
    public void setSparkExecutorMemory(String sparkExecutorMemory) {
        this.sparkExecutorMemory = sparkExecutorMemory;
    }

    public SparkProperties withSparkExecutorMemory(String sparkExecutorMemory) {
        this.sparkExecutorMemory = sparkExecutorMemory;
        return this;
    }

    @JsonProperty("spark.driver.memory")
    public String getSparkDriverMemory() {
        return sparkDriverMemory;
    }

    @JsonProperty("spark.driver.memory")
    public void setSparkDriverMemory(String sparkDriverMemory) {
        this.sparkDriverMemory = sparkDriverMemory;
    }

    public SparkProperties withSparkDriverMemory(String sparkDriverMemory) {
        this.sparkDriverMemory = sparkDriverMemory;
        return this;
    }

    @JsonProperty("spark.submit.deployMode")
    public String getSparkSubmitDeployMode() {
        return sparkSubmitDeployMode;
    }

    @JsonProperty("spark.submit.deployMode")
    public void setSparkSubmitDeployMode(String sparkSubmitDeployMode) {
        this.sparkSubmitDeployMode = sparkSubmitDeployMode;
    }

    public SparkProperties withSparkSubmitDeployMode(String sparkSubmitDeployMode) {
        this.sparkSubmitDeployMode = sparkSubmitDeployMode;
        return this;
    }

    @JsonProperty("spark.app.name")
    public String getSparkAppName() {
        return sparkAppName;
    }

    @JsonProperty("spark.app.name")
    public void setSparkAppName(String sparkAppName) {
        this.sparkAppName = sparkAppName;
    }

    public SparkProperties withSparkAppName(String sparkAppName) {
        this.sparkAppName = sparkAppName;
        return this;
    }

    @JsonProperty("spark.master")
    public String getSparkMaster() {
        return sparkMaster;
    }

    @JsonProperty("spark.master")
    public void setSparkMaster(String sparkMaster) {
        this.sparkMaster = sparkMaster;
    }

    public SparkProperties withSparkMaster(String sparkMaster) {
        this.sparkMaster = sparkMaster;
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

    public SparkProperties withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("sparkJars", sparkJars).append("sparkDriverSupervise", sparkDriverSupervise).append("sparkExecutorMemory", sparkExecutorMemory).append("sparkDriverMemory", sparkDriverMemory).append("sparkSubmitDeployMode", sparkSubmitDeployMode).append("sparkAppName", sparkAppName).append("sparkMaster", sparkMaster).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SparkProperties) == false) {
            return false;
        }
        SparkProperties rhs = ((SparkProperties) other);
        return new EqualsBuilder().append(sparkAppName, rhs.sparkAppName).append(sparkDriverMemory, rhs.sparkDriverMemory).append(additionalProperties, rhs.additionalProperties).append(sparkJars, rhs.sparkJars).append(sparkDriverSupervise, rhs.sparkDriverSupervise).append(sparkExecutorMemory, rhs.sparkExecutorMemory).append(sparkMaster, rhs.sparkMaster).append(sparkSubmitDeployMode, rhs.sparkSubmitDeployMode).isEquals();
    }

}
