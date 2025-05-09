package id.ac.ui.cs.advprog.papikos.notification.dto;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalNotificationRequest {
    @NotNull(message = "")
    private String recipientUserId;

    @NotNull(message = "")
    private NotificationType type;

    @NotNull(message = "")
    private String title;

    @NotNull(message = "")
    private String message;

    @NotNull(message = "")
    private String relatedPropertyId;

    @NotNull(message = "")
    private String relatedRentalId;

}
