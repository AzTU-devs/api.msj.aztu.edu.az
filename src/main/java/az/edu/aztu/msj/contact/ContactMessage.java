package az.edu.aztu.msj.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;
    private String phone;
    private String subject;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    @Column(nullable = false)
    private String status = "NEW";

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
