package com.africa.dinthialma_backend.adhesion.service.interfaces;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.dto.DemandeAdhesionResponse;
import com.africa.dinthialma_backend.adhesion.dto.DemandeRequests;
import com.africa.dinthialma_backend.adhesion.dto.InvitationApercuResponse;
import com.africa.dinthialma_backend.adhesion.dto.InvitationResponse;
import com.africa.dinthialma_backend.common.exception.CustomException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Invitations par code (B16) et demandes d'adhésion (B17). */
public interface AdhesionService {

  /**
   * Retourne l'invitation active de la tontine, en créant le code s'il n'existe pas (ou en le
   * régénérant si demandé). Créateur ou SUPER_ADMIN ; tontine BROUILLON ou ACTIVE.
   */
  InvitationResponse obtenirInvitation(String keycloakId, UUID tontineId, boolean regenerer)
      throws CustomException;

  /** Révoque l'invitation : le code et le QR ne fonctionnent plus. */
  void revoquerInvitation(String keycloakId, UUID tontineId) throws CustomException;

  /** Aperçu public (sans authentification) d'une tontine par son code. 404 si inconnu. */
  /**
   * Aperçu public d'une invitation ; {@code keycloakId} facultatif (null = anonyme) enrichit la
   * réponse de la relation de l'appelant à la tontine.
   */
  InvitationApercuResponse apercu(String code, String keycloakId) throws CustomException;

  /** Aperçu anonyme. */
  default InvitationApercuResponse apercu(String code) throws CustomException {
    return apercu(code, null);
  }

  /** Demande à rejoindre la tontine du code. Notifie le gestionnaire. */
  DemandeAdhesionResponse creerDemande(
      String keycloakId, String code, DemandeRequests.Creer request) throws CustomException;

  /** Demandes reçues sur mes tontines gérées. */
  Page<DemandeAdhesionResponse> demandesRecues(
      String keycloakId, StatutDemande statut, UUID tontineId, Pageable pageable)
      throws CustomException;

  /** Mes demandes envoyées. */
  Page<DemandeAdhesionResponse> mesDemandes(String keycloakId, Pageable pageable)
      throws CustomException;

  /** Accepte : ajoute le demandeur comme membre (mêmes règles que l'ajout manuel). */
  DemandeAdhesionResponse accepter(
      String keycloakId, UUID tontineId, UUID demandeId, DemandeRequests.Accepter request)
      throws CustomException;

  /** Refuse la demande (motif optionnel) et notifie le demandeur. */
  DemandeAdhesionResponse refuser(
      String keycloakId, UUID tontineId, UUID demandeId, DemandeRequests.Refuser request)
      throws CustomException;
}
