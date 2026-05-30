package com.africa.dinthialma_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger UI – Dinthialma Backend.
 *
 * <p>Le {@code serverUrl} intègre le {@code context-path} ({@code /api}) afin que les appels lancés
 * depuis Swagger UI atterrissent sur le bon chemin (ex. {@code /api/v1/auth/login}).
 *
 * <p>Swagger UI accessible sur : {@code http://localhost:8081/api/swagger-ui.html}
 */
@Configuration
public class OpenApiConfig {

  @Value("${server.servlet.context-path:/api}")
  private String contextPath;

  @Value("${keycloak.auth-server-url:http://localhost:8280}")
  private String keycloakUrl;

  @Value("${keycloak.realm:dinthialma}")
  private String realm;

  @Bean
  public OpenAPI dinthialmaOpenAPI() {

    String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    return new OpenAPI()
        // ── Serveur ─────────────────────────────────────────────────────
        .servers(
            List.of(
                new Server().url(contextPath).description("Serveur courant (context-path inclus)")))

        // ── Informations générales ───────────────────────────────────────
        .info(
            new Info()
                .title("Dinthialma API")
                .version("1.0.0")
                .description(buildApiDescription())
                .contact(
                    new Contact()
                        .name("Spirit Tech Revolution")
                        .email("spirittechrevolution@gmail.com")))

        // ── Ordre d'affichage des tags ────────────────────────────────────
        .tags(
            List.of(
                new Tag()
                    .name("Auth")
                    .description(
                        "Inscription (OTP 3 étapes), connexion (mot de passe ou PIN), déconnexion,"
                            + " reset mot de passe et reset PIN"),
                new Tag()
                    .name("Profil")
                    .description(
                        "Gestion du profil de l'utilisateur connecté : lecture, modification et"
                            + " changement de numéro (OTP 2 étapes)"),
                new Tag()
                    .name("Tontines")
                    .description(
                        "CRUD des groupes de tontine + cycle de vie (BROUILLON → ACTIVE →"
                            + " SUSPENDUE)"),
                new Tag()
                    .name("Cycles")
                    .description(
                        "Gestion des cycles de cotisation : liste, détail, ouverture manuelle et"
                            + " clôture avec calcul automatique du jackpot"),
                new Tag()
                    .name("Membres")
                    .description("Ajout, retrait et gestion du statut des cotisants d'une tontine"),
                new Tag()
                    .name("Cotisations")
                    .description(
                        "Enregistrement par le membre et validation par l'admin des paiements de"
                            + " cotisation"),
                new Tag()
                    .name("Commissions")
                    .description(
                        "Configuration des commissions du gestionnaire par tontine :"
                            + " pourcentage du jackpot, frais fixes par cycle ou frais"
                            + " d'adhésion"),
                new Tag()
                    .name("Administration")
                    .description(
                        "Dashboard global plateforme (SUPER_ADMIN) et tableau de bord personnel"
                            + " (ADMIN de tontine)"),
                new Tag()
                    .name("CodeList")
                    .description(
                        "Référentiels de valeurs persistées — peupler les dropdowns frontend."
                            + " Types : FREQUENCE_TONTINE, METHODE_PAIEMENT, STATUT_COTISATION,"
                            + " ORDRE_BENEFICIAIRE. Endpoint public : GET /v1/code-list/type/{type}")))

        // ── Schémas de sécurité ──────────────────────────────────────────
        .components(
            new Components()
                // Bearer JWT (header Authorization: Bearer <token>)
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "**Comment obtenir un token ?**\n\n"
                                + "1. POST `/v1/auth/register/send-otp` → recevoir l'OTP SMS\n"
                                + "2. POST `/v1/auth/register/verify-otp` → valider l'OTP\n"
                                + "3. POST `/v1/auth/register/complete` → créer le compte\n"
                                + "4. POST `/v1/auth/login` → obtenir `access_token` + `refresh_token`\n"
                                + "5. Copier l'`access_token` ici (sans le préfixe Bearer)\n\n"
                                + "Le token expire en ~5 minutes. Utilisez `/v1/auth/refresh` pour en"
                                + " obtenir un nouveau."))
                // OAuth2 Password Flow (bouton Authorize Swagger)
                .addSecuritySchemes(
                    "oauth2",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(
                            new OAuthFlows()
                                .password(
                                    new OAuthFlow().tokenUrl(tokenUrl).scopes(new Scopes())))));
  }

  private String buildApiDescription() {
    return """
        # Dinthialma API – Gestion de tontines et d'épargne collective

        Dinthialma digitalise la pratique de la **tontine** (épargne rotative communautaire) en
        offrant une plateforme transparente, traçable et accessible depuis tout appareil.

        ---

        ## 🔐 Authentification

        Tous les endpoints sécurisés nécessitent un **JWT Keycloak** dans le header :
        ```
        Authorization: Bearer <access_token>
        ```

        **Flux de connexion :**
        1. `POST /v1/auth/login` → renvoie `access_token` + `refresh_token`
        2. Coller l'`access_token` dans le bouton **Authorize** en haut de cette page
        3. Quand le token expire, utiliser `POST /v1/auth/refresh`

        ---

        ## 👥 Rôles et accès

        | Rôle Keycloak | Description | Portée |
        |---|---|---|
        | `DINTHIALMA_SUPER_ADMIN` | Équipe Dinthialma | Accès total plateforme |
        | `DINTHIALMA_ADMIN` | Créateur d'une tontine | Gère ses propres tontines |
        | `DINTHIALMA_MEMBER` | Cotisant d'une tontine | Accès lecture + ses cotisations |
        | `DINTHIALMA_USER` | Tout compte inscrit | Rôle de base attribué à l'inscription |

        > **Important :** Les rôles se cumulent. La logique d'accès par tontine est applicative :
        > être ADMIN ne suffit pas — il faut être le **créateur de cette tontine spécifique**.

        ---

        ## 📦 Format des réponses

        Toutes les réponses sont enveloppées dans un objet `CustomResponse` :
        ```json
        {
          "status": "success",
          "statusCode": 200,
          "message": "Opération réussie",
          "data": { ... }
        }
        ```

        Les listes paginées retournent dans `data` un objet `Page` Spring Data :
        ```json
        {
          "data": {
            "content": [ ... ],
            "totalElements": 42,
            "totalPages": 3,
            "size": 20,
            "number": 0,
            "first": true,
            "last": false
          }
        }
        ```

        ---

        ## 📄 Pagination

        Tous les endpoints de liste acceptent les query params suivants :
        - `page` — numéro de page (base 0, défaut 0)
        - `size` — taille de page (défaut 20)
        - `sort` — champ de tri (ex: `createdAt,desc`)

        **Defaults par domaine :**

        | Endpoint | Tri par défaut |
        |---|---|
        | `GET /v1/tontines` | `createdAt DESC` |
        | `GET .../cycles` | `numeroCycle ASC` |
        | `GET .../membres` | `ordreJackpot ASC` |
        | `GET .../cotisations` | `createdAt DESC` |
        | `GET .../commissions` | `createdAt ASC` |
        | `GET /v1/admin/dashboard/users` | `createdAt DESC` |

        ---

        ## ⚠️ Codes d'erreur

        | Code HTTP | Signification |
        |---|---|
        | 400 | Données invalides (validation) |
        | 401 | Token JWT manquant ou expiré |
        | 403 | Accès interdit (mauvais rôle ou pas le créateur) |
        | 404 | Ressource introuvable |
        | 409 | Conflit (ex: téléphone déjà utilisé, cycle déjà ouvert) |
        | 429 | Trop de tentatives PIN (lockout 30 min) |
        | 500 | Erreur serveur interne |

        ---

        ## 💡 Concepts clés

        - **Tontine** : groupe d'épargne rotative. Chaque membre cotise périodiquement et, à tour
          de rôle, reçoit le jackpot collecté (= la mise de tous les membres sur un cycle).
        - **Cycle** : période pendant laquelle les cotisations sont collectées et le jackpot remis.
        - **Bénéficiaire** : membre qui reçoit le jackpot du cycle en cours.
        - **Commission** : frais de gestion configurés par le gestionnaire (POURCENTAGE_JACKPOT,
          FRAIS_FIXES_PAR_CYCLE, FRAIS_ADHESION). Déduits automatiquement à la clôture du cycle.
        - **Mode AUTOMATIQUE** : les cycles sont générés à l'activation de la tontine, chacun
          démarrant automatiquement quand le précédent est clôturé.
        - **Mode MANUEL** : le gestionnaire ouvre chaque cycle manuellement au moment voulu.

        ---

        ## 📋 Référentiels de valeurs (Code Lists)

        Endpoint public (aucun token requis) : `GET /v1/code-list/type/{type}`

        | Type | Valeurs disponibles | Description |
        |------|---------------------|-------------|
        | `FREQUENCE_TONTINE` | `JOURNALIERE` · `HEBDOMADAIRE` · `BIMENSUEL` · `MENSUEL` · `TRIMESTRIEL` | Rythme de cotisation d'une tontine |
        | `METHODE_PAIEMENT` | `WAVE` · `ORANGE_MONEY` · `FREE_MONEY` · `CASH` | Moyen de paiement d'une cotisation |
        | `STATUT_COTISATION` | `EN_ATTENTE` · `VALIDE` · `EN_RETARD` | Cycle de vie d'une cotisation |
        | `ORDRE_BENEFICIAIRE` | `ALEATOIRE` · `MANUEL` · `ROTATION` | Règle de désignation du bénéficiaire |

        ### Statuts métier (enums Java)

        | Enum | Valeurs |
        |------|---------|
        | `TontineStatut` | `BROUILLON` → `ACTIVE` → `SUSPENDUE` / `TERMINEE` |
        | `CycleStatut` | `EN_ATTENTE` → `EN_COURS` → `TERMINE` |
        | `ModeCycle` | `AUTOMATIQUE` · `MANUEL` |
        | `CommissionType` | `POURCENTAGE_JACKPOT` · `FRAIS_FIXES_PAR_CYCLE` · `FRAIS_ADHESION` |
        | `MembreStatut` | `ACTIF` · `SUSPENDU` · `SORTI` |
        | `CotisationStatut` | `EN_ATTENTE` · `VALIDE` · `EN_RETARD` |
        | `ClientType` | `WEB` (TTL 24 h) · `MOBILE` (TTL 30 jours) |
        | `UserRole` | `USER` · `MEMBER` · `ADMIN` · `SUPER_ADMIN` |
        """;
  }
}
