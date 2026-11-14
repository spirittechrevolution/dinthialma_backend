package com.africa.dinthialma_backend.notification.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.notification.codeList.CanalRappel;
import com.africa.dinthialma_backend.notification.codeList.NatureRappel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Rappel d'échéance déjà envoyé (B26) : une ligne par échéance, destinataire, nature et canal. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "rappels_envoyes", schema = "dinthialma")
public class RappelEnvoye extends BaseEntity {

  @Column(name = "echeance_id", nullable = false)
  private UUID echeanceId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "nature", nullable = false, length = 30)
  private NatureRappel nature;

  @Enumerated(EnumType.STRING)
  @Column(name = "canal", nullable = false, length = 20)
  private CanalRappel canal;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
