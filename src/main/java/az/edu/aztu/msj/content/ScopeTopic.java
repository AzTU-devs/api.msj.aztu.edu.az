package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "scope_topics")
@Getter
@Setter
public class ScopeTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String icon = "gear";

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> title;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
