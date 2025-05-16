package id.ac.ui.cs.advprog.papikos.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.util.UUID;

import java.time.Instant;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantUserId", "propertyId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class WishlistItem {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    @GeneratedValue
    private UUID wishlistItemId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID tenantUserId; // Logical FK to User in Auth Service

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID propertyId; // Logical FK to Property in Property Service

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructor for service layer usage
    public WishlistItem(UUID tenantUserId, UUID propertyId) {
        this.tenantUserId = tenantUserId;
        this.propertyId = propertyId;
    }
}
