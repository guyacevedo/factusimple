package com.guyacevedo.factusimple.features.customer.dto;

import com.guyacevedo.factusimple.features.catalog.validation.ValidMunicipality;
import com.guyacevedo.factusimple.features.catalog.Catalog;
import com.guyacevedo.factusimple.features.catalog.validation.ValidCatalogCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload para crear o actualizar un cliente del establecimiento autenticado")
public record CustomerRequestDto(

        @Schema(description = "Tipo de identificación (catálogo DIAN): 13 = CC, 31 = NIT, etc.",
                example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El tipo de identificación es requerido")
        @ValidCatalogCode(Catalog.IDENTIFICATION_TYPE)
        String idTypeCode,

        @Schema(description = "Número de identificación sin puntos ni dígito de verificación. Único por establecimiento.",
                example = "1045123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La identificación es requerida")
        @Size(max = 30, message = "La identificación no puede superar 30 caracteres")
        String identification,

        @Schema(description = "Dígito de verificación (solo para NIT).", example = "7")
        @Pattern(regexp = "^[0-9]$", message = "El dígito de verificación debe ser un solo número")
        String dv,

        @Schema(description = "Tipo de organización: 1 = persona jurídica, 2 = persona natural.", example = "2")
        @ValidCatalogCode(Catalog.LEGAL_ORGANIZATION)
        String legalOrgCode,

        @Schema(description = "Razón social (persona jurídica).", example = "Distribuciones El Sol S.A.S.")
        @Size(max = 255, message = "La razón social no puede superar 255 caracteres")
        String company,

        @Schema(description = "Nombres y apellidos (persona natural).", example = "María Pérez")
        @Size(max = 255, message = "Los nombres no pueden superar 255 caracteres")
        String names,

        @Schema(description = "Nombre comercial.", example = "Tienda El Sol")
        @Size(max = 255, message = "El nombre comercial no puede superar 255 caracteres")
        String tradeName,

        @Schema(description = "Dirección del cliente.", example = "Calle 10 # 5-20, Magangué")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String address,

        @Schema(description = "Correo al que se envía la factura electrónica.", example = "cliente@correo.com")
        @Email(message = "El correo no es válido")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres")
        String email,

        @Schema(description = "Teléfono del cliente.", example = "3001234567")
        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String phone,

        @Schema(description = "Código DANE del municipio. Consultar GET /v1/catalogs/municipalities.", example = "13430")
        @ValidMunicipality
        String municipalityCode,

        @Schema(description = "Código del tributo (catálogo DIAN).", example = "ZZ")
        @ValidCatalogCode(Catalog.TRIBUTE)
        String tributeCode,

        @Schema(description = "Responsabilidad fiscal (catálogo DIAN).", example = "R-99-PN")
        @ValidCatalogCode(Catalog.FISCAL_RESPONSIBILITY)
        String fiscalResponsibility,

        @Schema(description = "Cupo de crédito. Si se omite: 0 al crear, sin cambios al actualizar.",
                example = "500000.00", defaultValue = "0")
        @DecimalMin(value = "0", message = "El cupo de crédito no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "El cupo de crédito admite 13 enteros y 2 decimales")
        BigDecimal creditLimit,

        @Schema(description = "Notas internas sobre el cliente.", example = "Paga los viernes")
        String notes,

        @Schema(description = "Si el cliente está activo. Si se omite: true al crear, sin cambios al actualizar.",
                example = "true", defaultValue = "true")
        Boolean isActive
) {

    @Schema(hidden = true)
    @AssertTrue(message = "Indica la razón social o los nombres del cliente")
    public boolean isNameProvided() {
        return (company != null && !company.isBlank()) || (names != null && !names.isBlank());
    }

    /** Same rule as Factus: a legal entity (1) is invoiced by its company name. */
    @Schema(hidden = true)
    @AssertTrue(message = "La razón social es requerida para una persona jurídica")
    public boolean isCompanyProvidedForLegalEntity() {
        return !"1".equals(legalOrgCode) || (company != null && !company.isBlank());
    }

    /** Same rule as Factus: a natural person (2) is invoiced by their names. */
    @Schema(hidden = true)
    @AssertTrue(message = "Los nombres son requeridos para una persona natural")
    public boolean isNamesProvidedForNaturalPerson() {
        return !"2".equals(legalOrgCode) || (names != null && !names.isBlank());
    }
}
