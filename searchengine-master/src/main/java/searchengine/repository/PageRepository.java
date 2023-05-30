package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelPage;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<ModelPage, Integer> {
    long count();
    long countBySiteId(int siteId);
}