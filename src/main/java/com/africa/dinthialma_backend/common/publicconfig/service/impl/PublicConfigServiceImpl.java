package com.africa.dinthialma_backend.common.publicconfig.service.impl;

import com.africa.dinthialma_backend.common.publicconfig.dto.PublicConfigResponse;
import com.africa.dinthialma_backend.common.publicconfig.service.interfaces.PublicConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PublicConfigServiceImpl implements PublicConfigService {

  private final String supportWhatsapp;
  private final String supportEmail;
  private final String versionMinimaleApp;

  public PublicConfigServiceImpl(
      @Value("${dinthialma.support.whatsapp:}") String supportWhatsapp,
      @Value("${dinthialma.support.email:}") String supportEmail,
      @Value("${dinthialma.version-minimale-app:}") String versionMinimaleApp) {
    this.supportWhatsapp = supportWhatsapp;
    this.supportEmail = supportEmail;
    this.versionMinimaleApp = versionMinimaleApp;
  }

  @Override
  public PublicConfigResponse getConfig() {
    return PublicConfigResponse.builder()
        .supportWhatsapp(vide(supportWhatsapp))
        .supportEmail(vide(supportEmail))
        .versionMinimaleApp(vide(versionMinimaleApp))
        .build();
  }

  private static String vide(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
