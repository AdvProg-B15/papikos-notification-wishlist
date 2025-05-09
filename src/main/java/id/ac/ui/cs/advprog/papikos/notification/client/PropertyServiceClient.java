package id.ac.ui.cs.advprog.papikos.notification.client;

import id.ac.ui.cs.advprog.papikos.notification.dto.PropertyBasicInfoDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.PropertySummaryDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.WishlistItemDto;

import java.util.Optional;
public class PropertyServiceClient {
    public boolean checkPropertyExists(String propertyId) {
        // TODO: Implement logic for check property with id = propertyId
        return true;
    }

    public Optional<PropertySummaryDto> getPropertySummary(String propertyId){
        // TODO: Implement logic for get property summary
        return null;
    }

    public Optional<PropertyBasicInfoDto> getPropertyBasicInfo(String propertyId) {
        // TODO: Implement logic for getting the information needed.
        return null;
    }
}
