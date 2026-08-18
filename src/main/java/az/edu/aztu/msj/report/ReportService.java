package az.edu.aztu.msj.report;

import az.edu.aztu.msj.metric.ArticleEventRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Aggregates the whole metrics picture for the printable report.
 *
 * Written as explicit SQL against JdbcClient rather than through JPA: these are
 * reporting roll-ups over the full table, and expressing them as entity graphs
 * would fan out into a query per article. One statement per section keeps the
 * report a fixed four queries regardless of how many articles the journal has.
 */
@Service
public class ReportService {

    private final JdbcClient db;
    private final ArticleEventRepository events;

    public ReportService(JdbcClient db, ArticleEventRepository events) {
        this.db = db;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public ReportDtos.MetricsExport export() {
        return new ReportDtos.MetricsExport(Instant.now(), totals(), articles(), issues(), countries());
    }

    private ReportDtos.Totals totals() {
        return db.sql("""
                select
                  (select count(*) from articles)                                as articles,
                  (select count(*) from articles where status = 'PUBLISHED')     as published,
                  (select count(*) from issues)                                  as issues,
                  coalesce(sum(m.view_count), 0)                                 as views,
                  coalesce(sum(m.download_count), 0)                             as downloads,
                  coalesce(sum(m.citation_count), 0)                             as citations
                from article_metrics m
                """)
                .query((rs, n) -> new ReportDtos.Totals(
                        rs.getLong("articles"), rs.getLong("published"), rs.getLong("issues"),
                        rs.getLong("views"), rs.getLong("downloads"), rs.getLong("citations")))
                .single();
    }

    private List<ReportDtos.ArticleRow> articles() {
        return db.sql("""
                select a.id, a.title, a.doi, a.subject_area, a.page_start, a.page_end,
                       i.title as issue_title, i.year as issue_year, i.number as issue_number,
                       coalesce(m.view_count,0)     as views,
                       coalesce(m.download_count,0) as downloads,
                       coalesce(m.citation_count,0) as citations
                  from articles a
                  left join issues i          on i.id = a.issue_id
                  left join article_metrics m on m.article_id = a.id
                 where a.status = 'PUBLISHED'
                 order by i.year desc nulls last, i.number desc nulls last,
                          a.article_order asc nulls last, a.title asc
                """)
                .query((rs, n) -> new ReportDtos.ArticleRow(
                        rs.getLong("id"), rs.getString("title"), rs.getString("doi"),
                        rs.getString("subject_area"), rs.getString("issue_title"),
                        (Integer) rs.getObject("issue_year"), (Integer) rs.getObject("issue_number"),
                        (Integer) rs.getObject("page_start"), (Integer) rs.getObject("page_end"),
                        rs.getLong("views"), rs.getLong("downloads"), rs.getLong("citations")))
                .list();
    }

    private List<ReportDtos.IssueRow> issues() {
        return db.sql("""
                select i.id, i.title, i.year, i.number, i.volume, i.status,
                       count(a.id)                          as articles,
                       coalesce(sum(m.view_count),0)        as views,
                       coalesce(sum(m.download_count),0)    as downloads,
                       coalesce(sum(m.citation_count),0)    as citations
                  from issues i
                  left join articles a        on a.issue_id = i.id and a.status = 'PUBLISHED'
                  left join article_metrics m on m.article_id = a.id
                 group by i.id, i.title, i.year, i.number, i.volume, i.status
                 order by i.year desc nulls last, i.number desc nulls last
                """)
                .query((rs, n) -> new ReportDtos.IssueRow(
                        rs.getLong("id"), rs.getString("title"),
                        (Integer) rs.getObject("year"), (Integer) rs.getObject("number"),
                        (Integer) rs.getObject("volume"), rs.getString("status"),
                        rs.getLong("articles"), rs.getLong("views"),
                        rs.getLong("downloads"), rs.getLong("citations")))
                .list();
    }

    private List<ReportDtos.CountryRow> countries() {
        return events.countryBreakdown().stream()
                .map(c -> new ReportDtos.CountryRow(c.getCode(), c.getViews()))
                .toList();
    }
}
