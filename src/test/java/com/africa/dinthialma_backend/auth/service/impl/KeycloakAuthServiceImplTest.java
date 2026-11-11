package com.africa.dinthialma_backend.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.config.KeycloakProperties;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Les appels au point de jeton (RestClient) passent par un faux serveur HTTP local ; l'API
 * d'administration Keycloak est simulée.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakAuthServiceImpl")
class KeycloakAuthServiceImplTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Keycloak keycloak;

  private KeycloakProperties props;
  private KeycloakAuthServiceImpl service;

  @BeforeEach
  void setUp() {
    props = new KeycloakProperties();
    props.setRealm("dinthialma");
    props.setClientId("client");
    props.setClientSecret("secret");
    props.setAuthServerUrl("http://127.0.0.1:1");
    service = new KeycloakAuthServiceImpl(props, keycloak);
  }

  @Nested
  @DisplayName("point de jeton (login, refresh, logout)")
  class PointDeJeton {

    private HttpServer serveur;
    private final List<String> corpsRecus = new ArrayList<>();
    private int statut = 200;
    private String reponse =
        "{\"access_token\":\"acc\",\"refresh_token\":\"ref\",\"expires_in\":300,\"token_type\":\"Bearer\"}";

    @BeforeEach
    void demarrer() throws IOException {
      serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      serveur.createContext(
          "/realms/dinthialma/protocol/openid-connect/",
          echange -> {
            corpsRecus.add(
                echange.getRequestURI().getPath()
                    + "?"
                    + new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] octets = reponse.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().add("Content-Type", "application/json");
            echange.sendResponseHeaders(statut, octets.length == 0 ? -1 : octets.length);
            try (OutputStream os = echange.getResponseBody()) {
              os.write(octets);
            }
          });
      serveur.start();
      props.setAuthServerUrl("http://127.0.0.1:" + serveur.getAddress().getPort());
    }

    @AfterEach
    void arreter() {
      serveur.stop(0);
    }

    @Test
    @DisplayName("login : grant password avec les identifiants du client, jetons désérialisés")
    void login() throws Exception {
      LoginResponse r = service.login("221770000001", "Secret@2026");
      assertThat(r.getAccessToken()).isEqualTo("acc");
      assertThat(r.getRefreshToken()).isEqualTo("ref");
      assertThat(corpsRecus.getFirst())
          .startsWith("/realms/dinthialma/protocol/openid-connect/token?")
          .contains(
              "grant_type=password",
              "client_id=client",
              "client_secret=secret",
              "username=221770000001");
    }

    @Test
    @DisplayName("login : 401 Keycloak -> INVALID_CREDENTIALS ; autre 4xx -> 502")
    void loginRefuse() {
      statut = 401;
      reponse = "{\"error\":\"invalid_grant\"}";
      assertThatThrownBy(() -> service.login("u", "p"))
          .isInstanceOfSatisfying(
              UnAuthorizedException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
      statut = 400;
      assertThatThrownBy(() -> service.login("u", "p"))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    @DisplayName("refresh : grant refresh_token ; jeton refusé -> 401 SESSION_EXPIRED")
    void refresh() throws Exception {
      assertThat(service.refreshToken("ref").getAccessToken()).isEqualTo("acc");
      assertThat(corpsRecus.getFirst()).contains("grant_type=refresh_token", "refresh_token=ref");

      statut = 400;
      reponse = "{\"error\":\"invalid_grant\"}";
      assertThatThrownBy(() -> service.refreshToken("ref"))
          .isInstanceOfSatisfying(
              UnAuthorizedException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("logout : révocation envoyée ; erreur Keycloak non bloquante")
    void logout() {
      reponse = "";
      statut = 204;
      assertThatCode(() -> service.logout("ref")).doesNotThrowAnyException();
      assertThat(corpsRecus.getFirst())
          .startsWith("/realms/dinthialma/protocol/openid-connect/logout?");
      statut = 500;
      assertThatCode(() -> service.logout("ref")).doesNotThrowAnyException();
    }
  }

  @Test
  @DisplayName("Keycloak injoignable : login et refresh -> 502, logout silencieux")
  void injoignable() {
    assertThatThrownBy(() -> service.login("u", "p"))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    assertThatThrownBy(() -> service.refreshToken("r"))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    assertThatCode(() -> service.logout("r")).doesNotThrowAnyException();
  }

  @Nested
  @DisplayName("API d'administration")
  class Administration {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RealmResource realm;

    @Mock private UsersResource users;
    @Mock private UserResource userResource;
    @Mock private RoleMappingResource roleMapping;
    @Mock private RoleScopeResource realmLevel;

    private UserRepresentation representation;

    @BeforeEach
    void cablage() {
      representation = new UserRepresentation();
      lenient().when(keycloak.realm("dinthialma")).thenReturn(realm);
      lenient().when(realm.users()).thenReturn(users);
      lenient().when(users.get("kc-1")).thenReturn(userResource);
      lenient().when(userResource.toRepresentation()).thenReturn(representation);
      lenient().when(userResource.roles()).thenReturn(roleMapping);
      lenient().when(roleMapping.realmLevel()).thenReturn(realmLevel);
    }

    private RegisterCompleteRequest inscription(String email) {
      RegisterCompleteRequest r = new RegisterCompleteRequest();
      r.setPhone("221770000001");
      r.setFirstName("Awa");
      r.setLastName("Diop");
      r.setEmail(email);
      r.setPassword("Secret@2026");
      return r;
    }

    private Response reponse(int statut) {
      Response r = org.mockito.Mockito.mock(Response.class);
      lenient().when(r.getStatus()).thenReturn(statut);
      lenient().when(r.getStatusInfo()).thenReturn(Response.Status.fromStatusCode(statut));
      lenient()
          .when(r.getLocation())
          .thenReturn(URI.create("http://kc/admin/realms/dinthialma/users/kc-nouveau"));
      lenient().when(r.readEntity(String.class)).thenReturn("{\"errorMessage\":\"conflit\"}");
      return r;
    }

    @Test
    @DisplayName("createUser : téléphone comme identifiant, attribut phone, mot de passe définitif")
    void creation() throws Exception {
      Response rep201 = reponse(201);
      when(users.create(any())).thenReturn(rep201);

      assertThat(service.createUser(inscription("awa@test.sn"))).isEqualTo("kc-nouveau");

      ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
      verify(users).create(captor.capture());
      UserRepresentation u = captor.getValue();
      assertThat(u.getUsername()).isEqualTo("221770000001");
      assertThat(u.getEmail()).isEqualTo("awa@test.sn");
      assertThat(u.isEnabled()).isTrue();
      assertThat(u.getAttributes()).containsEntry("phone", List.of("221770000001"));
      CredentialRepresentation c = u.getCredentials().getFirst();
      assertThat(c.getValue()).isEqualTo("Secret@2026");
      assertThat(c.isTemporary()).isFalse();
    }

    @Test
    @DisplayName("createUser sans e-mail : adresse de substitution dérivée du téléphone")
    void creationSansEmail() throws Exception {
      Response rep201 = reponse(201);
      when(users.create(any())).thenReturn(rep201);
      service.createUser(inscription(" "));
      ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
      verify(users).create(captor.capture());
      assertThat(captor.getValue().getEmail()).isEqualTo("221770000001@dinthialma.placeholder");
    }

    @Test
    @DisplayName("createUser refusé (409) ou en erreur -> 502")
    void creationRefusee() {
      Response rep409 = reponse(409);
      when(users.create(any())).thenReturn(rep409);
      assertThatThrownBy(() -> service.createUser(inscription(null)))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
      when(users.create(any())).thenThrow(new RuntimeException("réseau"));
      assertThatThrownBy(() -> service.createUser(inscription(null)))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    @DisplayName("resetPassword : mot de passe non temporaire ; erreur -> 502")
    void motDePasse() throws Exception {
      service.resetPassword("kc-1", "Nouveau@2026");
      ArgumentCaptor<CredentialRepresentation> captor =
          ArgumentCaptor.forClass(CredentialRepresentation.class);
      verify(userResource).resetPassword(captor.capture());
      assertThat(captor.getValue().getValue()).isEqualTo("Nouveau@2026");
      assertThat(captor.getValue().isTemporary()).isFalse();

      doThrow(new RuntimeException("KO")).when(userResource).resetPassword(any());
      assertThatThrownBy(() -> service.resetPassword("kc-1", "x"))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("assignRole / removeRole : rôle de realm ajouté ou retiré ; erreur -> 502")
    void roles() throws Exception {
      RoleRepresentation role = new RoleRepresentation();
      role.setName("DINTHIALMA_ADMIN");
      when(realm.roles().get("DINTHIALMA_ADMIN").toRepresentation()).thenReturn(role);

      service.assignRole("kc-1", "DINTHIALMA_ADMIN");
      verify(realmLevel).add(List.of(role));
      service.removeRole("kc-1", "DINTHIALMA_ADMIN");
      verify(realmLevel).remove(List.of(role));

      when(realm.roles().get("INCONNU")).thenThrow(new RuntimeException("404"));
      assertThatThrownBy(() -> service.assignRole("kc-1", "INCONNU"))
          .isInstanceOf(CustomException.class);
      assertThatThrownBy(() -> service.removeRole("kc-1", "INCONNU"))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName(
        "disableUser / enableUser basculent enabled ; deleteUser supprime ; erreurs -> 502")
    void etatDuCompte() throws Exception {
      service.disableUser("kc-1");
      assertThat(representation.isEnabled()).isFalse();
      service.enableUser("kc-1");
      assertThat(representation.isEnabled()).isTrue();
      verify(userResource, org.mockito.Mockito.times(2)).update(representation);
      service.deleteUser("kc-1");
      verify(userResource).remove();

      when(users.get("kc-x")).thenThrow(new RuntimeException("404"));
      assertThatThrownBy(() -> service.disableUser("kc-x")).isInstanceOf(CustomException.class);
      assertThatThrownBy(() -> service.enableUser("kc-x")).isInstanceOf(CustomException.class);
      assertThatThrownBy(() -> service.deleteUser("kc-x")).isInstanceOf(CustomException.class);
    }
  }
}
