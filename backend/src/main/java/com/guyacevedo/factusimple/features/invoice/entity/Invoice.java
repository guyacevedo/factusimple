package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank
    @Column(name = "reference_code", nullable = false, length = 100)
    private String referenceCode;

    @Column(name = "document_type", length = 4)
    private String documentType;   // default "01"

    @Column(name = "operation_type", length = 20)
    private String operationType;  // default "10"

    @Builder.Default
    @Column(name = "send_email", nullable = false)
    private Boolean sendEmail = false;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Builder.Default
    @Column(name = "cash_rounding", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashRounding = BigDecimal.ZERO;

    @NotNull
    @Column(name = "numbering_range_id", nullable = false)
    private Integer numberingRangeId;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Builder.Default
    @Column(name = "subtotal", nullable = false, precision = 16, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_taxes", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_holding_taxes", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalHoldingTaxes = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_discounts", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 16, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Cash the customer handed over (cash sales only). */
    @Column(name = "cash_received", precision = 16, scale = 2)
    private BigDecimal cashReceived;

    /** Change returned: cashReceived - what was paid in cash. */
    @Column(name = "change_amount", precision = 16, scale = 2)
    private BigDecimal changeAmount;

    /**
     * What the customer still owes (credit sales only, 0 for cash sales).
     * Credit payments lower it; cancelling releases whatever is left.
     */
    @Builder.Default
    @Column(name = "balance_due", nullable = false, precision = 16, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cufe", length = 200)
    private String cufe;

    @Column(name = "xml_url", columnDefinition = "TEXT")
    private String xmlUrl;

    @Column(name = "factus_number", length = 50)
    private String factusNumber;

    @Column(name = "factus_error", columnDefinition = "TEXT")
    private String factusError;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoicePayment> payments = new ArrayList<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AllowanceCharge> allowanceCharges = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }

    public void addPayment(InvoicePayment payment) {
        payments.add(payment);
        payment.setInvoice(this);
    }

    public void removePayment(InvoicePayment payment) {
        payments.remove(payment);
        payment.setInvoice(null);
    }

    public void addAllowanceCharge(AllowanceCharge charge) {
        allowanceCharges.add(charge);
        charge.setInvoice(this);
    }

    public void removeAllowanceCharge(AllowanceCharge charge) {
        allowanceCharges.remove(charge);
        charge.setInvoice(null);
    }

    /** Marks the invoice as cancelled, keeping status and cancelledAt in step. */
    public void cancel(String reason) {
        this.status = InvoiceStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }
}