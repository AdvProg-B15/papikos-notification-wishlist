package id.ac.ui.cs.advprog.papikos.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
/**
 * Represents summarized property information needed within the Notification service.
 * NOTE: In a real microservices architecture, this data structure mirrors data
 * fetched from the Property Service. It acts as the contract for that data
 * when embedded in Wishlist or Notification responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySummaryDto {

    private UUID propertyId;

    private String name;

    private String address;

    private Double monthlyRentPrice;
}