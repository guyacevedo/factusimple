package com.guyacevedo.factusimple.features.plan.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "plans")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan extends BaseEntity {

    @NotBlank(message = "El nombre del plan es requerido")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "El máximo de productos es requerido")
    @Min(value = 1, message = "El máximo de productos debe ser al menos 1")
    @Column(name = "max_products", nullable = false)
    private Integer maxProducts;

    @NotNull(message = "El máximo de clientes es requerido")
    @Min(value = 1, message = "El máximo de clientes debe ser al menos 1")
    @Column(name = "max_customers", nullable = false)
    private Integer maxCustomers;

    @NotNull(message = "El máximo de facturas es requerido")
    @Min(value = 1, message = "El máximo de facturas debe ser al menos 1")
    @Column(name = "max_invoices", nullable = false)
    private Integer maxInvoices;

    @Builder.Default
    @Column(name = "required_invited_code", nullable = false)
    private Boolean requiredInvitedCode = false;

    @Column(name = "code", length = 255)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}