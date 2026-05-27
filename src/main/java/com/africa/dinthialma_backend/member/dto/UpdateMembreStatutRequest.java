package com.africa.dinthialma_backend.member.dto;

import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour modifier le statut d'un cotisant. */
@Getter
@Setter
public class UpdateMembreStatutRequest {

  /**
   * Nouveau statut du cotisant.
   *
   * <ul>
   *   <li>{@code ACTIF} – activer ou réactiver un membre suspendu
   *   <li>{@code SUSPENDU} – suspendre temporairement
   *   <li>{@code SORTI} – retrait définitif (irréversible sans intervention admin)
   * </ul>
   */
  @NotNull private MembreStatut statut;

  /** Motif optionnel de la modification (affiché à l'historique). */
  private String motif;
}
