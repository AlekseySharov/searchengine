package searchengine.services;
import searchengine.model.ModelPage;
public interface PageIndexer {
    void indexHtml(String html, ModelPage indexingPage);
}
