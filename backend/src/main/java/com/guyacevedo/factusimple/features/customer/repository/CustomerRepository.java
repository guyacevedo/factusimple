package com.guyacevedo.factusimple.features.customer.repository;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). The identification is unique
 * per user (uk_customer_user_identification), compared exactly.
 * Derived queries use the Java field names: the field is "active", not "isActive".
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndIdentification(UUID userId, String identification);

    boolean existsByUserIdAndIdentificationAndIdNot(UUID userId, String identification, UUID id);

    Page<Customer> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    Page<Customer> findAllByUserId(UUID userId, Pageable pageable);
}
