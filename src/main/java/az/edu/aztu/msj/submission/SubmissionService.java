package az.edu.aztu.msj.submission;

import az.edu.aztu.msj.article.*;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.notification.NotificationService;
import az.edu.aztu.msj.review.Review;
import az.edu.aztu.msj.review.ReviewRepository;
import az.edu.aztu.msj.storage.FileStorageService;
import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SubmissionService {

    private static final Set<String> EDITABLE = Set.of("DRAFT", "REVISION_REQUESTED");
    private static final Set<String> AUTHOR_CAN_SEE_REVIEWS =
            Set.of("REVISION_REQUESTED", "RESUBMITTED", "ACCEPTED", "REJECTED", "PUBLISHED");

    private final ArticleRepository articles;
    private final ArticleFileRepository files;
    private final ArticleStatusHistoryRepository history;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final FileStorageService storage;
    private final NotificationService notifications;

    public SubmissionService(ArticleRepository articles, ArticleFileRepository files,
                             ArticleStatusHistoryRepository history, ReviewRepository reviews,
                             UserRepository users, FileStorageService storage, NotificationService notifications) {
        this.articles = articles;
        this.files = files;
        this.history = history;
        this.reviews = reviews;
        this.users = users;
        this.storage = storage;
        this.notifications = notifications;
    }

    @Transactional
    public SubmissionDtos.SubmissionDetail create(Long uid, SubmissionDtos.SubmissionInput in) {
        Article a = new Article();
        a.setStatus("DRAFT");
        a.setSubmitterId(uid);
        applyInput(a, in);
        articles.save(a);
        return detail(a, uid);
    }

    @Transactional
    public SubmissionDtos.SubmissionDetail update(Long uid, Long id, SubmissionDtos.SubmissionInput in) {
        Article a = owned(uid, id);
        requireEditable(a);
        applyInput(a, in);
        articles.save(a);
        return detail(a, uid);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDtos.SubmissionSummary> listMine(Long uid) {
        return articles.findBySubmitterIdOrderByCreatedAtDesc(uid,
                        org.springframework.data.domain.Pageable.unpaged())
                .map(a -> new SubmissionDtos.SubmissionSummary(a.getId(), a.getTitle(), a.getStatus(),
                        a.getSubjectArea(), a.getSubmittedAt(), a.getUpdatedAt()))
                .getContent();
    }

    @Transactional(readOnly = true)
    public SubmissionDtos.SubmissionDetail getMine(Long uid, Long id) {
        return detail(owned(uid, id), uid);
    }

    @Transactional
    public SubmissionDtos.FileDto upload(Long uid, Long id, MultipartFile file, String kind) {
        Article a = owned(uid, id);
        requireEditable(a);
        if (file.isEmpty()) throw ApiException.badRequest("Empty file");
        String k = normalizeKind(kind);
        int version = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .filter(f -> f.getKind().equals(k)).map(ArticleFile::getVersion).max(Integer::compareTo).orElse(0) + 1;
        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String key = "articles/" + id + "/" + k.toLowerCase() + "-v" + version + ext;
        storage.store(file, key);
        ArticleFile af = new ArticleFile();
        af.setArticleId(id);
        af.setKind(k);
        af.setOriginalName(original);
        af.setStorageKey(key);
        af.setContentType(file.getContentType());
        af.setSizeBytes(file.getSize());
        af.setVersion(version);
        af.setUploadedBy(uid);
        files.save(af);
        return toFileDto(af);
    }

    @Transactional
    public void deleteFile(Long uid, Long id, Long fileId) {
        Article a = owned(uid, id);
        requireEditable(a);
        ArticleFile f = files.findById(fileId).orElseThrow(() -> ApiException.notFound("File"));
        if (!f.getArticleId().equals(id)) throw ApiException.badRequest("File does not belong to this submission");
        files.deleteById(fileId);
    }

    @Transactional
    public SubmissionDtos.SubmissionDetail submit(Long uid, Long id) {
        Article a = owned(uid, id);
        requireEditable(a);
        // required fields
        if (blank(a.getTitle()) || blank(a.getAbstractText()) || blank(a.getKeywords()) || blank(a.getSubjectArea()))
            throw ApiException.badRequest("Title, abstract, keywords and subject area are required");
        if (a.getAuthors().isEmpty())
            throw ApiException.badRequest("At least one author is required");
        boolean hasManuscript = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .anyMatch(f -> f.getKind().equals("MANUSCRIPT"));
        if (!hasManuscript)
            throw ApiException.badRequest("A manuscript PDF is required");

        String from = a.getStatus();
        String to = "REVISION_REQUESTED".equals(from) ? "RESUBMITTED" : "SUBMITTED";
        a.setStatus(to);
        a.setSubmittedAt(OffsetDateTime.now());
        articles.save(a);
        addHistory(id, from, to, uid, null);

        // notify the editorial desk
        String kind = "RESUBMITTED".equals(to) ? "resubmitted" : "submitted";
        for (String role : List.of("EDITOR_IN_CHIEF", "EDITOR", "ADMIN")) {
            for (User editor : users.findByRole(role)) {
                notifications.notify(editor.getId(), "SUBMISSION_" + to,
                        "Manuscript " + kind, a.getTitle(), "/articles/" + id);
            }
        }
        return detail(a, uid);
    }

    // ---- helpers ----

    private void applyInput(Article a, SubmissionDtos.SubmissionInput in) {
        a.setTitle(in.title());
        a.setAbstractText(in.abstractText());
        a.setKeywords(in.keywords());
        a.setSubjectArea(in.subjectArea());
        a.setLanguage(in.language() == null || in.language().isBlank() ? "en" : in.language());

        a.getAuthors().clear();
        List<SubmissionDtos.AuthorInput> in2 = in.authors();
        boolean anyCorresponding = in2.stream().anyMatch(SubmissionDtos.AuthorInput::corresponding);
        for (int i = 0; i < in2.size(); i++) {
            SubmissionDtos.AuthorInput ai = in2.get(i);
            ArticleAuthor au = new ArticleAuthor();
            au.setFirstName(ai.firstName());
            au.setLastName(ai.lastName());
            au.setEmail(ai.email());
            au.setAffiliation(ai.affiliation());
            au.setCountry(ai.country());
            au.setOrcid(ai.orcid());
            au.setAuthorOrder(i);
            au.setCorresponding(ai.corresponding() || (!anyCorresponding && i == 0));
            a.addAuthor(au);
        }
    }

    private Article owned(Long uid, Long id) {
        Article a = articles.findById(id).orElseThrow(() -> ApiException.notFound("Submission"));
        if (!a.getSubmitterId().equals(uid)) throw ApiException.forbidden("Not your submission");
        return a;
    }

    private void requireEditable(Article a) {
        if (!EDITABLE.contains(a.getStatus()))
            throw ApiException.badRequest("This submission can no longer be edited (status " + a.getStatus() + ")");
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

    private SubmissionDtos.SubmissionDetail detail(Article a, Long uid) {
        List<SubmissionDtos.AuthorDto> authors = a.getAuthors().stream()
                .map(x -> new SubmissionDtos.AuthorDto(x.getFirstName(), x.getLastName(), x.getEmail(),
                        x.getAffiliation(), x.getCountry(), x.getOrcid(), x.isCorresponding()))
                .toList();
        List<SubmissionDtos.FileDto> fileDtos = files.findByArticleIdOrderByCreatedAtDesc(a.getId()).stream()
                .map(this::toFileDto).toList();
        List<ArticleStatusHistory> hist = history.findByArticleIdOrderByCreatedAtAsc(a.getId());
        List<SubmissionDtos.StatusEvent> events = hist.stream()
                .map(h -> new SubmissionDtos.StatusEvent(h.getFromStatus(), h.getToStatus(), h.getComment(), h.getCreatedAt()))
                .toList();
        String editorNote = hist.stream()
                .filter(h -> Set.of("REVISION_REQUESTED", "REJECTED", "PUBLISHED").contains(h.getToStatus()))
                .reduce((a1, b1) -> b1).map(ArticleStatusHistory::getComment).orElse(null);

        List<SubmissionDtos.ReviewForAuthor> reviewDtos = List.of();
        if (AUTHOR_CAN_SEE_REVIEWS.contains(a.getStatus())) {
            reviewDtos = reviews.findByArticleIdOrderBySubmittedAtAsc(a.getId()).stream()
                    .map(r -> new SubmissionDtos.ReviewForAuthor(r.getRecommendation(), r.getCommentsToAuthor(), r.getSubmittedAt()))
                    .toList();
        }
        boolean canEdit = EDITABLE.contains(a.getStatus());
        return new SubmissionDtos.SubmissionDetail(a.getId(), a.getTitle(), a.getAbstractText(), a.getKeywords(),
                a.getSubjectArea(), a.getLanguage(), a.getStatus(), a.getDoi(), a.getSubmittedAt(),
                a.getCreatedAt(), a.getUpdatedAt(), authors, fileDtos, events, reviewDtos, editorNote, canEdit);
    }

    private SubmissionDtos.FileDto toFileDto(ArticleFile f) {
        return new SubmissionDtos.FileDto(f.getId(), f.getKind(), f.getOriginalName(),
                f.getSizeBytes(), f.getContentType(), f.getCreatedAt());
    }

    private String normalizeKind(String kind) {
        String k = kind == null ? "MANUSCRIPT" : kind.toUpperCase();
        return Set.of("MANUSCRIPT", "SUPPLEMENTARY", "COVER_LETTER", "REVISION").contains(k) ? k : "MANUSCRIPT";
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
