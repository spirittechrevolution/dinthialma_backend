package com.africa.dinthialma_backend.common.audit;

import com.africa.dinthialma_backend.auth.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du journal d'audit – s'exécute dans la transaction courante. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

  private final TontineAuditLogRepository auditRepository;

  @Override
  public void log(
      User caller,
      String tableName,
      UUID recordId,
      String champ,
      String ancienneVal,
      String nouvelleVal) {

    TontineAuditLog entry =
        TontineAuditLog.builder()
            .tableName(tableName)
            .recordId(recordId)
            .action(AuditAction.UPDATE)
            .champ(champ)
            .ancienneVal(ancienneVal)
            .nouvelleVal(nouvelleVal)
            .faitPar(caller)
            .build();

    auditRepository.save(entry);
    log.debug("Audit UPDATE – {}.{} : {} → {}", tableName, champ, ancienneVal, nouvelleVal);
  }

  @Override
  public void logCreate(User caller, String tableName, UUID recordId) {
    TontineAuditLog entry =
        TontineAuditLog.builder()
            .tableName(tableName)
            .recordId(recordId)
            .action(AuditAction.CREATE)
            .faitPar(caller)
            .build();

    auditRepository.save(entry);
    log.debug("Audit CREATE – {} id={}", tableName, recordId);
  }

  @Override
  public void logDelete(User caller, String tableName, UUID recordId) {
    TontineAuditLog entry =
        TontineAuditLog.builder()
            .tableName(tableName)
            .recordId(recordId)
            .action(AuditAction.DELETE)
            .faitPar(caller)
            .build();

    auditRepository.save(entry);
    log.debug("Audit DELETE – {} id={}", tableName, recordId);
  }
}
