package com.guyacevedo.factusimple.features.catalog.dto;

import java.util.List;

public record CatalogsResponseDto(
    List<CatalogEntry> identificationTypes,
    List<CatalogEntry> legalOrganizations,
    List<CatalogEntry> tributes,
    List<CatalogEntry> fiscalResponsibilities,
    List<CatalogEntry> taxes,
    List<CatalogEntry> withholdingTaxes,
    List<CatalogEntry> paymentForms,
    List<CatalogEntry> paymentMethods,
    List<CatalogEntry> creditNoteConcepts,
    List<CatalogEntry> productStandards,
    List<CatalogEntry> documentTypes,
    List<CatalogEntry> operationTypes,
    List<CatalogEntry> creditNoteOperationTypes,
    List<CatalogEntry> allowanceChargeConcepts,
    List<UnitMeasureEntry> unitMeasures,
    List<String> saleTypes,
    List<String> adjustmentReasons
) {}
