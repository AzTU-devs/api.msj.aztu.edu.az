package az.edu.aztu.msj.report;

import java.time.Instant;
import java.util.List;

/** Everything the admin's printable metrics report renders, in one payload. */
public final class ReportDtos {

    public record ArticleRow(Long id, String title, String doi, String subjectArea,
                             String issueTitle, Integer issueYear, Integer issueNumber,
                             Integer pageStart, Integer pageEnd,
                             long views, long downloads, long citations) {}

    public record IssueRow(Long id, String title, Integer year, Integer number, Integer volume,
                           String status, long articles,
                           long views, long downloads, long citations) {}

    public record CountryRow(String code, long views) {}

    public record Totals(long articles, long publishedArticles, long issues,
                         long views, long downloads, long citations) {}

    public record MetricsExport(Instant generatedAt,
                                Totals totals,
                                List<ArticleRow> articles,
                                List<IssueRow> issues,
                                List<CountryRow> countries) {}

    private ReportDtos() {}
}
