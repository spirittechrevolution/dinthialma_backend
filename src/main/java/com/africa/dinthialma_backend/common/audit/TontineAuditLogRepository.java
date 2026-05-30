package com.africa.dinthialma_backend.common.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Accès base pour le journal d'audit des tontines. */
@Repository
public interface TontineAuditLogRepository extends JpaRepository<TontineAuditLog, UUID> {}
