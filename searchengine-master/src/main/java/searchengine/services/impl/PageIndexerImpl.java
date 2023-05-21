package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.ModelIndex;
import searchengine.model.Lemma;
import searchengine.model.ModelPage;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class PageIndexerImpl implements PageIndexer {
    private LemmaService lemmaService;
    private LemmaRepository lemmaRepository;
    private IndexSearchRepository indexSearchRepository;

    @Override
    public void indexHtml(String html, ModelPage indexingPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.warn("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }

    }

    @Transactional
    private void saveLemma(String k, Integer v, ModelPage indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, v);
        } else {
            try {
                Lemma newLemmaToDB = new Lemma();
                newLemmaToDB.setSiteId(indexingPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSitePage(indexingPage.getSitePage());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, v);
            } catch (DataIntegrityViolationException ex) {
                log.error("Ошибка при сохранении леммы, такая лемма уже существует. Вызов повторного сохранения");
                saveLemma(k, v, indexingPage);
            }
        }
    }

    private void createIndex(ModelPage indexingPage, Lemma lemmaInDB, Integer rank) {
        ModelIndex ModelIndexExist = indexSearchRepository.ModelIndexExist(indexingPage.getId(), lemmaInDB.getId());
        if (ModelIndexExist != null) {
            ModelIndexExist.setLemmaCount(ModelIndexExist.getLemmaCount() + rank);
            indexSearchRepository.save(ModelIndexExist);
        } else {
            ModelIndex index = new ModelIndex();
            index.setPageId(indexingPage.getId());
            index.setLemmaId(lemmaInDB.getId());
            index.setLemmaCount(rank);
            index.setLemma(lemmaInDB);
            index.setPage(indexingPage);
            indexSearchRepository.save(index);
        }
    }
}
