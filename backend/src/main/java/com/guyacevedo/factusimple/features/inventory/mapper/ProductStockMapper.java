package com.guyacevedo.factusimple.features.inventory.mapper;

import com.guyacevedo.factusimple.features.inventory.dto.StockResponseDto;
import com.guyacevedo.factusimple.features.inventory.entity.ProductStock;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity -> response only: stock changes through adjustments, never from a DTO. */
@Mapper(componentModel = "spring")
public interface ProductStockMapper extends BaseMapper<ProductStock, StockResponseDto> {

    @Override
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "sku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unitMeasure", source = "product.unitMeasure")
    @Mapping(target = "saleType", source = "product.saleType")
    @Mapping(target = "minStock", source = "product.minStock")
    @Mapping(target = "belowMinimum", expression = "java(stock.belowMinimumQuantity())")
    StockResponseDto toDto(ProductStock stock);
}
