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

    List<Lemma> findByLemmaContainingIgnoreCaseAndSiteId(String lemma, int siteId, Pageable pageable);

    default List<Lemma> findByLemmaContainingIgnoreCaseAndSiteIdWithLimit(String lemma, int siteId, Integer offset, Integer count) {
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
        return findByLemmaContainingIgnoreCaseAndSiteId(lemma, siteId, pageable);
    }

    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.siteId = :siteId")
    Long sumFrequencyBySiteId(@Param("siteId") Integer siteId);

    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.siteId = :siteId AND l.lemma LIKE %:query%")
    Long sumFrequencyBySiteIdAndLemmaContainingIgnoreCase(@Param("siteId") Integer siteId, @Param("query") String query);
}