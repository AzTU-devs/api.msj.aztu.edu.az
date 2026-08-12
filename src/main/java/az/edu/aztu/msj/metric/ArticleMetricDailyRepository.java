package az.edu.aztu.msj.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ArticleMetricDailyRepository extends JpaRepository<ArticleMetricDaily, ArticleMetricDaily.Key> {

    @Modifying
    @Query(value = """
            insert into article_metric_daily (article_id, day, views, downloads)
            values (:articleId, :day, :views, :downloads)
            on conflict (article_id, day) do update set
                views = article_metric_daily.views + :views,
                downloads = article_metric_daily.downloads + :downloads
            """, nativeQuery = true)
    void upsert(@Param("articleId") Long articleId, @Param("day") LocalDate day,
                @Param("views") int views, @Param("downloads") int downloads);

    @Query("select d from ArticleMetricDaily d where d.articleId = :articleId and d.day >= :from order by d.day asc")
    List<ArticleMetricDaily> series(@Param("articleId") Long articleId, @Param("from") LocalDate from);

    @Query(value = """
            select day, sum(views) as views, sum(downloads) as downloads
            from article_metric_daily
            where day >= :from
            group by day order by day asc
            """, nativeQuery = true)
    List<Object[]> seriesAll(@Param("from") LocalDate from);
}
