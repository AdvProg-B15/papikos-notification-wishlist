package id.ac.ui.cs.advprog.papikos.notification.controller;

import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.service.NotificationService;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.papikos.notification.client.PropertyServiceClient;

@RestController
@RequestMapping("") // Assuming a base path
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

    // === Wishlist Endpoints ===

    @PostMapping("/wishlist")
    @PreAuthorize("hasRole('TENANT')") // Only tenants can add to wishlist
    public ResponseEntity<WishlistItemDto> addToWishlist(
            @RequestBody AddToWishlistRequest request,
            @AuthenticationPrincipal Object principal) { // Inject principal
        UUID tenantUserId = getCurrentUserId(principal);
        log.info("API Request: Tenant {} adding property {} to wishlist", tenantUserId, request.getPropertyId());
        WishlistItemDto createdItem = notificationService.addToWishlist(tenantUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @GetMapping("/wishlist")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<WishlistItemDto>> getWishlist(
             @AuthenticationPrincipal Object principal) {
        UUID tenantUserId = getCurrentUserId(principal);
        System.out.println(principal.toString());
        log.info("API Request: Tenant {} retrieving wishlist", tenantUserId);
        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);
        return ResponseEntity.ok(wishlist);
    }

    @DeleteMapping("/wishlist/{propertyId}")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable UUID propertyId,
             @AuthenticationPrincipal Object principal) {
        UUID tenantUserId = getCurrentUserId(principal);
        log.info("API Request: Tenant {} removing property {} from wishlist", tenantUserId, propertyId);
        notificationService.removeFromWishlist(tenantUserId, propertyId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // === Notification Endpoints ===

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can get their notifications
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
             @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} retrieving notifications (unreadOnly={})", userId, unreadOnly);
        List<NotificationDto> notifications = notificationService.getNotifications(userId, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markNotificationAsRead(
            @PathVariable UUID notificationId,
             @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} marking notification {} as read", userId, notificationId);
        NotificationDto updatedNotification = notificationService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.ok(updatedNotification);
    }

    @PostMapping("/notifications/rentalUpdate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationDto> rentalUpdateNotification(
            @RequestBody RentalUpdateRequest request,
            @AuthenticationPrincipal Object principal) {
        log.info("API Request: User {} renting notification {}",request.getRecipientId(), request);
        NotificationDto notificationDto = notificationService.notifyRentalUpdate(request);
        return ResponseEntity.ok(notificationDto);
    }


    @PostMapping("/notifications/vacancy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationDto>> sendVacancyNotification(
            @RequestBody VacancyUpdateNotification request,
            @AuthenticationPrincipal Object principal) {
            log.info("API Request: User {} sending vacancy notification {}", principal, request);
            List<NotificationDto> notifications = notificationService.notifyWishlistUsersOnVacancy(request);
            return ResponseEntity.ok(notifications);
    }

    @PostMapping("/notifications/broadcast")
    @PreAuthorize("hasRole('ADMIN')") // Only Admins can broadcast
    public ResponseEntity<NotificationDto> sendBroadcastNotification(
            @RequestBody BroadcastNotificationRequest request) {
        log.info("API Request: Admin sending broadcast notification");
         NotificationDto notificationDto = notificationService.sendBroadcastNotification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationDto);
    }
}
