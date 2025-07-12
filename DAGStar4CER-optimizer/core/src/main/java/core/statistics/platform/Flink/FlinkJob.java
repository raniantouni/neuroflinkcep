package core.statistics.platform.Flink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.rest.Requests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "jid",
        "name",
        "isStoppable",
        "state",
        "start-time",
        "end-time",
        "duration",
        "now",
        "timestamps",
        "vertices",
        "status-counts",
        "optimizer/plan"
})
public class FlinkJob implements Serializable {

    private final static long serialVersionUID = 8756157243539207649L;
    @JsonProperty("jid")
    public String jid;
    @JsonProperty("name")
    public String name;
    @JsonProperty("isStoppable")
    public Boolean isStoppable;
    @JsonProperty("state")
    public String state;
    @JsonProperty("start-time")
    public Long startTime;
    @JsonProperty("end-time")
    public Long endTime;
    @JsonProperty("duration")
    public Long duration;
    @JsonProperty("now")
    public Long now;
    @JsonProperty("timestamps")
    public Timestamps timestamps;
    @JsonProperty("vertices")
    public List<Vertex> vertices = null;
    @JsonProperty("status-counts")
    public StatusCounts statusCounts;
    @JsonProperty("optimizer/plan")
    public Plan plan;

    @Override
    public String toString() {
        return "FlinkJob{" +
                "jid='" + jid + '\'' +
                ", name='" + name + '\'' +
                ", isStoppable=" + isStoppable +
                ", state='" + state + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                ", now=" + now +
                ", timestamps=" + timestamps +
                ", vertices=" + vertices +
                ", statusCounts=" + statusCounts +
                ", plan=" + plan +
                '}';
    }


    /**
     * Updates the necessary metrics by contacting the REST api endpoint.
     *
     * @param masterURL    Flink master URL.
     * @param objectMapper
     * @throws Exception
     */
    public void getPeriodicMetrics(String masterURL, ObjectMapper objectMapper) {
        //Latency metrics
        try {
            String lat_url = "http://" + masterURL + "/jobs/" + this.jid + "/metrics";
            String base_latency_fmt = "latency.source_id.%s.source_subtask_index.0.operator_id.%s.operator_subtask_index.0.latency_";

            List<Vertex> sources = this.vertices.stream().filter(v -> v.name.startsWith("Source")).collect(Collectors.toList());
            List<Vertex> non_sources = this.vertices.stream().filter(v -> !v.name.startsWith("Source")).collect(Collectors.toList());

            for (Vertex source : sources) {
                HashMap<String, LatencyNode> vLatencies = new HashMap<>(); // <Non-Source vertex id,Latencies>
                for (Vertex non_source : non_sources) {
                    List<String> params = new ArrayList<>();
                    params.add(String.format(base_latency_fmt + "stddev", source.id, non_source.id));
                    params.add(String.format(base_latency_fmt + "min", source.id, non_source.id));
                    params.add(String.format(base_latency_fmt + "max", source.id, non_source.id));
                    params.add(String.format(base_latency_fmt + "median", source.id, non_source.id));
                    params.add(String.format(base_latency_fmt + "p99", source.id, non_source.id));
                    List<HashMap<String, String>> lat_map = objectMapper.readValue(Requests.GET(lat_url, params), new TypeReference<List<HashMap<String, String>>>() {
                    });
                    LatencyNode latencyNode = new LatencyNode();
                    for (HashMap<String, String> json_map : lat_map) {
                        String id_field = json_map.get("id");
                        String val_field = json_map.get("value");
                        if (id_field.endsWith("stddev")) {
                            latencyNode.stddev = val_field;
                        } else if (id_field.endsWith("min")) {
                            latencyNode.min = val_field;
                        } else if (id_field.endsWith("max")) {
                            latencyNode.max = val_field;
                        } else if (id_field.endsWith("median")) {
                            latencyNode.median = val_field;
                        } else if (id_field.endsWith("p99")) {
                            latencyNode.p99 = val_field;
                        } else {
                            System.out.println("Error parsing latency at source:" + source + " where val=" + id_field);
                        }

                    }
                    vLatencies.put(non_source.id, latencyNode);
                }
                source.vertexLatencies = vLatencies; //Add all latency metrics for each non-source vertex to this source.
            }
        } catch (Exception ignored) {
        }

        //Update Throughput metrics
        try {
            for (Vertex v : this.vertices) {
                String tp_url = "http://" + masterURL + "/jobs/" + this.jid + "/vertices/" + v.id + "/subtasks/metrics";
                List<String> params = new ArrayList<>();
                params.add("numRecordsIn");
                params.add("numRecordsInPerSecond");
                params.add("numBytesInRemotePerSecond");
                params.add("numRecordsOut");
                params.add("numRecordsOutPerSecond");
                params.add("numBytesOutPerSecond");
                params.add("numBytesInLocalPerSecond");
                List<TPStats> tp_list = objectMapper.readValue(Requests.GET(tp_url, params), new TypeReference<List<TPStats>>() {
                });
                ThroughputNode tp = new ThroughputNode();
                for (TPStats entry : tp_list) {
                    switch (entry.id) {
                        case "numRecordsIn":
                            tp.numRecordsIn = entry;
                            break;
                        case "numRecordsInPerSecond":
                            tp.numRecordsInPerSecond = entry;
                            break;
                        case "numBytesInRemotePerSecond":
                            tp.numBytesInRemotePerSecond = entry;
                            break;
                        case "numRecordsOut":
                            tp.numRecordsOut = entry;
                            break;
                        case "numRecordsOutPerSecond":
                            tp.numRecordsOutPerSecond = entry;
                            break;
                        case "numBytesOutPerSecond":
                            tp.numBytesOutPerSecond = entry;
                            break;
                        case "numBytesInLocalPerSecond":
                            tp.numBytesInLocalPerSecond = entry;
                            break;
                        default:
                            System.out.println("Error parsing tp node: " + entry);
                            break;
                    }
                }
                v.throughputNode = tp;
            }
        } catch (Exception ignored) {
        }

        //Update Backpressure metrics.
        //Backpressure vertices go online AFTER topology
        try {
            for (Vertex v : this.vertices) {
                String bp_url = "http://" + masterURL + "/jobs/" + this.jid + "/vertices/" + v.id + "/backpressure";
                String res = Requests.GET(bp_url, null);
                if (res != null) {
                    v.backpressureNode = objectMapper.readValue(res, new TypeReference<BackpressureNode>() {
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Pretty print method.
     *
     * @param objectMapper The OM object.
     * @return The equivalent of toString() method.
     */
    public String print(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this).replace("-&gt;", "->");
        } catch (JsonProcessingException e) {
            return "Can't parse flink job: " + this.jid;
        }
    }
}
