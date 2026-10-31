package com.guyacevedo.factusimple.features.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponseDto(
    UUID id,
    String idTypeCode,
    String identification,
    String dv,
    String legalOrgCode,
    String company,
    String names,
    String tradeName,
    String address,
    String email,
    String phone,
    String municipalityCode,
    String tributeCode,
    String fiscalResponsibility,
    BigDecimal creditLimit,
    BigDecimal creditBalance,
    String notes,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
