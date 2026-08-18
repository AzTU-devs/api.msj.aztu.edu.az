package az.edu.aztu.msj.review;

import az.edu.aztu.msj.article.*;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.issue.Issue;
import az.edu.aztu.msj.issue.IssueRepository;
import az.edu.aztu.msj.notification.NotificationService;
import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EditorialService {

    private static final Set<String> CAN_ASSIGN =
            Set.of("SUBMITTED", "RESUBMITTED", "UNDER_REVIEW", "WITH_EDITOR");

    private final UserRepository users;
    private final ArticleRepository articles;
    private final ArticleFileRepository files;
    private final ArticleStatusHistoryRepository history;
    private final ReviewAssignmentRepository assignments;
    private final ReviewRepository reviews;
    private final NotificationService notifications;
    private final IssueRepository issues;

    public EditorialService(UserRepository users, ArticleRepository articles, ArticleFileRepository files,
                            ArticleStatusHistoryRepository history, ReviewAssignmentRepository assignments,
                            ReviewRepository reviews, NotificationService notifications,
                            IssueRepository issues) {
        this.users = users;
        this.articles = articles;
        this.files = files;
        this.history = history;
        this.assignments = assignments;
        this.reviews = reviews;
        this.notifications = notifications;
        this.issues = issues;
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.ReviewerUser> reviewers() {
        return users.findByRole("REVIEWER").stream()
                .map(u -> new ReviewDtos.ReviewerUser(u.getId(), u.fullName(), u.getEmail(), u.getAffiliation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewDtos.EditorialArticleDetail detail(Long articleId) {
        Article a = articles.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        Map<Long, String> names = userNames();

        var authors = a.getAuthors().stream().map(x -> new ReviewDtos.AuthorDto(
                x.getFirstName(), x.getLastName(), x.getEmail(), x.getAffiliation(),
                x.getCountry(), x.getOrcid(), x.isCorresponding())).toList();
        var fileDtos = files.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(f -> new ReviewDtos.FileDto(f.getId(), f.getKind(), f.getOriginalName(),
                        f.getSizeBytes(), f.getContentType())).toList();

        Map<Long, Boolean> hasReview = reviews.findByArticleIdOrderBySubmittedAtAsc(articleId).stream()
                .collect(Collectors.toMap(Review::getAssignmentId, r -> true, (x, y) -> x));
        var assignmentDtos = assignments.findByArticleIdOrderByCreatedAtAsc(articleId).stream()
                .map(ra -> new ReviewDtos.AssignmentDto(ra.getId(), ra.getReviewerId(),
                        names.getOrDefault(ra.getReviewerId(), "Reviewer #" + ra.getReviewerId()),
                        ra.getStatus(), ra.getDueDate(), ra.getInvitedAt(), ra.getCompletedAt(),
                        hasReview.getOrDefault(ra.getId(), false))).toList();

        var reviewDtos = reviews.findByArticleIdOrderBySubmittedAtAsc(articleId).stream()
                .map(r -> new ReviewDtos.EditorReview(r.getId(), r.getReviewerId(),
                        names.getOrDefault(r.getReviewerId(), "Reviewer #" + r.getReviewerId()),
                        r.getRecommendation(), r.getScore(), r.getCommentsToAuthor(),
                        r.getCommentsToEditor(), r.getSubmittedAt())).toList();

        var events = history.findByArticleIdOrderByCreatedAtAsc(articleId).stream()
                .map(h -> new ReviewDtos.StatusEvent(h.getFromStatus(), h.getToStatus(),
                        h.getChangedBy() == null ? null : names.get(h.getChangedBy()), h.getComment(), h.getCreatedAt()))
                .toList();

        // Volume and number live on the issue, not the article — carried here
        // so the editor form can show them without a second round trip.
        Issue issue = a.getIssueId() == null ? null : issues.findById(a.getIssueId()).orElse(null);

        return new ReviewDtos.EditorialArticleDetail(a.getId(), a.getTitle(), a.getAbstractText(), a.getKeywords(),
                a.getSubjectArea(), a.getLanguage(), a.getStatus(), a.getDoi(), a.getIssueId(),
                a.getPageStart(), a.getPageEnd(), a.getArticleOrder(),
                issue == null ? null : issue.getTitle(),
                issue == null ? null : issue.getVolume(),
                issue == null ? null : issue.getNumber(),
                a.getSubmittedAt(), a.getCreatedAt(), authors, fileDtos, assignmentDtos, reviewDtos, events);
    }

    @Transactional
    public void assign(Long articleId, ReviewDtos.AssignRequest req, Long actor) {
        Article a = articles.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        if (!CAN_ASSIGN.contains(a.getStatus()))
            throw ApiException.badRequest("Cannot assign reviewers while status is " + a.getStatus());
        if (req.reviewerIds() == null || req.reviewerIds().isEmpty())
            throw ApiException.badRequest("Select at least one reviewer");

        for (Long reviewerId : req.reviewerIds()) {
            User reviewer = users.findById(reviewerId).orElse(null);
            if (reviewer == null || assignments.existsByArticleIdAndReviewerId(articleId, reviewerId)) continue;
            ReviewAssignment ra = new ReviewAssignment();
            ra.setArticleId(articleId);
            ra.setReviewerId(reviewerId);
            ra.setAssignedBy(actor);
            ra.setStatus("INVITED");
            ra.setDueDate(req.dueDate());
            assignments.save(ra);
            notifications.notify(reviewerId, "REVIEW_ASSIGNED",
                    "New review assignment", a.getTitle(), "/reviewer");
        }

        if (!"UNDER_REVIEW".equals(a.getStatus())) {
            String from = a.getStatus();
            a.setStatus("UNDER_REVIEW");
            articles.save(a);
            addHistory(articleId, from, "UNDER_REVIEW", actor, "Assigned to reviewers");
        }
    }

    @Transactional
    public void cancelAssignment(Long articleId, Long assignmentId) {
        ReviewAssignment ra = assignments.findById(assignmentId).orElseThrow(() -> ApiException.notFound("Assignment"));
        if (!ra.getArticleId().equals(articleId)) throw ApiException.badRequest("Assignment/article mismatch");
        ra.setStatus("CANCELLED");
        assignments.save(ra);
    }

    @Transactional
    public void decide(Long articleId, ReviewDtos.DecisionRequest req, Long actor) {
        Article a = articles.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        String from = a.getStatus();
        String to;
        String event;
        switch (req.decision() == null ? "" : req.decision().toUpperCase()) {
            case "PUBLISH" -> {
                to = "PUBLISHED";
                a.setStatus(to);
                a.setDecidedAt(OffsetDateTime.now());
                if (a.getPublishedAt() == null) a.setPublishedAt(LocalDate.now());
                if (req.issueId() != null) a.setIssueId(req.issueId());
                event = "PUBLISHED — the article is now live";
            }
            case "REVISE" -> {
                to = "REVISION_REQUESTED";
                a.setStatus(to);
                event = "Revision requested";
            }
            case "REJECT" -> {
                to = "REJECTED";
                a.setStatus(to);
                a.setDecidedAt(OffsetDateTime.now());
                event = "Rejected";
            }
            default -> throw ApiException.badRequest("decision must be PUBLISH, REVISE or REJECT");
        }
        articles.save(a);
        addHistory(articleId, from, to, actor, req.note() == null ? event : req.note());

        // notify the submitting author
        String title = switch (to) {
            case "PUBLISHED" -> "Your manuscript has been published";
            case "REVISION_REQUESTED" -> "Revision requested for your manuscript";
            default -> "Decision on your manuscript";
        };
        notifications.notify(a.getSubmitterId(), "DECISION_" + to, title, a.getTitle(), "/dashboard");
    }

    private void addHistory(Long articleId, String from, String to, Long by, String comment) {
        ArticleStatusHistory h = new ArticleStatusHistory();
        h.setArticleId(articleId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(by);
        h.setComment(comment);
        history.save(h);
    }

    private Map<Long, String> userNames() {
        return users.findAll().stream().collect(Collectors.toMap(User::getId, User::fullName, (x, y) -> x));
    }
}
