package core.statistics.platform.Flink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "jid",
        "name",
        "nodes"
})
public class Plan implements Serializable {

    private final static long serialVersionUID = -3491838580827151831L;
    @JsonProperty("jid")
    public String jid;
    @JsonProperty("name")
    public String name;
    @JsonProperty("nodes")
    public List<Node> nodes = null;

    @Override
    public String toString() {
        return "Plan{" +
                "jid='" + jid + '\'' +
                ", name='" + name + '\'' +
                ", nodes=" + nodes +
                '}';
    }
}
