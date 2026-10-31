package com.guyacevedo.factusimple.features.product.service;

import com.guyacevedo.factusimple.features.category.entity.Category;
import com.guyacevedo.factusimple.features.category.repository.CategoryRepository;
import com.guyacevedo.factusimple.features.inventory.repository.StockAdjustmentRepository;
import com.guyacevedo.factusimple.features.inventory.service.InventoryService;
import com.guyacevedo.factusimple.features.product.dto.ProductRequestDto;
import com.guyacevedo.factusimple.features.product.dto.ProductResponseDto;
import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.features.product.mapper.ProductMapper;
import com.guyacevedo.factusimple.features.product.repository.ProductRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.UnprocessableEntityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final InventoryService inventoryService;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponseDto createProduct(UUID userId, ProductRequestDto requestDto) {
        if (productRepository.existsByUserIdAndSkuIgnoreCase(userId, requestDto.sku())) {
            throw new ConflictException("Ya tienes un producto con el SKU '" + requestDto.sku() + "'");
        }
        if (requestDto.barcode() != null && productRepository.existsByUserIdAndBarcode(userId, requestDto.barcode())) {
            throw new ConflictException("Ya tienes un producto con el código de barras '" + requestDto.barcode() + "'");
        }

        Product product = productMapper.toEntity(requestDto);
        checkProductRules(product);

        // Atomic check against the plan; rolled back with the transaction if anything below fails.
        // It clears the persistence context, so the category is loaded after it.
        if (userRepository.incrementProductCountIfBelowLimit(userId) == 0) {
            throw new UnprocessableEntityException(
                    "Alcanzaste el máximo de productos de tu plan", "PLAN_LIMIT_REACHED");
        }

        product.setCategory(resolveCategory(userId, requestDto.categoryId()));
        // Reference only: no SELECT on users, the FK is enough.
        product.setUser(userRepository.getReferenceById(userId));
        replaceTaxes(product, requestDto);
        product = productRepository.save(product);
        inventoryService.createStock(product, requestDto.initialStock());

        log.info("Product created: id={}, userId={}, sku={}", product.getId(), userId, product.getSku());
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(UUID userId, UUID id) {
        return productMapper.toDto(findProduct(userId, id));
    }

    /** For invoicing and the POS: only products that can be sold. */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> listActiveProducts(UUID userId, Pageable pageable) {
        return productRepository.findAllByUserIdAndActiveTrue(userId, pageable).map(productMapper::toDto);
    }

    /** Management list: includes inactive products so they can be reactivated. */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> listAllProducts(UUID userId, Pageable pageable) {
        return productRepository.findAllByUserId(userId, pageable).map(productMapper::toDto);
    }

    /** POS search by name, SKU or barcode; an empty text returns everything. */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(UUID userId, String query, UUID categoryId,
                                                   boolean activeOnly, Pageable pageable) {
        return productRepository.search(userId, toLikePattern(query), categoryId, activeOnly, pageable)
                .map(productMapper::toDto);
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID userId, UUID id, ProductRequestDto requestDto) {
        Product product = findProduct(userId, id);

        if (productRepository.existsByUserIdAndSkuIgnoreCaseAndIdNot(userId, requestDto.sku(), id)) {
            throw new ConflictException("Ya tienes otro producto con el SKU '" + requestDto.sku() + "'");
        }
        if (requestDto.barcode() != null
                && productRepository.existsByUserIdAndBarcodeAndIdNot(userId, requestDto.barcode(), id)) {
            throw new ConflictException("Ya tienes otro producto con el código de barras '" + requestDto.barcode() + "'");
        }

        SaleType previousSaleType = product.getSaleType();
        UUID currentCategoryId = product.getCategory() == null ? null : product.getCategory().getId();

        productMapper.updateEntity(requestDto, product);
        // Only a new category is checked, so keeping one that was deactivated later still works.
        if (!Objects.equals(requestDto.categoryId(), currentCategoryId)) {
            product.setCategory(resolveCategory(userId, requestDto.categoryId()));
        }
        replaceTaxes(product, requestDto);
        checkProductRules(product);

        if (product.getSaleType() != previousSaleType
                && !product.getSaleType().acceptsQuantity(inventoryService.currentQuantity(id))) {
            throw new BadRequestException(
                    "El stock actual tiene decimales: ajústalo a un número entero antes de vender por unidad");
        }

        log.info("Product updated: id={}", id);
        return productMapper.toDto(product);
    }

    /**
     * A product with inventory movements cannot be deleted (stock_adjustments.product_id
     * is ON DELETE RESTRICT, and an initial stock counts as one). Deactivate it
     * instead with isActive=false. Its stock row goes with it (ON DELETE CASCADE).
     */
    @Transactional
    public void deleteProduct(UUID userId, UUID id) {
        Product product = findProduct(userId, id);

        if (stockAdjustmentRepository.existsByProductId(id)) {
            throw new ConflictException(
                    "El producto tiene movimientos de inventario y no puede eliminarse. Desactívalo con isActive=false");
        }

        productRepository.delete(product);
        // Frees the slot in the plan; flushAutomatically writes the DELETE first.
        userRepository.decrementProductCount(userId);
        log.info("Product deleted: id={}", id);
    }

    /** Same rules as ck_product_unit_consistency, checked here because a PUT may send only one of the two. */
    private static void checkProductRules(Product product) {
        SaleType saleType = product.getSaleType();
        if (!product.getUnitMeasure().isConsistentWith(saleType)) {
            throw new BadRequestException(String.format(
                    "La unidad de medida %s no corresponde al tipo de venta %s", product.getUnitMeasure(), saleType));
        }
        if (!saleType.acceptsQuantity(product.getMinStock())
                || !saleType.acceptsQuantity(product.getMinStockWholesale())) {
            throw new BadRequestException("Los productos que se venden por unidad solo admiten cantidades mínimas enteras");
        }
    }

    /** A null list keeps the current taxes; a list, even empty, replaces them (orphanRemoval deletes the old ones). */
    private void replaceTaxes(Product product, ProductRequestDto requestDto) {
        if (requestDto.taxes() != null) {
            product.getTaxes().clear();
            requestDto.taxes().forEach(tax -> product.addTax(productMapper.toTaxEntity(tax)));
        }
        if (requestDto.withholdingTaxes() != null) {
            product.getWithholdingTaxes().clear();
            requestDto.withholdingTaxes().forEach(tax ->
                    product.addWithholdingTax(productMapper.toWithholdingTaxEntity(tax)));
        }
    }

    // Never null, and %, _ and the backslash are escaped so "50%" is searched as typed.
    private static String toLikePattern(String query) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private Category resolveCategory(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", categoryId.toString()));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BadRequestException("La categoría '" + category.getName() + "' está desactivada");
        }
        return category;
    }

    // Another user's product answers 404, not 403, so its existence is not revealed.
    private Product findProduct(UUID userId, UUID id) {
        return productRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id.toString()));
    }
}
