package com.africa.dinthialma_backend.config;

import com.africa.dinthialma_backend.common.util.KeycloakJwtRolesConverter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration Spring Security – Dinthialma Backend.
 *
 * <p>Deux filter chains :
 *
 * <ol>
 *   <li>Ordre 1 : routes publiques (WHITELIST) – aucun JWT requis
 *   <li>Ordre 2 : toutes les autres routes – JWT Keycloak obligatoire
 * </ol>
 *
 * <p>Routes publiques : Swagger/OpenAPI, actuator/health, endpoints auth publics.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] WHITELIST = {
    // Swagger / OpenAPI
    "/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    // Actuator (health publique)
    "/actuator/health",
    // Auth – endpoints publics
    "/v1/auth/login",
    "/v1/auth/login-pin",
    "/pin/reset",
    "/pin/reset/send-otp",
    "/pin/reset/verify-otp",
    "/v1/auth/login/phone",
    "/v1/auth/register/send-otp",
    "/v1/auth/register/verify-otp",
    "/v1/auth/register/complete",
    "/v1/auth/logout",
    "/v1/auth/forgot-password",
    "/v1/auth/forgot-password/send-otp",
    "/v1/auth/forgot-password/verify-otp",
    "/v1/auth/forgot-password/reset",
    // CodeList – endpoint public pour peupler les selects frontend
    "/v1/code-list/type/**",
  };

  private final CustomAccessDenied customAccessDenied;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Value("${dinthialma.endpoints.frontend:http://localhost:3000}")
  private String frontendUrls;

  @Value("${dinthialma.security.enabled:true}")
  private boolean securityEnabled;

  public SecurityConfig(
      CustomAccessDenied customAccessDenied,
      CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
    this.customAccessDenied = customAccessDenied;
    this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
  }

  /** Filter chain 1 – Routes publiques (WHITELIST). */
  @Bean
  @Order(1)
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(WHITELIST)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  /** Filter chain 2 – Routes protégées, JWT Keycloak obligatoire. */
  @Bean
  @Order(2)
  public SecurityFilterChain protectedFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(customAccessDenied)
                    .authenticationEntryPoint(customAuthenticationEntryPoint))
        .authorizeHttpRequests(
            auth -> {
              if (!securityEnabled) {
                // Mode dev – désactiver la sécurité
                auth.anyRequest().permitAll();
              } else {
                auth.requestMatchers(HttpMethod.GET, "/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated();
              }
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /** Convertit les rôles Keycloak realm_access.roles en GrantedAuthority. */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRolesConverter());
    return converter;
  }

  /** Configuration CORS – autorise les origines frontend déclarées dans application.yml. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> origins = Arrays.stream(frontendUrls.split(",")).map(String::trim).toList();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
