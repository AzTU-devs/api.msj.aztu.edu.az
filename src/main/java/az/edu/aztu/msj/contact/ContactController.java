package az.edu.aztu.msj.contact;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/contact")
@Tag(name = "Contact")
public class ContactController {

    private final ContactMessageRepository repository;

    public ContactController(ContactMessageRepository repository) {
        this.repository = repository;
    }

    public record ContactRequest(
            @NotBlank @Size(max = 200) String fullName,
            @Email String email,
            @Size(max = 60) String phone,
            @Size(max = 200) String subject,
            @NotBlank String message) {}

    @PostMapping
    @Operation(summary = "Submit a contact-form message")
    public ResponseEntity<Void> submit(@Valid @RequestBody ContactRequest req, HttpServletRequest http) {
        ContactMessage m = new ContactMessage();
        m.setFullName(req.fullName());
        m.setEmail(req.email());
        m.setPhone(req.phone());
        m.setSubject(req.subject());
        m.setMessage(req.message());
        m.setIpAddress(http.getRemoteAddr());
        repository.save(m);
        return ResponseEntity.accepted().build();
    }
}

interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    java.util.List<ContactMessage> findByStatusOrderByCreatedAtDesc(String status);
}
