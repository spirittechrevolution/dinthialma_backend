package com.africa.dinthialma_backend.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterResponse;
import com.africa.dinthialma_backend.auth.dto.UserSearchResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.mapper.UserMapper;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.LoginService;
import com.africa.dinthialma_backend.auth.service.interfaces.RegistrationService;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.common.publicconfig.dto.PublicConfigResponse;
import com.africa.dinthialma_backend.common.publicconfig.service.impl.PublicConfigServiceImpl;
import com.africa.dinthialma_backend.support.Fixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Services d'orchestration simples : inscription (B13), recherche par téléphone, config (B25). */
@ExtendWith(MockitoExtension.class)
@DisplayName("Inscription, recherche d'utilisateur et configuration publique")
class InscriptionEtRechercheServiceTest {

  @Mock private RegistrationService registrationService;
  @Mock private LoginService loginService;
  @Mock private UserRepository userRepository;

  @Nested
  @DisplayName("InscriptionServiceImpl")
  class Inscription {

    private RegisterCompleteRequest requete(ClientType clientType) {
      RegisterCompleteRequest r = new RegisterCompleteRequest();
      r.setPhone("+221770000001");
      r.setFirstName("Awa");
      r.setLastName("Diop");
      r.setPassword("Secret@2026");
      r.setClientType(clientType);
      r.setDeviceInfo("Pixel");
      return r;
    }

    private RegisterResponse reponse() {
      return new RegisterResponse(
          UUID.randomUUID(), "kc", "221770000001", "Awa", "Diop", null, "DINTHIALMA_USER");
    }

    @Test
    @DisplayName("sans clientType : inscription seule, aucune connexion")
    void sansConnexion() throws Exception {
      when(registrationService.completeRegistration(any())).thenReturn(reponse());
      RegisterResponse r =
          new InscriptionServiceImpl(registrationService, loginService)
              .finaliserInscription(requete(null));
      assertThat(r.getTokens()).isNull();
      verify(loginService, never()).login(any());
    }

    @Test
    @DisplayName("avec clientType : connexion avec le téléphone normalisé et les jetons renvoyés")
    void avecConnexion() throws Exception {
      LoginResponse jetons = new LoginResponse();
      when(registrationService.completeRegistration(any())).thenReturn(reponse());
      when(loginService.login(any())).thenReturn(jetons);

      RegisterResponse r =
          new InscriptionServiceImpl(registrationService, loginService)
              .finaliserInscription(requete(ClientType.MOBILE));

      assertThat(r.getTokens()).isSameAs(jetons);
      ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
      verify(loginService).login(captor.capture());
      assertThat(captor.getValue().getUsername()).isEqualTo("221770000001");
      assertThat(captor.getValue().getPassword()).isEqualTo("Secret@2026");
      assertThat(captor.getValue().getClientType()).isEqualTo(ClientType.MOBILE);
      assertThat(captor.getValue().getDeviceInfo()).isEqualTo("Pixel");
    }

    @Test
    @DisplayName("échec de connexion non bloquant : inscription réussie, sans jetons")
    void connexionEchoue() throws Exception {
      when(registrationService.completeRegistration(any())).thenReturn(reponse());
      when(loginService.login(any())).thenThrow(new UnAuthorizedException("Keycloak indisponible"));
      RegisterResponse r =
          new InscriptionServiceImpl(registrationService, loginService)
              .finaliserInscription(requete(ClientType.WEB));
      assertThat(r.getUserId()).isNotNull();
      assertThat(r.getTokens()).isNull();
    }

    @Test
    @DisplayName("échec d'inscription propagé, aucune connexion tentée")
    void inscriptionEchoue() throws Exception {
      when(registrationService.completeRegistration(any())).thenThrow(new NotFoundException("OTP"));
      assertThatThrownBy(
              () ->
                  new InscriptionServiceImpl(registrationService, loginService)
                      .finaliserInscription(requete(ClientType.WEB)))
          .isInstanceOf(CustomException.class);
      verify(loginService, never()).login(any());
    }
  }

  @Nested
  @DisplayName("UserSearchServiceImpl")
  class Recherche {

    @Test
    @DisplayName("téléphone avec « + » normalisé ; statut NULL renvoyé ACTIVE")
    void trouve() throws Exception {
      User u = Fixtures.user("Fatou");
      u.setAccountStatus(null);
      when(userRepository.findByPhoneAndDeletedAtIsNull("221771112233")).thenReturn(Optional.of(u));

      UserSearchResponse r =
          new UserSearchServiceImpl(userRepository, Mappers.getMapper(UserMapper.class))
              .searchByPhone("+221771112233");

      assertThat(r.getFirstName()).isEqualTo("Fatou");
      assertThat(r.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("numéro inconnu -> 404")
    void inconnu() {
      when(userRepository.findByPhoneAndDeletedAtIsNull("221700000000"))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  new UserSearchServiceImpl(userRepository, Mappers.getMapper(UserMapper.class))
                      .searchByPhone("221700000000"))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("PublicConfigServiceImpl")
  class ConfigurationPublique {

    @Test
    @DisplayName("valeurs renseignées rognées, valeurs vides -> null")
    void valeurs() {
      PublicConfigResponse r =
          new PublicConfigServiceImpl(" +221770000000 ", "  ", "2.0.0").getConfig();
      assertThat(r.getSupportWhatsapp()).isEqualTo("+221770000000");
      assertThat(r.getSupportEmail()).isNull();
      assertThat(r.getVersionMinimaleApp()).isEqualTo("2.0.0");
      assertThat(new PublicConfigServiceImpl(null, null, null).getConfig().getVersionMinimaleApp())
          .isNull();
    }
  }
}
