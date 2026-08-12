package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "announcements")
@Getter
@Setter
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> body;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private String status = "PUBLISHED";

    @Column(name = "published_at", nullable = false)
    private LocalDate publishedAt = LocalDate.now();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
