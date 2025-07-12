package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.INFORENetworkDocument;

public interface NetworkRepository extends ElasticsearchRepository<INFORENetworkDocument, String> {
}
