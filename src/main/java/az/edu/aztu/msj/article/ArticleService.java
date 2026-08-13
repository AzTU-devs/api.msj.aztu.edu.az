package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articles;
    private final ArticleMetricRepository metrics;

    public ArticleService(ArticleRepository articles, ArticleMetricRepository metrics) {
        this.articles = articles;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleDtos.ArticleSummary> listPublished(String q, Long issueId, String subject,
                                                                  int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Article> result = articles.searchPublished(blankToNull(q), issueId, blankToNull(subject), pageable);
        Map<Long, ArticleMetric> metricMap = metricMap(result.getContent());
        return PageResponse.of(result, a -> toSummary(a, metricMap.get(a.getId())));
    }

    @Transactional(readOnly = true)
    public List<ArticleDtos.ArticleSummary> listByIssue(Long issueId) {
        // public table of contents — only published articles appear
        List<Article> list = articles.findByIssueIdAndStatusOrderByArticleOrderAscTitleAsc(issueId, "PUBLISHED");
        Map<Long, ArticleMetric> metricMap = metricMap(list);
        return list.stream().map(a -> toSummary(a, metricMap.get(a.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ArticleDtos.ArticleDetail getPublishedById(Long id) {
        Article a = articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        if (!"PUBLISHED".equals(a.getStatus())) {
            throw ApiException.notFound("Article");
        }
        ArticleMetric m = metrics.findById(id).orElse(null);
        return toDetail(a, m);
    }

    private Map<Long, ArticleMetric> metricMap(List<Article> list) {
        if (list.isEmpty()) return Map.of();
        List<Long> ids = list.stream().map(Article::getId).toList();
        return metrics.findByArticleIdIn(ids).stream()
                .collect(Collectors.toMap(ArticleMetric::getArticleId, Function.identity()));
    }

    private ArticleDtos.ArticleSummary toSummary(Article a, ArticleMetric m) {
        return new ArticleDtos.ArticleSummary(a.getId(), a.getTitle(), a.getDoi(), a.getSubjectArea(),
                a.getKeywords(), a.getPageStart(), a.getPageEnd(), a.getPublishedAt(), a.getIssueId(),
                a.getAuthors().stream().map(ArticleAuthor::fullName).toList(),
                ArticleDtos.MetricsDto.from(m));
    }

    private ArticleDtos.ArticleDetail toDetail(Article a, ArticleMetric m) {
        return new ArticleDtos.ArticleDetail(a.getId(), a.getTitle(), a.getAbstractText(), a.getKeywords(),
                a.getSubjectArea(), a.getLanguage(), a.getStatus(), a.getDoi(),
                a.getPageStart(), a.getPageEnd(), a.getPublishedAt(), a.getIssueId(),
                a.getAuthors().stream().map(ArticleDtos.AuthorDto::from).toList(),
                ArticleDtos.MetricsDto.from(m));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
