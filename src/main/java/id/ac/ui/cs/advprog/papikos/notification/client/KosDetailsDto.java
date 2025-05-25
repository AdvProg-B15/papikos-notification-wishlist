package id.ac.ui.cs.advprog.papikos.notification.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KosDetailsDto {

    private UUID id;

    private UUID ownerUserId;

    private String name;

    private String address;

    private String description;

    private int numRooms;

    @JsonProperty("isListed")
    private boolean isListed;

    private BigDecimal monthlyRentPrice;

    private Date createdAt;
    private Date updatedAt;
}
