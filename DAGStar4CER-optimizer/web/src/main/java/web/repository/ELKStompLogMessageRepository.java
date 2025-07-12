package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.ELKStompLogDocument;

public interface ELKStompLogMessageRepository extends ElasticsearchRepository<ELKStompLogDocument, String> {
}
