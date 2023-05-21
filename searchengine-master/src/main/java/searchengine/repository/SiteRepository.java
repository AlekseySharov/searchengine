package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelSite;

@Repository
public interface SiteRepository extends JpaRepository<ModelSite, Integer> {

}