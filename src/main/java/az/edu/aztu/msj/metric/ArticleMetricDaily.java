package az.edu.aztu.msj.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "article_metric_daily")
@IdClass(ArticleMetricDaily.Key.class)
@Getter
@Setter
public class ArticleMetricDaily {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    private LocalDate day;

    @Column(nullable = false)
    private long views;

    @Column(nullable = false)
    private long downloads;

    public static class Key implements Serializable {
        private Long articleId;
        private LocalDate day;

        public Key() {}

        public Key(Long articleId, LocalDate day) {
            this.articleId = articleId;
            this.day = day;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(articleId, key.articleId) && Objects.equals(day, key.day);
        }

        @Override
        public int hashCode() {
            return Objects.hash(articleId, day);
        }
    }
}
