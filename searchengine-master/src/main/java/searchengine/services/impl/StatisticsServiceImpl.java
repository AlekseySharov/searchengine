package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.ModelSite;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.count());
        total.setPages(pageRepository.count());
        total.setLemmas(lemmaRepository.count());
        total.setIndexing(true);

        List<Site> sitesList = sites.getSites();
        List<DetailedStatisticsItem> detailedStatisticsItemList = sitesList.stream()
                .map(site -> {
                    ModelSite siteFromRepo = siteRepository.findById(site.getId())
                            .orElseGet(ModelSite::new);
                    DetailedStatisticsItem item = new DetailedStatisticsItem();
                    item.setName(site.getName());
                    item.setUrl(site.getUrl().toString());
                    long pages = pageRepository.countBySiteId(siteFromRepo.getId());
                    long lemmas = lemmaRepository.countBySiteId(siteFromRepo.getId());
                    item.setPages(pages);
                    item.setLemmas(lemmas);
                    item.setStatus(siteFromRepo.getStatus() == null ? EMPTY_STRING : siteFromRepo.getStatus().toString());
                    item.setError(siteFromRepo.getLastError() == null ? EMPTY_STRING : siteFromRepo.getLastError());
                    item.setStatusTime(siteFromRepo.getStatusTime() == null ? LocalDateTime.now() : siteFromRepo.getStatusTime().toLocalDateTime());
                    return item;
                })
                .toList();

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailedStatisticsItemList);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
