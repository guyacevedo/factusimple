package com.guyacevedo.factusimple.features.plan.repository;

import com.guyacevedo.factusimple.features.plan.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Derived queries use the Java field names: the field is "active", not "isActive". */
@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Plan> findByNameAndActiveTrue(String name);

    Page<Plan> findAllByActiveTrue(Pageable pageable);
}