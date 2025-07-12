package web.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.INFOREDictionaryDocument;

public interface DictionaryRepository extends ElasticsearchRepository<INFOREDictionaryDocument, String> {
}
