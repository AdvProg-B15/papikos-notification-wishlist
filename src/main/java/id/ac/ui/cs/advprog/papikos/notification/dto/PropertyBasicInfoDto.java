package id.ac.ui.cs.advprog.papikos.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyBasicInfoDto {
    @JsonProperty("id")
    UUID propertyId;
    String name;
}
