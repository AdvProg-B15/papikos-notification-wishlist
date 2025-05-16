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
// Import Principal or SecurityContextHolder to get user ID
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails; // Or your custom Principal class
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1") // Assuming a base path
@RequiredArgsConstructor
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    // Helper method to extract user ID (replace with your actual principal details)
    private UUID getCurrentUserId(Object principal) {
        if (principal instanceof UserDetails) {
             // If using UserDetails, you might need to parse the username if it's the ID,
             // or use a custom UserDetails implementation that holds the ID.
             // This is a common point of customization.
             // For example, if username IS the ID:
             // return Long.parseLong(((UserDetails) principal).getUsername());
             // --- OR --- If using a custom principal:
             // return ((CustomUserDetails) principal).getId();
             // --- Placeholder --- return a default/test ID if not implemented
             log.warn("Security principal type not fully handled: {}. Returning placeholder ID 1L.", principal.getClass().getName());
             // TODO: PARSE THE USER DETAIL -> USERID
             return new UUID(9696,9696); // !!! REPLACE WITH ACTUAL LOGIC !!!
        } else if (principal instanceof String) {
            try { // If the principal is just the user ID string
                return new UUID(6969,6969);
            } catch (NumberFormatException e) {
                 log.error("Could not parse user ID from Principal String: {}", principal);
                 throw new IllegalStateException("Invalid Principal format");
            }
        }
        // Handle anonymous or null principal
        log.error("Could not extract user ID from principal: {}", principal);
        throw new IllegalStateException("Cannot determine current user ID");
    }


    // === Wishlist Endpoints ===

    @PostMapping("/wishlist")
    //@PreAuthorize("hasRole('TENANT')") // Only tenants can add to wishlist
    public ResponseEntity<WishlistItemDto> addToWishlist(
            @RequestBody AddToWishlistRequest request,
            @AuthenticationPrincipal Object principal) { // Inject principal
        UUID tenantUserId = getCurrentUserId(principal);
        log.info("API Request: Tenant {} adding property {} to wishlist", tenantUserId, request.getPropertyId());
        WishlistItemDto createdItem = notificationService.addToWishlist(tenantUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @GetMapping("/wishlist")
    //@PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<WishlistItemDto>> getWishlist(
             @AuthenticationPrincipal Object principal) {
        UUID tenantUserId = getCurrentUserId(principal);
         log.info("API Request: Tenant {} retrieving wishlist", tenantUserId);
        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);
        return ResponseEntity.ok(wishlist);
    }

    @DeleteMapping("/wishlist/{propertyId}")
    //@PreAuthorize("hasRole('TENANT')")
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
    //@PreAuthorize("isAuthenticated()") // Any authenticated user can get their notifications
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
             @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} retrieving notifications (unreadOnly={})", userId, unreadOnly);
        List<NotificationDto> notifications = notificationService.getNotifications(userId, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markNotificationAsRead(
            @PathVariable UUID notificationId,
             @AuthenticationPrincipal Object principal) {
        UUID userId = getCurrentUserId(principal);
        log.info("API Request: User {} marking notification {} as read", userId, notificationId);
        NotificationDto updatedNotification = notificationService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.ok(updatedNotification);
    }

    @PostMapping("/notifications/broadcast")
    //@PreAuthorize("hasRole('ADMIN')") // Only Admins can broadcast
    public ResponseEntity<NotificationDto> sendBroadcastNotification(
            @RequestBody BroadcastNotificationRequest request) {
        log.info("API Request: Admin sending broadcast notification");
        // Service returns DTO, wrap it for acceptance status
         NotificationDto notificationDto = notificationService.sendBroadcastNotification(request);
        // Accepted is suitable as it might be processed async later
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationDto);
    }

    // === Internal Endpoints (Usually NOT exposed via Gateway, called service-to-service) ===
    // These might be better implemented using async events (e.g., Kafka, RabbitMQ)
    // But providing REST endpoints for completeness if needed for internal sync calls.

    @PostMapping("/notifications/internal/send")
    // Add security here if called directly, e.g., require specific service role/token
    // @PreAuthorize("hasAuthority('SERVICE_INTERNAL')")
    public ResponseEntity<NotificationDto> sendInternalNotification(
            @RequestBody InternalNotificationRequest request) {
        log.info("INTERNAL API Request: Send notification type {} to user {}", request.getType(), request.getRecipientUserId());
         NotificationDto notificationDto = notificationService.sendInternalNotification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationDto);
    }

     @PostMapping("/notifications/internal/vacancy")
     // Add security here if called directly
     // @PreAuthorize("hasAuthority('SERVICE_INTERNAL')")
     public ResponseEntity<Void> triggerVacancyNotification(
              @RequestBody VacancyTriggerRequest request) { // Simple DTO with propertyId
         log.info("INTERNAL API Request: Trigger vacancy notifications for property {}", request.getPropertyId());
         notificationService.notifyWishlistUsersOnVacancy(request.getPropertyId());
         return ResponseEntity.accepted().build(); // Accepted for async processing
     }

     // Simple DTO for the internal vacancy trigger endpoint
     @Data
     static class VacancyTriggerRequest {
         //@NotNull
         private UUID propertyId;
     }
}
