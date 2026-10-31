package com.guyacevedo.factusimple.features.catalog.dto;

/** code is the DANE code of the municipality (5 digits, e.g. 11001 Bogotá). */
public record MunicipalityDto(String code, String name, String departmentCode, String departmentName) {}
