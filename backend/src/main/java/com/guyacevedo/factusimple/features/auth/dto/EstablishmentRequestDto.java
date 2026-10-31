package com.guyacevedo.factusimple.features.auth.dto;

import com.guyacevedo.factusimple.features.catalog.validation.ValidMunicipality;
import com.guyacevedo.factusimple.features.catalog.Catalog;
import com.guyacevedo.factusimple.features.catalog.validation.ValidCatalogCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fiscal data of the establishment. There is no establishments table: these
 * fields are stored in users (company_name, nit, dv, ...), one establishment per user.
 */
@Schema(description = "Datos fiscales del establecimiento comercial que se registra")
public record EstablishmentRequestDto(

        @Schema(description = "Razón social o nombre del establecimiento.", example = "Comercializadora Neymar",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La razón social es requerida")
        @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
        String companyName,

        @Schema(description = "NIT sin puntos, guiones ni dígito de verificación.", example = "900123456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El NIT es requerido")
        @Pattern(regexp = "^[0-9]{5,15}$", message = "El NIT debe tener solo dígitos, sin puntos ni dígito de verificación")
        String nit,

        @Schema(description = "Dígito de verificación del NIT.", example = "7")
        @Pattern(regexp = "^[0-9]$", message = "El dígito de verificación debe ser un solo número")
        String dv,

        @Schema(description = "Tipo de organización: 1 = persona jurídica, 2 = persona natural.", example = "1")
        @ValidCatalogCode(Catalog.LEGAL_ORGANIZATION)
        String legalOrgCode,

        @Schema(description = "Código del tributo (catálogo DIAN).", example = "01")
        @ValidCatalogCode(Catalog.TRIBUTE)
        String tributeCode,

        @Schema(description = "Responsabilidad fiscal (catálogo DIAN).", example = "R-99-PN")
        @ValidCatalogCode(Catalog.FISCAL_RESPONSIBILITY)
        String fiscalResponsibility,

        @Schema(description = "Dirección del establecimiento.", example = "Calle 10 # 5-20, Magangué")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String address,

        @Schema(description = "Código DANE del municipio. Consultar GET /v1/catalogs/municipalities.", example = "13430")
        @ValidMunicipality
        String municipalityCode
) {}