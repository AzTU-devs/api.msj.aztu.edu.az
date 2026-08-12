package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/** A numbered step in the "From manuscript to publication" section. */
@Entity
@Table(name = "author_steps")
@Getter
@Setter
public class AuthorStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_no")
    private String stepNo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> body;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
