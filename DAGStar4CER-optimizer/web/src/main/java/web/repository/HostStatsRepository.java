package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.HostStatsDocument;

public interface HostStatsRepository extends ElasticsearchRepository<HostStatsDocument, String> {
}
