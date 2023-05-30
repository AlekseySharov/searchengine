package searchengine.services;
import org.springframework.http.ResponseEntity;
import searchengine.dto.lemma.SearchResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
public interface ApiService {
    void startIndexing(AtomicBoolean indexingProcessing);

    ResponseEntity<SearchResult> search(String decodedSite, String query, Integer offset, Integer limit);
}
