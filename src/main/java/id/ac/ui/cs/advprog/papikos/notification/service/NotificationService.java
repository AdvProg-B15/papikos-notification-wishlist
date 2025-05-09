package id.ac.ui.cs.advprog.papikos.notification.service;

import id.ac.ui.cs.advprog.papikos.notification.client.PropertyServiceClient;
import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ForbiddenException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import id.ac.ui.cs.advprog.papikos.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.papikos.notification.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final WishlistItemRepository wishlistItemRepository;
    private final NotificationRepository notificationRepository;
    private final PropertyServiceClient propertyServiceClient = new PropertyServiceClient();


    @Transactional
    public WishlistItemDto addToWishlist(String tenantUserId, AddToWishlistRequest request) {
        log.info("Tenant {} attempting to add property {} to wishlist", tenantUserId, request.getPropertyId());

        if (!propertyServiceClient.checkPropertyExists(request.getPropertyId())) {
             log.warn("Wishlist add failed: Property {} not found", request.getPropertyId());
            throw new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId(), new NoSuchElementException());
        }

        if (wishlistItemRepository.existsByTenantUserIdAndPropertyId(tenantUserId, request.getPropertyId())) {
             log.warn("Wishlist add failed: Tenant {} already has property {} in wishlist", tenantUserId, request.getPropertyId());
            throw new ConflictException("Property " + request.getPropertyId() + " is already in the wishlist.");
        }

        WishlistItem newItem = new WishlistItem(tenantUserId, request.getPropertyId());
        WishlistItem savedItem = wishlistItemRepository.save(newItem);
        log.info("Property {} added to wishlist for tenant {}", request.getPropertyId(), tenantUserId);


        PropertySummaryDto propertySummary = propertyServiceClient.getPropertySummary(savedItem.getPropertyId())
            .orElse(new PropertySummaryDto(savedItem.getPropertyId(), "Property " + savedItem.getPropertyId(), null, null)); // Fallback if summary fetch fails


        return new WishlistItemDto(
            savedItem.getWishlistItemId(),
            savedItem.getTenantUserId(),
            propertySummary, 
            savedItem.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlist(String tenantUserId) {
        log.debug("Fetching wishlist for tenant {}", tenantUserId);
        List<WishlistItem> items = wishlistItemRepository.findByTenantUserId(tenantUserId);

        return items.stream().map(item -> {
            // Fetch property summary for each item
             PropertySummaryDto propertySummary = propertyServiceClient.getPropertySummary(item.getPropertyId())
                .orElse(new PropertySummaryDto(item.getPropertyId(), "Property " + item.getPropertyId(), null, null)); // Fallback

             return new WishlistItemDto(
                item.getWishlistItemId(),
                item.getTenantUserId(),
                propertySummary,
                item.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public void sendUserNotification(String recipientId, NotificationType type, String title, String message, Object something, String relatedId) {

    }
    @Transactional
    public void removeFromWishlist(String tenantUserId, String propertyId) {
        log.info("Tenant {} attempting to remove property {} from wishlist", tenantUserId, propertyId);
        String deleteCount = wishlistItemRepository.deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId);
        if (deleteCount.isBlank()) {
             log.warn("Wishlist remove failed: Tenant {} did not have property {} in wishlist", tenantUserId, propertyId);
            throw new ResourceNotFoundException("Property " + propertyId + " not found in wishlist for this user.", new ConflictException("Property Not Found"));
        }
         log.info("Property {} removed from wishlist for tenant {}", propertyId, tenantUserId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userId, boolean unreadOnly) {
        log.debug("Fetching notifications for user {}, unreadOnly={}", userId, unreadOnly);
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else {
            notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
        }
        return notifications.stream().map(NotificationDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markNotificationAsRead(String userId, String notificationId) {
         log.info("User {} attempting to mark notification {} as read", userId, notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                     log.warn("Mark as read failed: Notification {} not found", notificationId);
                     return new ResourceNotFoundException("Notification not found with ID: " + notificationId, new NoSuchElementException());
                });

        if (notification.getRecipientUserId() == null || !notification.getRecipientUserId().equals(userId)) {
             log.warn("Mark as read failed: User {} is not the recipient of notification {}", userId, notificationId);
            throw new ForbiddenException("User is not the recipient of this notification.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
             log.info("Notification {} marked as read for user {}", notificationId, userId);
        } else {
            log.debug("Notification {} was already read for user {}", notificationId, userId);
        }

        return NotificationDto.fromEntity(notification);
    }

    @Transactional
    public NotificationDto sendBroadcastNotification(BroadcastNotificationRequest request) {
        log.info("Sending broadcast notification: Title='{}'", request.getTitle());
        Notification notification = new Notification();
        notification.setRecipientUserId(null); 
        notification.setNotificationType(NotificationType.BROADCAST);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRead(false); 

        Notification saved = notificationRepository.save(notification);
        log.info("Broadcast notification {} saved", saved.getNotificationId());
        return NotificationDto.fromEntity(saved);
    }

    @Transactional
    public NotificationDto sendInternalNotification(InternalNotificationRequest request) {
         log.info("Sending internal notification: Type={}, Recipient={}, Title='{}'", request.getType(), request.getRecipientUserId(), request.getTitle());
        Notification notification = new Notification();
        notification.setRecipientUserId(request.getRecipientUserId());
        notification.setNotificationType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRead(false);
        notification.setRelatedPropertyId(request.getRelatedPropertyId());
        notification.setRelatedRentalId(request.getRelatedRentalId());

        Notification saved = notificationRepository.save(notification);
         log.info("Internal notification {} for user {} saved", saved.getNotificationId(), request.getRecipientUserId());
        return NotificationDto.fromEntity(saved);
    }

    @Transactional 
    public void notifyWishlistUsersOnVacancy(String propertyId) {
         log.info("Processing vacancy notification for property {}", propertyId);

         String propertyName = propertyServiceClient.getPropertySummary(propertyId)
             .map(PropertySummaryDto::getName)
             .orElse("Property " + propertyId); // Fallback name

         List<WishlistItem> wishlistItems = wishlistItemRepository.findByPropertyId(propertyId);

         if (wishlistItems.isEmpty()) {
             log.info("No users found wishlisting property {}", propertyId);
             return;
         }

         List<Notification> notificationsToSend = wishlistItems.stream().map(item -> {
             Notification notification = new Notification();
             notification.setRecipientUserId(item.getTenantUserId());
             notification.setNotificationType(NotificationType.WISHLIST_VACANCY);
             notification.setTitle("Room Available!");
             notification.setMessage("A room has become available at '" + propertyName + "' which is on your wishlist.");
             notification.setRead(false);
             notification.setRelatedPropertyId(propertyId);
             return notification;
         }).collect(Collectors.toList());

         notificationRepository.saveAll(notificationsToSend);
         log.info("Sent {} vacancy notifications for property {}", notificationsToSend.size(), propertyId);
    }
}
