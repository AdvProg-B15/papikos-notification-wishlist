package id.ac.ui.cs.advprog.papikos.notification.dto;
import id.ac.ui.cs.advprog.papikos.notification.model.Notification; // Import the entity
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType; // Import the enum

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO representing a notification retrieved from the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String notificationId;

    // Nullable for BROADCAST notifications seen by all users.
    private String recipientUserId;

    private NotificationType notificationType; // Enum: BROADCAST, WISHLIST_VACANCY, etc.

    private String title;

    private String message;

    private boolean isRead;

    // Optional IDs linking the notification to related entities for context/navigation.
    private String relatedPropertyId;
    private String relatedRentalId;

    private Instant createdAt;

    public NotificationDto(String notificationId, NotificationType notificationType, String title, String message, boolean isRead, String relatedPropertyId, String relatedRentalId, Instant createdAt) {

    }

    /**
     * Static factory method to convert a Notification entity to its DTO representation.
     * Handles null input gracefully.
     *
     * @param entity The Notification entity from the database.
     * @return Corresponding NotificationDto, or null if the input entity is null.
     */
    public static NotificationDto fromEntity(Notification entity) {
        if (entity == null) {
            return null; // Return null if the entity is null
        }
        return new NotificationDto(
                entity.getNotificationId(),
                entity.getRecipientUserId(),
                entity.getNotificationType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.isRead(),
                entity.getRelatedPropertyId(),
                entity.getRelatedRentalId(),
                entity.getCreatedAt()
        );
    }
}