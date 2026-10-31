package com.guyacevedo.factusimple.features.catalog.controller;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Parameter;
import com.guyacevedo.factusimple.features.catalog.dto.MunicipalityDto;
import com.guyacevedo.factusimple.features.catalog.dto.DepartmentDto;
import com.guyacevedo.factusimple.features.catalog.MunicipalityCatalog;
import com.guyacevedo.factusimple.features.catalog.Catalog;
import com.guyacevedo.factusimple.features.catalog.dto.CatalogsResponseDto;
import com.guyacevedo.factusimple.features.catalog.dto.UnitMeasureEntry;
import com.guyacevedo.factusimple.features.inventory.entity.AdjustmentReason;
import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/catalogs")
@Tag(name = "Catalogs", description = """
        Catálogos de códigos DIAN que acepta la API, para llenar los selects del frontend.

        **Público:** el formulario de registro los necesita antes de iniciar sesión.
        Los campos de código de clientes, establecimiento, impuestos y pagos se validan contra estas listas.
        Los municipios (códigos DANE) van aparte, en `/v1/catalogs/municipalities`, porque son 1122.
        """)
@RequiredArgsConstructor
public class CatalogController {

    private final MunicipalityCatalog municipalityCatalog;

    // Static data: built once.
    private static final CatalogsResponseDto CATALOGS = new CatalogsResponseDto(
            Catalog.IDENTIFICATION_TYPE.entries(),
            Catalog.LEGAL_ORGANIZATION.entries(),
            Catalog.TRIBUTE.entries(),
            Catalog.FISCAL_RESPONSIBILITY.entries(),
            Catalog.TAX.entries(),
            Catalog.WITHHOLDING_TAX.entries(),
            Catalog.PAYMENT_FORM.entries(),
            Catalog.PAYMENT_METHOD.entries(),
            Catalog.CREDIT_NOTE_CONCEPT.entries(),
            Catalog.PRODUCT_STANDARD.entries(),
            Catalog.INVOICE_DOCUMENT_TYPE.entries(),
            Catalog.INVOICE_OPERATION_TYPE.entries(),
            Catalog.CREDIT_NOTE_OPERATION_TYPE.entries(),
            Catalog.ALLOWANCE_CHARGE_CONCEPT.entries(),
            Arrays.stream(UnitMeasurement.values())
                    .map(unit -> new UnitMeasureEntry(unit.name(), unit.code(), unit.label(), unit.abbreviation(), unit.saleType()))
                    .toList(),
            Arrays.stream(SaleType.values()).map(Enum::name).toList(),
            Arrays.stream(AdjustmentReason.values()).map(Enum::name).toList());

    @Operation(summary = "Obtener catálogos (público)",
            description = "Todas las listas de códigos en una sola respuesta; son estáticas y se pueden cachear en el frontend.")
    @ApiResponse(responseCode = "200", description = "Catálogos")
    @GetMapping
    public ResponseEntity<ApiResponseDto<CatalogsResponseDto>> getCatalogs() {
        return ResponseEntity.ok(ApiResponseDto.success("Catálogos obtenidos", CATALOGS));
    }

    @Operation(summary = "Listar municipios (público)",
            description = "Municipios de Colombia con su código DANE, ordenados por departamento y nombre. "
                    + "Con `department` (código DANE de 2 dígitos) filtra por departamento.")
    @ApiResponse(responseCode = "200", description = "Municipios")
    @GetMapping("/municipalities")
    public ResponseEntity<ApiResponseDto<List<MunicipalityDto>>> getMunicipalities(
            @Parameter(description = "Código DANE del departamento", example = "13")
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(ApiResponseDto.success("Municipios obtenidos",
                municipalityCatalog.municipalities(department)));
    }

    @Operation(summary = "Listar departamentos (público)",
            description = "Departamentos de Colombia con su código DANE, para el primer select del municipio.")
    @ApiResponse(responseCode = "200", description = "Departamentos")
    @GetMapping("/departments")
    public ResponseEntity<ApiResponseDto<List<DepartmentDto>>> getDepartments() {
        return ResponseEntity.ok(ApiResponseDto.success("Departamentos obtenidos", municipalityCatalog.departments()));
    }
}
