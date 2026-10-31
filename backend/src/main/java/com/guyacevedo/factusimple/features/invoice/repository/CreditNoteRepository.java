package com.guyacevedo.factusimple.features.invoice.repository;

import com.guyacevedo.factusimple.features.invoice.entity.CreditNote;
import com.guyacevedo.factusimple.features.invoice.entity.CreditNoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Every query is scoped to the owner (user_id). The invoice is fetched because the DTO prints its code. */
@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, UUID> {

    @EntityGraph(attributePaths = "invoice")
    Optional<CreditNote> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = "invoice")
    @Query("""
            SELECT c FROM CreditNote c
             WHERE c.user.id = :userId
               AND (:status IS NULL OR c.status = :status)
               AND (:invoiceId IS NULL OR c.invoice.id = :invoiceId)
            """)
    Page<CreditNote> search(@Param("userId") UUID userId,
                            @Param("status") CreditNoteStatus status,
                            @Param("invoiceId") UUID invoiceId,
                            Pageable pageable);
}
