package id.ac.ui.cs.advprog.papikos.notification.repository;

import id.ac.ui.cs.advprog.papikos.notification.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByTenantUserId(String tenantUserId);

    boolean existsByTenantUserIdAndPropertyId(String tenantUserId, String propertyId);

    @Transactional 
    String deleteByTenantUserIdAndPropertyId(String tenantUserId, String propertyId);

    List<WishlistItem> findByPropertyId(String propertyId);

    Optional<WishlistItem> findByTenantUserIdAndPropertyId(String tenantUserId, String propertyId);

    Optional<List<WishlistItem>> findByTenantUserIdOrderByCreatedAtDesc(String tenantUserId);
    Optional<List<String>> findTenantUserIdsByPropertyId(String propertyId);
}
