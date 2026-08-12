package az.edu.aztu.msj.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    private String title;
    private String degree;
    private String position;
    @Column(columnDefinition = "text")
    private String affiliation;
    private String country;
    private String city;
    @Column(name = "postal_code")
    private String postalCode;
    private String phone;
    private String orcid;
    @Column(name = "scopus_id")
    private String scopusId;
    @Column(name = "website_url")
    private String websiteUrl;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @Column(columnDefinition = "text")
    private String bio;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "is_available_reviewer", nullable = false)
    private boolean availableReviewer = true;

    @Column(name = "preferred_locale", nullable = false)
    private String preferredLocale = "en";

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_code")
    private Set<String> roles = new HashSet<>();

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
