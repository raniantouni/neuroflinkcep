package spark;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "SPARK_ENV_LOADED"
})
public class EnvironmentVariables implements Serializable {

    private final static long serialVersionUID = -828148701300417255L;
    @JsonProperty("SPARK_ENV_LOADED")
    private String sPARKENVLOADED;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("SPARK_ENV_LOADED")
    public String getSPARKENVLOADED() {
        return sPARKENVLOADED;
    }

    @JsonProperty("SPARK_ENV_LOADED")
    public void setSPARKENVLOADED(String sPARKENVLOADED) {
        this.sPARKENVLOADED = sPARKENVLOADED;
    }

    public EnvironmentVariables withSPARKENVLOADED(String sPARKENVLOADED) {
        this.sPARKENVLOADED = sPARKENVLOADED;
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

    public EnvironmentVariables withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("sPARKENVLOADED", sPARKENVLOADED).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EnvironmentVariables) == false) {
            return false;
        }
        EnvironmentVariables rhs = ((EnvironmentVariables) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(sPARKENVLOADED, rhs.sPARKENVLOADED).isEquals();
    }

}
