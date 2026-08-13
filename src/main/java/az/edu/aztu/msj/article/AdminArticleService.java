package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Lets editors add a fully-published article directly into an issue. */
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
        List<AdminArticleDtos.AuthorInput> authorInputs = req.authors();
        if (authorInputs == null || authorInputs.isEmpty()) {
            throw ApiException.badRequest("At least one author is required");
        }

        Article a = new Article();
        a.setTitle(req.title());
        a.setAbstractText(req.abstractText());
        a.setKeywords(req.keywords());
        a.setSubjectArea(req.subjectArea());
        a.setLanguage(req.language() == null || req.language().isBlank() ? "en" : req.language());
        a.setDoi(req.doi());
        a.setIssueId(req.issueId());
        a.setPageStart(req.pageStart());
        a.setPageEnd(req.pageEnd());
        a.setArticleOrder(req.articleOrder());
        a.setStatus("PUBLISHED");
        a.setSubmitterId(editorId);                 // the editor who added it
        a.setHandlingEditorId(editorId);
        a.setSubmittedAt(OffsetDateTime.now());
        a.setDecidedAt(OffsetDateTime.now());
        a.setPublishedAt(LocalDate.now());

        boolean anyCorresponding = authorInputs.stream().anyMatch(AdminArticleDtos.AuthorInput::corresponding);
        for (int i = 0; i < authorInputs.size(); i++) {
            AdminArticleDtos.AuthorInput ai = authorInputs.get(i);
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
}
