package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.BenchmarkingRequestDocument;

public interface BenchmarkingRequestRepository extends ElasticsearchRepository<BenchmarkingRequestDocument, String> {
}
