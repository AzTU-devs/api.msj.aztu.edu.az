package az.edu.aztu.msj.content;

import az.edu.aztu.msj.board.BoardMember;
import az.edu.aztu.msj.board.BoardMemberRepository;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.issue.Issue;
import az.edu.aztu.msj.issue.IssueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for every editable content type. Secured to ADMIN / EDITOR* by
 * {@code SecurityConfig} (path {@code /api/v1/admin/**}).
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin — content management")
public class ContentAdminController {

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

    public ContentAdminController(JournalSettingsRepository settings, SiteTextRepository texts,
                                  HeroSlideRepository heroSlides, ScopeTopicRepository scopeTopics,
                                  AuthorStepRepository authorSteps, AuthorTermRepository authorTerms,
                                  AnnouncementRepository announcements, ContentPageRepository pages,
                                  BoardMemberRepository board, IssueRepository issues) {
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
    }

    // ---- journal settings (singleton) ----
    @GetMapping("/settings")
    @Operation(summary = "Get journal settings for editing")
    public JournalSettings getSettings() {
        return settings.findById((short) 1).orElseThrow(() -> ApiException.notFound("Settings"));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update journal settings")
    public JournalSettings saveSettings(@RequestBody JournalSettings body) {
        body.setId((short) 1);
        return settings.save(body);
    }

    // ---- site texts (i18n labels) ----
    @GetMapping("/texts")
    @Operation(summary = "All UI labels/copy for editing")
    public List<SiteText> listTexts() {
        return texts.findAll();
    }

    @PutMapping("/texts/{key}")
    @Operation(summary = "Create or update one UI label")
    public SiteText saveText(@PathVariable String key, @RequestBody Map<String, String> value) {
        SiteText t = texts.findById(key).orElseGet(() -> {
            SiteText s = new SiteText();
            s.setKey(key);
            return s;
        });
        t.setValue(value);
        return texts.save(t);
    }

    @DeleteMapping("/texts/{key}")
    public ResponseEntity<Void> deleteText(@PathVariable String key) {
        texts.deleteById(key);
        return ResponseEntity.noContent().build();
    }

    // ---- hero slides ----
    @GetMapping("/hero-slides")
    public List<HeroSlide> listSlides() { return heroSlides.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/hero-slides")
    public HeroSlide createSlide(@RequestBody HeroSlide s) { s.setId(null); return heroSlides.save(s); }

    @PutMapping("/hero-slides/{id}")
    public HeroSlide updateSlide(@PathVariable Long id, @RequestBody HeroSlide s) {
        s.setId(id); return heroSlides.save(s);
    }

    @DeleteMapping("/hero-slides/{id}")
    public ResponseEntity<Void> deleteSlide(@PathVariable Long id) {
        heroSlides.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- scope topics ----
    @GetMapping("/scope-topics")
    public List<ScopeTopic> listScope() { return scopeTopics.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/scope-topics")
    public ScopeTopic createScope(@RequestBody ScopeTopic s) { s.setId(null); return scopeTopics.save(s); }

    @PutMapping("/scope-topics/{id}")
    public ScopeTopic updateScope(@PathVariable Long id, @RequestBody ScopeTopic s) {
        s.setId(id); return scopeTopics.save(s);
    }

    @DeleteMapping("/scope-topics/{id}")
    public ResponseEntity<Void> deleteScope(@PathVariable Long id) {
        scopeTopics.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- author steps ----
    @GetMapping("/author-steps")
    public List<AuthorStep> listSteps() { return authorSteps.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/author-steps")
    public AuthorStep createStep(@RequestBody AuthorStep s) { s.setId(null); return authorSteps.save(s); }

    @PutMapping("/author-steps/{id}")
    public AuthorStep updateStep(@PathVariable Long id, @RequestBody AuthorStep s) {
        s.setId(id); return authorSteps.save(s);
    }

    @DeleteMapping("/author-steps/{id}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long id) {
        authorSteps.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- author terms ----
    @GetMapping("/author-terms")
    public List<AuthorTerm> listTerms() { return authorTerms.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/author-terms")
    public AuthorTerm createTerm(@RequestBody AuthorTerm s) { s.setId(null); return authorTerms.save(s); }

    @PutMapping("/author-terms/{id}")
    public AuthorTerm updateTerm(@PathVariable Long id, @RequestBody AuthorTerm s) {
        s.setId(id); return authorTerms.save(s);
    }

    @DeleteMapping("/author-terms/{id}")
    public ResponseEntity<Void> deleteTerm(@PathVariable Long id) {
        authorTerms.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- announcements ----
    @GetMapping("/announcements")
    public List<Announcement> listAnnouncements() { return announcements.findAllByOrderByPinnedDescPublishedAtDesc(); }

    @PostMapping("/announcements")
    public Announcement createAnnouncement(@RequestBody Announcement a) { a.setId(null); return announcements.save(a); }

    @PutMapping("/announcements/{id}")
    public Announcement updateAnnouncement(@PathVariable Long id, @RequestBody Announcement a) {
        a.setId(id); return announcements.save(a);
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcements.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- content pages ----
    @GetMapping("/pages")
    public List<ContentPage> listPages() { return pages.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/pages")
    public ContentPage createPage(@RequestBody ContentPage p) { p.setId(null); return pages.save(p); }

    @PutMapping("/pages/{id}")
    public ContentPage updatePage(@PathVariable Long id, @RequestBody ContentPage p) {
        p.setId(id); return pages.save(p);
    }

    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        pages.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- editorial board ----
    @GetMapping("/board")
    public List<BoardMember> listBoard() { return board.findAll(); }

    @PostMapping("/board")
    public BoardMember createBoard(@RequestBody BoardMember m) { m.setId(null); return board.save(m); }

    @PutMapping("/board/{id}")
    public BoardMember updateBoard(@PathVariable Long id, @RequestBody BoardMember m) {
        m.setId(id); return board.save(m);
    }

    @DeleteMapping("/board/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        board.deleteById(id); return ResponseEntity.noContent().build();
    }

    // ---- issues ----
    @GetMapping("/issues")
    public List<Issue> listIssues() { return issues.findAll(); }

    @PostMapping("/issues")
    public Issue createIssue(@RequestBody Issue i) { i.setId(null); return issues.save(i); }

    @PutMapping("/issues/{id}")
    public Issue updateIssue(@PathVariable Long id, @RequestBody Issue i) {
        i.setId(id); return issues.save(i);
    }

    @DeleteMapping("/issues/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        issues.deleteById(id); return ResponseEntity.noContent().build();
    }

    /** Open a new year: create its two sections (Number I & II) as drafts if absent. */
    @PostMapping("/years/{year}")
    @Operation(summary = "Open a new year — creates Number I and Number II as drafts")
    public List<Issue> openYear(@PathVariable int year) {
        String[] roman = {"I", "II"};
        for (int n = 1; n <= 2; n++) {
            if (!issues.existsByYearAndNumber(year, n)) {
                Issue i = new Issue();
                i.setYear(year);
                i.setNumber(n);
                i.setTitle("Machine Science " + year + " - Number " + roman[n - 1]);
                i.setSlug("machine-science-" + year + "-" + n);
                i.setStatus("DRAFT");
                i.setSortOrder(0);
                issues.save(i);
            }
        }
        return issues.findAll().stream()
                .filter(i -> i.getYear() != null && i.getYear() == year)
                .sorted((a, b) -> Integer.compare(a.getNumber() == null ? 0 : a.getNumber(),
                                                  b.getNumber() == null ? 0 : b.getNumber()))
                .toList();
    }
}
