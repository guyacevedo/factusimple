package com.guyacevedo.factusimple.features.product.entity;

import com.guyacevedo.factusimple.features.category.entity.Category;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;
import com.guyacevedo.factusimple.shared.util.UnitMeasurementConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SKU and barcode are unique per establishment (SKU ignoring case), see V2.
 * Taxes and withholding taxes belong to the product and are saved with it.
 * Stock lives in ProductStock and is changed through its own repository.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank(message = "El SKU es requerido")
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "barcode", length = 60)
    private String barcode;

    @NotBlank(message = "El nombre del producto es requerido")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "El precio de venta es requerido")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @NotNull(message = "El precio mayorista es requerido")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @NotNull(message = "El precio de costo es requerido")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Builder.Default
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "min_stock_wholesale", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStockWholesale = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    /** Stored as the DIAN code ("94", "KGM", ...), not as the enum name. */
    @NotNull(message = "La unidad de medida es requerida")
    @Builder.Default
    @Convert(converter = UnitMeasurementConverter.class)
    @Column(name = "unit_measure_code", nullable = false, length = 10)
    private UnitMeasurement unitMeasure = UnitMeasurement.UNIDAD;

    @Column(name = "standard_code", length = 10)
    private String standardCode;

    @NotNull(message = "El tipo de venta es requerido")
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 20)
    private SaleType saleType = SaleType.BY_UNIT;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTax> taxes = new ArrayList<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductWithholdingTax> withholdingTaxes = new ArrayList<>();

    /** Same rule as ck_product_unit_consistency, checked before reaching the database. */
    @AssertTrue(message = "La unidad de medida no corresponde al tipo de venta")
    public boolean isUnitConsistentWithSaleType() {
        return unitMeasure == null || saleType == null || unitMeasure.isConsistentWith(saleType);
    }

    public void addTax(ProductTax tax) {
        taxes.add(tax);
        tax.setProduct(this);
    }

    public void removeTax(ProductTax tax) {
        taxes.remove(tax);
        tax.setProduct(null);
    }

    public void addWithholdingTax(ProductWithholdingTax tax) {
        withholdingTaxes.add(tax);
        tax.setProduct(this);
    }

    public void removeWithholdingTax(ProductWithholdingTax tax) {
        withholdingTaxes.remove(tax);
        tax.setProduct(null);
    }
}