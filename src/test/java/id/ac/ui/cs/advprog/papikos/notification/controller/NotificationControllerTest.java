package id.ac.ui.cs.advprog.papikos.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.dto.BroadcastNotificationRequest;
import id.ac.ui.cs.advprog.papikos.notification.dto.InternalNotificationRequest;
import id.ac.ui.cs.advprog.papikos.notification.dto.NotificationDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.PropertySummaryDto;
import id.ac.ui.cs.advprog.papikos.notification.dto.AddToWishlistRequest;


import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import id.ac.ui.cs.advprog.papikos.notification.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import org.springframework.http.MediaType;
// Import security test annotations if needed for role checks
// import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.ZoneId;
@WebMvcTest(NotificationController.class)
// Use @AutoConfigureMockMvc(addFilters = false) if you want to disable security filters for tests
// Or use @WithMockUser for specific role testing
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    // Assuming a simple mapper or direct DTO creation in controller/service for tests
    // @MockBean private NotificationMapper notificationMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantUserId;
    private String propertyId1;
    private String wishlistItemId1;
    private String notificationId1;
    private AddToWishlistRequest addToWishlistRequest;
    private BroadcastNotificationRequest broadcastRequest;
    private WishlistItem wishlistItem;
    private Notification notification;
    private WishlistItemDto wishlistItemDto;
    private NotificationDto notificationDto;


    @BeforeEach
    void setUp() {
        tenantUserId = "user-tenant-123"; // Use a predictable ID for controller tests if needed
        propertyId1 = UUID.randomUUID().toString();
        wishlistItemId1 = UUID.randomUUID().toString();
        notificationId1 = UUID.randomUUID().toString();

        addToWishlistRequest = new AddToWishlistRequest(propertyId1);
        broadcastRequest = new BroadcastNotificationRequest("Important Update", "Service details changed.");

        wishlistItem = new WishlistItem(wishlistItemId1, tenantUserId, propertyId1,  LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        notification = new Notification(notificationId1, tenantUserId, NotificationType.WISHLIST_VACANCY, "Vacancy", "Room available", false, propertyId1, null,  LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        // Assume DTOs are created correctly for responses
        wishlistItemDto = new WishlistItemDto(wishlistItemId1, propertyId1, new PropertySummaryDto(), wishlistItem.getCreatedAt()); // Add propertyName if needed
        notificationDto = new NotificationDto(notificationId1, notification.getNotificationType(), notification.getTitle(), notification.getMessage(), notification.isRead(), notification.getRelatedPropertyId(), notification.getRelatedRentalId(), notification.getCreatedAt());
    }

    // Utility to simulate getting current user ID (replace with actual mechanism if needed)
    private String getCurrentUserId() {
        // In real tests with Spring Security, this might come from @AuthenticationPrincipal
        // or SecurityContextHolder. For MockMvc, we often pass it or assume it's handled.
        // Here, we pass it explicitly where needed or assume filter sets it.
        return tenantUserId;
    }

    // --- Wishlist Endpoint Tests ---

    @Test
    @DisplayName("POST /wishlist - Success (201 Created)")
    // @WithMockUser(username = "user-tenant-123", roles = {"TENANT"}) // Example security
    void addWishlistItem_Success() throws Exception {
        when(notificationService.addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class)))
                .thenReturn(wishlistItemDto);
        // Mock DTO mapping if separate mapper exists
        // when(notificationMapper.toWishlistItemDto(wishlistItem)).thenReturn(wishlistItemDto);

        mockMvc.perform(post("/wishlist")
                        // .with(SecurityMockMvcRequestPostProcessors.csrf()) // If CSRF enabled
                        // .principal(() -> tenantUserId) // Basic way to set principal if not using full security mock
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.wishlistItemId", is(wishlistItem.getWishlistItemId())))
                .andExpect(jsonPath("$.propertyId", is(propertyId1)));

        verify(notificationService).addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class));
    }

    @Test
    @DisplayName("POST /wishlist - Property Not Found (404 Not Found)")
    // @WithMockUser(roles = {"TENANT"})
    void addWishlistItem_PropertyNotFound() throws Exception {
        when(notificationService.addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class)))
                .thenThrow(new ResourceNotFoundException("Property not found: " + propertyId1, null));

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isNotFound());

        verify(notificationService).addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class));
    }

    @Test
    @DisplayName("POST /wishlist - Already Exists (409 Conflict)")
    // @WithMockUser(roles = {"TENANT"})
    void addWishlistItem_AlreadyExists() throws Exception {
        when(notificationService.addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class)))
                .thenThrow(new ConflictException("Item already in wishlist"));

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isConflict());

        verify(notificationService).addToWishlist(eq(getCurrentUserId()), any(AddToWishlistRequest.class));
    }

    @Test
    @DisplayName("GET /wishlist - Success (200 OK)")
    // @WithMockUser(roles = {"TENANT"})
    void getWishlist_Success() throws Exception {
        List<WishlistItem> items = List.of(wishlistItem);
        List<WishlistItemDto> dtos = items.stream().map(item -> new WishlistItemDto(item.getWishlistItemId(), item.getPropertyId(), new PropertySummaryDto(), item.getCreatedAt())).collect(Collectors.toList());


        when(notificationService.getWishlist(getCurrentUserId())).thenReturn(dtos);
         // Assume controller or service maps to DTOs
//          when(notificationService.getWishlist(items)).thenReturn(dtos);

        mockMvc.perform(get("/wishlist"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].wishlistItemId", is(wishlistItem.getWishlistItemId())));

        verify(notificationService).getWishlist(getCurrentUserId());
    }

     @Test
    @DisplayName("GET /wishlist - Success Empty List (200 OK)")
    // @WithMockUser(roles = {"TENANT"})
    void getWishlist_SuccessEmpty() throws Exception {
        when(notificationService.getWishlist(getCurrentUserId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/wishlist"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(notificationService).getWishlist(getCurrentUserId());
    }

    @Test
    @DisplayName("DELETE /wishlist/{propertyId} - Success (204 No Content)")
    // @WithMockUser(roles = {"TENANT"})
    void removeWishlistItem_Success() throws Exception {
        doNothing().when(notificationService).removeFromWishlist(getCurrentUserId(), propertyId1);

        mockMvc.perform(delete("/wishlist/{propertyId}", propertyId1))
                .andExpect(status().isNoContent()); // 204

        verify(notificationService).removeFromWishlist(getCurrentUserId(), propertyId1);
    }

    @Test
    @DisplayName("DELETE /wishlist/{propertyId} - Not Found (404 Not Found)")
    // @WithMockUser(roles = {"TENANT"})
    void removeWishlistItem_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Wishlist item not found", null))
                .when(notificationService).removeFromWishlist(getCurrentUserId(), propertyId1);

        mockMvc.perform(delete("/wishlist/{propertyId}", propertyId1))
                .andExpect(status().isNotFound());

        verify(notificationService).removeFromWishlist(getCurrentUserId(), propertyId1);
    }

    // --- Notification Endpoint Tests ---

    @Test
    @DisplayName("GET /notifications - Success (200 OK, All)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"}) // Accessible by all authenticated
    void getNotifications_SuccessAll() throws Exception {
        List<Notification> notifications = List.of(notification);
         List<NotificationDto> dtos = notifications.stream().map(n -> new NotificationDto(n.getNotificationId(), n.getNotificationType(), n.getTitle(), n.getMessage(), n.isRead(), n.getRelatedPropertyId(), n.getRelatedRentalId(), n.getCreatedAt())).collect(Collectors.toList());

        when(notificationService.getNotifications(getCurrentUserId(), false)).thenReturn(dtos);
        // when(notificationMapper.toNotificationDtoList(notifications)).thenReturn(dtos);

        mockMvc.perform(get("/notifications")) // No query param -> get all
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notificationId", is(notification.getNotificationId())));

        verify(notificationService).getNotifications(getCurrentUserId(), false);
    }

    @Test
    @DisplayName("GET /notifications?isRead=false - Success (200 OK, Unread Only)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"})
    void getNotifications_SuccessUnreadOnly() throws Exception {
        notification.setRead(false); // Ensure it's unread for this test
        List<Notification> notifications = List.of(notification);
         List<NotificationDto> dtos = notifications.stream().map(n -> new NotificationDto(n.getNotificationId(), n.getNotificationType(), n.getTitle(), n.getMessage(), n.isRead(), n.getRelatedPropertyId(), n.getRelatedRentalId(), n.getCreatedAt())).collect(Collectors.toList());

        when(notificationService.getNotifications(getCurrentUserId(), false)).thenReturn(dtos);
        // when(notificationMapper.toNotificationDtoList(notifications)).thenReturn(dtos);


        mockMvc.perform(get("/notifications").param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].read", is(false)));

        verify(notificationService).getNotifications(getCurrentUserId(), false);
    }

     @Test
    @DisplayName("GET /notifications?isRead=true - Success (200 OK, Read Only)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"})
    void getNotifications_SuccessReadOnly() throws Exception {
        notification.setRead(true); // Ensure it's read for this test
        List<Notification> notifications = List.of(notification);
        List<NotificationDto> dtos = notifications.stream().map(n -> new NotificationDto(n.getNotificationId(), n.getNotificationType(), n.getTitle(), n.getMessage(), n.isRead(), n.getRelatedPropertyId(), n.getRelatedRentalId(), n.getCreatedAt())).collect(Collectors.toList());


        when(notificationService.getNotifications(getCurrentUserId(), true)).thenReturn(dtos);
        // when(notificationMapper.toNotificationDtoList(notifications)).thenReturn(dtos);

        mockMvc.perform(get("/notifications").param("isRead", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].read", is(true)));

        verify(notificationService).getNotifications(getCurrentUserId(), true);
    }


    @Test
    @DisplayName("PATCH /notifications/{id}/read - Success (200 OK)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"})
    void markNotificationAsRead_Success() throws Exception {
        notification.setRead(true); // Simulate the state after marking as read
        NotificationDto updatedDto = new NotificationDto(notificationId1, notification.getNotificationType(), notification.getTitle(), notification.getMessage(), true, notification.getRelatedPropertyId(), notification.getRelatedRentalId(), notification.getCreatedAt());

        when(notificationService.markNotificationAsRead(getCurrentUserId(), notificationId1)).thenReturn(updatedDto);
        // when(notificationMapper.toNotificationDto(notification)).thenReturn(updatedDto);

        mockMvc.perform(patch("/notifications/{id}/read", notificationId1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.notificationId", is(notificationId1)))
                .andExpect(jsonPath("$.read", is(true)));

        verify(notificationService).markNotificationAsRead(getCurrentUserId(), notificationId1);
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read - Not Found (404 Not Found)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"})
    void markNotificationAsRead_NotFound() throws Exception {
        when(notificationService.markNotificationAsRead(getCurrentUserId(), notificationId1))
                .thenThrow(new ResourceNotFoundException("Notification not found", null));

        mockMvc.perform(patch("/notifications/{id}/read", notificationId1))
                .andExpect(status().isNotFound());

        verify(notificationService).markNotificationAsRead(getCurrentUserId(), notificationId1);
    }

     // Assuming marking someone else's notification read would also result in Not Found from the service
    @Test
    @DisplayName("PATCH /notifications/{id}/read - Forbidden (Implicitly Not Found for User)")
    // @WithMockUser(roles = {"TENANT", "OWNER", "ADMIN"})
    void markNotificationAsRead_Forbidden() throws Exception {
        // The service query findByNotificationIdAndRecipientUserId handles this.
        // If the service threw a specific ForbiddenException, we'd catch that.
        // Here, we assume it returns NotFound if the ID doesn't match the user.
         when(notificationService.markNotificationAsRead(getCurrentUserId(), notificationId1))
                .thenThrow(new ResourceNotFoundException("Notification not found for this user", null)); // Or potentially a ForbiddenException

        mockMvc.perform(patch("/notifications/{id}/read", notificationId1))
                .andExpect(status().isNotFound()); // Or isForbidden() if service throws that

        verify(notificationService).markNotificationAsRead(getCurrentUserId(), notificationId1);
    }


    @Test
    @DisplayName("POST /notifications/broadcast - Success (202 Accepted)")
    // @WithMockUser(roles = {"ADMIN"}) // Requires ADMIN role
    void sendBroadcastNotification_Success() throws Exception {
        doNothing().when(notificationService).sendBroadcastNotification(any(BroadcastNotificationRequest.class));

        mockMvc.perform(post("/notifications/broadcast")
                        // Add Auth header or @WithMockUser with ADMIN role
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcastRequest)))
                .andExpect(status().isAccepted()); // 202 Accepted for async/fire-and-forget

        verify(notificationService).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
    }

    @Test
    @DisplayName("POST /notifications/broadcast - Unauthorized (403 Forbidden)")
    // @WithMockUser(roles = {"TENANT"}) // Test with non-admin role
    void sendBroadcastNotification_Unauthorized() throws Exception {
        // Assuming Spring Security blocks this based on @PreAuthorize("hasRole('ADMIN')") or similar
        // No need to mock the service call as security should intercept first

        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcastRequest)))
                .andExpect(status().isForbidden()); // 403 Forbidden

        verify(notificationService, never()).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
    }

     @Test
    @DisplayName("POST /notifications/broadcast - Bad Request (Invalid Input)")
    // @WithMockUser(roles = {"ADMIN"})
    void sendBroadcastNotification_BadRequest() throws Exception {
        BroadcastNotificationRequest invalidRequest = new BroadcastNotificationRequest("", ""); // Empty title/message

         // Assume @Valid annotation on request body in controller method
        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // 400 Bad Request from validation

        verify(notificationService, never()).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
    }
}
