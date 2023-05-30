package searchengine.dto.lemma;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double revelance;
}
