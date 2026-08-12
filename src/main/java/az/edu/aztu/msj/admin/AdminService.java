package az.edu.aztu.msj.admin;

import az.edu.aztu.msj.article.*;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.PageResponse;
import az.edu.aztu.msj.metric.ArticleMetricDailyRepository;
import az.edu.aztu.msj.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminService {

    private static final Set<String> TERMINAL = Set.of("PUBLISHED", "REJECTED", "WITHDRAWN");

    private final ArticleRepository articles;
    private final ArticleMetricRepository metrics;
    private final ArticleStatusHistoryRepository history;
    private final ArticleMetricDailyRepository daily;
    private final UserRepository users;

    public AdminService(ArticleRepository articles, ArticleMetricRepository metrics,
                        ArticleStatusHistoryRepository history, ArticleMetricDailyRepository daily,
                        UserRepository users) {
        this.articles = articles;
        this.metrics = metrics;
        this.history = history;
        this.daily = daily;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Dashboard dashboard() {
        ArticleMetricRepository.Totals t = metrics.totals();
        long views = t.getViews(), downloads = t.getDownloads(), citations = t.getCitations();
        Map<String, Long> byStatus = Map.of(
                "SUBMITTED", articles.countByStatus("SUBMITTED"),
                "UNDER_REVIEW", articles.countByStatus("UNDER_REVIEW"),
                "REVISION_REQUESTED", articles.countByStatus("REVISION_REQUESTED"),
                "ACCEPTED", articles.countByStatus("ACCEPTED"),
                "PUBLISHED", articles.countByStatus("PUBLISHED"));
        return new AdminDtos.Dashboard(articles.count(), articles.countByStatus("PUBLISHED"),
                users.count(), views, downloads, citations, byStatus);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.ArticleRow> listArticles(String status, int page, int size) {
        Page<Article> result = articles.findForAdmin(blankToNull(status),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(result, a -> new AdminDtos.ArticleRow(
                a.getId(), a.getTitle(), a.getStatus(), a.getSubjectArea(), a.getDoi(),
                a.getSubmitterId(), a.getSubmittedAt(), a.getCreatedAt()));
    }

    @Transactional
    public void updateStatus(Long articleId, String newStatus, String comment, Long actorId) {
        Article a = articles.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        String old = a.getStatus();
        a.setStatus(newStatus);
        if (TERMINAL.contains(newStatus)) {
            a.setDecidedAt(OffsetDateTime.now());
            if ("PUBLISHED".equals(newStatus) && a.getPublishedAt() == null) {
                a.setPublishedAt(LocalDate.now());
            }
        }
        ArticleStatusHistory h = new ArticleStatusHistory();
        h.setArticleId(articleId);
        h.setFromStatus(old);
        h.setToStatus(newStatus);
        h.setChangedBy(actorId);
        h.setComment(comment);
        history.save(h);
    }

    @Transactional(readOnly = true)
    public AdminDtos.MetricsOverview metricsOverview(int days) {
        LocalDate from = LocalDate.now().minusDays(Math.max(days, 1));
        ArticleMetricRepository.Totals t = metrics.totals();
        List<AdminDtos.SeriesPoint> series = daily.seriesAll(from).stream()
                .map(r -> new AdminDtos.SeriesPoint(r[0].toString(), num(r, 1), num(r, 2)))
                .toList();
        List<AdminDtos.TopArticle> top = metrics.topArticles(10).stream()
                .map(r -> new AdminDtos.TopArticle(((Number) r[0]).longValue(), (String) r[1],
                        ((Number) r[2]).longValue(), ((Number) r[3]).longValue(), ((Number) r[4]).longValue()))
                .toList();
        return new AdminDtos.MetricsOverview(t.getViews(), t.getDownloads(), t.getCitations(), series, top);
    }

    private long num(Object[] row, int i) {
        return row != null && row[i] instanceof Number n ? n.longValue() : 0L;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
