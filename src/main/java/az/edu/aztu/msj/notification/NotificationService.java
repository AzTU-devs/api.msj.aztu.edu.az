package az.edu.aztu.msj.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes in-app notifications on workflow events. (Email is a no-op until SMTP is configured.) */
@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void notify(Long userId, String type, String title, String body, String linkUrl) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLinkUrl(linkUrl);
        repo.save(n);
    }
}
