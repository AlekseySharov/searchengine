package searchengine.dto.lemma;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class SearchResult {
    private Boolean result;
    private Long count;
    private List<SearchResultItem> data;
    private String error;
}
