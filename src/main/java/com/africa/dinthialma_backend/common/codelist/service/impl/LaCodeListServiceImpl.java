package com.africa.dinthialma_backend.common.codelist.service.impl;

import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.africa.dinthialma_backend.common.codelist.repository.LaCodeListRepository;
import com.africa.dinthialma_backend.common.codelist.service.interfaces.LaCodeListService;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
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

  @Override
  @Transactional(readOnly = true)
  public List<LaCodeList> findAllByType(String type) {
    return codeListRepository.findAllByType(type);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LaCodeList> findAll(Pageable pageable) {
    return codeListRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public LaCodeList findById(UUID id) throws CustomException {
    return codeListRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Code list introuvable avec l'id : " + id));
  }

  @Override
  @Transactional
  public LaCodeList createCodeList(LaCodeList codeList) throws CustomException {
    if (codeListRepository.existsByTypeAndValue(codeList.getType(), codeList.getValue())) {
      throw new ConflictException(
          "Un code list avec le type '"
              + codeList.getType()
              + "' et la valeur '"
              + codeList.getValue()
              + "' existe déjà");
    }
    try {
      return codeListRepository.save(codeList);
    } catch (DataIntegrityViolationException ex) {
      log.error("Conflit d'unicité lors de la création du code list : {}", ex.getMessage());
      throw new ConflictException("Un code list avec ce type et cette valeur existe déjà");
    } catch (Exception ex) {
      log.error("Erreur lors de la création du code list : {}", ex.getMessage());
      throw new CustomException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
          "Erreur serveur lors de la création du code list");
    }
  }

  @Override
  @Transactional
  public LaCodeList updateCodeList(LaCodeList codeList, UUID id) throws CustomException {
    // Vérifie que le code list existe
    LaCodeList existing = findById(id);

    // Vérifie l'unicité du nouveau couple type/value si changé
    if (!existing.getType().equals(codeList.getType())
        || !existing.getValue().equals(codeList.getValue())) {
      if (codeListRepository.existsByTypeAndValue(codeList.getType(), codeList.getValue())) {
        throw new ConflictException(
            "Un code list avec le type '"
                + codeList.getType()
                + "' et la valeur '"
                + codeList.getValue()
                + "' existe déjà");
      }
    }

    existing.setType(codeList.getType());
    existing.setValue(codeList.getValue());
    existing.setDescription(codeList.getDescription());
    existing.setSystemAssign(codeList.isSystemAssign());

    try {
      return codeListRepository.save(existing);
    } catch (DataIntegrityViolationException ex) {
      log.error("Conflit d'unicité lors de la mise à jour du code list : {}", ex.getMessage());
      throw new ConflictException("Un code list avec ce type et cette valeur existe déjà");
    }
  }
}
