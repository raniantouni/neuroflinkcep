package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.SiteLatencyDocument;

public interface SiteLatencyRepository extends ElasticsearchRepository<SiteLatencyDocument, String> {
}
