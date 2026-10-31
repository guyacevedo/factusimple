package com.guyacevedo.factusimple.features.customer.mapper;

import com.guyacevedo.factusimple.features.customer.dto.CustomerRequestDto;
import com.guyacevedo.factusimple.features.customer.dto.CustomerResponseDto;
import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import com.guyacevedo.factusimple.shared.mapper.IgnoreAuditFields;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Same "active" vs "isActive" mapping as PlanMapper. The owner (user) is set
 * by the service, and creditBalance only moves with credit sales, never from a request.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper extends BaseMapper<Customer, CustomerResponseDto> {

    @Override
    @Mapping(target = "isActive", source = "active")
    CustomerResponseDto toDto(Customer customer);

    // A null from the request would overwrite the @Builder.Default and break NOT NULL.
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "creditBalance", ignore = true)
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    @Mapping(target = "creditLimit", source = "creditLimit", defaultValue = "0")
    Customer toEntity(CustomerRequestDto dto);

    // Omitted isActive / creditLimit keep their current value; the optional texts can be cleared with null.
    @IgnoreAuditFields
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "creditBalance", ignore = true)
    @Mapping(target = "active", source = "isActive",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "creditLimit", source = "creditLimit",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CustomerRequestDto dto, @MappingTarget Customer customer);
}
