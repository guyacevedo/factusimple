package com.guyacevedo.factusimple.features.inventory.repository;

import com.guyacevedo.factusimple.features.inventory.entity.ProductStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Stock rows have no user_id: they are scoped to the owner through product.user. */
@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {

    @EntityGraph(attributePaths = "product")
    Optional<ProductStock> findByProductIdAndProductUserId(UUID productId, UUID userId);

    /** Locks the row (SELECT ... FOR UPDATE) so a sale cannot change the stock while it is adjusted. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStock s JOIN FETCH s.product p WHERE p.id = :productId AND p.user.id = :userId")
    Optional<ProductStock> findForUpdate(@Param("productId") UUID productId, @Param("userId") UUID userId);

    @Query("SELECT s.quantity FROM ProductStock s WHERE s.product.id = :productId")
    Optional<BigDecimal> findQuantityByProductId(@Param("productId") UUID productId);

    @EntityGraph(attributePaths = "product")
    Page<ProductStock> findAllByProductUserId(UUID userId, Pageable pageable);

    // Active products at or below their minimum (same rule as ProductStock.belowMinimumQuantity).
    @Query(value = """
            SELECT s FROM ProductStock s JOIN FETCH s.product p
            WHERE p.user.id = :userId AND p.active = true AND s.quantity <= p.minStock
            """,
            countQuery = """
            SELECT COUNT(s) FROM ProductStock s JOIN s.product p
            WHERE p.user.id = :userId AND p.active = true AND s.quantity <= p.minStock
            """)
    Page<ProductStock> findLowStockByUserId(@Param("userId") UUID userId, Pageable pageable);

    // ── Sales ───────────────────────────────────────────────────────────────
    // One statement each, like the plan counters in UserRepository: two cashiers
    // selling the last unit cannot both get it (no read-check-write window).
    // Bulk updates skip @Version and auditing, so version and updated_at are set here.
    // If adjustStock holds the row (FOR UPDATE) these wait, and PostgreSQL re-checks
    // the WHERE against the committed quantity.

    /** @return 1 if it was deducted, 0 if there was not enough stock */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ProductStock s
               SET s.quantity = s.quantity - :quantity,
                   s.version = s.version + 1,
                   s.updatedAt = LOCAL DATETIME
             WHERE s.product.id = :productId
               AND s.quantity >= :quantity
            """)
    int deductIfAvailable(@Param("productId") UUID productId, @Param("quantity") BigDecimal quantity);

    /** Adding never leaves the stock invalid, so there is no condition. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ProductStock s
               SET s.quantity = s.quantity + :quantity,
                   s.version = s.version + 1,
                   s.updatedAt = LOCAL DATETIME
             WHERE s.product.id = :productId
            """)
    int increment(@Param("productId") UUID productId, @Param("quantity") BigDecimal quantity);
}
