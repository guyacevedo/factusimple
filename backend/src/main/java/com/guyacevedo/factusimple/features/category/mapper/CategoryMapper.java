package com.guyacevedo.factusimple.features.category.mapper;

import com.guyacevedo.factusimple.features.category.dto.CategoryRequestDto;
import com.guyacevedo.factusimple.features.category.dto.CategoryResponseDto;
import com.guyacevedo.factusimple.features.category.entity.Category;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import com.guyacevedo.factusimple.shared.mapper.IgnoreAuditFields;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Same "active" vs "isActive" mapping as PlanMapper. The owner (user) never
 * comes from the request: the service sets it from the authenticated user.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<Category, CategoryResponseDto> {

    @Override
    @Mapping(target = "isActive", source = "active")
    CategoryResponseDto toDto(Category category);

    // A null from the request would overwrite the @Builder.Default and break NOT NULL.
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
    Category toEntity(CategoryRequestDto dto);

    // Omitted isActive / sortOrder keep their current value; description can be cleared with null.
    @IgnoreAuditFields
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", source = "isActive",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sortOrder", source = "sortOrder",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CategoryRequestDto dto, @MappingTarget Category category);
}
