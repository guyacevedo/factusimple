package com.guyacevedo.factusimple.features.user.entity;

import com.guyacevedo.factusimple.features.plan.entity.Plan;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * An establishment (tenant) or a platform admin. Email is unique ignoring case (see V1).
 *
 * @DynamicUpdate: the counters (products, customers, invoices, failed logins)
 * change through bulk UPDATEs in UserRepository. Without it, saving a User
 * loaded earlier would write every column back and undo those increments.
 */
@Entity
@DynamicUpdate
@Table(name = "users")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @ToString.Exclude
    @NotBlank(message = "La contraseña es requerida")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotBlank(message = "El nombre es requerido")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{7,20}$", message = "Teléfono inválido")
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    @NotBlank(message = "El NIT es requerido")
    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "dv", length = 2)
    private String dv;

    @NotBlank(message = "La razón social es requerida")
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "legal_org_code", length = 10)
    private String legalOrgCode;

    @Column(name = "tribute_code", length = 4)
    private String tributeCode;

    @Column(name = "fiscal_responsibility", length = 50)
    private String fiscalResponsibility;

    @Builder.Default
    @Column(name = "product_count", nullable = false)
    private Integer productCount = 0;

    @Builder.Default
    @Column(name = "customer_count", nullable = false)
    private Integer customerCount = 0;

    @Builder.Default
    @Column(name = "invoice_count", nullable = false)
    private Integer invoiceCount = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    // ── Login lock ──────────────────────────────────────────────────────────
    // After MAX_FAILED_LOGIN_ATTEMPTS wrong passwords, blocked_at is set and
    // login is refused for LOCK_DURATION. The lock lifts by itself; a correct
    // login or a password reset clears blocked_at and the counter.

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 10;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public boolean isLocked() {
        return blockedAt != null && blockedAt.plus(LOCK_DURATION).isAfter(LocalDateTime.now());
    }
}