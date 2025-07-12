package core.statistics.platform.Spark;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.statistics.util.ClusterConstants;
import core.rest.Requests;

import java.util.HashMap;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "attempts"
})
public class SparkJobApp {

    @JsonProperty("id")
    public String id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("attempts")
    public List<Attempt> attempts;

    //Extra components
    List<Batch> batches;
    int processed_batches_cnt;
    StreamingStatistic statistics;
    List<StreamingReceiver> receivers;
    HashMap<Long, Integer> eventRates_cnt;
    SparkEnv environment;
    List<Executor> executors;
    List<Stage> stages;
    int stage_cnt;

    public void getPeriodicMetrics(String appName, ObjectMapper objectMapper) throws Exception {
        String master = String.format(ClusterConstants.yarn_master + "/proxy/%s/api/v1/applications/%s", appName, appName);
        this.batches = objectMapper.readValue(Requests.GET(master + "/streaming/batches", null), new TypeReference<List<Batch>>() {
        });
        this.processed_batches_cnt = 0;
        this.statistics = objectMapper.readValue(Requests.GET(master + "/streaming/statistics", null), new TypeReference<StreamingStatistic>() {
        });
        this.receivers = objectMapper.readValue(Requests.GET(master + "/streaming/receivers", null), new TypeReference<List<StreamingReceiver>>() {
        });
        this.eventRates_cnt = new HashMap<>();
        for (StreamingReceiver receiver : this.receivers) {
            this.eventRates_cnt.put(receiver.streamId, 0);
        }
        this.environment = objectMapper.readValue(Requests.GET(master + "/environment", null), new TypeReference<SparkEnv>() {
        });
        this.executors = objectMapper.readValue(Requests.GET(master + "/executors", null), new TypeReference<List<Executor>>() {
        });
        this.stages = objectMapper.readValue(Requests.GET(master + "/stages", null), new TypeReference<List<Stage>>() {
        });
        this.stage_cnt = 0;
    }

    @Override
    public String toString() {
        return "SparkJobApp{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", attempts=" + attempts +
                ", batches[0]=" + batches.get(0) +
                ", statistics=" + statistics +
                ", receivers=" + receivers +
                ", stage[0]=" + stages.get(0) +
                '}';
    }
}