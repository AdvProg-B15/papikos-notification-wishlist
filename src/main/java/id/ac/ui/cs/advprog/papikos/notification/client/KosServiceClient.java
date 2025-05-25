package id.ac.ui.cs.advprog.papikos.notification.client;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "kos-service", url = "${kos.service.url:http://localhost:8081/api/v1}")
public interface KosServiceClient {

    @GetMapping("/{kosId}")
    ApiResponseWrapper<KosDetailsDto> getKosDetailsApiResponse(@PathVariable("kosId") UUID kosId);
}