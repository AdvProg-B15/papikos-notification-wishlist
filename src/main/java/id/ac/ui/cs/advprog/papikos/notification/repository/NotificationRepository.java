package id.ac.ui.cs.advprog.papikos.notification.repository;

import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    List<Notification> findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(String recipientUserId);
}
