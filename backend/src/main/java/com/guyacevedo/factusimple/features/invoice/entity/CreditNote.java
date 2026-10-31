package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "credit_notes")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNote extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ToString.Exclude
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotNull
    @Column(name = "numbering_range_id", nullable = false)
    private Integer numberingRangeId;

    @NotBlank
    @Column(name = "reference_code", nullable = false, length = 100)
    private String referenceCode;

    @NotBlank
    @Column(name = "correction_concept_code", nullable = false, length = 100)
    private String correctionConceptCode;

    @Builder.Default
    @Column(name = "customization_id", length = 2)
    private String customizationId = "20";

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditNoteStatus status = CreditNoteStatus.PENDING;

    @Column(name = "factus_number", length = 50)
    private String factusNumber;

    @Column(name = "cufe", length = 200)
    private String cufe;

    @Column(name = "xml_url", columnDefinition = "TEXT")
    private String xmlUrl;

    @Column(name = "factus_error", columnDefinition = "TEXT")
    private String factusError;
}