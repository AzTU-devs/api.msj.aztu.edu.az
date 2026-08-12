package az.edu.aztu.msj.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByArticleIdOrderBySubmittedAtAsc(Long articleId);
    Optional<Review> findByAssignmentId(Long assignmentId);
}
