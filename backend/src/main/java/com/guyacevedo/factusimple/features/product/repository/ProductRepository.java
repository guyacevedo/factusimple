package com.guyacevedo.factusimple.features.product.repository;

import com.guyacevedo.factusimple.features.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCategoryId(UUID categoryId);
}
