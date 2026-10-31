package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;
import com.guyacevedo.factusimple.shared.util.UnitMeasurementConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A sold line, frozen at the moment of the sale (see V8): name, unit, price,
 * taxes and amounts are copied from the product so the invoice does not change
 * when the product does. product is only a link and may become null.
 *
 * unitPrice includes taxes; total = baseAmount + taxAmount is what the customer
 * pays for the line. Withholding taxes are informed and do not lower the total.
 */
@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "code_reference", length = 50)
    private String codeReference;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Stored as the DIAN code ("94", "KGM", ...), not as the enum name. */
    @Convert(converter = UnitMeasurementConverter.class)
    @Column(name = "unit_measure_code", length = 10)
    private UnitMeasurement unitMeasure;

    @Column(name = "standard_code", length = 10)
    private String standardCode;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** After the discount, before taxes. */
    @Builder.Default
    @Column(name = "base_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "withholding_tax_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal withholdingTaxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 16, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemTax> taxes = new ArrayList<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemWithholdingTax> withholdingTaxes = new ArrayList<>();

    public void addTax(InvoiceItemTax tax) {
        taxes.add(tax);
        tax.setInvoiceItem(this);
    }

    public void addWithholdingTax(InvoiceItemWithholdingTax tax) {
        withholdingTaxes.add(tax);
        tax.setInvoiceItem(this);
    }
}
