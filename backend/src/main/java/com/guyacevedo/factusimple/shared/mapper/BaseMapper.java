package com.guyacevedo.factusimple.shared.mapper;

import java.util.List;

/**
 * Entity -> response DTO.
 *
 * There is no generic toEntity(D): a response DTO never carries what an entity
 * needs (password, plan, audit fields). Each mapper declares its own
 * toEntity(XRequestDto) and updateEntity(XRequestDto, @MappingTarget X).
 */
public interface BaseMapper<E, D> {

    D toDto(E entity);

    List<D> toDtoList(List<E> entities);
}