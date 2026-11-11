package com.africa.dinthialma_backend.common.codelist.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.common.codelist.dto.LaCodeListDto;
import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.africa.dinthialma_backend.common.codelist.repository.LaCodeListRepository;
import com.africa.dinthialma_backend.common.codelist.service.interfaces.LaCodeListService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.security.AccessControl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LaCodeListServiceImpl implements LaCodeListService {

  private final LaCodeListRepository codeListRepository;
  private final AccessControl accessControl;

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<LaCodeListDto> findPublicByType(String type) throws CustomException {
    if (TYPE_RESTREINT.equals(type)) {
      throw new ForbiddenException(
          "Le type '"
              + type
              + "' est restreint. Utiliser GET /v1/code-list/admin/type/"
              + type
              + " (ADMIN requis).");
    }
    return actifsParType(type);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LaCodeListDto> findByTypeAdmin(String keycloakId, String type)
      throws CustomException {
    accessControl.exigerUnRole(keycloakId, UserRole.SUPER_ADMIN, UserRole.ADMIN);
    return actifsParType(type);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LaCodeListDto> findAll(Pageable pageable) {
    return codeListRepository.findByDeletedAtIsNull(pageable).map(LaCodeListDto::from);
  }

  @Override
  @Transactional(readOnly = true)
  public LaCodeListDto findById(UUID id) throws CustomException {
    return LaCodeListDto.from(trouverActif(id));
  }

  // ─── Écriture ────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public LaCodeListDto create(String keycloakId, LaCodeListDto dto) throws CustomException {
    accessControl.exigerUnRole(keycloakId, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    LaCodeList codeList = dto.toEntity();
    log.info("Création code list type={} value={}", codeList.getType(), codeList.getValue());

    // Un couple type/value supprimé (soft delete) est restauré
    var supprimee =
        codeListRepository
            .findByTypeAndValue(codeList.getType(), codeList.getValue())
            .filter(c -> c.getDeletedAt() != null);
    if (supprimee.isPresent()) {
      LaCodeList restauree = supprimee.get();
      restauree.setDeletedAt(null);
      restauree.setDescription(codeList.getDescription());
      restauree.setSystemAssign(codeList.isSystemAssign());
      log.info("Code list restauré – {} / {}", restauree.getType(), restauree.getValue());
      return LaCodeListDto.from(codeListRepository.save(restauree));
    }
    if (codeListRepository.existsByTypeAndValue(codeList.getType(), codeList.getValue())) {
      throw doublon(codeList);
    }
    try {
      return LaCodeListDto.from(codeListRepository.save(codeList));
    } catch (DataIntegrityViolationException ex) {
      log.error("Conflit d'unicité lors de la création du code list : {}", ex.getMessage());
      throw new ConflictException("Un code list avec ce type et cette valeur existe déjà");
    }
  }

  @Override
  @Transactional
  public LaCodeListDto update(String keycloakId, UUID id, LaCodeListDto dto)
      throws CustomException {
    accessControl.exigerUnRole(keycloakId, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    log.info("Mise à jour code list id : {}", id);
    LaCodeList existing = trouverActif(id);
    LaCodeList modifs = dto.toEntity();

    boolean coupleModifie =
        !existing.getType().equals(modifs.getType())
            || !existing.getValue().equals(modifs.getValue());
    if (coupleModifie
        && codeListRepository.existsByTypeAndValue(modifs.getType(), modifs.getValue())) {
      throw doublon(modifs);
    }

    existing.setType(modifs.getType());
    existing.setValue(modifs.getValue());
    existing.setDescription(modifs.getDescription());
    existing.setSystemAssign(modifs.isSystemAssign());
    try {
      return LaCodeListDto.from(codeListRepository.save(existing));
    } catch (DataIntegrityViolationException ex) {
      log.error("Conflit d'unicité lors de la mise à jour du code list : {}", ex.getMessage());
      throw new ConflictException("Un code list avec ce type et cette valeur existe déjà");
    }
  }

  @Override
  @Transactional
  public void delete(String keycloakId, UUID id) throws CustomException {
    accessControl.exigerUnRole(keycloakId, UserRole.SUPER_ADMIN);
    LaCodeList existing = trouverActif(id);
    if (existing.isSystemAssign()) {
      throw new BadRequestException(
          "Les valeurs système ne peuvent pas être supprimées", ErrorCode.CODE_LIST_SYSTEM_VALUE);
    }
    existing.setDeletedAt(LocalDateTime.now());
    codeListRepository.save(existing);
    log.info(
        "Code list supprimé (soft delete) – id={} {} / {}",
        id,
        existing.getType(),
        existing.getValue());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private List<LaCodeListDto> actifsParType(String type) {
    return codeListRepository.findAllByTypeAndDeletedAtIsNull(type).stream()
        .map(LaCodeListDto::from)
        .toList();
  }

  private LaCodeList trouverActif(UUID id) throws CustomException {
    return codeListRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("Code list introuvable avec l'id : " + id));
  }

  private static ConflictException doublon(LaCodeList c) {
    return new ConflictException(
        "Un code list avec le type '"
            + c.getType()
            + "' et la valeur '"
            + c.getValue()
            + "' existe déjà");
  }
}
