package com.guyacevedo.factusimple.features.product.repository;

import com.guyacevedo.factusimple.features.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). The SKU is unique per user
 * ignoring case (uk_product_user_sku on UPPER(sku)) and the barcode is unique
 * per user when present (uk_product_user_barcode).
 * Derived queries use the Java field names: the field is "active", not "isActive".
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCategoryId(UUID categoryId);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndSkuIgnoreCase(UUID userId, String sku);

    boolean existsByUserIdAndSkuIgnoreCaseAndIdNot(UUID userId, String sku, UUID id);

    boolean existsByUserIdAndBarcode(UUID userId, String barcode);

    boolean existsByUserIdAndBarcodeAndIdNot(UUID userId, String barcode, UUID id);

    /** The products of an invoice; their taxes load in batches when the lines are calculated. */
    List<Product> findAllByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * POS search: by name, SKU or barcode at once. The cashier types whatever
     * they have at hand and should not have to pick the field.
     * The pattern always arrives built and never null (see ProductService.toLikePattern),
     * so PostgreSQL never gets an untyped null parameter.
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
             WHERE p.user.id = :userId
               AND (:activeOnly = false OR p.active = true)
               AND (:categoryId IS NULL OR p.category.id = :categoryId)
               AND (LOWER(p.name) LIKE :pattern ESCAPE '\\'
                    OR LOWER(p.sku) LIKE :pattern ESCAPE '\\'
                    OR LOWER(p.barcode) LIKE :pattern ESCAPE '\\')
            """)
    Page<Product> search(@Param("userId") UUID userId,
                         @Param("pattern") String pattern,
                         @Param("categoryId") UUID categoryId,
                         @Param("activeOnly") boolean activeOnly,
                         Pageable pageable);
}
