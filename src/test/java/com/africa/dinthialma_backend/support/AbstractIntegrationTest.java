package com.africa.dinthialma_backend.support;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base des tests d'intégration : application complète sur un port aléatoire, branchée sur un
 * PostgreSQL et un Keycloak réels (Testcontainers).
 *
 * <p>Les conteneurs sont démarrés une seule fois pour toute la suite (pattern « singleton container
 * ») et le contexte Spring est partagé : chaque test crée ses propres données (numéros uniques) et
 * ne dépend jamais de l'ordre d'exécution.
 *
 * <p>Prérequis : Docker démarré. Sans Docker, les tests sont ignorés.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// Docker obligatoire : sans Docker, le build échoue (pas de saut silencieux des *IT).
// Pour ne lancer que les tests unitaires : mvn install -DskipITs
@Testcontainers
public abstract class AbstractIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("db-dinthialma-test")
          .withUsername("postgres")
          .withPassword("postgres");

  static final KeycloakContainer KEYCLOAK =
      new KeycloakContainer("quay.io/keycloak/keycloak:26.1.5")
          .withRealmImportFile("keycloak/realm-dinthialma.json");

  static {
    POSTGRES.start();
    KEYCLOAK.start();
  }

  @DynamicPropertySource
  static void proprietes(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    String keycloakUrl = KEYCLOAK.getAuthServerUrl().replaceAll("/$", "");
    String issuer = keycloakUrl + "/realms/dinthialma";
    registry.add("keycloak.auth-server-url", () -> keycloakUrl);
    registry.add("keycloak.admin-username", KEYCLOAK::getAdminUsername);
    registry.add("keycloak.admin-password", KEYCLOAK::getAdminPassword);
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> issuer + "/protocol/openid-connect/certs");
  }

  @LocalServerPort protected int port;

  @Autowired protected TestDataFactory donnees;

  protected ApiClient api;

  @BeforeEach
  void initialiserClient() {
    api = new ApiClient("http://localhost:" + port + "/api");
    donnees.utiliser(api);
  }
}
