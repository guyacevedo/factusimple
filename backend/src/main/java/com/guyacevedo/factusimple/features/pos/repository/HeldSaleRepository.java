package com.guyacevedo.factusimple.features.pos.repository;

import com.guyacevedo.factusimple.features.pos.entity.HeldSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). Labels are unique per user
 * ignoring case (uk_held_sale_user_label on LOWER(label)), so checks ignore case too.
 */
@Repository
public interface HeldSaleRepository extends JpaRepository<HeldSale, UUID> {

    /** The one that has been waiting longest comes first. */
    List<HeldSale> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<HeldSale> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndLabelIgnoreCase(UUID userId, String label);

    boolean existsByUserIdAndLabelIgnoreCaseAndIdNot(UUID userId, String label, UUID id);
}
