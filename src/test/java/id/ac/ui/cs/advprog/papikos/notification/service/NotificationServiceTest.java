package id.ac.ui.cs.advprog.papikos.notification.service;

import id.ac.ui.cs.advprog.papikos.notification.client.ApiResponseWrapper;
import id.ac.ui.cs.advprog.papikos.notification.client.KosServiceClient;
import id.ac.ui.cs.advprog.papikos.notification.client.KosDetailsDto; // Correct DTO for Kos
import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceInteractionException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceUnavailableException;
import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import id.ac.ui.cs.advprog.papikos.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.papikos.notification.repository.WishlistItemRepository;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Date; // For KosDetailsDto createdAt/updatedAt
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KosServiceClient kosServiceClient;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<WishlistItem> wishlistItemCaptor;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationListCaptor;

    private UUID tenantUserId;
    private UUID propertyId1;
    private UUID propertyId2;
    private UUID wishlistItemId1;
    private UUID notificationId1;
    private WishlistItem wishlistItem1;
    private Notification notification1;
    private KosDetailsDto kosDetailsDto1;

    @BeforeEach
    void setUp() {
        tenantUserId = UUID.randomUUID();
        propertyId1 = UUID.randomUUID();
        propertyId2 = UUID.randomUUID();
        wishlistItemId1 = UUID.randomUUID();
        notificationId1 = UUID.randomUUID();

        kosDetailsDto1 = new KosDetailsDto(
                propertyId1,            // id
                UUID.randomUUID(),      // ownerUserId
                "Cozy Room",            // name
                "Jl. Margonda Raya No.1", // address
                "A very nice room",     // description
                10,                     // numRooms
                5,                      // occupiedRooms
                true,                   // isListed
                BigDecimal.valueOf(2000000), // monthlyRentPrice
                new Date(),             // createdAt
                new Date()              // updatedAt
        );

        wishlistItem1 = new WishlistItem();
        wishlistItem1.setWishlistItemId(wishlistItemId1);
        wishlistItem1.setTenantUserId(tenantUserId);
        wishlistItem1.setPropertyId(propertyId1);
        wishlistItem1.setCreatedAt(Instant.now());

        notification1 = new Notification();
        notification1.setNotificationId(notificationId1);
        notification1.setRecipientUserId(tenantUserId);
        notification1.setNotificationType(NotificationType.RENTAL_UPDATE);
        notification1.setTitle("Test Notification");
        notification1.setMessage("Your rental status updated.");
        notification1.setRead(false);
        notification1.setCreatedAt(Instant.now());
    }

    private ApiResponseWrapper<KosDetailsDto> createSuccessKosResponse(KosDetailsDto data) {
        ApiResponseWrapper<KosDetailsDto> response = new ApiResponseWrapper<>();
        response.setData(data);
        response.setStatus(HttpStatus.OK.value());
        response.setMessage("Success");
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    private ApiResponseWrapper<KosDetailsDto> createNotFoundKosResponse() {
        ApiResponseWrapper<KosDetailsDto> response = new ApiResponseWrapper<>();
        response.setData(null);
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setMessage("Not Found");
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    private ApiResponseWrapper<KosDetailsDto> createErrorKosResponse(HttpStatus status, String message) {
        ApiResponseWrapper<KosDetailsDto> response = new ApiResponseWrapper<>();
        response.setData(null);
        response.setStatus(status.value());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }


    // --- Wishlist Tests ---

    @Test
    @DisplayName("Add to Wishlist - Success")
    void addToWishlist_Success()  throws ResourceNotFoundException, ServiceUnavailableException, ServiceInteractionException, ConflictException {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createSuccessKosResponse(kosDetailsDto1);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);
        when(wishlistItemRepository.existsByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(false);

        UUID generatedWishlistItemId = UUID.randomUUID();
        Instant generatedCreatedAt = Instant.now();
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> {
            WishlistItem itemToSave = invocation.getArgument(0);
            itemToSave.setWishlistItemId(generatedWishlistItemId);
            itemToSave.setCreatedAt(generatedCreatedAt);
            return itemToSave;
        });
        WishlistItemDto resultDto = notificationService.addToWishlist(tenantUserId, request);

        assertNotNull(resultDto);
        assertEquals(generatedWishlistItemId, resultDto.getWishlistItemId());
        assertEquals(tenantUserId, resultDto.getTenantUserId());
        assertNotNull(resultDto.getProperty());
        assertEquals(kosDetailsDto1.getId(), resultDto.getProperty().getPropertyId());
        assertEquals(kosDetailsDto1.getName(), resultDto.getProperty().getName());
        assertEquals(kosDetailsDto1.getAddress(), resultDto.getProperty().getAddress());
        assertEquals(kosDetailsDto1.getMonthlyRentPrice(), resultDto.getProperty().getMonthlyRentPrice());
        assertEquals(generatedCreatedAt, resultDto.getCreatedAt());

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository).existsByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository).save(wishlistItemCaptor.capture());
        WishlistItem capturedItem = wishlistItemCaptor.getValue();
        assertEquals(tenantUserId, capturedItem.getTenantUserId());
        assertEquals(propertyId1, capturedItem.getPropertyId());
        assertEquals(generatedWishlistItemId, capturedItem.getWishlistItemId());
    }

    @Test
    @DisplayName("Add to Wishlist - Property Not Found (KosService returns 404 in wrapper)")
    void addToWishlist_PropertyNotFound_Wrapper404() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createNotFoundKosResponse();
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });
        assertTrue(exception.getMessage().contains("Property not found with ID: " + propertyId1));

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository, never()).existsByTenantUserIdAndPropertyId(any(UUID.class), any(UUID.class));
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("Add to Wishlist - Property Not Found (KosService FeignClient throws 404)")
    void addToWishlist_PropertyNotFound_Feign404() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        Request feignReq = Request.create(Request.HttpMethod.GET, "/api/kos/" + propertyId1, Collections.emptyMap(), null, feign.Util.UTF_8);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1))
                .thenThrow(new FeignException.NotFound("Not Found from Feign", feignReq, null, Collections.emptyMap()));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });
        assertTrue(exception.getMessage().contains("Property not found with ID: " + propertyId1));

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository, never()).existsByTenantUserIdAndPropertyId(any(UUID.class), any(UUID.class));
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }


    @Test
    @DisplayName("Add to Wishlist - Already Exists")
    void addToWishlist_AlreadyExists() throws Exception {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createSuccessKosResponse(kosDetailsDto1);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);
        when(wishlistItemRepository.existsByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });
        assertTrue(exception.getMessage().contains("Property " + propertyId1 + " is already in the wishlist."));

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository).existsByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("Add to Wishlist - ServiceInteractionException from KosService")
    void addToWishlist_ServiceInteractionException() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createErrorKosResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);

        assertThrows(ServiceInteractionException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
    }

    @Test
    @DisplayName("Add to Wishlist - ServiceUnavailableException from KosService (Feign non-404)")
    void addToWishlist_ServiceUnavailableException_Feign() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        Request feignReq = Request.create(Request.HttpMethod.GET, "/api/kos/" + propertyId1, Collections.emptyMap(), null, feign.Util.UTF_8);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1))
                .thenThrow(new FeignException.ServiceUnavailable("Service unavailable", feignReq, null, Collections.emptyMap()));

        assertThrows(id.ac.ui.cs.advprog.papikos.notification.exception.ServiceUnavailableException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });
        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
    }


    @Test
    @DisplayName("Get Wishlist - Success (Multiple Items)")
    void getWishlist_Success_MultipleItems() {
        WishlistItem item2 = new WishlistItem(tenantUserId, propertyId2);
        item2.setWishlistItemId(UUID.randomUUID());
        item2.setCreatedAt(Instant.now().minusSeconds(3600));

        wishlistItem1.setCreatedAt(Instant.now());

        KosDetailsDto kosDetailsDto2 = new KosDetailsDto(propertyId2, UUID.randomUUID(), "Spacious Studio", "Jl. Sudirman No. 2", "...", 5, 2, true, BigDecimal.valueOf(3000000), new Date(), new Date());

        ApiResponseWrapper<KosDetailsDto> kosResponse1 = createSuccessKosResponse(kosDetailsDto1);
        ApiResponseWrapper<KosDetailsDto> kosResponse2 = createSuccessKosResponse(kosDetailsDto2);

        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse1);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId2)).thenReturn(kosResponse2);

        when(wishlistItemRepository.findByTenantUserId(tenantUserId)).thenReturn(List.of(wishlistItem1, item2));

        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);

        assertNotNull(wishlist);
        assertEquals(2, wishlist.size());
        assertTrue(wishlist.stream().anyMatch(dto -> dto.getProperty().getPropertyId().equals(propertyId1)));
        assertTrue(wishlist.stream().anyMatch(dto -> dto.getProperty().getPropertyId().equals(propertyId2)));

        verify(wishlistItemRepository).findByTenantUserId(tenantUserId);
        verify(kosServiceClient, times(2)).getKosDetailsApiResponse(any(UUID.class));
    }

    @Test
    @DisplayName("Get Wishlist - Success (Empty)")
    void getWishlist_Success_Empty() {
        when(wishlistItemRepository.findByTenantUserId(tenantUserId)).thenReturn(Collections.emptyList());

        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);

        assertNotNull(wishlist);
        assertTrue(wishlist.isEmpty());
        verify(wishlistItemRepository).findByTenantUserId(tenantUserId);
    }

    @Test
    @DisplayName("Remove From Wishlist - Success")
    void removeFromWishlist_Success() {
        when(wishlistItemRepository.deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(1);

        assertDoesNotThrow(() -> {
            notificationService.removeFromWishlist(tenantUserId, propertyId1);
        });

        verify(wishlistItemRepository).deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
    }

    @Test
    @DisplayName("Remove From Wishlist - Item Not Found")
    void removeFromWishlist_ItemNotFound() {
        when(wishlistItemRepository.deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.removeFromWishlist(tenantUserId, propertyId1);
        });

        verify(wishlistItemRepository).deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
    }

    // --- Notification Tests ---

    @Test
    @DisplayName("Get Notifications - Success (All, unreadOnly=false)")
    void getNotifications_Success_All() {
        Notification notification2 = new Notification();
        notification2.setNotificationId(UUID.randomUUID());
        notification2.setRecipientUserId(tenantUserId);
        notification2.setRead(true);
        notification2.setCreatedAt(Instant.now().minusSeconds(100));
        notification1.setCreatedAt(Instant.now());

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(tenantUserId)).thenReturn(List.of(notification1, notification2));

        List<NotificationDto> notifications = notificationService.getNotifications(tenantUserId, false);

        assertEquals(2, notifications.size());
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(tenantUserId);
        verify(notificationRepository, never()).findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(any(UUID.class));
    }

    @Test
    @DisplayName("Get Notifications - Success (Unread Only, unreadOnly=true)")
    void getNotifications_Success_UnreadOnly() {
        notification1.setRead(false);
        when(notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(tenantUserId)).thenReturn(List.of(notification1));

        List<NotificationDto> notifications = notificationService.getNotifications(tenantUserId, true);

        assertEquals(1, notifications.size());
        assertFalse(notifications.get(0).isRead());
        verify(notificationRepository, never()).findByRecipientUserIdOrderByCreatedAtDesc(any(UUID.class));
        verify(notificationRepository).findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(tenantUserId);
    }

    @Test
    @DisplayName("Mark Notification As Read - Success")
    void markNotificationAsRead_Success() {
        notification1.setRead(false);
        when(notificationRepository.findById(notificationId1)).thenReturn(Optional.of(notification1));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto updatedNotification = notificationService.markNotificationAsRead(tenantUserId, notificationId1);

        assertTrue(updatedNotification.isRead());
        verify(notificationRepository).findById(notificationId1);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertTrue(captured.isRead());
        assertEquals(notificationId1, captured.getNotificationId());
    }

    @Test
    @DisplayName("Mark Notification As Read - Already Read")
    void markNotificationAsRead_AlreadyRead() {
        notification1.setRead(true);
        when(notificationRepository.findById(notificationId1)).thenReturn(Optional.of(notification1));

        NotificationDto result = notificationService.markNotificationAsRead(tenantUserId, notificationId1);

        assertTrue(result.isRead());
        verify(notificationRepository).findById(notificationId1);
        verify(notificationRepository, never()).save(any(Notification.class));
    }


    @Test
    @DisplayName("Mark Notification As Read - Not Found")
    void markNotificationAsRead_NotFound() {
        when(notificationRepository.findById(notificationId1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.markNotificationAsRead(tenantUserId, notificationId1);
        });

        verify(notificationRepository).findById(notificationId1);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Send Broadcast Notification - Success")
    void sendBroadcastNotification_Success() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest("System Maintenance", "App will be down briefly.");
        Notification savedNotification = new Notification();
        savedNotification.setNotificationId(UUID.randomUUID());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setNotificationId(savedNotification.getNotificationId());
            return n;
        });

        NotificationDto resultDto = notificationService.sendBroadcastNotification(request);
        assertNotNull(resultDto);
        assertEquals(savedNotification.getNotificationId(), resultDto.getNotificationId());


        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertNull(captured.getRecipientUserId());
        assertEquals(NotificationType.BROADCAST, captured.getNotificationType());
        assertEquals(request.getTitle(), captured.getTitle());
        assertEquals(request.getMessage(), captured.getMessage());
        assertFalse(captured.isRead());
    }

    @Test
    @DisplayName("Send User Notification (Internal) - Success")
    void sendUserNotification_Success() {
        UUID recipientUuid = UUID.randomUUID();
        String recipientIdStr = recipientUuid.toString();
        String title = "Welcome!";
        String message = "Thanks for joining PapiKos.";
        NotificationType type = NotificationType.ACCOUNT_APPROVED;
        UUID relatedPropertyUuid = UUID.randomUUID();
        String relatedPropertyIdStr = relatedPropertyUuid.toString();

        Notification savedNotification = new Notification();
        savedNotification.setNotificationId(UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setNotificationId(savedNotification.getNotificationId());
            return n;
        });

        notificationService.sendUserNotification(recipientIdStr, type, title, message, null, relatedPropertyIdStr);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertEquals(recipientUuid, captured.getRecipientUserId());
        assertEquals(type, captured.getNotificationType());
        assertEquals(title, captured.getTitle());
        assertEquals(message, captured.getMessage());
        assertEquals(relatedPropertyUuid, captured.getRelatedPropertyId());
        assertNull(captured.getRelatedRentalId());
        assertFalse(captured.isRead());
    }


    @Test
    @DisplayName("Notify Wishlist Users On Vacancy - Success")
    void notifyWishlistUsersOnVacancy_Success() throws Exception {
        UUID tenantUserId2 = UUID.randomUUID();
        List<WishlistItem> wishlistItemsForProperty = List.of(
                new WishlistItem(tenantUserId, propertyId1),
                new WishlistItem(tenantUserId2, propertyId1)
        );
        when(wishlistItemRepository.findByPropertyId(propertyId1)).thenReturn(wishlistItemsForProperty);

        VacancyUpdateNotification vacancyRequest = new VacancyUpdateNotification("Vacancy Alert", "A room is now available at %s!", propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createSuccessKosResponse(kosDetailsDto1);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);

        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Notification> notificationsToSave = invocation.getArgument(0);
            notificationsToSave.forEach(n -> n.setNotificationId(UUID.randomUUID()));
            return notificationsToSave;
        });

        List<NotificationDto> resultDtos = notificationService.notifyWishlistUsersOnVacancy(vacancyRequest);

        assertNotNull(resultDtos);
        assertEquals(2, resultDtos.size());

        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository).findByPropertyId(propertyId1);
        verify(notificationRepository).saveAll(notificationListCaptor.capture());

        List<Notification> capturedList = notificationListCaptor.getValue();
        assertEquals(2, capturedList.size());

        Notification n1 = capturedList.stream().filter(n -> n.getRecipientUserId().equals(tenantUserId)).findFirst().orElseThrow();
        assertEquals(vacancyRequest.getTitle(), n1.getTitle());
        assertTrue(n1.getMessage().contains(kosDetailsDto1.getName()));
        assertEquals(NotificationType.WISHLIST_VACANCY, n1.getNotificationType());

        Notification n2 = capturedList.stream().filter(n -> n.getRecipientUserId().equals(tenantUserId2)).findFirst().orElseThrow();
        assertEquals(vacancyRequest.getTitle(), n2.getTitle());
        assertTrue(n2.getMessage().contains(kosDetailsDto1.getName()));
    }


    @Test
    @DisplayName("Notify Wishlist Users On Vacancy - Property Details Unavailable")
    void notifyWishlistUsersOnVacancy_PropertyDetailsUnavailable() throws Exception {
        UUID tenantUserId2 = UUID.randomUUID();
        List<WishlistItem> wishlistItemsForProperty = List.of(
                new WishlistItem(tenantUserId, propertyId1),
                new WishlistItem(tenantUserId2, propertyId1)
        );
        when(wishlistItemRepository.findByPropertyId(propertyId1)).thenReturn(wishlistItemsForProperty);
        VacancyUpdateNotification vacancyRequest = new VacancyUpdateNotification("Vacancy Alert", "A room is now available at %s!", propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createNotFoundKosResponse();
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);

        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Notification> notificationsToSave = invocation.getArgument(0);
            notificationsToSave.forEach(n -> n.setNotificationId(UUID.randomUUID()));
            return notificationsToSave;
        });

        List<NotificationDto> resultDtos = notificationService.notifyWishlistUsersOnVacancy(vacancyRequest);
        assertEquals(2, resultDtos.size());
        verify(notificationRepository).saveAll(notificationListCaptor.capture());
        List<Notification> capturedList = notificationListCaptor.getValue();

        String expectedFallbackMessagePart = "Property " + propertyId1 + " (details unavailable)";
        assertTrue(capturedList.get(0).getMessage().contains(expectedFallbackMessagePart));
        assertTrue(capturedList.get(1).getMessage().contains(expectedFallbackMessagePart));
    }

    @Test
    @DisplayName("Notify Wishlist Users On Vacancy - No Users Wishlisted")
    void notifyWishlistUsersOnVacancy_NoUsersWishlisted() throws Exception {
        VacancyUpdateNotification vacancyRequest = new VacancyUpdateNotification("Vacancy Alert", "A room is now available!", propertyId1);

        ApiResponseWrapper<KosDetailsDto> kosResponse = createSuccessKosResponse(kosDetailsDto1);
        when(kosServiceClient.getKosDetailsApiResponse(propertyId1)).thenReturn(kosResponse);

        when(wishlistItemRepository.findByPropertyId(propertyId1)).thenReturn(Collections.emptyList());

        List<NotificationDto> resultDtos = notificationService.notifyWishlistUsersOnVacancy(vacancyRequest);

        assertTrue(resultDtos.isEmpty());
        verify(kosServiceClient).getKosDetailsApiResponse(propertyId1);
        verify(wishlistItemRepository).findByPropertyId(propertyId1);
        verify(notificationRepository, never()).saveAll(anyList());
    }
}