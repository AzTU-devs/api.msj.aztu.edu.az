package az.edu.aztu.msj.review;

import az.edu.aztu.msj.article.*;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.HtmlSanitizer;
import az.edu.aztu.msj.notification.NotificationService;
import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReviewerService {

    private static final Set<String> PENDING = Set.of("INVITED", "ACCEPTED", "IN_PROGRESS");
    private static final Set<String> VALID_RECS = Set.of("ACCEPT", "MINOR_REVISION", "MAJOR_REVISION", "REJECT");

    private final ReviewAssignmentRepository assignments;
    private final ReviewRepository reviews;
    private final ArticleRepository articles;
    private final ArticleFileRepository files;
    private final ArticleStatusHistoryRepository history;
    private final UserRepository users;
    private final NotificationService notifications;

    public ReviewerService(ReviewAssignmentRepository assignments, ReviewRepository reviews,
                           ArticleRepository articles, ArticleFileRepository files,
                           ArticleStatusHistoryRepository history, UserRepository users,
                           NotificationService notifications) {
        this.assignments = assignments;
        this.reviews = reviews;
        this.articles = articles;
        this.files = files;
        this.history = history;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.AssignmentSummary> myAssignments(Long uid) {
        return assignments.findByReviewerIdOrderByCreatedAtDesc(uid).stream()
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ReviewDtos.AssignmentDetail getAssignment(Long uid, Long assignmentId) {
        ReviewAssignment ra = owned(uid, assignmentId);
        Article a = articles.findById(ra.getArticleId()).orElseThrow(() -> ApiException.notFound("Article"));
        var authors = a.getAuthors().stream().map(x -> new ReviewDtos.AuthorDto(
                x.getFirstName(), x.getLastName(), x.getEmail(), x.getAffiliation(),
                x.getCountry(), x.getOrcid(), x.isCorresponding())).toList();
        var fileDtos = files.findByArticleIdOrderByCreatedAtDesc(a.getId()).stream()
                .map(f -> new ReviewDtos.FileDto(f.getId(), f.getKind(), f.getOriginalName(),
                        f.getSizeBytes(), f.getContentType())).toList();
        var forReview = new ReviewDtos.ArticleForReview(a.getId(), a.getTitle(), a.getAbstractText(),
                a.getKeywords(), a.getSubjectArea(), a.getLanguage(), a.getStatus(), a.getSubmittedAt(),
                authors, fileDtos);
        ReviewDtos.MyReview my = reviews.findByAssignmentId(assignmentId)
                .map(r -> new ReviewDtos.MyReview(r.getId(), r.getRecommendation(), r.getScore(),
                        r.getCommentsToAuthor(), r.getCommentsToEditor(), r.getSubmittedAt())).orElse(null);
        return new ReviewDtos.AssignmentDetail(summary(ra), forReview, my);
    }

    @Transactional
    public void respond(Long uid, Long assignmentId, boolean accept) {
        ReviewAssignment ra = owned(uid, assignmentId);
        if (!PENDING.contains(ra.getStatus()))
            throw ApiException.badRequest("This invitation was already answered");
        ra.setStatus(accept ? "ACCEPTED" : "DECLINED");
        ra.setRespondedAt(OffsetDateTime.now());
        assignments.save(ra);
    }

    @Transactional
    public ReviewDtos.MyReview submitReview(Long uid, Long assignmentId, ReviewDtos.ReviewInput in) {
        ReviewAssignment ra = owned(uid, assignmentId);
        if ("SUBMITTED".equals(ra.getStatus()))
            throw ApiException.badRequest("You have already submitted this review");
        if (in.recommendation() == null || !VALID_RECS.contains(in.recommendation()))
            throw ApiException.badRequest("A valid recommendation is required");

        Review r = reviews.findByAssignmentId(assignmentId).orElseGet(Review::new);
        r.setAssignmentId(assignmentId);
        r.setArticleId(ra.getArticleId());
        r.setReviewerId(uid);
        r.setRecommendation(in.recommendation());
        r.setScore(in.score());
        r.setCommentsToAuthor(HtmlSanitizer.clean(in.commentsToAuthor()));
        r.setCommentsToEditor(HtmlSanitizer.clean(in.commentsToEditor()));
        reviews.save(r);

        ra.setStatus("SUBMITTED");
        ra.setCompletedAt(OffsetDateTime.now());
        assignments.save(ra);

        maybeMoveToEditor(ra.getArticleId(), uid);

        return new ReviewDtos.MyReview(r.getId(), r.getRecommendation(), r.getScore(),
                r.getCommentsToAuthor(), r.getCommentsToEditor(), r.getSubmittedAt());
    }

    /** When no reviews are still pending, hand the article to the Editor-in-Chief. */
    private void maybeMoveToEditor(Long articleId, Long actor) {
        List<ReviewAssignment> all = assignments.findByArticleIdOrderByCreatedAtAsc(articleId);
        boolean anyPending = all.stream().anyMatch(x -> PENDING.contains(x.getStatus()));
        boolean anySubmitted = all.stream().anyMatch(x -> "SUBMITTED".equals(x.getStatus()));
        if (anyPending || !anySubmitted) return;

        Article a = articles.findById(articleId).orElse(null);
        if (a == null || !"UNDER_REVIEW".equals(a.getStatus())) return;
        String from = a.getStatus();
        a.setStatus("WITH_EDITOR");
        articles.save(a);

        ArticleStatusHistory h = new ArticleStatusHistory();
        h.setArticleId(articleId);
        h.setFromStatus(from);
        h.setToStatus("WITH_EDITOR");
        h.setChangedBy(actor);
        h.setComment("All reviews received");
        history.save(h);

        for (String role : List.of("EDITOR_IN_CHIEF", "ADMIN")) {
            for (User ed : users.findByRole(role)) {
                notifications.notify(ed.getId(), "REVIEWS_COMPLETE",
                        "Reviews complete — decision needed", a.getTitle(), "/articles/" + articleId);
            }
        }
    }

    private ReviewAssignment owned(Long uid, Long assignmentId) {
        ReviewAssignment ra = assignments.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("Assignment"));
        if (!ra.getReviewerId().equals(uid)) throw ApiException.forbidden("Not your assignment");
        return ra;
    }

    private ReviewDtos.AssignmentSummary summary(ReviewAssignment ra) {
        Article a = articles.findById(ra.getArticleId()).orElse(null);
        boolean submitted = reviews.findByAssignmentId(ra.getId()).isPresent();
        return new ReviewDtos.AssignmentSummary(ra.getId(), ra.getArticleId(),
                a == null ? "" : a.getTitle(), a == null ? null : a.getSubjectArea(),
                ra.getStatus(), a == null ? null : a.getStatus(), ra.getDueDate(), ra.getInvitedAt(), submitted);
    }
}
