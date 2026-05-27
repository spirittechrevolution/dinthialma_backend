package com.africa.dinthialma_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriétés de création automatique du Super Admin au premier démarrage. */
@Component
@ConfigurationProperties(prefix = "dinthialma.bootstrap")
@Getter
@Setter
public class BootstrapProperties {

  /** Active ou désactive le bootstrap. Mettre à {@code false} après le premier démarrage. */
  private boolean enabled = true;

  private String email;
  private String firstName;
  private String lastName;
  private String phone;

  /**
   * Mot de passe initial du Super Admin. ⚠️ Doit être changé immédiatement après le premier login.
   */
  private String password;
}
