package com.guyacevedo.factusimple.features.auth.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt extends BaseEntity {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "successful", nullable = false)
    private Boolean successful;

    @Column(name = "ip", length = 45)
    private String ip;
}