package az.edu.aztu.msj.board;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "board_members")
@Getter
@Setter
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String title;

    @Column(nullable = false)
    private String section = "BOARD";

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "orcid_url")
    private String orcidUrl;

    @Column(name = "scopus_url")
    private String scopusUrl;

    private String email;
    private String country;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
