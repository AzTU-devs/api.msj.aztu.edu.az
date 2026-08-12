package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/** Key/value store for every UI label & short copy string (i18n JSONB value). */
@Entity
@Table(name = "site_texts")
@Getter
@Setter
public class SiteText {

    @Id
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> value;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
