package searchengine.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.ModelIndex;

import java.util.List;

@Repository
public interface IndexSearchRepository extends JpaRepository<ModelIndex, Integer> {
    @Query(value = "select * from index_search t where t.page_id = :pageId and t.lemma_id = :lemmaId", nativeQuery = true)
    ModelIndex ModelIndexExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

    List<ModelIndex> findModelIndexByLemmaIdInAndPage_SiteId(List<Integer> lemmaIds, int siteId, Pageable pageable);

    default List<ModelIndex> findModelIndexByLemmaIdInAndPage_SiteIdWithLimit(List<Integer> lemmaIds, int siteId, Integer offset, Integer count) {
        Pageable pageable;
        if (offset == null && count == null) { // Если оба параметра null, то выбираем все записи
            pageable = Pageable.unpaged();
        } else if (offset == null) { // Если offset null, то начинаем выборку с первой записи
            pageable = PageRequest.of(0, count);
        } else if (count == null) { // Если count null, то выбираем все записи, начиная с заданного offset
            pageable = PageRequest.of(offset, Integer.MAX_VALUE);
        } else { // Если оба параметра заданы, то выбираем записи с заданным сдвигом и количеством
            pageable = PageRequest.of(offset, count);
        }
        return findModelIndexByLemmaIdInAndPage_SiteId(lemmaIds, siteId, pageable);
    }
}