package com.guyacevedo.factusimple.features.catalog.dto;

import com.guyacevedo.factusimple.features.product.entity.SaleType;

/**
 * name is the enum value the API expects in ProductRequestDto.unitMeasure,
 * code the DIAN code and label the name Factus lists it with.
 */
public record UnitMeasureEntry(String name, String code, String label, String abbreviation, SaleType saleType) {}
