package id.ac.ui.cs.advprog.papikos.notification.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalDetailsDto {
    private UUID rentalId;
    private UUID tenantUserId;
    private UUID kosId;
    private UUID ownerUserId;
    private String kosName;
    private String submittedTenantName;
    private String submittedTenantPhone;
    private LocalDate rentalStartDate;
    private int rentalDurationMonths;
    private LocalDate rentalEndDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
