package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelSite;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<ModelSite, Integer> {
    long count();

    Optional<ModelSite> findModelSiteByUrl(String name);
}