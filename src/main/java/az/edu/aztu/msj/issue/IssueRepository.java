package az.edu.aztu.msj.issue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByStatusOrderBySortOrderAsc(String status);

    /**
     * Newest issue first, by the journal's own numbering: 2025 Number II ranks
     * above 2025 Number I, which ranks above 2024 Number II. Used to pick the
     * "current issue" on the homepage.
     *
     * The homepage previously took the first row of
     * {@link #findByStatusOrderBySortOrderAsc(String)} and called it the latest.
     * sortOrder is a manual display order that defaults to 0 for every issue, so
     * with both numbers of a year published the tie broke on insertion order and
     * Number I was shown as current for the rest of the year.
     */
    List<Issue> findByStatusOrderByYearDescNumberDescIdDesc(String status);

    /** Archive: publicly-visible issues grouped by year (newest first), Number I before II. */
    List<Issue> findByStatusInOrderByYearDescNumberAsc(List<String> statuses);

    Optional<Issue> findBySlug(String slug);

    boolean existsByYearAndNumber(Integer year, Integer number);

    /** Sections currently accepting submissions (author dropdown). */
    @Query("""
            select i from Issue i
            where i.status = 'OPEN'
              and (i.submissionDeadline is null or i.submissionDeadline >= :today)
            order by i.submissionDeadline asc, i.year desc, i.number asc
            """)
    List<Issue> findOpenForSubmission(@Param("today") LocalDate today);
}
