package id.ac.ui.cs.advprog.papikos.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO representing an item retrieved from a tenant's wishlist,
 * including embedded summary information about the property.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDto {

    private Long wishlistItemId;

    private Long tenantUserId; // ID of the tenant who owns this wishlist item

    private PropertySummaryDto property; // Embedded summary of the wishlisted property

    private Instant createdAt; // Timestamp when the item was added
}

