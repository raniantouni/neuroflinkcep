package benchmarking;

import com.google.gson.Gson;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableElasticsearchRepositories
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {
    private static final Gson gson = new Gson();

    @Value("#{systemEnvironment['ES_URL']}")
    private String esURL;

    @Value("#{systemEnvironment['ES_USERNAME']}")
    private String esUsername;

    @Value("#{systemEnvironment['ES_PASSWORD']}")
    private String esPassword;

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        return RestClients.create(ClientConfiguration.builder()
                .connectedTo(esURL)
                .withBasicAuth(esUsername, esPassword)
                .build()).rest();
    }

    @Bean
    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        List<Converter> list = new ArrayList<>();
        list.add(new MapWriteConverterString());
        list.add(new MapWriteConverterDouble());
        return new ElasticsearchCustomConversions(list);
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
