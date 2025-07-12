package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.DockerStatsDocument;

public interface DockerStatsRepository extends ElasticsearchRepository<DockerStatsDocument, String> {
}
