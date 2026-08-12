package az.edu.aztu.msj.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleFileRepository extends JpaRepository<ArticleFile, Long> {

    List<ArticleFile> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    Optional<ArticleFile> findFirstByArticleIdAndKindOrderByVersionDesc(Long articleId, String kind);
}
