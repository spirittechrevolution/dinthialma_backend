package com.africa.dinthialma_backend.contribution.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service métier pour la gestion des cotisations. */
public interface CotisationService {

  /**
   * Enregistre une cotisation (paiement signalé par le membre).
   *
   * <p>Statut initial : {@code EN_ATTENTE}. Une seule cotisation par membre par cycle.
   *
   * @param keycloakId sub JWT du membre appelant
   * @param tontineId identifiant de la tontine
   * @param request données du paiement
   */
  CotisationResponse recordCotisation(
      String keycloakId, UUID tontineId, RecordCotisationRequest request) throws CustomException;

  /**
   * Valide une cotisation ({@code EN_ATTENTE} → {@code VALIDE}).
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN. Remplit {@code valide_par} et {@code
   * date_validation}.
   *
   * @param keycloakId sub JWT de l'admin valideur
   * @param tontineId identifiant de la tontine
   * @param cotisationId identifiant de la cotisation à valider
   */
  CotisationResponse validateCotisation(String keycloakId, UUID tontineId, UUID cotisationId)
      throws CustomException;

  /**
   * Liste les cotisations d'une tontine, avec filtre optionnel par cycle.
   *
   * <p>Accès : SUPER_ADMIN et admin voient tout ; membre voit seulement ses propres cotisations.
   *
   * @param keycloakId sub JWT
   * @param tontineId identifiant de la tontine
   * @param cycleId filtre optionnel (null = tous les cycles)
   */
  Page<CotisationResponse> listCotisations(
      String keycloakId, UUID tontineId, UUID cycleId, Pageable pageable) throws CustomException;

  /**
   * Récupère une cotisation par son identifiant.
   *
   * <p>Accès : propriétaire de la cotisation, admin de la tontine, SUPER_ADMIN.
   */
  CotisationResponse getCotisation(String keycloakId, UUID tontineId, UUID cotisationId)
      throws CustomException;
}
