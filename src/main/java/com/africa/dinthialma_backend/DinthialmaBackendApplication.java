package com.africa.dinthialma_backend;

import com.africa.dinthialma_backend.common.util.Fuseau;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class DinthialmaBackendApplication {

  // Fuseau de référence (B26) : échéances, rappels et dates enregistrées en heure de Dakar, quel
  // que soit le fuseau du serveur. Bloc statique : appliqué aussi aux tests d'intégration.
  static {
    TimeZone.setDefault(TimeZone.getTimeZone(Fuseau.DAKAR));
  }

  public static void main(String[] args) {
    SpringApplication.run(DinthialmaBackendApplication.class, args);
  }
}
