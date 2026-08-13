package az.edu.aztu.msj.admin;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    public record Dashboard(long totalArticles, long publishedArticles, long totalUsers,
                            long totalViews, long totalDownloads, long totalCitations,
                            Map<String, Long> articlesByStatus) {}

    public record ArticleRow(Long id, String title, String status, String subjectArea, String doi,
                             Long submitterId, OffsetDateTime submittedAt, Instant createdAt,
                             long views, long downloads, long citations) {}

    public record SeriesPoint(String day, long views, long downloads) {}

    public record TopArticle(Long id, String title, long views, long downloads, long citations) {}

    public record MetricsOverview(long totalViews, long totalDownloads, long totalCitations,
                                  List<SeriesPoint> series, List<TopArticle> topArticles) {}

    public record StatusUpdateRequest(String status, String comment) {}

    private AdminDtos() {}
}
