package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.lemma.SearchResult;
import searchengine.dto.lemma.SearchResultItem;
import searchengine.model.*;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    private static final Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);
    private final PageIndexer pageIndexer;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesToIndexing;
    private final Set<ModelSite> sitePagesAllFromDB;
    private final Connection connection;
    private AtomicBoolean indexingProcessing;


    @Override
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            logger.error("Error: ", ex);
        }
    }

    private ResponseEntity<SearchResult> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(SearchResult.builder()
                        .result(Boolean.FALSE)
                        .error(message)
                        .data(Collections.emptyList())
                        .count(0L)
                        .build());
    }

    @Override
    public ResponseEntity<SearchResult> search(String decodedSite, String query, Integer offset, Integer limit) {
        if (StringUtils.isEmpty(query)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Не задан поисковый запрос!");
        }

        Optional<ModelSite> siteToSearchOptional = getModelSiteByUrl(decodedSite);
        if (siteToSearchOptional.isEmpty()) {
            return createErrorResponse(HttpStatus.NOT_FOUND, "Сайт по этому адресу не найден!");
        }

        ModelSite siteToSearch = siteToSearchOptional.get();
        long amountOfWordsInSite = getAmountOfWordsInSite(siteToSearch);
        if (amountOfWordsInSite == 0) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Сайт не содержит слов!");
        }

        Long amountOfWordsInSiteByQuery = getAmountOfWordsInSiteByQuery(siteToSearch, query);
        List<Lemma> lemmas = lemmaRepository.findByLemmaContainingIgnoreCaseAndSiteId(query, siteToSearch.getId());
        List<Integer> lemmasIdsList = lemmas.stream()
                .map(Lemma::getId)
                .toList();

        List<ModelIndex> modelIndexByLemmaIdInAndPageSiteId = indexSearchRepository.findModelIndexByLemmaIdInAndPage_SiteIdWithLimit(lemmasIdsList, siteToSearch.getId(), offset, limit);

        List<SearchResultItem> searchResultItems = getSearchResultItems(siteToSearch, modelIndexByLemmaIdInAndPageSiteId, amountOfWordsInSite, query);

        return ResponseEntity.ok()
                .body(SearchResult.builder()
                        .result(Boolean.TRUE)
                        .count(amountOfWordsInSiteByQuery != null ? amountOfWordsInSiteByQuery : 0L)
                        .data(searchResultItems)
                        .build());
    }

    private Optional<ModelSite> getModelSiteByUrl(String decodedSite) {
        return siteRepository.findModelSiteByUrl(decodedSite);
    }

    private long getAmountOfWordsInSite(ModelSite site) {
        return sumFrequencyBySiteId(site.getId());
    }

    private Long getAmountOfWordsInSiteByQuery(ModelSite site, String query) {
        return lemmaRepository.sumFrequencyBySiteIdAndLemmaContainingIgnoreCase(site.getId(), query);
    }

    private List<SearchResultItem> getSearchResultItems(ModelSite site, List<ModelIndex> modelIndexByLemmaIdInAndPageSiteId, long amountOfWordsInSite, String query) {
        return modelIndexByLemmaIdInAndPageSiteId.stream()
                .map(index -> SearchResultItem.builder()
                        .site(site.getUrl())
                        .siteName(site.getName())
                        .title(getTitleFromHtml(index.getPage().getContent()))
                        .snippet(index.getLemma().getLemma())
                        .revelance(index.getLemmaCount() / amountOfWordsInSite)
                        .uri(index.getPage().getPath())
                        .build())
                .toList();
    }

    public long sumFrequencyBySiteId(int siteId) {
        Long result = lemmaRepository.sumFrequencyBySiteId(siteId);
        return result != null ? result : 0L;
    }

    public String getTitleFromHtml(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Element titleElement = doc.selectFirst("title");
        if (titleElement != null) {
            return titleElement.text();
        }
        return null;
    }


    private void deleteSitePagesAndPagesInDB() {
        List<ModelSite> sitesFromDB = siteRepository.findAll();
        for (ModelSite ModelSiteDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (ModelSiteDb.getUrl().equals(siteApp.getUrl())) {
                    siteRepository.deleteById(ModelSiteDb.getId());
                }
            }
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            ModelSite ModelSiteDAO = new ModelSite();
            ModelSiteDAO.setStatus(SiteStatus.INDEXING);
            ModelSiteDAO.setName(siteApp.getName());
            ModelSiteDAO.setUrl(siteApp.getUrl().toString());
            siteRepository.save(ModelSiteDAO);
        }

    }

    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (ModelSite siteDomain : sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, ModelPage> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    System.out.println("Запущена индексация " + siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(siteRepository, pageRepository, siteDomain, "", resultForkJoinPageIndexer, connection, lemmaService, pageIndexer, indexingProcessing));
                } catch (SecurityException ex) {
                    ModelSite sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(SiteStatus.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }
                if (!indexingProcessing.get()) {
                    ModelSite sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(SiteStatus.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    System.out.println("Проиндексирован сайт: " + siteDomain.getName());
                    ModelSite sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(SiteStatus.INDEXED);
                    siteRepository.save(sitePage);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }
}
