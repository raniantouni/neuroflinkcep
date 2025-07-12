package web.repository;


import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.OptimizerResponseDocument;

public interface OptimizerResultRepository extends ElasticsearchRepository<OptimizerResponseDocument, String> {
}
