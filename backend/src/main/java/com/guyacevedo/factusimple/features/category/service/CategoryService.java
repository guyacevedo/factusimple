package com.guyacevedo.factusimple.features.category.service;

import com.guyacevedo.factusimple.features.category.dto.CategoryRequestDto;
import com.guyacevedo.factusimple.features.category.dto.CategoryResponseDto;
import com.guyacevedo.factusimple.features.category.entity.Category;
import com.guyacevedo.factusimple.features.category.mapper.CategoryMapper;
import com.guyacevedo.factusimple.features.category.repository.CategoryRepository;
import com.guyacevedo.factusimple.features.product.repository.ProductRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponseDto createCategory(UUID userId, CategoryRequestDto requestDto) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, requestDto.name())) {
            throw new ConflictException("Ya tienes una categoría con el nombre '" + requestDto.name() + "'");
        }

        Category category = categoryMapper.toEntity(requestDto);
        // Reference only: no SELECT on users, the FK is enough.
        category.setUser(userRepository.getReferenceById(userId));
        category = categoryRepository.save(category);
        log.info("Category created: id={}, userId={}, name={}", category.getId(), userId, category.getName());
        return categoryMapper.toDto(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDto getCategory(UUID userId, UUID id) {
        return categoryMapper.toDto(findCategory(userId, id));
    }

    /** For product forms and the POS: only categories in use. */
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> listActiveCategories(UUID userId, Pageable pageable) {
        return categoryRepository.findAllByUserIdAndActiveTrue(userId, pageable).map(categoryMapper::toDto);
    }

    /** Management list: includes inactive categories so they can be reactivated. */
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> listAllCategories(UUID userId, Pageable pageable) {
        return categoryRepository.findAllByUserId(userId, pageable).map(categoryMapper::toDto);
    }

    @Transactional
    public CategoryResponseDto updateCategory(UUID userId, UUID id, CategoryRequestDto requestDto) {
        Category category = findCategory(userId, id);

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, requestDto.name(), id)) {
            throw new ConflictException("Ya tienes otra categoría con el nombre '" + requestDto.name() + "'");
        }

        categoryMapper.updateEntity(requestDto, category);
        log.info("Category updated: id={}", id);
        return categoryMapper.toDto(category);
    }

    /**
     * A category with products cannot be deleted (products.category_id has no
     * ON DELETE). Deactivate it instead with isActive=false.
     */
    @Transactional
    public void deleteCategory(UUID userId, UUID id) {
        Category category = findCategory(userId, id);

        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "La categoría tiene productos y no puede eliminarse. Desactívala con isActive=false");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: id={}", id);
    }

    // Another user's category answers 404, not 403, so its existence is not revealed.
    private Category findCategory(UUID userId, UUID id) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", id.toString()));
    }
}
