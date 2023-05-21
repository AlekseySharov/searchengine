package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelPage;

@Repository
public interface PageRepository extends JpaRepository<ModelPage, Integer> {
}