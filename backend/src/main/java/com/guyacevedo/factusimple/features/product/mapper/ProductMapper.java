package com.guyacevedo.factusimple.features.product.mapper;

import com.guyacevedo.factusimple.features.product.dto.ProductRequestDto;
import com.guyacevedo.factusimple.features.product.dto.ProductResponseDto;
import com.guyacevedo.factusimple.features.product.dto.ProductTaxDto;
import com.guyacevedo.factusimple.features.product.dto.ProductWithholdingTaxDto;
import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.features.product.entity.ProductTax;
import com.guyacevedo.factusimple.features.product.entity.ProductWithholdingTax;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import com.guyacevedo.factusimple.shared.mapper.IgnoreAuditFields;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Same "active" vs "isActive" mapping as PlanMapper. The owner, the category
 * and the tax lists are set by the service: the category has to be checked
 * against the owner, and the taxes need product.addTax() to link both sides.
 * initialStock is not a Product field; the service hands it to inventory.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductResponseDto> {

    @Override
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "isActive", source = "active")
    ProductResponseDto toDto(Product product);

    @Mapping(target = "isExcluded", source = "excluded")
    ProductTaxDto toTaxDto(ProductTax tax);

    ProductWithholdingTaxDto toWithholdingTaxDto(ProductWithholdingTax tax);

    // A null from the request would overwrite the @Builder.Default and break NOT NULL.
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "taxes", ignore = true)
    @Mapping(target = "withholdingTaxes", ignore = true)
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    @Mapping(target = "costPrice", source = "costPrice", defaultValue = "0")
    @Mapping(target = "minStockWholesale", source = "minStockWholesale", defaultValue = "1")
    @Mapping(target = "minStock", source = "minStock", defaultValue = "0")
    @Mapping(target = "unitMeasure", source = "unitMeasure", defaultValue = "UNIDAD")
    @Mapping(target = "saleType", source = "saleType", defaultValue = "BY_UNIT")
    Product toEntity(ProductRequestDto dto);

    // Omitted values with a default keep their current value; the optional texts can be cleared with null.
    @IgnoreAuditFields
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "taxes", ignore = true)
    @Mapping(target = "withholdingTaxes", ignore = true)
    @Mapping(target = "active", source = "isActive",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "costPrice", source = "costPrice",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "minStockWholesale", source = "minStockWholesale",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "minStock", source = "minStock",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "unitMeasure", source = "unitMeasure",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "saleType", source = "saleType",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ProductRequestDto dto, @MappingTarget Product product);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "excluded", source = "isExcluded", defaultValue = "false")
    ProductTax toTaxEntity(ProductTaxDto dto);

    @Mapping(target = "product", ignore = true)
    ProductWithholdingTax toWithholdingTaxEntity(ProductWithholdingTaxDto dto);
}
