package az.edu.aztu.msj.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByDoi(String doi);

    /** Published articles that carry a DOI — the candidates for a Crossref citation lookup. */
    @Query("select a from Article a where a.status = 'PUBLISHED' and a.doi is not null and a.doi <> ''")
    List<Article> findPublishedWithDoi();

    List<Article> findByIssueIdOrderByArticleOrderAscTitleAsc(Long issueId);

    List<Article> findByIssueIdAndStatusOrderByArticleOrderAscTitleAsc(Long issueId, String status);

    @Query("""
            select a from Article a
            where a.status = 'PUBLISHED'
              and (:issueId is null or a.issueId = :issueId)
              and (:subject is null or a.subjectArea = :subject)
              and (:q is null or lower(a.title)    like lower(concat('%', cast(:q as string), '%'))
                              or lower(a.keywords) like lower(concat('%', cast(:q as string), '%')))
            """)
    Page<Article> searchPublished(@Param("q") String q,
                                  @Param("issueId") Long issueId,
                                  @Param("subject") String subject,
                                  Pageable pageable);

    Page<Article> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId, Pageable pageable);

    @Query("select a from Article a where (:status is null or a.status = :status) order by a.createdAt desc")
    Page<Article> findForAdmin(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);
}
