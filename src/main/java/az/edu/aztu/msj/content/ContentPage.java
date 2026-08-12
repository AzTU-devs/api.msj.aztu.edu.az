package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "content_pages")
@Getter
@Setter
public class ContentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slug;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> body;

    @Column(nullable = false)
    private String status = "PUBLISHED";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
