package az.edu.aztu.msj.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStatusOrderBySortOrderAsc(String status);
    Optional<Issue> findBySlug(String slug);
}
