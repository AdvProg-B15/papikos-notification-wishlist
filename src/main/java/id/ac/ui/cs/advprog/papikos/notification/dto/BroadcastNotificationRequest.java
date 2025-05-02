package id.ac.ui.cs.advprog.papikos.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationRequest {

    @NotBlank(message = "Notification title cannot be blank")
    private String title;

    @NotBlank(message = "Notification message cannot be blank")
    private String message;
}
