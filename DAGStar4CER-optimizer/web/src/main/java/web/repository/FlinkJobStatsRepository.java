package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.FlinkJobDocument;
import web.document.INFOREDictionaryDocument;

public interface FlinkJobStatsRepository extends ElasticsearchRepository<FlinkJobDocument, String> {

}
