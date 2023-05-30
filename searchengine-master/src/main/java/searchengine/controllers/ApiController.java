package searchengine.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.lemma.SearchResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final ApiService apiService;
    private final LemmaService lemmaService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final SitesList sitesList;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if (indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"result\": false, \"error\": \"Индексация уже запущена\"}");
        } else {
            indexingProcessing.set(true);
            Runnable start = () -> apiService.startIndexing(indexingProcessing);
            new Thread(start).start();
            return ResponseEntity.status(HttpStatus.OK).body("{\"result\": true}");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        if (!indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("'result' : false, " +
                    "{\"result\": false, \"error\": \"Индексация не запущена\"}");
        } else {
            indexingProcessing.set(false);
            return ResponseEntity.status(HttpStatus.OK).body("{\"result\": true}");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResult> search(@RequestParam @NonNull String query,
                                               @RequestParam int offset,
                                               @RequestParam int limit,
                                               @RequestParam @NonNull String site) {
        String decodedSite = URLDecoder.decode(site, StandardCharsets.UTF_8);
        return apiService.search(decodedSite, query, offset, limit);
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam URL url) throws IOException {

        try {
            sitesList.getSites().stream().filter(site -> url.getHost().equals(site.getUrl().getHost())).findFirst().orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("result: false " +
                    "error: Данная страница находится за пределами сайтов " +
                    "указанных в конфигурационном файле");
        }
        lemmaService.getLemmasFromUrl(url);
        return ResponseEntity.status(HttpStatus.OK).body("result: true");
    }
}




