package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.common.security.JwtOptionnel;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtOptionnel")
class JwtOptionnelTest {

  @Mock private JwtDecoder jwtDecoder;
  @InjectMocks private JwtOptionnel jwtOptionnel;

  private static MockHttpServletRequest requete(String authorization) {
    MockHttpServletRequest r = new MockHttpServletRequest();
    if (authorization != null) r.addHeader("Authorization", authorization);
    return r;
  }

  @Test
  @DisplayName("jeton valide : keycloakId (sub) ; préfixe Bearer insensible à la casse")
  void valide() {
    Jwt jwt =
        Jwt.withTokenValue("ok")
            .header("alg", "RS256")
            .subject("kc-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    when(jwtDecoder.decode("ok")).thenReturn(jwt);
    assertThat(jwtOptionnel.keycloakId(requete("Bearer ok"))).contains("kc-123");
    assertThat(jwtOptionnel.keycloakId(requete("bearer ok"))).contains("kc-123");
  }

  @Test
  @DisplayName("jeton invalide ou expiré : vide, jamais d'exception")
  void invalide() {
    when(jwtDecoder.decode("expire")).thenThrow(new BadJwtException("expiré"));
    assertThat(jwtOptionnel.keycloakId(requete("Bearer expire"))).isEmpty();
  }

  @Test
  @DisplayName("sans en-tête, autre schéma ou jeton vide : vide sans appel au décodeur")
  void absent() {
    assertThat(jwtOptionnel.keycloakId(requete(null))).isEmpty();
    assertThat(jwtOptionnel.keycloakId(requete("Basic abc"))).isEmpty();
    assertThat(jwtOptionnel.keycloakId(requete("Bearer   "))).isEmpty();
    verify(jwtDecoder, never()).decode(anyString());
  }
}
