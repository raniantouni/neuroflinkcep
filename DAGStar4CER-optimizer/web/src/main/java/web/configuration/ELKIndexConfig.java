package web.configuration;

import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * ELK indexes and mappings.
 */
@Configuration
public class ELKIndexConfig {

    @Bean
    public Map<String, CreateIndexRequest> getAllCreateIndexRequests() {
        Map<String, CreateIndexRequest> requestMap = new HashMap<>();
        requestMap.put("platform-stats", createRequest("platform-stats", getPlatformStatsIndexMapping()));
        requestMap.put("site-latency", createRequest("site-latency", getSiteLatencyIndexMapping()));
        requestMap.put("flink-http-stats", createRequest("flink-http-stats", getFlinkHttpMapping()));
        requestMap.put("docker-stats", createRequest("docker-stats", getDockerStatsMapping()));
        requestMap.put("operator-jobs", createRequest("operator-jobs", getOperatorJobsMapping()));
        requestMap.put("infore_network", createRequest("infore_network", getInforeNetworkMapping()));
        requestMap.put("infore_request", createRequest("infore_request", getInforeRequestMapping()));
        requestMap.put("infore_dictionary", createRequest("infore_dictionary", getInforeDictionaryMapping()));
        requestMap.put("operator-stats-transform-1", createRequest("operator-stats-transform-1", getOperatorStatsTransform1()));
        requestMap.put("site-latency-transform-1", createRequest("site-latency-transform-1", getSiteLatencyTransform()));
        requestMap.put("host-stats-transform-1", createRequest("host-stats-transform-1", getHostStatsTransform()));
        requestMap.put("stomp-logs", createRequest("stomp-logs", getStompLogsMapping()));
        requestMap.put("bo-estimations", createRequest("bo-estimations", getBOEstimationsMapping()));
        requestMap.put("infore_result", createRequest("infore_result", getInforeResultMapping()));
        requestMap.put("infore_result_v2", createRequest("infore_result_v2", getInforeResultv2Mapping()));
        requestMap.put("optimizer-logs", createRequest("optimizer-logs", getOptimizerLogsMapping()));
        // requestMap.put("optimizer-app-metrics-2021", createRequest("optimizer-app-metrics-2021", getOptimizerAppMetricsMapping()));
        requestMap.put("benchmarking_request", createRequest("benchmarking_request", getBenchmarkingRequestMapping()));
        requestMap.put("bsc-ingestion", createRequest("bsc-ingestion", getBSCIngestionMapping()));
        return requestMap;
    }



    private CreateIndexRequest createRequest(String name, String mapping) {
        CreateIndexRequest request = new CreateIndexRequest(name);
        request.mapping(mapping, XContentType.JSON);
        return request;
    }

    private String getBSCIngestionMapping() {
        return "{\n" +
                "  \"_source\": {\n" +
                "    \"enabled\": true\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"throughput\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"network\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"memory\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"cores\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"sim_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"param1\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"param2\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"param3\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"status\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String getBenchmarkingRequestMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"request\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

    private String getOptimizerAppMetricsMapping() {
        return "{\n" +
                "  \"_source\": {\n" +
                "    \"enabled\": true\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"action\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"active\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"area\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"cause\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"count\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"duration\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"level\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"max\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"mean\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"name\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"name_tag\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"pool\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"state\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"sum\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"total\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"unknown\": {\n" +
                "      \"type\": \"double\"\n" +
                "    },\n" +
                "    \"value\": {\n" +
                "      \"type\": \"double\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String getOptimizerLogsMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"@version\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"msg\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"thread\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"level\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"logger\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"path\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }," +
                "\n" +
                "    \"tags\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String getInforeResultMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"algorithmUsed\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dictionaryName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"networkName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operatorsPretty\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"performance\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"placementSiteDiffFromRoot\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"requestID\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"workflow\": {\n" +
                "      \"properties\": {\n" +
                "        \"enclosingOperatorName\": {\n" +
                "          \"type\": \"text\",\n" +
                "          \"fields\": {\n" +
                "            \"keyword\": {\n" +
                "              \"type\": \"keyword\",\n" +
                "              \"ignore_above\": 256\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"modifiedAt\": {\n" +
                "          \"type\": \"long\"\n" +
                "        },\n" +
                "        \"operatorConnections\": {\n" +
                "          \"properties\": {\n" +
                "            \"fromOperator\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"fromPort\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"fromPortType\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toOperator\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toPort\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toPortType\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"operators\": {\n" +
                "          \"properties\": {\n" +
                "            \"classKey\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"hasSubprocesses\": {\n" +
                "              \"type\": \"boolean\"\n" +
                "            },\n" +
                "            \"inputPortsAndSchemas\": {\n" +
                "              \"properties\": {\n" +
                "                \"isConnected\": {\n" +
                "                  \"type\": \"boolean\"\n" +
                "                },\n" +
                "                \"name\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"objectClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"portType\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"isEnabled\": {\n" +
                "              \"type\": \"boolean\"\n" +
                "            },\n" +
                "            \"name\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"numberOfSubprocesses\": {\n" +
                "              \"type\": \"long\"\n" +
                "            },\n" +
                "            \"operatorClass\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"outputPortsAndSchemas\": {\n" +
                "              \"properties\": {\n" +
                "                \"isConnected\": {\n" +
                "                  \"type\": \"boolean\"\n" +
                "                },\n" +
                "                \"name\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"objectClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"portType\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"parameters\": {\n" +
                "              \"properties\": {\n" +
                "                \"defaultValue\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"key\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"range\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"typeClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"value\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"placementSites\": {\n" +
                "          \"properties\": {\n" +
                "            \"availablePlatforms\": {\n" +
                "              \"properties\": {\n" +
                "                \"address\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"operators\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"platformName\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"siteName\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"workflowName\": {\n" +
                "          \"type\": \"text\",\n" +
                "          \"fields\": {\n" +
                "            \"keyword\": {\n" +
                "              \"type\": \"keyword\",\n" +
                "              \"ignore_above\": 256\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    //Added by Xenia to reflect the change of the Operator from string to object
    private String getInforeResultv2Mapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"algorithmUsed\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dictionaryName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"networkName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operatorsPretty\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"performance\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"placementSiteDiffFromRoot\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"requestID\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"workflow\": {\n" +
                "      \"properties\": {\n" +
                "        \"enclosingOperatorName\": {\n" +
                "          \"type\": \"text\",\n" +
                "          \"fields\": {\n" +
                "            \"keyword\": {\n" +
                "              \"type\": \"keyword\",\n" +
                "              \"ignore_above\": 256\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"modifiedAt\": {\n" +
                "          \"type\": \"long\"\n" +
                "        },\n" +
                "        \"operatorConnections\": {\n" +
                "          \"properties\": {\n" +
                "            \"fromOperator\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"fromPort\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"fromPortType\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toOperator\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toPort\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"toPortType\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"operators\": {\n" +
                "          \"properties\": {\n" +
                "            \"classKey\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"hasSubprocesses\": {\n" +
                "              \"type\": \"boolean\"\n" +
                "            },\n" +
                "            \"inputPortsAndSchemas\": {\n" +
                "              \"properties\": {\n" +
                "                \"isConnected\": {\n" +
                "                  \"type\": \"boolean\"\n" +
                "                },\n" +
                "                \"name\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"objectClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"portType\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"isEnabled\": {\n" +
                "              \"type\": \"boolean\"\n" +
                "            },\n" +
                "            \"name\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"numberOfSubprocesses\": {\n" +
                "              \"type\": \"long\"\n" +
                "            },\n" +
                "            \"operatorClass\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"outputPortsAndSchemas\": {\n" +
                "              \"properties\": {\n" +
                "                \"isConnected\": {\n" +
                "                  \"type\": \"boolean\"\n" +
                "                },\n" +
                "                \"name\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"objectClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"portType\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"parameters\": {\n" +
                "              \"properties\": {\n" +
                "                \"defaultValue\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"key\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"range\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"typeClass\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"value\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"placementSites\": {\n" +
                "          \"properties\": {\n" +
                "            \"availablePlatforms\": {\n" +
                "              \"properties\": {\n" +
                "                \"address\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"operators\":{\n" +
                "                  \"properties\":{\n" +
                "                    \"name\":{\n" +
                "                      \"type\":\"text\",\n" +
                "                      \"fields\":{\n" +
                "                        \"keyword\":{\n" +
                "                          \"type\":\"keyword\",\n" +
                "                          \"ignore_above\":256\n" +
                "                        }\n" +
                "                      }\n" +
                "                    },\n" +
                "                    \"old_path\":{\n" +
                "                      \"type\":\"text\",\n" +
                "                      \"fields\":{\n" +
                "                        \"keyword\":{\n" +
                "                          \"type\":\"keyword\",\n" +
                "                          \"ignore_above\":256\n" +
                "                        }\n" +
                "                      }\n" +
                "                    },\n" +
                "                    \"new_path\":{\n" +
                "                      \"type\":\"text\",\n" +
                "                      \"fields\":{\n" +
                "                        \"keyword\":{\n" +
                "                          \"type\":\"keyword\",\n" +
                "                          \"ignore_above\":256\n" +
                "                        }\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"platformName\": {\n" +
                "                  \"type\": \"text\",\n" +
                "                  \"fields\": {\n" +
                "                    \"keyword\": {\n" +
                "                      \"type\": \"keyword\",\n" +
                "                      \"ignore_above\": 256\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"siteName\": {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fields\": {\n" +
                "                \"keyword\": {\n" +
                "                  \"type\": \"keyword\",\n" +
                "                  \"ignore_above\": 256\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"workflowName\": {\n" +
                "          \"type\": \"text\",\n" +
                "          \"fields\": {\n" +
                "            \"keyword\": {\n" +
                "              \"type\": \"keyword\",\n" +
                "              \"ignore_above\": 256\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String getBOEstimationsMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"JobMainClass\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"parallelism\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"sourceRate1\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"sourceRate2\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"stream_out_type\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"stream_type_1\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"stream_type_2\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"throughput\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"usage\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getStompLogsMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\",\n" +
                "      \"format\": \"uuuu-MM-dd'T'HH:mm:ss.SSS'Z'\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"command\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"detailedLogMessage\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"payload\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"receipt\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"simpSessionId\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"source\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"toString\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"topic\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"user\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getHostStatsTransform() {
        return "{\n" +
                "  \"_meta\": {\n" +
                "    \"created_by\": \"transform\",\n" +
                "    \"_transform\": {\n" +
                "      \"transform\": \"transform0\",\n" +
                "      \"version\": {\n" +
                "        \"created\": \"7.10.2\"\n" +
                "      },\n" +
                "      \"creation_date_in_millis\": 1616173464695\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"properties\": {\n" +
                "        \"max\": {\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"hostname\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_key\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_type\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_value_number\": {\n" +
                "      \"properties\": {\n" +
                "        \"avg\": {\n" +
                "          \"type\": \"double\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_id\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getSiteLatencyTransform() {
        return "{\n" +
                "  \"_meta\": {\n" +
                "    \"created_by\": \"transform\",\n" +
                "    \"_transform\": {\n" +
                "      \"transform\": \"transform2\",\n" +
                "      \"version\": {\n" +
                "        \"created\": \"7.10.2\"\n" +
                "      },\n" +
                "      \"creation_date_in_millis\": 1616173465146\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"properties\": {\n" +
                "        \"max\": {\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dst_ip\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dst_site\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"latency_ms\": {\n" +
                "      \"properties\": {\n" +
                "        \"max\": {\n" +
                "          \"type\": \"float\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"source_ip\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"source_site\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getInforeDictionaryMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dictionary\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

    public String getOperatorStatsTransform1() {
        return "{\n" +
                "  \"_meta\": {\n" +
                "    \"created_by\": \"transform\",\n" +
                "    \"_transform\": {\n" +
                "      \"transform\": \"transform1\",\n" +
                "      \"version\": {\n" +
                "        \"created\": \"7.10.2\"\n" +
                "      },\n" +
                "      \"creation_date_in_millis\": 1616173464946\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"properties\": {\n" +
                "        \"max\": {\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"hostname\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_id\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_name\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_key\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_type\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_value_number\": {\n" +
                "      \"properties\": {\n" +
                "        \"avg\": {\n" +
                "          \"type\": \"double\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_id\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_type\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_workflow_name\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_id\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"properties\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getInforeNetworkMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"network\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

    public String getInforeRequestMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"_class\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"request\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"modifiedAt\": {\n" +
                "      \"type\": \"long\"\n" +
                "    },\n" +
                "    \"isContinuous\": {\n" +
                "      \"type\": \"boolean\"\n" +
                "    },\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"text\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getPlatformStatsIndexMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"@version\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"event_time\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"ingestion_time\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"operator_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"error\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 1024\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"cluster\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"framework\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"role\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_value_number\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"block\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"port\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"task_attempt_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_workflow_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"host\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"path\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"hostname\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"tm_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"ip_external\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }" +
                ",\n" +
                "    \"task_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_key\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_path\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"task_attempt_num\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_role\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"ip_internal\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"task_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"ip_host\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getSiteLatencyIndexMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"dst_ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dst_site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"source_ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"latency_ms\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"source_site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }" +
                "}";
    }

    public String getFlinkHttpMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"flink_ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"duration\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_parallelism\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"total_task_slots\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"avail_task_slots\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"start_time\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"format\": \"epoch_millis\",\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"end_time\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"format\": \"epoch_millis\",\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"status\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_url\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"jobName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_memory_utilization_percentage\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_cpu_utilization_percentage\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"jobId\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

    public String getDockerStatsMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"flink_ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"duration\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_parallelism\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"total_task_slots\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"avail_task_slots\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"start_time\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"format\": \"epoch_millis\",\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"end_time\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"format\": \"epoch_millis\",\n" +
                "          \"type\": \"date\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"status\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_url\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"jobName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_memory_utilization_percentage\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_cpu_utilization_percentage\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"jobId\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

    public String getOperatorJobsMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"@timestamp\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"start_time\": {\n" +
                "      \"type\": \"date\"\n" +
                "    },\n" +
                "    \"platform\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_id\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"hostname\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_key\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_sites\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_platforms\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_demo_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site_demo_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"metric_value_number\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_type\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operator_workflow_name\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"status\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"site\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"duration\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_parallelism\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"total_task_slots\": {\n" +
                "      \"type\": \"long\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"avail_task_slots\": {\n" +
                "      \"type\": \"long\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"flink_ip\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_url\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_memory_utilization_percentage\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"platform_cpu_utilization_percentage\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_memory_utilization_percentage\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_cpu_utilization_percentage\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"records_out_per_second\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"expected_mem_utilization_percentage\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"avg_records_out_per_second\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"avg_records_in_per_second\": {\n" +
                "      \"type\": \"float\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"late_records\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"throughput\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_records_out\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"job_records_in\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"dropped_records\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"ingestion_rate\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"slots_utilization_times_100\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"slots_utilization\": {\n" +
                "      \"type\": \"double\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}\n" +
                "}";
    }

}
