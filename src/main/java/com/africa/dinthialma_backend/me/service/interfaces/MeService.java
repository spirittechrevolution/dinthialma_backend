package com.africa.dinthialma_backend.me.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.me.dto.MeSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Vues transverses de l'utilisateur connecté, toutes tontines confondues ({@code /v1/me}). */
public interface MeService {

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
