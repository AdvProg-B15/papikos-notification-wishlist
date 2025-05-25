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
import java.util.UUID;
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
    public WishlistItemDto addToWishlist(UUID tenantUserId, AddToWishlistRequest request) {
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
    public List<WishlistItemDto> getWishlist(UUID tenantUserId) {
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
    public void removeFromWishlist(UUID tenantUserId, UUID propertyId) {
        log.info("Tenant {} attempting to remove property {} from wishlist", tenantUserId, propertyId);
        int deleteCount = wishlistItemRepository.deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId);
        if (0 == deleteCount) {
             log.warn("Wishlist remove failed: Tenant {} did not have property {} in wishlist", tenantUserId.toString(), propertyId.toString());
            throw new ResourceNotFoundException("Property " + propertyId + " not found in wishlist for this user.", new ConflictException("Property Not Found"));
        }
         log.info("Property {} removed from wishlist for tenant {}", propertyId.toString(), tenantUserId.toString());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId, boolean unreadOnly) {
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
    public NotificationDto markNotificationAsRead(UUID userId, UUID notificationId) {
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
    public void notifyApprovedAccount(UUID recipientId) {
        log.info("Notifying approved account for recipient {}", recipientId);
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientId);
        notification.setNotificationType(NotificationType.ACCOUNT_APPROVED);
        notification.setRead(false);
        notification.setTitle("Approved Account");
        notification.setMessage("Approved account for recipient " + recipientId.toString());
        notificationRepository.save(notification);
    }

    @Transactional
    public NotificationDto notifyRentalUpdate(RentalUpdateRequest request) {
        log.info("Rental update for Rental Id: {}", request.getRelatedRentalId());

        Boolean rentalExist = propertyServiceClient.checkRentalExists(request.getRelatedRentalId());
        if (!rentalExist) {
            log.info("Rental {} does not exist", request.getRelatedRentalId());
            return null;
        }

        PropertySummaryDto propertySummary = propertyServiceClient.getPropertySummary(request.getRelatedPropertyId()).orElse(null);
        if (propertySummary == null) {
            log.info("Rental update failed: Property {} not found", request.getRelatedPropertyId());
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipientUserId(request.getRecipientId());
        notification.setNotificationType(NotificationType.RENTAL_UPDATE);
        notification.setMessage(request.getMessage());
        notification.setRelatedRentalId(request.getRelatedRentalId());
        notification.setRelatedPropertyId(request.getRelatedPropertyId());
        notification.setTitle(request.getTitle());

        notificationRepository.save(notification);

        return NotificationDto.fromEntity(notification);
    }

    @Transactional
    public void notifyPaymentUpdate(UUID rentalId) {
        log.info("Payment update for rental {}", rentalId);

        Notification notification = new Notification();
        notification.setRecipientUserId(rentalId);
        notification.setNotificationType(NotificationType.PAYMENT_UPDATE);
        notification.setMessage("Payment update for rental " + rentalId.toString());
        notification.setTitle("Payment update");

        notificationRepository.save(notification);
    }

    @Transactional 
    public List<NotificationDto> notifyWishlistUsersOnVacancy(VacancyUpdateNotification request) {
         log.info("Processing vacancy notification for property {}", request.getRelatedPropertyId());

         String propertyName = propertyServiceClient.getPropertySummary(request.getRelatedPropertyId())
             .map(PropertySummaryDto::getName)
             .orElse("Property " + request.getRelatedPropertyId()); // Fallback name

         List<WishlistItem> wishlistItems = wishlistItemRepository.findByPropertyId(request.getRelatedPropertyId());

         if (wishlistItems.isEmpty()) {
             log.info("No users found wishlisting property {}", request.getRelatedPropertyId());
             return null;
         }

         List<Notification> notificationsToSend = wishlistItems.stream().map(item -> {
             Notification notification = new Notification();
             notification.setRecipientUserId(item.getTenantUserId());
             notification.setNotificationType(NotificationType.WISHLIST_VACANCY);
             notification.setTitle(request.getTitle());
             notification.setMessage(request.getMessage());
             notification.setRead(false);
             notification.setRelatedPropertyId(request.getRelatedPropertyId());
             return notification;
         }).toList();

         notificationRepository.saveAll(notificationsToSend);
         log.info("Sent {} vacancy notifications for property {}", notificationsToSend.size(), request.getRelatedPropertyId());
         return notificationsToSend.stream().map(NotificationDto::fromEntity).collect(Collectors.toList());
    }
}
