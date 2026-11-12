package com.africa.dinthialma_backend.common.publicconfig.service.impl;

import com.africa.dinthialma_backend.common.publicconfig.dto.PublicConfigResponse;
import com.africa.dinthialma_backend.common.publicconfig.service.interfaces.PublicConfigService;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Configuration publique (GET /v1/public/config).
 *
 * <p>Versions du front au format SemVer strict {@code MAJEUR.MINEUR.CORRECTIF} (ex. {@code 1.4.0})
 * : une valeur mal formée, ou une version recommandée inférieure à la minimale, empêche le
 * démarrage — mieux vaut un déploiement refusé qu'un écran « Mise à jour requise » pour tous.
 */
@Service
public class PublicConfigServiceImpl implements PublicConfigService {

  private static final Pattern SEMVER =
      Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

  private final String supportWhatsapp;
  private final String supportEmail;
  private final String versionMinimaleApp;
  private final String versionRecommandeeApp;

  public PublicConfigServiceImpl(
      @Value("${dinthialma.support.whatsapp:}") String supportWhatsapp,
      @Value("${dinthialma.support.email:}") String supportEmail,
      @Value("${dinthialma.version-minimale-app:}") String versionMinimaleApp,
      @Value("${dinthialma.version-recommandee-app:}") String versionRecommandeeApp) {
    this.supportWhatsapp = vide(supportWhatsapp);
    this.supportEmail = vide(supportEmail);
    this.versionMinimaleApp = version("APP_VERSION_MINIMALE", versionMinimaleApp);
    this.versionRecommandeeApp = version("APP_VERSION_RECOMMANDEE", versionRecommandeeApp);
    if (this.versionMinimaleApp != null
        && this.versionRecommandeeApp != null
        && comparer(this.versionRecommandeeApp, this.versionMinimaleApp) < 0) {
      throw new IllegalStateException(
          "APP_VERSION_RECOMMANDEE ("
              + this.versionRecommandeeApp
              + ") doit être supérieure ou égale à APP_VERSION_MINIMALE ("
              + this.versionMinimaleApp
              + ")");
    }
  }

  @Override
  public PublicConfigResponse getConfig() {
    return PublicConfigResponse.builder()
        .supportWhatsapp(supportWhatsapp)
        .supportEmail(supportEmail)
        .versionMinimaleApp(versionMinimaleApp)
        .versionRecommandeeApp(versionRecommandeeApp)
        .build();
  }

  private static String version(String variable, String valeur) {
    String v = vide(valeur);
    if (v != null && !SEMVER.matcher(v).matches()) {
      throw new IllegalStateException(
          variable
              + " invalide : « "
              + v
              + " ». Format attendu MAJEUR.MINEUR.CORRECTIF (ex. 1.4.0), ou vide pour désactiver.");
    }
    return v;
  }

  /** Comparaison numérique composante par composante (1.10.0 > 1.9.0). */
  static int comparer(String a, String b) {
    String[] pa = a.split("\\.");
    String[] pb = b.split("\\.");
    for (int i = 0; i < 3; i++) {
      int c = Long.compare(Long.parseLong(pa[i]), Long.parseLong(pb[i]));
      if (c != 0) return c;
    }
    return 0;
  }

  private static String vide(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
