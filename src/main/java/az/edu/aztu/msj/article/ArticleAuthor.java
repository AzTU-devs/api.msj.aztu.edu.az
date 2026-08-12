package az.edu.aztu.msj.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "article_authors")
@Getter
@Setter
public class ArticleAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;

    @Column(columnDefinition = "text")
    private String affiliation;

    private String country;
    private String orcid;

    @Column(name = "author_order", nullable = false)
    private int authorOrder = 0;

    @Column(name = "is_corresponding", nullable = false)
    private boolean corresponding = false;

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
