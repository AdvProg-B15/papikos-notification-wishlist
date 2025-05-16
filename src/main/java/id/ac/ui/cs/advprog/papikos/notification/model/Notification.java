package id.ac.ui.cs.advprog.papikos.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.UUID;

import java.time.Instant;

@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"notificationId", "relatedPropertyId","relatedRentalId", "recipientUserId"} )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID notificationId;

    @Column(columnDefinition = "uuid", nullable = false)
    private UUID recipientUserId; // Logical FK to User in Auth Service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, columnDefinition = "uuid") // Nullable
    private UUID relatedPropertyId; // Logical FK to Property Service

    @Column(nullable = false, columnDefinition = "uuid") // Nullable
    private UUID relatedRentalId; // Logical FK to Rental Service

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
