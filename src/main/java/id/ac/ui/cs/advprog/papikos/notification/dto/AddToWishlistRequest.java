package id.ac.ui.cs.advprog.papikos.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistRequest {

    @NotNull(message = "Property ID cannot be null")
    private String propertyId;
}