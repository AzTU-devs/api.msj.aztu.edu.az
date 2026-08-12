package az.edu.aztu.msj.content;

import az.edu.aztu.msj.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Site content (public)")
public class PublicContentController {

    private final JournalSettingsRepository settings;
    private final ContentPageRepository pages;
    private final AnnouncementRepository announcements;

    public PublicContentController(JournalSettingsRepository settings, ContentPageRepository pages,
                                   AnnouncementRepository announcements) {
        this.settings = settings;
        this.pages = pages;
        this.announcements = announcements;
    }

    public record PageDto(Long id, String slug, Map<String, String> title,
                          Map<String, String> body, int sortOrder) {
        static PageDto from(ContentPage p) {
            return new PageDto(p.getId(), p.getSlug(), p.getTitle(), p.getBody(), p.getSortOrder());
        }
    }

    public record AnnouncementDto(Long id, Map<String, String> title, Map<String, String> body,
                                  String imageUrl, String linkUrl, boolean pinned, LocalDate publishedAt) {
        static AnnouncementDto from(Announcement a) {
            return new AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getImageUrl(),
                    a.getLinkUrl(), a.isPinned(), a.getPublishedAt());
        }
    }

    @GetMapping("/settings")
    @Operation(summary = "Journal-wide settings (title, ISSN, contacts, indexing)")
    public JournalSettings settings() {
        return settings.findById((short) 1).orElseThrow(() -> ApiException.notFound("Settings"));
    }

    @GetMapping("/pages")
    @Operation(summary = "List published CMS pages")
    public List<PageDto> pages() {
        return pages.findByStatusOrderBySortOrderAsc("PUBLISHED").stream().map(PageDto::from).toList();
    }

    @GetMapping("/pages/{slug}")
    @Operation(summary = "Get a CMS page by slug")
    public PageDto page(@PathVariable String slug) {
        return pages.findBySlug(slug).map(PageDto::from)
                .orElseThrow(() -> ApiException.notFound("Page"));
    }

    @GetMapping("/announcements")
    @Operation(summary = "List published announcements")
    public List<AnnouncementDto> announcements() {
        return announcements.findByStatusOrderByPinnedDescPublishedAtDesc("PUBLISHED").stream()
                .map(AnnouncementDto::from).toList();
    }
}
