package com.africa.dinthialma_backend.me.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.util.FichierExport;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.me.codeList.ModeActivite;
import com.africa.dinthialma_backend.me.codeList.TypeActivite;
import com.africa.dinthialma_backend.me.dto.ActiviteResponse;
import com.africa.dinthialma_backend.me.dto.MeSummaryResponse;
import com.africa.dinthialma_backend.me.dto.MesJackpotsResponse;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeRequest;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Vues transverses de l'utilisateur connecté, toutes tontines confondues ({@code /v1/me}). */
public interface MeService {

  /** Export CSV du fil d'activité (mêmes filtres que {@link #getActivite}, 10 000 lignes max). */
  FichierExport exportActivite(
      String keycloakId,
      String format,
      ModeActivite mode,
      UUID tontineId,
      TypeActivite type,
      CotisationStatut statut)
      throws CustomException;

  /**
   * Fil d'activité paginé (date décroissante) : cotisations et jackpots, soit les miens ({@code
   * COTISE}), soit ceux des tontines que je gère ({@code GERE}).
   */
  Page<ActiviteResponse> getActivite(
      String keycloakId,
      ModeActivite mode,
      UUID tontineId,
      TypeActivite type,
      CotisationStatut statut,
      Pageable pageable)
      throws CustomException;

  /**
   * Mes jackpots (ROTATIVE) : reçus, et estimation du prochain par tontine (rang, tour, date de fin
   * prévue, montant brut) quand l'ordre est une rotation fixe.
   */
  MesJackpotsResponse getJackpots(String keycloakId) throws CustomException;

  /**
   * Valide un lot de cotisations EN_ATTENTE des tontines gérées par l'appelant. Chaque cotisation
   * est traitée indépendamment (mêmes règles et notifications qu'une validation unitaire) ; le
   * résultat est rendu par identifiant.
   */
  ValidationGroupeeResponse validerCotisations(String keycloakId, ValidationGroupeeRequest request)
      throws CustomException;

  /**
   * Résumé « Je cotise » : totaux, jackpots reçus et cotisation due du tour en cours pour chaque
   * tontine où l'appelant est inscrit comme cotisant.
   */
  MeSummaryResponse getSummary(String keycloakId) throws CustomException;

  /**
   * Paiements de toutes les tontines dont l'appelant est le créateur (file « À valider »).
   *
   * <p>Une tontine qui n'est pas gérée par l'appelant ne renvoie rien, même si son id est passé en
   * filtre. Le SUPER_ADMIN ne voit ici que ses propres tontines (vue plateforme : {@code
   * /v1/admin/dashboard/contributions}).
   *
   * @param statuts filtre multi-valeurs (null ou vide = tous les statuts)
   * @param tontineId filtre optionnel sur une tontine gérée
   * @param q recherche : prénom, nom, téléphone du cotisant ou référence de transaction
   */
  Page<CotisationResponse> listCotisationsGerees(
      String keycloakId,
      List<CotisationStatut> statuts,
      UUID tontineId,
      String q,
      Pageable pageable)
      throws CustomException;
}
