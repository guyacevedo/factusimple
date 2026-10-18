package com.guyacevedo.factusimple.features.parameter.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Java fields are paramKey / paramValue because KEY and VALUE are reserved
 * words in JPQL; the columns keep their names.
 */
@Entity
@Table(name = "parameters")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parameter extends BaseEntity {

    @Column(name = "key", nullable = false, length = 80)
    private String paramKey;

    @ToString.Exclude
    @Column(name = "value", columnDefinition = "TEXT")
    private String paramValue;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "encryption", nullable = false)
    private Boolean encryption = false;
}