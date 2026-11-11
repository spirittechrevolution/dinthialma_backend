package com.africa.dinthialma_backend.me.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.me.dto.MonQrResponse;
import com.africa.dinthialma_backend.me.dto.QrResolutionResponse;

/** QR membre : jeton opaque stable et révocable, résolu sans exposer le téléphone (B15). */
public interface QrService {

  /** Retourne mon jeton QR, en le générant à la première demande. */
  MonQrResponse getMonQr(String keycloakId) throws CustomException;

  /** Génère un nouveau jeton : l'ancien QR (capture d'écran, impression) devient invalide. */
  MonQrResponse regenererMonQr(String keycloakId) throws CustomException;

  /**
   * Résout un QR scanné. Jeton inconnu ou révoqué → 404. Les tontines listées sont uniquement
   * celles que l'appelant gère.
   */
  QrResolutionResponse resoudre(String keycloakId, String token) throws CustomException;
}
