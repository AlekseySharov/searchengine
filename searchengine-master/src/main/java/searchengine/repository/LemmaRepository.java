package searchengine.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma for update", nativeQuery = true)
    Lemma lemmaExist(@Param("lemma") String lemma);

    @Modifying
    @Query(value = "update Lemma t set t.frequency = t.frequency + :frequency where t.id = :idLemma")
    void incrementFrequency(Integer idLemma, Integer frequency);

    long count();

    long countBySiteId(int siteId);

    List<Lemma> findByLemmaContainingIgnoreCaseAndSiteId(String lemma, int siteId);

    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.siteId = :siteId")
    Long sumFrequencyBySiteId(@Param("siteId") Integer siteId);

    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.siteId = :siteId AND l.lemma LIKE %:query%")
    Long sumFrequencyBySiteIdAndLemmaContainingIgnoreCase(@Param("siteId") Integer siteId, @Param("query") String query);
}