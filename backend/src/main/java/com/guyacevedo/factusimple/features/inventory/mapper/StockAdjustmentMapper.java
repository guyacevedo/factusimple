package com.guyacevedo.factusimple.features.inventory.mapper;

import com.guyacevedo.factusimple.features.inventory.dto.StockAdjustmentResponseDto;
import com.guyacevedo.factusimple.features.inventory.entity.StockAdjustment;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity -> response only: the service builds adjustments from the locked stock row. */
@Mapper(componentModel = "spring")
public interface StockAdjustmentMapper extends BaseMapper<StockAdjustment, StockAdjustmentResponseDto> {

    @Override
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "sku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    StockAdjustmentResponseDto toDto(StockAdjustment adjustment);
}
