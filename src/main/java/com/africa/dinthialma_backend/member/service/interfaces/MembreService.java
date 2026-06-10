package com.africa.dinthialma_backend.member.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.dto.UpdateMembreStatutRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service métier pour la gestion des cotisants d'une tontine. */
public interface MembreService {

  /**
   * Ajoute un cotisant à une tontine.
   *
   * <p>L'utilisateur ajouté se voit assigner le rôle {@code DINTHIALMA_MEMBER} s'il ne l'a pas.
   * Réservé au créateur de la tontine et au SUPER_ADMIN.
   *
   * @param keycloakId sub JWT de l'admin appelant
   * @param tontineId identifiant de la tontine
   * @param request données du membre à ajouter
   */
  MembreResponse addMembre(String keycloakId, UUID tontineId, AddMembreRequest request)
      throws CustomException;

  /**
   * Retire (soft delete) un cotisant d'une tontine.
   *
   * <p>Réservé au créateur et au SUPER_ADMIN. Interdit si la tontine est {@code ACTIVE} et que le
   * membre a des cotisations validées.
   */
  void removeMembre(String keycloakId, UUID tontineId, UUID membreId) throws CustomException;

  /**
   * Liste les cotisants d'une tontine.
   *
   * <p>Accès : membres de la tontine + admin + SUPER_ADMIN.
   */
  Page<MembreResponse> listMembres(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException;

  /**
   * Modifie le statut d'un cotisant ({@code ACTIF} / {@code SUSPENDU} / {@code SORTI}).
   *
   * <p>Réservé au créateur et au SUPER_ADMIN.
   */
  MembreResponse updateStatut(
      String keycloakId, UUID tontineId, UUID membreId, UpdateMembreStatutRequest request)
      throws CustomException;
}
