package id.ac.ui.cs.advprog.papikos.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String notificationId;

    @Column // Nullable for broadcast
    private String recipientUserId; // Logical FK to User in Auth Service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column // Nullable
    private String relatedPropertyId; // Logical FK to Property Service

    @Column // Nullable
    private String relatedRentalId; // Logical FK to Rental Service

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
