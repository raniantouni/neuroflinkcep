package web.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.java.Log;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableElasticsearchRepositories
@Log
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {
    //Local
    private static Gson gson;

    @Value("#{systemEnvironment['ATHENA_ES_URL']}")
    private String esURL;

    @Value("#{systemEnvironment['ATHENA_KIBANA_URL']}")
    private String kibanaURL;

    @Value("#{systemEnvironment['ES_USERNAME']}")
    private String esUsername;

    @Value("#{systemEnvironment['ES_PASSWORD']}")
    private String esPassword;

    @Value("#{systemEnvironment['ATHENA_OPTIMIZER_WAIT_FOR_ELK_STACK_ATTEMPTS']}")
    private int WAIT_FOR_ELK_STACK_ATTEMPTS;

    @Value("classpath:kibana/dashboards")
    private Resource kibanaDashboardFolder;

    @Value("classpath:kibana/transformations")
    private Resource kibanaTransformationFolder;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ELKIndexConfig kibanaConfiguration;

    @Autowired
    private RestHighLevelClient ELKClient;

    @PostConstruct
    public void setup() {
        gson = new Gson();

        //Health checks, make sure all containers are up
        ensureELKIsUp();

        //Upload ELK resources (Dashboards,Indexes,Index Patterns)
        ensureElasticsearchIndexesAndMappings();
        ensureKibanaDashboards();
        ensureKibanaTransformations();
        log.info("ELK services are up and running.");
    }

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(esURL)
                .withBasicAuth(esUsername, esPassword)
                .build();
        return RestClients.create(clientConfiguration).rest();
    }

    @Bean
    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        List<Converter> list = new ArrayList<>();
        list.add(new MapWriteConverterString());
        list.add(new MapWriteConverterDouble());
        return new ElasticsearchCustomConversions(list);
    }

    private void ensureELKIsUp() {
        for (int attempt = 1; attempt <= WAIT_FOR_ELK_STACK_ATTEMPTS; attempt++) {
            //Health check the ELK stack
            try {
                //Elasticsearch check
                String esJSON = this.restTemplate.getForEntity(new URI(String.format("http://%s", esURL)), String.class).getBody();
                JsonObject esObj = gson.fromJson(esJSON, JsonObject.class);
                if (!esObj.get("tagline").getAsString().equals("You Know, for Search")) {
                    throw new IllegalStateException("");
                }

                //Kibana checks
                String kibanaJSON = this.restTemplate.getForEntity(new URI(String.format("http://%s/api/status", kibanaURL)), String.class).getBody();
                JsonObject kibanaObj = gson.fromJson(kibanaJSON, JsonObject.class);
                if (!kibanaObj.get("name").getAsString().equalsIgnoreCase("kibana")) {
                    throw new IllegalStateException("");
                }
                return;
            } catch (Exception e) {
                //Retry later
                log.warning(e.toString());
                log.fine(String.format("Waiting for ELK stack - attempt No. %d.", attempt));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    log.warning(e2.toString());
                }
            }
        }

        log.severe("Early stop because ELK containers are unavailable.");
        System.exit(SpringApplication.exit(appContext, () -> -1));
    }

    private void ensureElasticsearchIndexesAndMappings() {
        try {
            //Get all available indexes, skip the ones starting with dots (e.g .settings)
            GetIndexResponse allIndexes = this.ELKClient.indices().get(new GetIndexRequest("*"), RequestOptions.DEFAULT);
            Set<String> existingIndexes = Arrays.stream(allIndexes.getIndices()).sequential().filter(i -> !i.startsWith(".")).collect(Collectors.toSet());

            //Create all missing indexes and index patterns
            for (Map.Entry<String, CreateIndexRequest> requestEntry : kibanaConfiguration.getAllCreateIndexRequests().entrySet()) {
                String indexName = requestEntry.getKey();
                if (!existingIndexes.contains(indexName)) {
                    //Create index, skip transformation indexes
                    if (!indexName.contains("transform")) {
                        CreateIndexResponse createIndexResponse = elasticsearchClient().indices().create(requestEntry.getValue(), RequestOptions.DEFAULT);
                        if (!createIndexResponse.isAcknowledged()) {
                            throw new IllegalStateException(String.format("Index %s creation not acknowledged.", indexName));
                        }
                    }

                    //Log the success
                    log.fine(String.format("Created index [%s].", indexName));
                } else {
                    log.fine(String.format("Index %s is present, skipping creation.", indexName));
                }

                //Create index pattern (handle transform indexes differently)
                String indexPatternRequestBody = String.format("{ \"attributes\": { \"title\": \"%s\", \"timeFieldName\": \"%s\" } }",
                        indexName, indexName.contains("transform") ? "@timestamp.max" : "@timestamp");
                HttpHeaders postHeaders = new HttpHeaders();
                postHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> postEntity = new HttpEntity<>(indexPatternRequestBody, postHeaders);
                try {
                    this.restTemplate.exchange(String.format("http://%s/api/saved_objects/index-pattern/%s", kibanaURL, indexName), HttpMethod.POST, postEntity, String.class);
                } catch (HttpClientErrorException e) {
                    if (!e.getMessage().contains("already exists")) {
                        throw e;
                    }
                }
                log.fine(String.format("Mappings are set for index [%s].", indexName));
            }
        } catch (Exception e) {
            log.severe(e.getMessage());
            System.exit(SpringApplication.exit(appContext, () -> -2));
        }
        log.info("ELK indexes and mappings are up and ready.");
    }

    private void ensureKibanaDashboards() {
        try {
            if (!this.kibanaDashboardFolder.exists()) {
                throw new FileNotFoundException("Kibana dashboards folder not found.");
            }
            for (File file : Objects.requireNonNull(this.kibanaDashboardFolder.getFile().listFiles((dir, name) -> !name.startsWith(".")))) {
                String content = new String(Files.readAllBytes(file.toPath()));
                ResponseEntity<String> response = this.restTemplate.postForEntity(new URI(String.format("http://%s/api/kibana/dashboards/import?exclude=index-pattern", this.kibanaURL)), content, String.class);
                String fileName = file.getName().split("\\.")[0];
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.fine(String.format("Dashboard %s was uploaded successfully.", fileName));
                } else {
                    log.warning(String.format("Dashboard %s was not uploaded with response [%s].", fileName, response.getBody()));
                }
            }
        } catch (Exception e) {
            log.severe(e.getMessage());
            System.exit(SpringApplication.exit(this.appContext, () -> -3));
        }
        log.info("ELK dashboards are up and ready.");
    }

    private void ensureKibanaTransformations() {
        try {
            if (!this.kibanaTransformationFolder.exists()) {
                throw new FileNotFoundException("Kibana transformations folder not found.");
            }
            for (File file : Objects.requireNonNull(this.kibanaTransformationFolder.getFile().listFiles((dir, name) -> !name.startsWith(".")))) {
                String fileName = file.getName().split("\\.")[0];

                //Upload transformation
                HttpHeaders putHeaders = new HttpHeaders();
                putHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> putEntity = new HttpEntity<>(new String(Files.readAllBytes(file.toPath())), putHeaders);
                try {
                    this.restTemplate.exchange(String.format("http://%s/_transform/%s", this.esURL, fileName), HttpMethod.PUT, putEntity, Void.class);
                    log.fine(String.format("Transformation %s uploaded successfully.", fileName));
                } catch (HttpClientErrorException e) {
                    if (!e.getMessage().contains("resource_already_exists")) {
                        throw e;
                    }
                }

                //Start the transformation
                HttpHeaders startHeaders = new HttpHeaders();
                startHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> startEntity = new HttpEntity<>(fileName, startHeaders);
                try {
                    this.restTemplate.exchange(String.format("http://%s/_transform/%s/_start", this.esURL, fileName), HttpMethod.POST, startEntity, String.class);
                    log.fine(String.format("Transformation %s started successfully.", fileName));
                } catch (HttpClientErrorException e) {
                    if (!e.getMessage().contains("already started")) {
                        throw e;
                    }
                    log.fine(String.format("Transformation %s has already started.", fileName));
                }
            }
        } catch (Exception e) {
            log.severe(e.getMessage());
            System.exit(SpringApplication.exit(appContext, () -> -4));
        }
        log.info("ELK transformations are up and ready.");
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder()
                .basicAuthentication(esUsername, esPassword)
                .defaultHeader("kbn-xsrf", "true")
                .errorHandler(new DefaultResponseErrorHandler())
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }

    @WritingConverter
    public static class MapWriteConverterString implements Converter<Map<String, String>, String> {
        @Override
        public String convert(Map<String, String> source) {
            if (source.containsKey("keyword")) {
                return source.get("keyword");
            } else {
                return gson.toJson(source);
            }
        }
    }

    @WritingConverter
    public static class MapWriteConverterDouble implements Converter<Map<String, String>, Double> {

        //FIXME I dont know why..
        @Override
        public Double convert(Map<String, String> source) {
            Map.Entry<String, String> entry = source.entrySet().iterator().next();
            return Double.valueOf(String.valueOf(entry.getValue()));
        }
    }
}
