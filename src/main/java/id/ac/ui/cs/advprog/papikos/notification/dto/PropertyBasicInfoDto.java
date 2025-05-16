package id.ac.ui.cs.advprog.papikos.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyBasicInfoDto {
    UUID propertyId;
    String name;
}
