package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.OptimizationRequestDocument;

public interface OptimizerRequestRepository extends ElasticsearchRepository<OptimizationRequestDocument, String> {
}
