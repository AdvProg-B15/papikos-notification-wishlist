package id.ac.ui.cs.advprog.papikos.notification.client;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import id.ac.ui.cs.advprog.papikos.notification.dto.PropertyBasicInfoDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.PropertySummaryDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.WishlistItemDto;

import java.util.Optional;
import java.util.UUID;

public class PropertyServiceClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "http://localhost:8000";

    private boolean isAllFieldsNull(PropertySummaryDto dto) {
        return dto.getPropertyId() == null &&
                dto.getName() == null &&
                dto.getAddress() == null &&
                dto.getMonthlyRentPrice() == null;
    }

    public Boolean checkRentalExists(UUID rentalId) {
        ResponseEntity<String> response = restTemplate.getForEntity(URL + "/checkRental", String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return true;
        }
        return false;
    }

    public boolean checkPropertyExists(UUID propertyId) {
        ResponseEntity<PropertySummaryDto> response =
                restTemplate.getForEntity(URL + "/kos/" + propertyId.toString(), PropertySummaryDto.class);
        PropertySummaryDto dto = response.getBody();
        System.out.println(dto);
        return dto != null && !isAllFieldsNull(dto);
    }

    public Optional<PropertySummaryDto> getPropertySummary(UUID propertyId){
        ResponseEntity<PropertySummaryDto> response =
                restTemplate.getForEntity(URL + "/kos/" + propertyId.toString(), PropertySummaryDto.class);
        PropertySummaryDto dto = response.getBody();
        return Optional.ofNullable(dto);
    }

    public Optional<PropertyBasicInfoDto> getPropertyBasicInfo(UUID propertyId) {
        ResponseEntity<PropertyBasicInfoDto> response =
                restTemplate.getForEntity(URL + "/kos/" + propertyId.toString(), PropertyBasicInfoDto.class);
        PropertyBasicInfoDto dto = response.getBody();
        return Optional.ofNullable(dto);
    }
}
