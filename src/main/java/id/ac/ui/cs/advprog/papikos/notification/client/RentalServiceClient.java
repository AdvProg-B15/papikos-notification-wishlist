package id.ac.ui.cs.advprog.papikos.notification.client;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "rental-service", url = "${rental.service.url:http://localhost:8081/api/v1}")
public interface RentalServiceClient {

    @GetMapping("/{rentalId}")
    ApiResponseWrapper<RentalDetailsDto> getRentalDetail(@PathVariable("rentalId") UUID rentalId);
}