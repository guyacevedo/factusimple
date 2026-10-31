package com.guyacevedo.factusimple.features.inventory.service;

import com.guyacevedo.factusimple.features.inventory.dto.StockAdjustmentRequestDto;
import com.guyacevedo.factusimple.features.inventory.dto.StockAdjustmentResponseDto;
import com.guyacevedo.factusimple.features.inventory.dto.StockResponseDto;
import com.guyacevedo.factusimple.features.inventory.entity.AdjustmentReason;
import com.guyacevedo.factusimple.features.inventory.entity.ProductStock;
import com.guyacevedo.factusimple.features.inventory.entity.StockAdjustment;
import com.guyacevedo.factusimple.features.inventory.mapper.ProductStockMapper;
import com.guyacevedo.factusimple.features.inventory.mapper.StockAdjustmentMapper;
import com.guyacevedo.factusimple.features.inventory.repository.ProductStockRepository;
import com.guyacevedo.factusimple.features.inventory.repository.StockAdjustmentRepository;
import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.UnprocessableEntityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Every stock change that is not a sale goes through here and leaves a
 * StockAdjustment with the previous and new quantity and its reason.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductStockMapper productStockMapper;
    private final StockAdjustmentMapper stockAdjustmentMapper;

    /**
     * Creates the stock row of a new product. Runs inside ProductService's
     * transaction: an invalid initial stock rolls back the product too.
     * A positive initial stock is recorded as a RECEIPT adjustment.
     */
    @Transactional
    public void createStock(Product product, BigDecimal initialQuantity) {
        BigDecimal quantity = initialQuantity == null ? BigDecimal.ZERO : initialQuantity;
        checkQuantity(product.getSaleType(), quantity);

        productStockRepository.save(ProductStock.builder().product(product).quantity(quantity).build());
        if (quantity.signum() > 0) {
            stockAdjustmentRepository.save(StockAdjustment.builder()
                    .product(product)
                    .previousQuantity(BigDecimal.ZERO)
                    .newQuantity(quantity)
                    .reason(AdjustmentReason.RECEIPT)
                    .notes("Stock inicial")
                    .build());
        }
    }

    /** Stock of a product the caller already checked; 0 if it has no stock row. */
    @Transactional(readOnly = true)
    public BigDecimal currentQuantity(UUID productId) {
        return productStockRepository.findQuantityByProductId(productId).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public StockResponseDto getStock(UUID userId, UUID productId) {
        return productStockMapper.toDto(productStockRepository.findByProductIdAndProductUserId(productId, userId)
                .orElseThrow(() -> productNotFound(productId)));
    }

    @Transactional(readOnly = true)
    public Page<StockResponseDto> listStocks(UUID userId, boolean lowStockOnly, Pageable pageable) {
        Page<ProductStock> stocks = lowStockOnly
                ? productStockRepository.findLowStockByUserId(userId, pageable)
                : productStockRepository.findAllByProductUserId(userId, pageable);
        return stocks.map(productStockMapper::toDto);
    }

    @Transactional
    public StockAdjustmentResponseDto adjustStock(UUID userId, StockAdjustmentRequestDto requestDto) {
        ProductStock stock = productStockRepository.findForUpdate(requestDto.productId(), userId)
                .orElseThrow(() -> productNotFound(requestDto.productId()));

        checkQuantity(stock.getProduct().getSaleType(), requestDto.newQuantity());
        BigDecimal previousQuantity = stock.getQuantity();
        if (previousQuantity.compareTo(requestDto.newQuantity()) == 0) {
            throw new BadRequestException("La nueva cantidad es igual al stock actual");
        }

        stock.setQuantity(requestDto.newQuantity());
        StockAdjustment adjustment = stockAdjustmentRepository.save(StockAdjustment.builder()
                .product(stock.getProduct())
                .previousQuantity(previousQuantity)
                .newQuantity(requestDto.newQuantity())
                .reason(requestDto.reason())
                .notes(requestDto.notes())
                .build());
        log.info("Stock adjusted: productId={}, {} -> {}, reason={}",
                requestDto.productId(), previousQuantity, requestDto.newQuantity(), requestDto.reason());
        return stockAdjustmentMapper.toDto(adjustment);
    }

    /**
     * Deducts the stock of a sale in one statement, or fails if there is not
     * enough, so two concurrent checkouts cannot oversell. Sales leave no
     * StockAdjustment: their trace is the invoice items.
     * The caller already checked that the product belongs to the user and that
     * the quantity fits its sale type (SaleType.validQuantity).
     */
    @Transactional
    public void deductStock(UUID productId, String productName, BigDecimal quantity) {
        checkPositive(quantity);
        if (productStockRepository.deductIfAvailable(productId, quantity) == 0) {
            throw new UnprocessableEntityException(
                    "No hay stock suficiente de '" + productName + "'", "INSUFFICIENT_STOCK");
        }
    }

    /** Undoes a deduction, e.g. when Factus rejects the invoice. Same caller checks as deductStock. */
    @Transactional
    public void restoreStock(UUID productId, BigDecimal quantity) {
        checkPositive(quantity);
        if (productStockRepository.increment(productId, quantity) == 0) {
            throw productNotFound(productId);
        }
    }

    /** History of one product when productId is given, otherwise of the whole establishment. */
    @Transactional(readOnly = true)
    public Page<StockAdjustmentResponseDto> listAdjustments(UUID userId, UUID productId, Pageable pageable) {
        Page<StockAdjustment> adjustments = productId == null
                ? stockAdjustmentRepository.findAllByProductUserId(userId, pageable)
                : stockAdjustmentRepository.findAllByProductIdAndProductUserId(productId, userId, pageable);
        return adjustments.map(stockAdjustmentMapper::toDto);
    }

    // A zero or negative quantity here is a bug in the caller, not bad user input.
    private static void checkPositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0: " + quantity);
        }
    }

    private static void checkQuantity(SaleType saleType, BigDecimal quantity) {
        if (!saleType.acceptsQuantity(quantity)) {
            throw new BadRequestException("Los productos que se venden por unidad solo admiten cantidades enteras");
        }
    }

    // Another user's product answers 404, not 403, so its existence is not revealed.
    private static ResourceNotFoundException productNotFound(UUID productId) {
        return new ResourceNotFoundException("Producto", "id", productId.toString());
    }
}
