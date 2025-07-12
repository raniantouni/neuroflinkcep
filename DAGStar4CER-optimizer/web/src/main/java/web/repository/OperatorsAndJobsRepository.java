package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.OperatorsAndJobsDocument;

public interface OperatorsAndJobsRepository extends ElasticsearchRepository<OperatorsAndJobsDocument, String> {
}
