package com.guyacevedo.factusimple.features.user.mapper;

import com.guyacevedo.factusimple.features.user.dto.UserResponseDto;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity -> response only. Users are created and changed through their own
 * requests (registration, change plan, change password), never from a
 * response DTO, so there is no toEntity here. toDtoList reuses this toDto.
 */
@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserResponseDto> {

    @Override
    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "planName", source = "plan.name")
    @Mapping(target = "maxProducts", source = "plan.maxProducts")
    @Mapping(target = "maxCustomers", source = "plan.maxCustomers")
    @Mapping(target = "maxInvoices", source = "plan.maxInvoices")
    @Mapping(target = "isActive", source = "active")
    UserResponseDto toDto(User user);
}