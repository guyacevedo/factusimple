package com.guyacevedo.factusimple.features.user.dto;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String companyName,
        String nit,
        String role,
        UUID planId,
        String planName,
        Integer maxProducts,
        Integer productCount,
        Integer maxCustomers,
        Integer customerCount,
        Integer maxInvoices,
        Integer invoiceCount,
        Boolean isActive
) {}