package id.ac.ui.cs.advprog.papikos.notification.repository;

import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByTenantUserId(UUID tenantUserId);

    boolean existsByTenantUserIdAndPropertyId(UUID tenantUserId, UUID propertyId);

    @Transactional 
    int deleteByTenantUserIdAndPropertyId(UUID tenantUserId, UUID propertyId);

    List<WishlistItem> findByPropertyId(UUID propertyId);

    Optional<WishlistItem> findByTenantUserIdAndPropertyId(UUID tenantUserId, UUID propertyId);

    Optional<List<WishlistItem>> findByTenantUserIdOrderByCreatedAtDesc(UUID tenantUserId);
    Optional<List<UUID>> findTenantUserIdsByPropertyId(UUID propertyId);
}
