package com.guyacevedo.factusimple.features.category.repository;

import com.guyacevedo.factusimple.features.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). Names are unique per user
 * ignoring case (uk_category_user_name on LOWER(name)), so checks ignore case too.
 * Derived queries use the Java field names: the field is "active", not "isActive".
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    Page<Category> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    Page<Category> findAllByUserId(UUID userId, Pageable pageable);
}
