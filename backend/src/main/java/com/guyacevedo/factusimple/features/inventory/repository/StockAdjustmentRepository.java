package com.guyacevedo.factusimple.features.inventory.repository;

import com.guyacevedo.factusimple.features.inventory.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Adjustments have no user_id: they are scoped to the owner through product.user. */
@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    boolean existsByProductId(UUID productId);

    @EntityGraph(attributePaths = "product")
    Page<StockAdjustment> findAllByProductUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<StockAdjustment> findAllByProductIdAndProductUserId(UUID productId, UUID userId, Pageable pageable);
}
