package core.statistics.platform.Flink;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "cpuCores",
        "physicalMemory",
        "freeMemory",
        "managedMemory"
})
public class Hardware {
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonProperty("cpuCores")
    public Long cpuCores;
    @JsonProperty("physicalMemory")
    public Long physicalMemory;
    @JsonProperty("freeMemory")
    public Long freeMemory;
    @JsonProperty("managedMemory")
    public Long managedMemory;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return "Hardware{" +
                "cpuCores=" + cpuCores +
                ", physicalMemory=" + physicalMemory +
                ", freeMemory=" + freeMemory +
                ", managedMemory=" + managedMemory +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
