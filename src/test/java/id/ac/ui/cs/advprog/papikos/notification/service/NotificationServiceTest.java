package id.ac.ui.cs.advprog.papikos.notification.service;

import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import id.ac.ui.cs.advprog.papikos.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.papikos.notification.repository.WishlistItemRepository;
import id.ac.ui.cs.advprog.papikos.notification.client.PropertyServiceClient; // Mocked client interface

import java.util.TimeZone;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

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
    private PropertyServiceClient propertyServiceClient; // Mock client

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
    private PropertyBasicInfoDto propertyInfo1;

    @BeforeEach
    void setUp() {
        tenantUserId = UUID.randomUUID();
        propertyId1 = UUID.randomUUID();
        propertyId2 = UUID.randomUUID(); // Another property
        wishlistItemId1 = UUID.randomUUID();
        notificationId1 = UUID.randomUUID();

        propertyInfo1 = new PropertyBasicInfoDto(propertyId1, "Cozy Room");

        wishlistItem1 = new WishlistItem();
        wishlistItem1.setWishlistItemId(wishlistItemId1);
        wishlistItem1.setTenantUserId(tenantUserId);
        wishlistItem1.setPropertyId(propertyId1);
        wishlistItem1.setCreatedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        notification1 = new Notification();
        notification1.setNotificationId(notificationId1);
        notification1.setRecipientUserId(tenantUserId);
        notification1.setNotificationType(NotificationType.RENTAL_UPDATE); // Example type
        notification1.setTitle("Test Notification");
        notification1.setMessage("Your rental status updated.");
        notification1.setRead(false);
        notification1.setCreatedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
    }

    // --- Wishlist Tests ---

    @Test
    @DisplayName("Add to Wishlist - Success")
    void addToWishlist_Success() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);

        // Assume property exists
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.of(propertyInfo1));
        // Assume item doesn't exist yet
        when(wishlistItemRepository.findByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(Optional.empty());
        // Mock save operation
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> {
            WishlistItem item = invocation.getArgument(0);
            item.setWishlistItemId(UUID.randomUUID()); // Simulate ID generation on save
            return item;
        });

        WishlistItemDto savedItem = notificationService.addToWishlist(tenantUserId, request);

        assertNotNull(savedItem);
        assertEquals(tenantUserId, savedItem.getTenantUserId());
        assertEquals(propertyId1, savedItem.getPropertyId());

        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(wishlistItemRepository).findByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository).save(wishlistItemCaptor.capture());

        WishlistItem capturedItem = wishlistItemCaptor.getValue();
        assertEquals(tenantUserId, capturedItem.getTenantUserId());
        assertEquals(propertyId1, capturedItem.getPropertyId());
    }

    @Test
    @DisplayName("Add to Wishlist - Property Not Found")
    void addToWishlist_PropertyNotFound() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.empty()); // Simulate property not found

        assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });

        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(wishlistItemRepository, never()).findByTenantUserIdAndPropertyId(any(UUID.class), any(UUID.class));
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("Add to Wishlist - Already Exists")
    void addToWishlist_AlreadyExists() {
        AddToWishlistRequest request = new AddToWishlistRequest(propertyId1);
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.of(propertyInfo1));
        when(wishlistItemRepository.findByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(Optional.of(wishlistItem1)); // Item exists

        assertThrows(ConflictException.class, () -> {
            notificationService.addToWishlist(tenantUserId, request);
        });

        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(wishlistItemRepository).findByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("Get Wishlist - Success (Multiple Items)")
    void getWishlist_Success_MultipleItems() {
        WishlistItem wishlistItem2 = new WishlistItem();
        wishlistItem2.setWishlistItemId(UUID.randomUUID());
        wishlistItem2.setTenantUserId(tenantUserId);
        wishlistItem2.setPropertyId(propertyId2);

        WishlistItemDto wishlistItemDto1 = new WishlistItemDto(wishlistItem1.getWishlistItemId(), wishlistItem1.getTenantUserId(),null, wishlistItem1.getCreatedAt());
        WishlistItemDto wishlistItemDto2 = new WishlistItemDto(wishlistItem2.getWishlistItemId(), wishlistItem2.getTenantUserId(),null, wishlistItem2.getCreatedAt());

        when(wishlistItemRepository.findByTenantUserIdOrderByCreatedAtDesc(tenantUserId)).thenReturn(Optional.of(List.of(wishlistItem1, wishlistItem2)));

        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);

        assertNotNull(wishlist);
        assertEquals(2, wishlist.size());
        verify(wishlistItemRepository).findByTenantUserIdOrderByCreatedAtDesc(tenantUserId);
    }

    @Test
    @DisplayName("Get Wishlist - Success (Empty)")
    void getWishlist_Success_Empty() {
        when(wishlistItemRepository.findByTenantUserIdOrderByCreatedAtDesc(tenantUserId)).thenReturn(Optional.empty());

        List<WishlistItemDto> wishlist = notificationService.getWishlist(tenantUserId);

        assertNotNull(wishlist);
        assertTrue(wishlist.isEmpty());
        verify(wishlistItemRepository).findByTenantUserIdOrderByCreatedAtDesc(tenantUserId);
    }

    @Test
    @DisplayName("Remove From Wishlist - Success")
    void removeFromWishlist_Success() {
        when(wishlistItemRepository.findByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(Optional.of(wishlistItem1));
        doNothing().when(wishlistItemRepository).delete(wishlistItem1);

        notificationService.removeFromWishlist(tenantUserId, propertyId1);

        verify(wishlistItemRepository).findByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository).delete(wishlistItem1);
    }

    @Test
    @DisplayName("Remove From Wishlist - Item Not Found")
    void removeFromWishlist_ItemNotFound() {
        when(wishlistItemRepository.findByTenantUserIdAndPropertyId(tenantUserId, propertyId1)).thenReturn(Optional.empty()); // Item not found

        assertThrows(ResourceNotFoundException.class, () -> {
             notificationService.removeFromWishlist(tenantUserId, propertyId1);
        });

        verify(wishlistItemRepository).findByTenantUserIdAndPropertyId(tenantUserId, propertyId1);
        verify(wishlistItemRepository, never()).delete(any(WishlistItem.class));
    }

    // --- Notification Tests ---

    @Test
    @DisplayName("Get Notifications - Success (All)")
    void getNotifications_Success_All() {
        Notification notification2 = new Notification(); // Another notification for the user
        notification2.setNotificationId(UUID.randomUUID());
        notification2.setRecipientUserId(tenantUserId);
        notification2.setRead(true); // One read, one unread

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(tenantUserId)).thenReturn(List.of(notification1, notification2));

        List<NotificationDto> notifications = notificationService.getNotifications(tenantUserId, false); // null status means all

        assertEquals(2, notifications.size());
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(tenantUserId);
        verify(notificationRepository, never()).findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(any(UUID.class), anyBoolean());
    }

    @Test
    @DisplayName("Get Notifications - Success (Unread Only)")
    void getNotifications_Success_UnreadOnly() {
        Notification notification2 = new Notification(); // Another notification for the user
        notification2.setNotificationId(UUID.randomUUID());
        notification2.setRecipientUserId(tenantUserId);
        notification2.setRead(true); // One read, one unread

        when(notificationRepository.findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(tenantUserId, true)).thenReturn(List.of(notification1));

        List<NotificationDto> notifications = notificationService.getNotifications(tenantUserId, false); // isRead = false

        assertEquals(1, notifications.size());
        assertFalse(notifications.get(0).isRead());
        verify(notificationRepository, never()).findByRecipientUserIdOrderByCreatedAtDesc(any(UUID.class));
        verify(notificationRepository).findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(tenantUserId, false);
    }

    @Test
    @DisplayName("Mark Notification As Read - Success")
    void markNotificationAsRead_Success() {
        when(notificationRepository.findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId)).thenReturn(List.of(notification1));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1); // Return the saved entity

        NotificationDto updatedNotification = notificationService.markNotificationAsRead(tenantUserId, notificationId1);

        assertTrue(updatedNotification.isRead());
        verify(notificationRepository).findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertTrue(captured.isRead());
        assertEquals(notificationId1, captured.getNotificationId());
    }

    @Test
    @DisplayName("Mark Notification As Read - Already Read")
    void markNotificationAsRead_AlreadyRead() {
        notification1.setRead(true); // Mark as read initially
        when(notificationRepository.findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId)).thenReturn(List.of(notification1));

        NotificationDto result = notificationService.markNotificationAsRead(tenantUserId, notificationId1);

        assertTrue(result.isRead()); // Should still be read
        verify(notificationRepository).findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId);
        // Save might or might not be called depending on implementation (e.g., if check is done before save)
        // verify(notificationRepository, never()).save(any(Notification.class)); // Or verify(..., times(1)) if save is always called
    }


    @Test
    @DisplayName("Mark Notification As Read - Not Found")
    void markNotificationAsRead_NotFound() {
        when(notificationRepository.findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.markNotificationAsRead(tenantUserId, notificationId1);
        });

        verify(notificationRepository).findByNotificationIdAndRecipientUserId(notificationId1, tenantUserId);
        verify(notificationRepository, never()).save(any(Notification.class));
    }


    @Test
    @DisplayName("Send Broadcast Notification - Success")
    void sendBroadcastNotification_Success() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest("System Maintenance", "App will be down briefly.");
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification()); // Simulate save

        notificationService.sendBroadcastNotification(request);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertNull(captured.getRecipientUserId()); // Broadcast notifications have null recipient
        assertEquals(NotificationType.BROADCAST, captured.getNotificationType());
        assertEquals(request.getTitle(), captured.getTitle());
        assertEquals(request.getMessage(), captured.getMessage());
        assertFalse(captured.isRead());
    }

    @Test
    @DisplayName("Send User Notification (Internal) - Success")
    void sendUserNotification_Success() {
        String recipientId = UUID.randomUUID().toString();
        String title = "Welcome!";
        String message = "Thanks for joining PapiKos.";
        NotificationType type = NotificationType.ACCOUNT_APPROVED;
        String relatedId = UUID.randomUUID().toString(); // e.g., rental ID

        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        notificationService.sendUserNotification(recipientId, type, title, message, null, relatedId); // Example with relatedRentalId

         verify(notificationRepository).save(notificationCaptor.capture());
         Notification captured = notificationCaptor.getValue();

         assertEquals(recipientId, captured.getRecipientUserId());
         assertEquals(type, captured.getNotificationType());
         assertEquals(title, captured.getTitle());
         assertEquals(message, captured.getMessage());
         assertNull(captured.getRelatedPropertyId());
         assertEquals(relatedId, captured.getRelatedRentalId());
         assertFalse(captured.isRead());
    }

    @Test
    @DisplayName("Notify Wishlist Users On Vacancy - Success")
    void notifyWishlistUsersOnVacancy_Success() {
        UUID tenantUserId2 = UUID.randomUUID();
        List<UUID> userIds = List.of(tenantUserId, tenantUserId2);

        when(wishlistItemRepository.findTenantUserIdsByPropertyId(propertyId1)).thenReturn(Optional.of(userIds));
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.of(propertyInfo1));
        when(notificationRepository.saveAll(anyList())).thenReturn(Collections.emptyList()); // Simulate saveAll

        notificationService.notifyWishlistUsersOnVacancy(propertyId1);

        verify(wishlistItemRepository).findTenantUserIdsByPropertyId(propertyId1);
        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(notificationRepository).saveAll(notificationListCaptor.capture());

        List<Notification> capturedList = notificationListCaptor.getValue();
        assertEquals(2, capturedList.size()); // One notification per user

        // Check notification details for the first user
        Notification n1 = capturedList.stream().filter(n -> n.getRecipientUserId().equals(tenantUserId)).findFirst().orElse(null);
        assertNotNull(n1);
        assertEquals(tenantUserId, n1.getRecipientUserId());
        assertEquals(NotificationType.WISHLIST_VACANCY, n1.getNotificationType());
        assertEquals("Vacancy Alert: " + propertyInfo1.getName(), n1.getTitle());
        assertTrue(n1.getMessage().contains(propertyInfo1.getName()));
        assertEquals(propertyId1, n1.getRelatedPropertyId());
        assertNull(n1.getRelatedRentalId());
        assertFalse(n1.isRead());

         // Check notification details for the second user
        Notification n2 = capturedList.stream().filter(n -> n.getRecipientUserId().equals(tenantUserId2)).findFirst().orElse(null);
        assertNotNull(n2);
        assertEquals(tenantUserId2, n2.getRecipientUserId());
         assertEquals(NotificationType.WISHLIST_VACANCY, n2.getNotificationType());
    }

     @Test
    @DisplayName("Notify Wishlist Users On Vacancy - Property Not Found")
    void notifyWishlistUsersOnVacancy_PropertyNotFound() {
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.empty()); // Property doesn't exist

        notificationService.notifyWishlistUsersOnVacancy(propertyId1);

        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(wishlistItemRepository, never()).findTenantUserIdsByPropertyId(any(UUID.class)); // Should not query wishlist if property missing
        verify(notificationRepository, never()).saveAll(anyList()); // Should not save notifications
    }

    @Test
    @DisplayName("Notify Wishlist Users On Vacancy - No Users Wishlisted")
    void notifyWishlistUsersOnVacancy_NoUsersWishlisted() {
        when(propertyServiceClient.getPropertyBasicInfo(propertyId1)).thenReturn(Optional.of(propertyInfo1));
        when(wishlistItemRepository.findTenantUserIdsByPropertyId(propertyId1)).thenReturn(Optional.empty()); // No users found

        notificationService.notifyWishlistUsersOnVacancy(propertyId1);

        verify(propertyServiceClient).getPropertyBasicInfo(propertyId1);
        verify(wishlistItemRepository).findTenantUserIdsByPropertyId(propertyId1);
        verify(notificationRepository, never()).saveAll(anyList()); // Should not save if no users found
    }
}
