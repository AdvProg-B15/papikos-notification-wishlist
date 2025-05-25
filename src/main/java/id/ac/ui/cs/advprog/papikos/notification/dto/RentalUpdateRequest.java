package id.ac.ui.cs.advprog.papikos.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalUpdateRequest {
    @NotNull
    private UUID relatedRentalId;

    @NotNull
    private UUID recipientId;

    @NotNull
    private UUID relatedPropertyId;

    @NotNull
    private String title;

    @NotNull
    private String message;
}
