package az.edu.aztu.msj.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Content-area repositories (package-private interfaces sharing one file). */

interface JournalSettingsRepository extends JpaRepository<JournalSettings, Short> {
}

interface ContentPageRepository extends JpaRepository<ContentPage, Long> {
    List<ContentPage> findByStatusOrderBySortOrderAsc(String status);
    List<ContentPage> findAllByOrderBySortOrderAsc();
    Optional<ContentPage> findBySlug(String slug);
}

interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByStatusOrderByPinnedDescPublishedAtDesc(String status);
    List<Announcement> findAllByOrderByPinnedDescPublishedAtDesc();
}

interface SiteTextRepository extends JpaRepository<SiteText, String> {
}

interface HeroSlideRepository extends JpaRepository<HeroSlide, Long> {
    List<HeroSlide> findByActiveTrueOrderBySortOrderAsc();
    List<HeroSlide> findAllByOrderBySortOrderAsc();
}

interface ScopeTopicRepository extends JpaRepository<ScopeTopic, Long> {
    List<ScopeTopic> findByActiveTrueOrderBySortOrderAsc();
    List<ScopeTopic> findAllByOrderBySortOrderAsc();
}

interface AuthorStepRepository extends JpaRepository<AuthorStep, Long> {
    List<AuthorStep> findAllByOrderBySortOrderAsc();
}

interface AuthorTermRepository extends JpaRepository<AuthorTerm, Long> {
    List<AuthorTerm> findAllByOrderBySortOrderAsc();
}
