package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/** A term/policy card in the "For Authors" section. */
@Entity
@Table(name = "author_terms")
@Getter
@Setter
public class AuthorTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> body;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
