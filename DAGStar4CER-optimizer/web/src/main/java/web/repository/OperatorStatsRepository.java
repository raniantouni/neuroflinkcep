package web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import web.document.OperatorStatsDocument;

public interface OperatorStatsRepository extends ElasticsearchRepository<OperatorStatsDocument, String> {

//    @Query("{\"bool\": {\"must\": [{\"match\": {\"authors.name\": \"?0\"}}]}}")
//    Page<OperatorStatsDocument> findBy(String name, Pageable pageable);
}
