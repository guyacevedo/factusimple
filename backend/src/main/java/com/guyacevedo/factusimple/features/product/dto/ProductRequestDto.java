package com.guyacevedo.factusimple.features.product.dto;

import com.guyacevedo.factusimple.features.catalog.validation.ValidCatalogCode;
import com.guyacevedo.factusimple.features.catalog.Catalog;
import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Schema(description = "Payload para crear o actualizar un producto del establecimiento autenticado")
public record ProductRequestDto(

        @Schema(description = "UUID de una categoría activa del establecimiento. null deja el producto sin categoría.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Código interno. Único por establecimiento sin distinguir mayúsculas.",
                example = "BEB-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El SKU es requerido")
        @Size(max = 50, message = "El SKU no puede superar 50 caracteres")
        String sku,

        @Schema(description = "Código de barras. Único por establecimiento.", example = "7702004003508")
        @Pattern(regexp = "^\\S+$", message = "El código de barras no puede estar vacío ni tener espacios")
        @Size(max = 60, message = "El código de barras no puede superar 60 caracteres")
        String barcode,

        @Schema(description = "Nombre del producto.", example = "Gaseosa 400 ml", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del producto es requerido")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        String name,

        @Schema(description = "Descripción del producto.", example = "Gaseosa sabor cola, botella PET")
        String description,

        @Schema(description = "Precio de venta al detal.", example = "3500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio de venta es requerido")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite 10 enteros y 2 decimales")
        BigDecimal retailPrice,

        @Schema(description = "Precio mayorista.", example = "3000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio mayorista es requerido")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite 10 enteros y 2 decimales")
        BigDecimal wholesalePrice,

        @Schema(description = "Precio de costo. Si se omite: 0 al crear, sin cambios al actualizar.",
                example = "2200.00", defaultValue = "0")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite 10 enteros y 2 decimales")
        BigDecimal costPrice,

        @Schema(description = "Cantidad mínima para aplicar el precio mayorista. Si se omite: 1 al crear, sin cambios al actualizar.",
                example = "12", defaultValue = "1")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad mínima para mayorista debe ser mayor a 0")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite 9 enteros y 3 decimales")
        BigDecimal minStockWholesale,

        @Schema(description = "Stock mínimo para alertas. Si se omite: 0 al crear, sin cambios al actualizar.",
                example = "5", defaultValue = "0")
        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite 9 enteros y 3 decimales")
        BigDecimal minStock,

        @Schema(description = "Unidad de medida; debe corresponder al tipo de venta. Si se omite: UNIDAD al crear, sin cambios al actualizar.",
                example = "UNIDAD", defaultValue = "UNIDAD")
        UnitMeasurement unitMeasure,

        @Schema(description = "Código estándar del producto (catálogo DIAN).", example = "999")
        @ValidCatalogCode(Catalog.PRODUCT_STANDARD)
        String standardCode,

        @Schema(description = "Tipo de venta. Si se omite: BY_UNIT al crear, sin cambios al actualizar.",
                example = "BY_UNIT", defaultValue = "BY_UNIT")
        SaleType saleType,

        @Schema(description = "URL de la imagen del producto.", example = "https://cdn.ejemplo.com/gaseosa.png")
        @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
        String imageUrl,

        @Schema(description = "Si el producto está activo. Si se omite: true al crear, sin cambios al actualizar.",
                example = "true", defaultValue = "true")
        Boolean isActive,

        @Schema(description = "Stock inicial; queda registrado como ajuste RECEIPT. Solo se usa al crear: "
                + "al actualizar se ignora y el stock se cambia con /v1/inventory/adjustments.",
                example = "24", defaultValue = "0")
        @DecimalMin(value = "0", message = "El stock inicial no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite 9 enteros y 3 decimales")
        BigDecimal initialStock,

        @Schema(description = "Impuestos del producto. Al actualizar: si se omite se conservan, una lista (incluso vacía) los reemplaza.")
        List<@NotNull(message = "Hay un impuesto vacío") @Valid ProductTaxDto> taxes,

        @Schema(description = "Retenciones del producto. Al actualizar: si se omite se conservan, una lista (incluso vacía) las reemplaza.")
        List<@NotNull(message = "Hay una retención vacía") @Valid ProductWithholdingTaxDto> withholdingTaxes
) {

    @Schema(hidden = true)
    @AssertTrue(message = "Hay impuestos repetidos")
    public boolean isTaxCodesUnique() {
        return hasUniqueCodes(taxes, ProductTaxDto::taxCode);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "Hay retenciones repetidas")
    public boolean isWithholdingTaxCodesUnique() {
        return hasUniqueCodes(withholdingTaxes, ProductWithholdingTaxDto::withholdingTaxCode);
    }

    private static <T> boolean hasUniqueCodes(List<T> items, Function<T, String> code) {
        if (items == null) {
            return true;
        }
        List<String> codes = items.stream().filter(Objects::nonNull).map(code).filter(Objects::nonNull).toList();
        return codes.stream().distinct().count() == codes.size();
    }
}
