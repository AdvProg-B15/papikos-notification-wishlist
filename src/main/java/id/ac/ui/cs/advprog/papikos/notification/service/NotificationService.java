package id.ac.ui.cs.advprog.papikos.notification.service;

import id.ac.ui.cs.advprog.papikos.notification.client.*;
import id.ac.ui.cs.advprog.papikos.notification.dto.*;
import id.ac.ui.cs.advprog.papikos.notification.exception.ConflictException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ForbiddenException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceInteractionException;
import id.ac.ui.cs.advprog.papikos.notification.exception.ServiceUnavailableException; // Custom one
import id.ac.ui.cs.advprog.papikos.notification.model.Notification;
import id.ac.ui.cs.advprog.papikos.notification.model.NotificationType;
import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import id.ac.ui.cs.advprog.papikos.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.papikos.notification.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final WishlistItemRepository wishlistItemRepository;
    private final NotificationRepository notificationRepository;
    private final KosServiceClient kosServiceClient;
    private final RentalServiceClient rentalServiceClient;

    RentalDetailsDto fetchRentalDetails(UUID rentalId) throws ResourceNotFoundException, ServiceUnavailableException, ServiceInteractionException {
        ApiResponseWrapper<RentalDetailsDto> responseWrapper;
        try {
            responseWrapper = rentalServiceClient.getRentalDetail(rentalId);
        } catch (FeignException e) {
            log.error("FeignException while fetching Rental details for ID {}: Status {}, Body {}",
                    rentalId, e.status(), e.contentUTF8(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Kos not found with ID: " + rentalId + " (via Kos Service Feign 404).", e);
            }
            throw new ServiceUnavailableException("Error fetching Kos details: Kos service unavailable or unexpected Feign error. " + e.getMessage());
        }
        if (responseWrapper == null) {
            throw new ServiceUnavailableException("No response from Rental service for Rental ID: " + rentalId);
        }
        if (responseWrapper.getStatus() != HttpStatus.OK.value()) {
            log.warn("Rental service returned non-OK status ({}) for Rental ID {}: {}",
                    responseWrapper.getStatus(), rentalId, responseWrapper.getMessage());
            if (responseWrapper.getStatus() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Rental not found with ID: " + rentalId + " (Reported by Rental Service wrapper: " + responseWrapper.getMessage() + ")", null);
            }
            throw new ServiceInteractionException("Rental service reported an issue for Rental ID " + rentalId + ": " +
                    responseWrapper.getStatus() + " - " + responseWrapper.getMessage());
        }
        if (responseWrapper.getData() == null) {
            log.error("Rental service returned OK status but no data for Rental ID {}", rentalId);
            throw new ResourceNotFoundException("Rental details data not found for ID: " + rentalId + " (Rental Service returned no data).", null);
        }
        return responseWrapper.getData();
    }

    KosDetailsDto fetchKosDetails(UUID kosId) throws ResourceNotFoundException, ServiceUnavailableException, ServiceInteractionException {
        ApiResponseWrapper<KosDetailsDto> responseWrapper;
        try {
            responseWrapper = kosServiceClient.getKosDetailsApiResponse(kosId);
        } catch (FeignException e) {
            log.error("FeignException while fetching Kos details for ID {}: Status {}, Body {}",
                    kosId, e.status(), e.contentUTF8(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Kos not found with ID: " + kosId + " (via Kos Service Feign 404).", e);
            }
            throw new ServiceUnavailableException("Error fetching Kos details: Kos service unavailable or unexpected Feign error. " + e.getMessage());
        }

        if (responseWrapper == null) {
            throw new ServiceUnavailableException("No response from Kos service for Kos ID: " + kosId);
        }

        if (responseWrapper.getStatus() != HttpStatus.OK.value()) {
            log.warn("Kos service returned non-OK status ({}) for Kos ID {}: {}",
                    responseWrapper.getStatus(), kosId, responseWrapper.getMessage());
            if (responseWrapper.getStatus() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Kos not found with ID: " + kosId + " (Reported by Kos Service wrapper: " + responseWrapper.getMessage() + ")", null);
            }
            throw new ServiceInteractionException("Kos service reported an issue for Kos ID " + kosId + ": " +
                    responseWrapper.getStatus() + " - " + responseWrapper.getMessage());
        }

        if (responseWrapper.getData() == null) {
            log.error("Kos service returned OK status but no data for Kos ID {}", kosId);
            throw new ResourceNotFoundException("Kos details data not found for ID: " + kosId + " (Kos Service returned no data).", null);
        }
        return responseWrapper.getData();
    }

    private Optional<PropertySummaryDto> getKosAsPropertySummary(UUID kosId) {
        try {
            KosDetailsDto kosDetails = fetchKosDetails(kosId);
            PropertySummaryDto propertySummary = new PropertySummaryDto(
                    kosDetails.getId(),
                    kosDetails.getName(),
                    kosDetails.getAddress(),
                    kosDetails.getMonthlyRentPrice()
            );
            return Optional.of(propertySummary);
        } catch (ResourceNotFoundException e) {
            log.warn("KOS not found with ID: {} when trying to get summary. Message: {}", kosId, e.getMessage());
            return Optional.empty();
        } catch (ServiceUnavailableException | ServiceInteractionException e) {
            log.error("Error fetching KOS details for summary for ID {}: {}. Returning empty summary.", kosId, e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional
    public WishlistItemDto addToWishlist(UUID tenantUserId, AddToWishlistRequest request)
            throws ResourceNotFoundException, ServiceUnavailableException, ServiceInteractionException, ConflictException {
        log.info("Tenant {} attempting to add property {} to wishlist", tenantUserId, request.getPropertyId());

        KosDetailsDto kosDetails;
        try {
            kosDetails = fetchKosDetails(request.getPropertyId());
        } catch (ResourceNotFoundException e) {
            log.warn("Wishlist add failed: Property {} not found. Details: {}", request.getPropertyId(), e.getMessage());
            throw new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId() + ". Cannot add to wishlist.", e);
        } // ServiceUnavailableException and ServiceInteractionException will propagate as they are declared

        if (wishlistItemRepository.existsByTenantUserIdAndPropertyId(tenantUserId, request.getPropertyId())) {
            log.warn("Wishlist add failed: Tenant {} already has property {} in wishlist", tenantUserId, request.getPropertyId());
            throw new ConflictException("Property " + request.getPropertyId() + " is already in the wishlist.");
        }

        WishlistItem newItem = new WishlistItem(tenantUserId, request.getPropertyId());
        WishlistItem savedItem = wishlistItemRepository.save(newItem);
        log.info("Property {} added to wishlist for tenant {}", request.getPropertyId(), tenantUserId);

        PropertySummaryDto propertySummary = new PropertySummaryDto(
                kosDetails.getId(),
                kosDetails.getName(),
                kosDetails.getAddress(),
                kosDetails.getMonthlyRentPrice()
        );

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
            PropertySummaryDto propertySummary = getKosAsPropertySummary(item.getPropertyId())
                    .orElse(new PropertySummaryDto(item.getPropertyId(), "Property " + item.getPropertyId() + " (details unavailable)", "N/A", BigDecimal.ZERO));

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
        log.warn("sendUserNotification is a generic method. Consider using more specific notification methods if possible.");
        Notification notification = new Notification();
        try {
            notification.setRecipientUserId(UUID.fromString(recipientId));
        } catch (IllegalArgumentException e) {
            log.error("Invalid recipientId UUID format: {}. Notification not sent.", recipientId, e);
            return;
        }
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        if (relatedId != null) {
            try {
                notification.setRelatedPropertyId(UUID.fromString(relatedId));
            } catch (IllegalArgumentException e) {
                log.warn("relatedId '{}' is not a valid UUID for sendUserNotification. It will not be set as relatedPropertyId.", relatedId);
            }
        }
        notification.setRead(false);
        notificationRepository.save(notification);
        log.info("Sent user notification of type {} to {} with title '{}'", type, recipientId, title);
    }

    @Transactional
    public void removeFromWishlist(UUID tenantUserId, UUID propertyId) {
        log.info("Tenant {} attempting to remove property {} from wishlist", tenantUserId, propertyId);
        int deleteCount = wishlistItemRepository.deleteByTenantUserIdAndPropertyId(tenantUserId, propertyId);
        if (0 == deleteCount) {
            log.warn("Wishlist remove failed: Tenant {} did not have property {} in wishlist", tenantUserId.toString(), propertyId.toString());
            throw new ResourceNotFoundException("Property " + propertyId + " not found in wishlist for this user.", null);
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
                    return new ResourceNotFoundException("Notification not found with ID: " + notificationId, new NoSuchElementException("Notification not found"));
                });

        if (notification.getRecipientUserId() != null && !notification.getRecipientUserId().equals(userId)) {
            log.warn("Mark as read failed: User {} is not the recipient of notification {}", userId, notificationId);
            throw new ForbiddenException("User is not the recipient of this notification.");
        } else if (notification.getRecipientUserId() == null && notification.getNotificationType() == NotificationType.BROADCAST) {
            log.warn("Mark as read attempt on BROADCAST notification {} by user {}. This operation is not supported for broadcasts via this endpoint.", notificationId, userId);
            throw new ForbiddenException("Broadcast notifications cannot be marked as read by individual users through this endpoint.");
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
        notification.setTitle("Account Approved");
        notification.setMessage("Congratulations! Your Papikos account has been approved.");
        notification.setRead(false);
        notificationRepository.save(notification);
        log.info("Account approved notification sent to {}", recipientId);
    }

    @Transactional
    public NotificationDto notifyRentalUpdate(RentalUpdateRequest request)
            throws ResourceNotFoundException, ServiceUnavailableException, ServiceInteractionException {
        log.info("Processing rental update notification for Rental ID: {}, Property ID: {}, Recipient: {}",
                request.getRelatedRentalId(), request.getRelatedPropertyId(), request.getRecipientId());

        KosDetailsDto kosDetails = fetchKosDetails(request.getRelatedPropertyId());

        Notification notification = new Notification();
        notification.setRecipientUserId(request.getRecipientId());
        notification.setNotificationType(NotificationType.RENTAL_UPDATE);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRelatedRentalId(request.getRelatedRentalId());
        notification.setRelatedPropertyId(request.getRelatedPropertyId());
        notification.setRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Rental update notification {} created for rental {} concerning property {} (Name: {})",
                savedNotification.getNotificationId(), request.getRelatedRentalId(), kosDetails.getId(), kosDetails.getName());

        return NotificationDto.fromEntity(savedNotification);
    }

    @Transactional
    public void notifyPaymentUpdate(UUID rentalId, UUID recipientId, String message, String title) {
        log.info("Notifying payment update for rental {}, recipient {}", rentalId, recipientId);

        Notification notification = new Notification();
        notification.setRecipientUserId(recipientId);
        notification.setNotificationType(NotificationType.PAYMENT_UPDATE);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedRentalId(rentalId);
        notification.setRead(false);

        notificationRepository.save(notification);
        log.info("Payment update notification sent for rental {} to recipient {}", rentalId, recipientId);
    }

    @Transactional
    public List<NotificationDto> notifyWishlistUsersOnVacancy(VacancyUpdateNotification request) {
        log.info("Processing vacancy notification for property {}", request.getRelatedPropertyId());

        String propertyName = getKosAsPropertySummary(request.getRelatedPropertyId())
                .map(PropertySummaryDto::getName)
                .orElse("Property " + request.getRelatedPropertyId() + " (details unavailable)");

        List<WishlistItem> wishlistItems = wishlistItemRepository.findByPropertyId(request.getRelatedPropertyId());

        if (wishlistItems.isEmpty()) {
            log.info("No users found wishlisting property {} (Name: {})", request.getRelatedPropertyId(), propertyName);
            return List.of();
        }

        log.info("Found {} users wishlisting property {} (Name: {}). Preparing notifications.",
                wishlistItems.size(), request.getRelatedPropertyId(), propertyName);

        List<Notification> notificationsToSend = wishlistItems.stream().map(item -> {
            Notification notification = new Notification();
            notification.setRecipientUserId(item.getTenantUserId());
            notification.setNotificationType(NotificationType.WISHLIST_VACANCY);
            notification.setTitle(request.getTitle());

            String message = request.getMessage();
            if (message != null && message.contains("%s")) {
                message = String.format(message, propertyName);
            } else if (message == null || message.isBlank()) { // Use default if message is null or blank
                message = "A property on your wishlist, " + propertyName + ", now has a vacancy!";
            } // else use the provided message as is

            notification.setMessage(message);
            notification.setRead(false);
            notification.setRelatedPropertyId(request.getRelatedPropertyId());
            return notification;
        }).collect(Collectors.toList());

        if (!notificationsToSend.isEmpty()) {
            notificationRepository.saveAll(notificationsToSend);
            log.info("Sent {} vacancy notifications for property {} (Name: {})", notificationsToSend.size(), request.getRelatedPropertyId(), propertyName);
        }
        return notificationsToSend.stream().map(NotificationDto::fromEntity).collect(Collectors.toList());
    }
}