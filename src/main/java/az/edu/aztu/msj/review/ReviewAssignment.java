package az.edu.aztu.msj.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "review_assignments")
@Getter
@Setter
public class ReviewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(nullable = false)
    private int round = 1;

    @Column(nullable = false)
    private String status = "INVITED";   // INVITED, ACCEPTED, DECLINED, IN_PROGRESS, SUBMITTED, CANCELLED, OVERDUE

    @Column(name = "invited_at", insertable = false, updatable = false)
    private OffsetDateTime invitedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
