package id.ac.ui.cs.advprog.papikos.notification.controller;

import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceInteractionException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceUnavailableException;
import id.ac.ui.cs.advprog.papikos.notification.response.ApiResponse;
import id.ac.ui.cs.advprog.papikos.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.papikos.notification.client.PropertyServiceClient;

@RestController
@RequestMapping("/api/v1") // Assuming a base path
@RequiredArgsConstructor
public class NotificationController {
    private final PropertyServiceClient propertyServiceClient = new PropertyServiceClient();

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    // Helper method to extract user ID (replace with your actual principal details)
    private UUID getCurrentUserId(Object principal) {
        if (principal instanceof String userIdStr) {
            try {
                return UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format: {}", userIdStr);
                throw new IllegalStateException("Invalid user ID format");
            }
        }

        log.error("Unexpected principal type: {}", principal.getClass().getName());
        throw new IllegalStateException("Cannot determine current user ID");
    }
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("Health check endpoint called");
        return ResponseEntity.ok("Notification Service is running");
    }

    // === Wishlist Endpoints ===

    @PostMapping("/wishlist")
    @PreAuthorize("hasAuthority('TENANT')") // Only tenants can add to wishlist
    public ResponseEntity<ApiResponse<WishlistItemDto>> addToWishlist(
            @RequestBody AddToWishlistRequest request,
            @AuthenticationPrincipal Object principal) { // Inject principal
        UUID tenantUserId = getCurrentUserId(principal);
        log.info("API Request: Tenant {} adding property {} to wishlist", tenantUserId, request.getPropertyId());
        try {
            WishlistItemDto createdItem = notificationService.addToWishlist(tenantUserId, request);
            ApiResponse<WishlistItemDto> response = ApiResponse.<WishlistItemDto>builder()
                    .created(createdItem);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ServiceInteractionException | ServiceUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/wishlist")
    @PreAuthorize("hasAuthority('TENANT')")
    public ResponseEntity<ApiResponse<List<WishlistItemDto>>> getWishlist(
            @AuthenticationPrincipal Object principal) {
        UUID tenantUserId = getCurrentUserId(principal);
        System.out.println(principal.toString());
        log.info("API Request: Tenant {} retrieving wishlist", tenantUserId);
        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);

        ApiResponse<List<WishlistItemDto>> response = ApiResponse.<List<WishlistItemDto>>builder()
                .ok(wishlist);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/wishlist/{propertyId}")
    @PreAuthorize("hasAuthority('TENANT')")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Object principal) {
        UUID tenantUserId = getCurrentUserId(principal);
        log.info("API Request: Tenant {} removing property {} from wishlist", tenantUserId, propertyId);
        notificationService.removeFromWishlist(tenantUserId, propertyId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT)
                .message("Property removed from wishlist successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    // === Notification Endpoints ===

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can get their notifications
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} retrieving notifications (unreadOnly={})", userId, unreadOnly);
        List<NotificationDto> notifications = notificationService.getNotifications(userId, unreadOnly);

        ApiResponse<List<NotificationDto>> response = ApiResponse.<List<NotificationDto>>builder()
                .ok(notifications);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationDto>> markNotificationAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} marking notification {} as read", userId, notificationId);
        NotificationDto updatedNotification = notificationService.markNotificationAsRead(userId, notificationId);

        ApiResponse<NotificationDto> response = ApiResponse.<NotificationDto>builder()
                .ok(updatedNotification);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/rentalUpdate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationDto>> rentalUpdateNotification(
            @RequestBody RentalUpdateRequest request,
            @AuthenticationPrincipal Object principal) {
        log.info("API Request: User {} renting notification {}",request.getRecipientId(), request);
        try {
            NotificationDto notificationDto = notificationService.notifyRentalUpdate(request);
            ApiResponse<NotificationDto> response = ApiResponse.<NotificationDto>builder()
                    .ok(notificationDto);
            return ResponseEntity.ok(response);
        } catch (ServiceInteractionException | ServiceUnavailableException | ResourceNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @PostMapping("/notifications/vacancy")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> sendVacancyNotification(
            @RequestBody VacancyUpdateNotification request,
            @AuthenticationPrincipal Object principal) throws ServiceInteractionException, ServiceUnavailableException, ResourceNotFoundException {
        log.info("API Request: User {} sending vacancy notification {}", principal, request);
        List<NotificationDto> notifications = notificationService.notifyWishlistUsersOnVacancy(request);

        ApiResponse<List<NotificationDto>> response = ApiResponse.<List<NotificationDto>>builder()
                .ok(notifications);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/broadcast")
    @PreAuthorize("hasAuthority('ADMIN')") // Only Admins can broadcast
    public ResponseEntity<ApiResponse<NotificationDto>> sendBroadcastNotification(
            @RequestBody BroadcastNotificationRequest request) throws ServiceInteractionException, ServiceUnavailableException {
        log.info("API Request: Admin sending broadcast notification");
        NotificationDto notificationDto = notificationService.sendBroadcastNotification(request);

        ApiResponse<NotificationDto> response = ApiResponse.<NotificationDto>builder()
                .status(HttpStatus.ACCEPTED)
                .message("Broadcast notification sent successfully")
                .data(notificationDto)
                .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

