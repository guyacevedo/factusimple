package com.guyacevedo.factusimple.features.plan.mapper;

import com.guyacevedo.factusimple.features.plan.dto.PlanRequestDto;
import com.guyacevedo.factusimple.features.plan.dto.PlanResponseDto;
import com.guyacevedo.factusimple.features.plan.entity.Plan;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import com.guyacevedo.factusimple.shared.mapper.IgnoreAuditFields;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * The entity field is "active" (getActive()) while the DTOs use "isActive",
 * so the names have to be mapped by hand or MapStruct leaves them null.
 */
@Mapper(componentModel = "spring")
public interface PlanMapper extends BaseMapper<Plan, PlanResponseDto> {

    @Override
    @Mapping(target = "isActive", source = "active")
    PlanResponseDto toDto(Plan plan);

    // A null from the request would overwrite the @Builder.Default and break NOT NULL.
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    @Mapping(target = "requiredInvitedCode", source = "requiredInvitedCode", defaultValue = "false")
    Plan toEntity(PlanRequestDto dto);

    // Omitted booleans keep their current value; description and code can be cleared with null.
    @IgnoreAuditFields
    @Mapping(target = "active", source = "isActive",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "requiredInvitedCode", source = "requiredInvitedCode",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PlanRequestDto dto, @MappingTarget Plan plan);
}