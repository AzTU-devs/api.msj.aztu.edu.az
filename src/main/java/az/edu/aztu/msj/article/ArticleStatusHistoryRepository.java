package az.edu.aztu.msj.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleStatusHistoryRepository extends JpaRepository<ArticleStatusHistory, Long> {
    List<ArticleStatusHistory> findByArticleIdOrderByCreatedAtAsc(Long articleId);
}
