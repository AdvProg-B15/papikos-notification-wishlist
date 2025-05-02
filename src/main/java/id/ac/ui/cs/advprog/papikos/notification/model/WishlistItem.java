package id.ac.ui.cs.advprog.papikos.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantUserId", "propertyId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishlistItemId;

    @Column(nullable = false)
    private Long tenantUserId; // Logical FK to User in Auth Service

    @Column(nullable = false)
    private Long propertyId; // Logical FK to Property in Property Service

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructor for service layer usage
    public WishlistItem(Long tenantUserId, Long propertyId) {
        this.tenantUserId = tenantUserId;
        this.propertyId = propertyId;
    }
}
