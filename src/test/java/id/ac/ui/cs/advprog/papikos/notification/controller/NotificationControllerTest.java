//package id.ac.ui.cs.advprog.papikos.notification.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import id.ac.ui.cs.advprog.papikos.notification.dto.*;
//import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
//import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
//// Removed unused ServiceInteractionException import from test, controller handles it or it's a RuntimeException
//import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceInteractionException;
//import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceUnavailableException;
//import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
//import id.ac.ui.cs.advprog.papikos.notification.security.TokenAuthenticationFilter;
//import id.ac.ui.cs.advprog.papikos.notification.service.NotificationService;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser; // For @PreAuthorize tests
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.client.RestTemplate;
//
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(NotificationController.class)
//class NotificationControllerTest {
//
//    @MockBean
//    private RestTemplate restTemplate;
//
//    @MockBean
//    private TokenAuthenticationFilter tokenAuthenticationFilter;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private NotificationService notificationService;
//
//    private UUID tenantUserId;
//    private String tenantUserIdString; // String version for principal
//    private UUID propertyId1;
//    private UUID wishlistItemId1;
//    private UUID notificationId1;
//
//    private AddToWishlistRequest addToWishlistRequest;
//    private BroadcastNotificationRequest broadcastRequest;
//    private WishlistItemDto wishlistItemDto;
//    private NotificationDto notificationDto;
//
//
//    @BeforeEach
//    void setUp() {
//        tenantUserId = UUID.randomUUID();
//        tenantUserIdString = tenantUserId.toString(); // For principal simulation
//
//        propertyId1 = UUID.randomUUID();
//        wishlistItemId1 = UUID.randomUUID();
//        notificationId1 = UUID.randomUUID();
//
//        addToWishlistRequest = new AddToWishlistRequest(propertyId1);
//        broadcastRequest = new BroadcastNotificationRequest("Important Update", "Service details changed.");
//
//        PropertySummaryDto propertySummary = new PropertySummaryDto(propertyId1, "Test Property", "123 Test St", BigDecimal.valueOf(1000));
//        // Using @AllArgsConstructor for WishlistItemDto
//        wishlistItemDto = new WishlistItemDto(wishlistItemId1, tenantUserId, propertySummary, Instant.now());
//        // Using @AllArgsConstructor for NotificationDto
//        notificationDto = new NotificationDto(notificationId1, tenantUserId, NotificationType.WISHLIST_VACANCY, "Vacancy", "Room available", false, propertyId1, null, Instant.now());
//    }
//
//    // --- Wishlist Endpoint Tests ---
//
//    @Test
//    @DisplayName("POST /api/v1/wishlist - Success (201 Created)")
//    @WithMockUser(authorities = {"TENANT"}) // Provides authority for @PreAuthorize
//    void addWishlistItem_Success() throws Exception, ServiceUnavailableException, ServiceInteractionException, JsonProcessingException {
//        when(notificationService.addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class)))
//                .thenReturn(wishlistItemDto);
//
//        mockMvc.perform(post("/api/v1/wishlist")
//                        .principal(() -> tenantUserIdString) // Provides principal for @AuthenticationPrincipal
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
//                .andExpect(status().isCreated())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())))
//                .andExpect(jsonPath("$.message", is("Resource created successfully")))
//                .andExpect(jsonPath("$.data.wishlistItemId", is(wishlistItemDto.getWishlistItemId().toString())))
//                .andExpect(jsonPath("$.data.tenantUserId", is(wishlistItemDto.getTenantUserId().toString())))
//                .andExpect(jsonPath("$.data.property.propertyId", is(wishlistItemDto.getProperty().getPropertyId().toString())));
//
//        verify(notificationService).addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/wishlist - Property Not Found (404 Not Found)")
//    @WithMockUser(authorities = {"TENANT"})
//    void addWishlistItem_PropertyNotFound() throws Exception, ServiceInteractionException, ServiceUnavailableException, JsonProcessingException {
//        when(notificationService.addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class)))
//                .thenThrow(new ResourceNotFoundException("Property not found: " + propertyId1, null));
//
//        mockMvc.perform(post("/api/v1/wishlist")
//                        .principal(() -> tenantUserIdString)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
//                .andExpect(status().isNotFound());
//
//        verify(notificationService).addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/wishlist - Already Exists (409 Conflict)")
//    @WithMockUser(authorities = {"TENANT"})
//    void addWishlistItem_AlreadyExists() throws Exception, ServiceUnavailableException, ServiceInteractionException, JsonProcessingException {
//        when(notificationService.addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class)))
//                .thenThrow(new ConflictException("Item already in wishlist"));
//
//        mockMvc.perform(post("/api/v1/wishlist")
//                        .principal(() -> tenantUserIdString)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
//                .andExpect(status().isConflict());
//
//        verify(notificationService).addToWishlist(eq(tenantUserId), any(AddToWishlistRequest.class));
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/wishlist - Success (200 OK)")
//    @WithMockUser(authorities = {"TENANT"})
//    void getWishlist_Success() throws Exception {
//        List<WishlistItemDto> dtos = List.of(wishlistItemDto);
//        when(notificationService.getWishlist(tenantUserId)).thenReturn(dtos);
//
//        mockMvc.perform(get("/api/v1/wishlist")
//                        .principal(() -> tenantUserIdString))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].wishlistItemId", is(wishlistItemDto.getWishlistItemId().toString())));
//
//        verify(notificationService).getWishlist(tenantUserId);
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/wishlist - Success Empty List (200 OK)")
//    @WithMockUser(authorities = {"TENANT"})
//    void getWishlist_SuccessEmpty() throws Exception {
//        when(notificationService.getWishlist(tenantUserId)).thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/wishlist")
//                        .principal(() -> tenantUserIdString))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data", hasSize(0)));
//
//        verify(notificationService).getWishlist(tenantUserId);
//    }
//
//    @Test
//    @DisplayName("DELETE /api/v1/wishlist/{propertyId} - Success (204 No Content)")
//    @WithMockUser(authorities = {"TENANT"})
//    void removeWishlistItem_Success() throws Exception {
//        doNothing().when(notificationService).removeFromWishlist(tenantUserId, propertyId1);
//
//        mockMvc.perform(delete("/api/v1/wishlist/{propertyId}", propertyId1)
//                        .principal(() -> tenantUserIdString))
//                .andExpect(status().isNoContent())
//                .andExpect(content().string("")); // HTTP 204 usually has no body
//
//        verify(notificationService).removeFromWishlist(tenantUserId, propertyId1);
//    }
//
//    @Test
//    @DisplayName("DELETE /api/v1/wishlist/{propertyId} - Not Found (404 Not Found)")
//    @WithMockUser(authorities = {"TENANT"})
//    void removeWishlistItem_NotFound() throws Exception {
//        doThrow(new ResourceNotFoundException("Wishlist item not found", null))
//                .when(notificationService).removeFromWishlist(tenantUserId, propertyId1);
//
//        mockMvc.perform(delete("/api/v1/wishlist/{propertyId}", propertyId1)
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isNotFound());
//
//        verify(notificationService).removeFromWishlist(tenantUserId, propertyId1);
//    }
//
//    // --- Notification Endpoint Tests ---
//
//    @Test
//    @DisplayName("GET /api/v1/notifications - Success (200 OK, All, unreadOnly=false by default)")
//    @WithMockUser // @PreAuthorize("isAuthenticated()")
//    void getNotifications_SuccessAll() throws Exception {
//        notificationDto.setRead(false); // Example notification state
//        List<NotificationDto> dtos = List.of(notificationDto);
//        when(notificationService.getNotifications(tenantUserId, false)).thenReturn(dtos);
//
//        mockMvc.perform(get("/api/v1/notifications") // unreadOnly defaults to false
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].notificationId", is(notificationDto.getNotificationId().toString())))
//                .andExpect(jsonPath("$.data[0].read", is(false)));
//
//        verify(notificationService).getNotifications(tenantUserId, false);
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/notifications?unreadOnly=true - Success (200 OK, Unread Only)")
//    @WithMockUser
//    void getNotifications_SuccessUnreadOnly() throws Exception {
//        notificationDto.setRead(false); // This notification is unread
//        List<NotificationDto> dtos = List.of(notificationDto);
//        when(notificationService.getNotifications(tenantUserId, true)).thenReturn(dtos);
//
//        mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "true")
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].read", is(false)));
//
//        verify(notificationService).getNotifications(tenantUserId, true);
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/notifications?unreadOnly=false - Success (200 OK, Includes Read Notification)")
//    @WithMockUser
//    void getNotifications_IncludesReadNotification_WhenUnreadOnlyFalse() throws Exception {
//        notificationDto.setRead(true); // This notification is read
//        List<NotificationDto> dtos = List.of(notificationDto);
//        when(notificationService.getNotifications(tenantUserId, false)).thenReturn(dtos);
//
//        mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "false")
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].read", is(true)));
//
//        verify(notificationService).getNotifications(tenantUserId, false);
//    }
//
//
//    @Test
//    @DisplayName("PATCH /api/v1/notifications/{id}/read - Success (200 OK)")
//    @WithMockUser
//    void markNotificationAsRead_Success() throws Exception {
//        NotificationDto updatedDto = new NotificationDto(
//                notificationId1, tenantUserId, notificationDto.getNotificationType(),
//                notificationDto.getTitle(), notificationDto.getMessage(), true, // isRead = true
//                notificationDto.getRelatedPropertyId(), notificationDto.getRelatedRentalId(),
//                notificationDto.getCreatedAt()
//        );
//        when(notificationService.markNotificationAsRead(tenantUserId, notificationId1)).thenReturn(updatedDto);
//
//        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId1)
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
//                .andExpect(jsonPath("$.message", is("Success")))
//                .andExpect(jsonPath("$.data.notificationId", is(notificationId1.toString())))
//                .andExpect(jsonPath("$.data.read", is(true)));
//
//        verify(notificationService).markNotificationAsRead(tenantUserId, notificationId1);
//    }
//
//    @Test
//    @DisplayName("PATCH /api/v1/notifications/{id}/read - Not Found (404 Not Found)")
//    @WithMockUser
//    void markNotificationAsRead_NotFound() throws Exception {
//        when(notificationService.markNotificationAsRead(tenantUserId, notificationId1))
//                .thenThrow(new ResourceNotFoundException("Notification not found", null));
//
//        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId1)
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isNotFound());
//
//        verify(notificationService).markNotificationAsRead(tenantUserId, notificationId1);
//    }
//
//    @Test
//    @DisplayName("PATCH /api/v1/notifications/{id}/read - User mismatch (Effectively Not Found for User)")
//    @WithMockUser
//    void markNotificationAsRead_Forbidden() throws Exception {
//        // Service throws ResourceNotFound if notification doesn't belong to user or doesn't exist
//        when(notificationService.markNotificationAsRead(tenantUserId, notificationId1))
//                .thenThrow(new ResourceNotFoundException("Notification not found for this user", null));
//
//        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId1)
//                        .principal(() -> tenantUserIdString))
//                .andDo(print())
//                .andExpect(status().isNotFound());
//
//        verify(notificationService).markNotificationAsRead(tenantUserId, notificationId1);
//    }
//
//
//    @Test
//    @DisplayName("POST /api/v1/notifications/broadcast - Success (202 Accepted)")
//    @WithMockUser(authorities = "ADMIN") // For @PreAuthorize("hasAuthority('ADMIN')")
//    void sendBroadcastNotification_Success() throws Exception {
//        NotificationDto returnedNotificationDto = new NotificationDto(
//                UUID.randomUUID(), null, NotificationType.BROADCAST, // Broadcast may have null recipientUserId in DTO
//                broadcastRequest.getTitle(), broadcastRequest.getMessage(),
//                false, null, null, Instant.now()
//        );
//        when(notificationService.sendBroadcastNotification(any(BroadcastNotificationRequest.class)))
//                .thenReturn(returnedNotificationDto);
//
//        mockMvc.perform(post("/api/v1/notifications/broadcast")
//                        // @WithMockUser provides principal and authority for @PreAuthorize
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(broadcastRequest)))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.status", is(HttpStatus.ACCEPTED.value())))
//                .andExpect(jsonPath("$.message", is("Broadcast notification sent successfully")))
//                .andExpect(jsonPath("$.data.title", is(broadcastRequest.getTitle())));
//
//        verify(notificationService).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/notifications/broadcast - Unauthorized (403 Forbidden)")
//    @WithMockUser(authorities = "TENANT") // Non-admin user
//    void sendBroadcastNotification_Unauthorized() throws Exception {
//        mockMvc.perform(post("/api/v1/notifications/broadcast")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(broadcastRequest)))
//                .andExpect(status().isForbidden());
//
//        verify(notificationService, never()).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/notifications/broadcast - Handles Empty Input (202 Accepted as no validation currently)")
//    @WithMockUser(authorities = "ADMIN")
//    void sendBroadcastNotification_HandlesEmptyInputAsNoValidation() throws Exception {
//        BroadcastNotificationRequest emptyRequest = new BroadcastNotificationRequest("", "");
//        NotificationDto returnedDto = new NotificationDto(
//                UUID.randomUUID(), null, NotificationType.BROADCAST,
//                "", "", false, null, null, Instant.now()
//        );
//        when(notificationService.sendBroadcastNotification(any(BroadcastNotificationRequest.class)))
//                .thenReturn(returnedDto);
//
//        // Since there's no @Valid on controller and no active constraints on DTO,
//        // this will not result in a 400 Bad Request based on current implementation.
//        // It will be processed by the service.
//        mockMvc.perform(post("/api/v1/notifications/broadcast")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(emptyRequest)))
//                .andExpect(status().isAccepted()) // Expect 202 because it proceeds to service
//                .andExpect(jsonPath("$.data.title", is("")));
//
//
//        verify(notificationService).sendBroadcastNotification(any(BroadcastNotificationRequest.class));
//    }
//}