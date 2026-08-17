package az.edu.aztu.msj.content;

import az.edu.aztu.msj.article.ArticleDtos;
import az.edu.aztu.msj.article.ArticleService;
import az.edu.aztu.msj.board.BoardMember;
import az.edu.aztu.msj.board.BoardMemberRepository;
import az.edu.aztu.msj.issue.Issue;
import az.edu.aztu.msj.issue.IssueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single aggregate feed for the public homepage — every string and section it
 * renders comes from here, so nothing is hard-coded in the web app.
 */
@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "Homepage feed (public)")
public class HomeController {

    private final JournalSettingsRepository settings;
    private final SiteTextRepository texts;
    private final HeroSlideRepository heroSlides;
    private final ScopeTopicRepository scopeTopics;
    private final AuthorStepRepository authorSteps;
    private final AuthorTermRepository authorTerms;
    private final AnnouncementRepository announcements;
    private final ContentPageRepository pages;
    private final BoardMemberRepository board;
    private final IssueRepository issues;
    private final ArticleService articleService;

    public HomeController(JournalSettingsRepository settings, SiteTextRepository texts,
                          HeroSlideRepository heroSlides, ScopeTopicRepository scopeTopics,
                          AuthorStepRepository authorSteps, AuthorTermRepository authorTerms,
                          AnnouncementRepository announcements, ContentPageRepository pages,
                          BoardMemberRepository board, IssueRepository issues, ArticleService articleService) {
        this.settings = settings;
        this.texts = texts;
        this.heroSlides = heroSlides;
        this.scopeTopics = scopeTopics;
        this.authorSteps = authorSteps;
        this.authorTerms = authorTerms;
        this.announcements = announcements;
        this.pages = pages;
        this.board = board;
        this.issues = issues;
        this.articleService = articleService;
    }

    public record CurrentIssue(Issue issue, List<ArticleDtos.ArticleSummary> articles) {}

    public record HomeResponse(
            JournalSettings settings,
            Map<String, Map<String, String>> texts,
            List<HeroSlide> heroSlides,
            List<ScopeTopic> scopeTopics,
            List<AuthorStep> authorSteps,
            List<AuthorTerm> authorTerms,
            List<BoardMember> board,
            List<Announcement> announcements,
            CurrentIssue currentIssue,
            List<Issue> archive,
            List<Issue> openCalls,
            List<ContentPage> pages) {}

    @GetMapping
    @Operation(summary = "Everything the public homepage renders, in one call")
    public HomeResponse home() {
        Map<String, Map<String, String>> textMap = texts.findAll().stream()
                .collect(Collectors.toMap(SiteText::getKey, SiteText::getValue));

        // "Current issue" = the newest by (year, number), not the lowest sortOrder:
        // sortOrder defaults to 0 for every issue, so ordering by it made Number I
        // of the current year outrank Number II once both were published.
        List<Issue> published = issues.findByStatusOrderByYearDescNumberDescIdDesc("PUBLISHED");
        CurrentIssue current = null;
        if (!published.isEmpty()) {
            Issue latest = published.get(0);
            current = new CurrentIssue(latest, articleService.listByIssue(latest.getId()));
        }
        // archive shows published + archived sections (grouped by year on the web)
        List<Issue> archive = issues.findByStatusInOrderByYearDescNumberAsc(List.of("PUBLISHED", "ARCHIVED"));
        // open calls = sections currently accepting submissions (shown as the current call on the home)
        List<Issue> openCalls = issues.findByStatusOrderBySortOrderAsc("OPEN");

        return new HomeResponse(
                settings.findById((short) 1).orElse(null),
                textMap,
                heroSlides.findByActiveTrueOrderBySortOrderAsc(),
                scopeTopics.findByActiveTrueOrderBySortOrderAsc(),
                authorSteps.findAllByOrderBySortOrderAsc(),
                authorTerms.findAllByOrderBySortOrderAsc(),
                board.findByActiveTrueOrderBySortOrderAsc(),
                announcements.findByStatusOrderByPinnedDescPublishedAtDesc("PUBLISHED"),
                current,
                archive,
                openCalls,
                pages.findByStatusOrderBySortOrderAsc("PUBLISHED")
        );
    }
}
