package com.africa.dinthialma_backend.tontine.mapper;

import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions des commissions. */
@Mapper
public interface CommissionMapper {

  @Mapping(target = "tontineId", source = "tontine.id")
  CommissionResponse toResponse(TontineCommission commission);
}
