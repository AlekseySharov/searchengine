package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model. ModelIndex;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexSearchRepository extends JpaRepository<ModelIndex,Integer> {
    @Query(value = "select * from index_search t where t.page_id = :pageId and t.lemma_id = :lemmaId",nativeQuery = true)
    ModelIndex ModelIndexExist(@Param("pageId")Integer pageId,@Param("lemmaId")Integer lemmaId);

    List<ModelIndex> findModelIndexByLemmaIdInAndPage_SiteId(List<Integer> lemmaIds, int siteId);
}