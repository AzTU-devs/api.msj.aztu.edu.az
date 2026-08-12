package az.edu.aztu.msj.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    List<ReviewAssignment> findByArticleIdOrderByCreatedAtAsc(Long articleId);
    List<ReviewAssignment> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);
    boolean existsByArticleIdAndReviewerIdAndRound(Long articleId, Long reviewerId, int round);
    boolean existsByArticleIdAndReviewerId(Long articleId, Long reviewerId);
    long countByArticleId(Long articleId);
    long countByArticleIdAndStatusIn(Long articleId, List<String> statuses);
}
