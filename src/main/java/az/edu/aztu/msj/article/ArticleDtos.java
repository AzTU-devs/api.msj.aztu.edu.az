package az.edu.aztu.msj.article;

import java.time.LocalDate;
import java.util.List;

public final class ArticleDtos {

    public record AuthorDto(String firstName, String lastName, String email, String affiliation,
                            String country, String orcid, boolean corresponding) {
        static AuthorDto from(ArticleAuthor a) {
            return new AuthorDto(a.getFirstName(), a.getLastName(), a.getEmail(), a.getAffiliation(),
                    a.getCountry(), a.getOrcid(), a.isCorresponding());
        }
    }

    public record MetricsDto(long views, long abstractViews, long downloads, long citations) {
        static MetricsDto from(ArticleMetric m) {
            return m == null ? new MetricsDto(0, 0, 0, 0)
                    : new MetricsDto(m.getViewCount(), m.getAbstractViewCount(),
                                     m.getDownloadCount(), m.getCitationCount());
        }
    }

    public record ArticleSummary(Long id, String title, String doi, String subjectArea,
                                 String keywords, Integer pageStart, Integer pageEnd, LocalDate publishedAt,
                                 Long issueId, List<String> authorNames, MetricsDto metrics) {}

    public record ArticleDetail(Long id, String title, String abstractText, String keywords,
                                String subjectArea, String language, String status, String doi,
                                Integer pageStart, Integer pageEnd, LocalDate publishedAt,
                                Long issueId, List<AuthorDto> authors, MetricsDto metrics) {}

    private ArticleDtos() {}
}
