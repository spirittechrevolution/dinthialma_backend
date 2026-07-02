package com.africa.dinthialma_backend.contribution.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.contribution.dto.AdminRecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.CycleRecapCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.MembreTotalCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.UpdateCotisationRequest;
import java.util.List;
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
   * Enregistre et valide une cotisation en une seule opération (réservé admin/SUPER_ADMIN).
   *
   * <p>Cas d'usage : paiement cash encaissé par l'admin, ou membre PRE_ENROLLED sans accès app. La
   * cotisation est créée directement en statut {@code VALIDE} – pas de passage par EN_ATTENTE. Une
   * notification WhatsApp est envoyée au membre si son compte est ACTIVE.
   *
   * @param keycloakId sub JWT de l'admin appelant
   * @param tontineId identifiant de la tontine
   * @param request données du paiement incluant le membreId
   */
  CotisationResponse adminRecordCotisation(
      String keycloakId, UUID tontineId, AdminRecordCotisationRequest request)
      throws CustomException;

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
   * Liste les cotisations d'une tontine, avec filtres optionnels par cycle et par membre.
   *
   * <p>Accès : SUPER_ADMIN et admin voient tout ; membre voit seulement ses propres cotisations. Le
   * filtre {@code membreId} n'est appliqué que pour les admin/SUPER_ADMIN.
   *
   * @param keycloakId sub JWT
   * @param tontineId identifiant de la tontine
   * @param cycleId filtre optionnel (null = tous les cycles)
   * @param membreId filtre optionnel admin (null = tous les membres)
   */
  Page<CotisationResponse> listCotisations(
      String keycloakId, UUID tontineId, UUID cycleId, UUID membreId, Pageable pageable)
      throws CustomException;

  /**
   * Récupère une cotisation par son identifiant.
   *
   * <p>Accès : propriétaire de la cotisation, admin de la tontine, SUPER_ADMIN.
   */
  CotisationResponse getCotisation(String keycloakId, UUID tontineId, UUID cotisationId)
      throws CustomException;

  /**
   * Modifie une cotisation existante (sémantique PATCH – seuls les champs non-null sont mis à
   * jour).
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   *
   * <p>Conditions d'édition :
   *
   * <ul>
   *   <li>Statut {@code EN_ATTENTE} → modifiable sans restriction.
   *   <li>Statut {@code VALIDE} + cycle {@code EN_COURS} → modifiable, reste VALIDE.
   *   <li>Statut {@code VALIDE} + cycle {@code TERMINE} ou {@code EN_RETARD} → refus 400.
   * </ul>
   *
   * @param keycloakId sub JWT de l'admin appelant
   * @param tontineId identifiant de la tontine
   * @param cotisationId identifiant de la cotisation à modifier
   * @param request champs à mettre à jour (null = inchangé)
   */
  CotisationResponse updateCotisation(
      String keycloakId, UUID tontineId, UUID cotisationId, UpdateCotisationRequest request)
      throws CustomException;

  /**
   * Retourne le récapitulatif des cotisations d'un cycle par membre.
   *
   * <p>Un enregistrement par membre actif/suspendu de la tontine. {@code statutCotisation} est
   * {@code null} quand le membre n'a soumis aucune cotisation pour ce cycle.
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   *
   * @param keycloakId sub JWT de l'admin appelant
   * @param tontineId identifiant de la tontine
   * @param cycleId identifiant du cycle
   */
  List<CycleRecapCotisationResponse> getCycleRecap(String keycloakId, UUID tontineId, UUID cycleId)
      throws CustomException;

  /**
   * Retourne le total cumulé cotisé (VALIDÉ) par membre, tous cycles confondus, pour une tontine.
   *
   * <p>Un enregistrement par membre actif/suspendu de la tontine. Les membres sans cotisation
   * validée apparaissent avec {@code totalCotise = 0}. Les membres SORTI sont exclus.
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   *
   * @param keycloakId sub JWT de l'admin appelant
   * @param tontineId identifiant de la tontine
   */
  List<MembreTotalCotisationResponse> getTontineRecapTotal(String keycloakId, UUID tontineId)
      throws CustomException;
}
