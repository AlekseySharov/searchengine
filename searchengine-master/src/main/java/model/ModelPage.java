package model;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

    @Entity
    @Table(name = "page")
   public class ModelPage {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "site_id", nullable = false)
        private ModelSite site;

        @Column(name = "path", nullable = false)
        private String path;

        @Column(name = "code", nullable = false)
        private int code;

        @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
        private String content;
}
