package com.africa.dinthialma_backend.common.codelist.mapper;

import com.africa.dinthialma_backend.common.codelist.dto.LaCodeListDto;
import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions entité ↔ DTO du référentiel de valeurs. */
@Mapper
public interface LaCodeListMapper {

  @Mapping(target = "isSystemAssign", source = "systemAssign")
  LaCodeListDto toDto(LaCodeList entity);

  /** Nouvelle entité (sans id ni dates, gérés par la base). */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "isSystemAssign", source = "systemAssign")
  LaCodeList toEntity(LaCodeListDto dto);
}
