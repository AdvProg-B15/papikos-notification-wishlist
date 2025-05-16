package id.ac.ui.cs.advprog.papikos.notification.repository;

import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(UUID recipientUserId, Boolean isRead);

    List<Notification> findByNotificationIdAndRecipientUserId(UUID notificationId, UUID recipientUserId);
}
