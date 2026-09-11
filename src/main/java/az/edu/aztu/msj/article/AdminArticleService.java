package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.TextSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Lets editors add, edit, and delete articles directly (outside the author-submission flow). */
@Service
public class AdminArticleService {

    private final ArticleRepository articles;
    private final ArticleStatusHistoryRepository history;

    public AdminArticleService(ArticleRepository articles, ArticleStatusHistoryRepository history) {
        this.articles = articles;
        this.history = history;
    }

    @Transactional
    public Long createPublished(AdminArticleDtos.CreateArticleRequest req, Long editorId) {
        requireAuthors(req.authors());
        Article a = new Article();
        a.setStatus("PUBLISHED");
        a.setSubmitterId(editorId);                 // the editor who added it
        a.setHandlingEditorId(editorId);
        a.setSubmittedAt(OffsetDateTime.now());
        a.setDecidedAt(OffsetDateTime.now());
        a.setPublishedAt(LocalDate.now());
        a.setPageStart(req.pageStart());
        a.setPageEnd(req.pageEnd());
        a.setArticleOrder(req.articleOrder());
        applyMetadata(a, req);
        articles.save(a);

        ArticleStatusHistory h = new ArticleStatusHistory();
        h.setArticleId(a.getId());
        h.setFromStatus(null);
        h.setToStatus("PUBLISHED");
        h.setChangedBy(editorId);
        h.setComment("Added directly via the editorial console");
        history.save(h);

        return a.getId();
    }

    @Transactional
    public void update(Long id, AdminArticleDtos.CreateArticleRequest req, Long editorId) {
        requireAuthors(req.authors());
        Article a = articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        applyMetadata(a, req);
        // page range / order are only edited when supplied, so metadata-only edits keep them
        if (req.pageStart() != null) a.setPageStart(req.pageStart());
        if (req.pageEnd() != null) a.setPageEnd(req.pageEnd());
        if (req.articleOrder() != null) a.setArticleOrder(req.articleOrder());
        articles.save(a);
    }

    @Transactional
    public void delete(Long id) {
        if (!articles.existsById(id)) throw ApiException.notFound("Article");
        // article_authors / files / status_history / reviews / metrics all
        // REFERENCE articles(id) ON DELETE CASCADE, so the DB cleans them up.
        articles.deleteById(id);
    }

    // ---- helpers ----

    private void requireAuthors(List<AdminArticleDtos.AuthorInput> authors) {
        if (authors == null || authors.isEmpty()) {
            throw ApiException.badRequest("At least one author is required");
        }
    }

    /**
     * Shared title/abstract/keywords/subject/language/doi/issue + author replacement.
     *
     * <p>Sanitised on the same terms as the author-submission path: an editor
     * account is more trusted, but it is still an account that can be phished,
     * and these values are published verbatim on the public site.
     */
    private void applyMetadata(Article a, AdminArticleDtos.CreateArticleRequest req) {
        String title = TextSanitizer.title(req.title());
        if (title == null) throw ApiException.badRequest("Title is required");

        a.setTitle(title);
        a.setAbstractText(TextSanitizer.longText(req.abstractText()));
        a.setKeywords(TextSanitizer.text(req.keywords(), 1000));
        a.setSubjectArea(TextSanitizer.shortText(req.subjectArea()));
        String lang = req.language() == null ? "" : req.language().trim().toLowerCase();
        a.setLanguage(List.of("en", "az", "ru").contains(lang) ? lang : "en");
        // blank DOI must be NULL, not "" — the unique index ux_articles_doi treats
        // every "" as the same value, so two DOI-less articles would collide.
        a.setDoi(TextSanitizer.shortText(req.doi()));
        a.setIssueId(req.issueId());

        a.getAuthors().clear();   // orphanRemoval deletes the previous rows
        List<AdminArticleDtos.AuthorInput> in = req.authors();
        boolean anyCorresponding = in.stream().anyMatch(AdminArticleDtos.AuthorInput::corresponding);
        for (int i = 0; i < in.size(); i++) {
            AdminArticleDtos.AuthorInput ai = in.get(i);
            String first = TextSanitizer.shortText(ai.firstName());
            String last = TextSanitizer.shortText(ai.lastName());
            if (first == null || last == null)
                throw ApiException.badRequest("Each author needs a first and last name");

            ArticleAuthor au = new ArticleAuthor();
            au.setFirstName(first);
            au.setLastName(last);
            au.setEmail(TextSanitizer.email(ai.email()));
            au.setAffiliation(TextSanitizer.text(ai.affiliation(), 1000));
            au.setCountry(TextSanitizer.shortText(ai.country()));
            au.setOrcid(TextSanitizer.shortText(ai.orcid()));
            au.setAuthorOrder(i);
            au.setCorresponding(ai.corresponding() || (!anyCorresponding && i == 0));
            a.addAuthor(au);
        }
    }
}
