package com.guyacevedo.factusimple.features.customer.entity;

import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "El tipo de identificación es requerido")
    @Column(name = "id_type_code", nullable = false, length = 4)
    private String idTypeCode; // "31" = NIT, "13" = CC, etc.

    @NotBlank(message = "La identificación es requerida")
    @Column(name = "identification", nullable = false, length = 30)
    private String identification;

    @Column(name = "dv", length = 2)
    private String dv;

    @Column(name = "legal_org_code", length = 10)
    private String legalOrgCode; // "1" = Persona Jurídica, "2" = Persona Natural

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "names", length = 255)
    private String names;

    @Column(name = "trade_name", length = 255)
    private String tradeName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    @Column(name = "tribute_code", length = 4)
    private String tributeCode;

    @Column(name = "fiscal_responsibility", length = 50)
    private String fiscalResponsibility;

    @Builder.Default
    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "credit_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}