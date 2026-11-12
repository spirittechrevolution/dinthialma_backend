package com.africa.dinthialma_backend.common.publicconfig.service.interfaces;

import com.africa.dinthialma_backend.common.publicconfig.dto.PublicConfigResponse;

/** Configuration publique lue par le front avant connexion (B25). */
public interface PublicConfigService {

  /** Contact support et version minimale ; un champ non configuré vaut null (absent du JSON). */
  PublicConfigResponse getConfig();
}
