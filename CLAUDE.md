# CLAUDE.md – dinthialma-backend

## Vue d'ensemble

**dinthialma-backend** est le backend de la plateforme Dinthialma : application de gestion de tontines et d'épargne collective permettant à des groupes de gérer leurs cotisations, bénéficiaires et historiques de manière numérique et transparente.

- **Stack** : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak
- **Port** : `8081` (local) · Context path : `/api`
- **Realm Keycloak** : `dinthialma`
- **Swagger UI** : `http://localhost:8081/api/swagger-ui.html`
- **Schéma DB** : `dinthialma`

---

## Matrice des rôles

| Rôle | Realm Keycloak | Qui est-ce ? |
|------|----------------|--------------|
| `SUPER_ADMIN` | `DINTHIALMA_SUPER_ADMIN` | Équipe Dinthialma – accès total plateforme |
| `ADMIN` | `DINTHIALMA_ADMIN` | Créateur/gestionnaire d'une tontine |
| `MEMBER` | `DINTHIALMA_MEMBER` | Cotisant membre d'une tontine |
| `USER` | *(rôle de base)* | Tout compte inscrit (tous ont ce rôle) |

> **Règle** : chaque utilisateur peut cumuler plusieurs rôles (ex. ADMIN + USER, MEMBER + USER).
> Un ADMIN est toujours aussi USER. SUPER_ADMIN peut tout faire sans restriction.

---

## Matrice d'accès par fonctionnalité

### 🔐 Auth (`/v1/auth`)

| Endpoint | Méthode | Public | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|--------|------|--------|-------|-------------|-------|
| `/login` | POST | ✅ | ✅ | ✅ | ✅ | ✅ | username(phone/email) + password + clientType |
| `/logout` | POST | ✅ | ✅ | ✅ | ✅ | ✅ | révoque session Keycloak |
| `/refresh` | POST | ✅ | ✅ | ✅ | ✅ | ✅ | refresh_token → nouveaux tokens |
| `/register/send-otp` | POST | ✅ | – | – | – | – | envoie OTP SMS |
| `/register/verify-otp` | POST | ✅ | – | – | – | – | vérifie OTP |
| `/register/complete` | POST | ✅ | – | – | – | – | finalise inscription |
| `/forgot-password/send-otp` | POST | ✅ | – | – | – | – | anti-énumération |
| `/forgot-password/verify-otp` | POST | ✅ | – | – | – | – | |
| `/forgot-password/reset` | POST | ✅ | – | – | – | – | |
| `/pin/setup` | POST | ❌ | ✅ | ✅ | ✅ | ✅ | 🔒 JWT requis |
| `/login-pin` | POST | ✅ | ✅ | ✅ | ✅ | ✅ | nécessite session active |
| `/pin/reset/send-otp` | POST | ✅ | – | – | – | – | anti-énumération |
| `/pin/reset/verify-otp` | POST | ✅ | – | – | – | – | |
| `/pin/reset` | POST | ✅ | – | – | – | – | |

### 👤 Profil utilisateur (`/v1/profile`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 JWT – voir son profil |
| `/` | PUT | ✅ | ✅ | ✅ | ✅ | 🔒 firstName, lastName, email |
| `/phone/request-change` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 OTP envoyé au nouveau numéro |
| `/phone/verify` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 vérifie OTP + change phone partout |
| `/sessions` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 liste ses sessions actives |
| `/sessions/{id}` | DELETE | ✅ | ✅ | ✅ | ✅ | 🔒 révoque une session |
| `/sessions` | DELETE | ✅ | ✅ | ✅ | ✅ | 🔒 révoque toutes les sessions |

### 🏦 Tontines (`/v1/tontines`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | POST | ❌ | ❌ | ✅ | ✅ | 🔒 créer une tontine |
| `/` | GET | ❌ | ✅ | ✅ | ✅ | 🔒 liste ses tontines (pageable) |
| `/{id}` | GET | ❌ | ✅¹ | ✅ | ✅ | 🔒 ¹ seulement si membre |
| `/{id}` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 ² seulement si admin de la tontine |
| `/{id}/activate` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 activer la tontine |
| `/{id}/close` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 clôturer |
| `/admin/all` | GET | ❌ | ❌ | ❌ | ✅ | 🔒 toutes les tontines plateforme |

### 👥 Membres (`/v1/tontines/{id}/members`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 ajouter un membre |
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 liste membres |
| `/{memberId}` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 modifier (ordre jackpot, statut) |
| `/{memberId}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 retirer un membre |
| `/{memberId}/promote` | POST | ❌ | ❌ | ❌ | ✅ | 🔒 promouvoir en ADMIN |

### 💰 Cotisations (`/v1/tontines/{id}/contributions`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 enregistrer une cotisation |
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 historique (pageable) |
| `/{id}/validate` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 valider EN_ATTENTE → VALIDE |
| `/{id}/reject` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 rejeter |
| `/overdue` | GET | ❌ | ❌ | ✅² | ✅ | 🔒 cotisations en retard |

### 🔄 Cycles & Jackpots (`/v1/tontines/{id}/cycles`)

| Endpoint | Méthode | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|--------|-------|-------------|-------|
| `/` | POST | ❌ | ✅² | ✅ | 🔒 démarrer un cycle |
| `/` | GET | ✅¹ | ✅² | ✅ | 🔒 historique cycles |
| `/current` | GET | ✅¹ | ✅² | ✅ | 🔒 cycle en cours |
| `/{id}/remit` | POST | ❌ | ✅² | ✅ | 🔒 confirmer remise jackpot |

### 📊 Dashboards Admin (`/v1/admin`)

| Endpoint | Méthode | ADMIN | SUPER_ADMIN | Description |
|----------|---------|-------|-------------|-------------|
| `/dashboard` | GET | ❌ | ✅ | Statistiques globales plateforme |
| `/dashboard/tontines` | GET | ❌ | ✅ | Toutes tontines + statuts |
| `/dashboard/users` | GET | ❌ | ✅ | Tous les utilisateurs (pageable) |
| `/dashboard/users/{id}` | GET | ❌ | ✅ | Détail utilisateur |
| `/dashboard/users/{id}/disable` | POST | ❌ | ✅ | Désactiver un compte |
| `/dashboard/users/{id}/enable` | POST | ❌ | ✅ | Réactiver un compte |
| `/dashboard/users/{id}/roles` | PUT | ❌ | ✅ | Modifier les rôles |
| `/dashboard/contributions` | GET | ❌ | ✅ | Toutes cotisations (filtrables) |
| `/my-dashboard` | GET | ✅ | ✅ | Dashboard de l'admin pour ses tontines |

### 📊 Dashboard Super Admin — métriques clés

```
┌─────────────────────────────────────────────────────────────┐
│  SUPER ADMIN DASHBOARD                                      │
├─────────────────┬───────────────────┬───────────────────────┤
│ Utilisateurs    │ Tontines          │ Finances              │
│ • Total         │ • Total           │ • Cotisations ce mois │
│ • Actifs        │ • Actives         │ • Jackpots remis      │
│ • Nouveaux /mois│ • En attente      │ • Commissions totales │
│ • Désactivés    │ • Clôturées       │ • Retards en cours    │
├─────────────────┴───────────────────┴───────────────────────┤
│ Activité récente (dernières 24h)                            │
│ • Nouveaux inscrits · Connexions · Cotisations enregistrées │
└─────────────────────────────────────────────────────────────┘
```

### ⚙️ Administration système (`/v1/admin/system`) — SUPER_ADMIN uniquement

| Endpoint | Description |
|----------|-------------|
| `GET /config` | Paramètres plateforme (commission globale, TTL PIN...) |
| `PUT /config` | Modifier paramètres plateforme |
| `GET /audit-logs` | Journal d'audit complet (pageable) |
| `POST /users/{id}/reset-password` | Forcer reset mot de passe |

---

## Architecture des modules

```
src/main/java/com/africa/dinthialma_backend/
├── auth/           ✅ Auth, inscription OTP, Keycloak, PIN, profil
│   ├── codeList/       UserRole, ClientType
│   ├── config/         KeycloakClientConfig, KeycloakProperties
│   ├── controller/     AuthController, UserProfileController
│   ├── dto/            LoginRequest/Response, RegisterCompleteRequest/Response,
│   │                   SendOtpRequest, VerifyOtpRequest, LogoutRequest,
│   │                   RefreshTokenRequest,
│   │                   ForgotPasswordRequest, ResetPasswordByPhoneRequest,
│   │                   PinSetupRequest, PinLoginRequest, PinResetRequest,
│   │                   UserProfileResponse, UpdateProfileRequest,
│   │                   PhoneChangeRequest, PhoneChangeVerifyRequest
│   ├── entity/         User, OtpVerification, UserRoleAssignment, UserSession
│   ├── repository/     UserRepository, OtpVerificationRepository,
│   │                   UserRoleAssignmentRepository, UserSessionRepository
│   └── service/
│       ├── interfaces/ KeycloakAuthService, LoginService, RegistrationService,
│       │               PasswordResetService, PinService, UserSessionService,
│       │               UserProfileService
│       └── impl/       (implémentations correspondantes)
├── tontine/        🔶 Entités OK – services/controllers à créer
├── member/         🔶 Entités OK – services/controllers à créer
├── contribution/   🔶 Entités OK – services/controllers à créer
├── notification/   ✅ SMS (LAfricaMobile) – mock mode dev
├── admin/          🔲 TODO – DashboardController, DashboardService
├── common/         ✅ BaseEntity, exceptions, CustomResponse, utils, CodeList, Audit
└── config/         ✅ SecurityConfig, OpenApiConfig, BootstrapService
```

---

## Sécurité & Rôles

### Deux filter chains Spring Security

| Ordre | Périmètre | Comportement |
|-------|-----------|--------------|
| 1 | Routes publiques (`WHITELIST`) | Aucun JWT requis |
| 2 | Toutes les autres routes | JWT Keycloak obligatoire |

Routes publiques : `/api-docs/**`, `/swagger-ui/**`, `/actuator/health`,
`/v1/auth/login`, `/v1/auth/logout`, `/v1/auth/refresh`,
`/v1/auth/register/**`, `/v1/auth/forgot-password/**`,
`/v1/auth/login-pin`, `/v1/auth/pin/reset/**`

### Annotations Swagger — règle obligatoire

Tout endpoint protégé **doit** porter :
```java
@Operation(
    summary = "...",
    security = @SecurityRequirement(name = "bearerAuth"))
```

Et spécifier les rôles dans la description :
```java
description = "🔒 Rôles requis : ADMIN, SUPER_ADMIN"
```

### Vérification des rôles dans les contrôleurs

```java
// Exiger un rôle spécifique
RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.ADMIN);

// Exiger ADMIN ou SUPER_ADMIN
RequestUser caller = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

// Super admin uniquement
RequestUser caller = RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

// L'utilisateur lui-même (profil, sessions)
String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
```

---

## Conventions de code

- **Formatage** : Google Java Format via `fmt-maven-plugin` – `./mvnw fmt:apply`
- **Lombok** : `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder` entités ; `@RequiredArgsConstructor` services/controllers
- **Entités** : toutes étendent `BaseEntity` (id UUID, createdAt, updatedAt)
- **Soft delete** : `deletedAt` (`LocalDateTime`, null = actif) – jamais de suppression physique
- **Réponses API** : toujours `CustomResponse(status, statusCode, message, data)`
- **Exceptions** : hiérarchie `CustomException` → `ApiExceptionHandler` → JSON normalisé
- **Services** : interface dans `service/interfaces/` + impl dans `service/impl/` ; `@Transactional` sur écriture
- **Pagination** : toutes les listes utilisent `Pageable` + `Page<T>`
- **Phone** : toujours stocké SANS le `+` (normalisé via `PhoneUtils.normalize()` en entrée de service)

### ⛔ Imports — règle absolue : PAS de wildcard `*`

**Interdit** :
```java
import com.africa.dinthialma_backend.auth.dto.*;       // ❌
import com.africa.dinthialma_backend.auth.entity.*;    // ❌
import java.util.*;                                     // ❌
```

**Obligatoire** : chaque classe importée sur sa propre ligne :
```java
import com.africa.dinthialma_backend.auth.dto.LoginRequest;    // ✅
import com.africa.dinthialma_backend.auth.dto.LoginResponse;   // ✅
import com.africa.dinthialma_backend.auth.entity.User;         // ✅
import java.util.List;                                          // ✅
import java.util.Optional;                                      // ✅
```

**Pourquoi** : les imports `*` créent des conflits de noms silencieux quand deux classes portent
le même nom dans des packages différents (ex. `PhoneChangeRequest` en `dto/` et en `entity/`).
Java résout alors l'import explicite en priorité sur le wildcard, rendant le comportement
imprévisible et difficile à déboguer.

### Pattern contrôleur type

```java
@RestController
@RequestMapping("/v1/module")
@RequiredArgsConstructor
@Tag(name = "Module", description = "...")
public class ModuleController {

  @PostMapping
  @Operation(
      summary = "...",
      description = "🔒 Rôles requis : ADMIN, SUPER_ADMIN",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateRequest request,
      HttpServletRequest httpRequest) throws CustomException {

    RequestUser caller = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    ModuleResponse response = moduleService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(SUCCESS_BODY, CREATED, MODULE_CREATE_SUCCESS, response));
  }
}
```

---

## Base de données

- **Schéma** : `dinthialma`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` — Flyway est **la seule** source de vérité

### Migrations actuelles

| Version | Fichier | Tables / contenu |
|---------|---------|-----------------|
| V001 | `create_schema.sql` | Schéma `dinthialma` |
| V002 | `create_users.sql` | `users` |
| V003 | `create_otp_verifications.sql` | `otp_verifications` |
| V004 | `create_la_code_list.sql` | `la_code_list` |
| V005 | `create_tontines.sql` | `tontines` |
| V006 | `create_tontine_membres.sql` | `tontine_membres` |
| V007 | `create_cycles_tontine.sql` | `cycles_tontine` |
| V008 | `create_cotisations.sql` | `cotisations` |
| V009 | `create_tontine_audit_log.sql` | `tontine_audit_log` |
| V010 | `seed_frequence_tontine.sql` | Seeds FREQUENCE_TONTINE |
| V011 | `seed_methode_paiement.sql` | Seeds METHODE_PAIEMENT |
| V012 | `seed_statut_cotisation.sql` | Seeds STATUT_COTISATION |
| V013 | `seed_ordre_beneficiaire.sql` | Seeds ORDRE_BENEFICIAIRE |
| V014 | `create_user_roles.sql` | `user_roles` |
| V015 | `create_tontine_commissions.sql` | `tontine_commissions` |
| V016 | `create_user_sessions.sql` | `user_sessions` |
| V017 | `alter_user_sessions_token_column.sql` | `refresh_token_hash` → TEXT |
| V018 | `add_pin_created_at.sql` | `users.pin_created_at` (expiration PIN 90j) |
| V019 | `create_phone_change_requests.sql` | `phone_change_requests` |

Prochaine version disponible : **V020**

---

## Fonctionnalités & Statut

### Module Auth ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Login (phone/email + password) | ✅ | Public |
| Login PIN (WEB/MOBILE) | ✅ | Public |
| Inscription OTP 3 étapes | ✅ | Public |
| Reset mot de passe OTP | ✅ | Public |
| Reset PIN OTP | ✅ | Public |
| Refresh token | ✅ | Public |
| Setup PIN | ✅ | 🔒 USER+ |
| PIN expiration 90 jours | ✅ | AUTO |
| Lockout PIN 5 tentatives/30min | ✅ | AUTO |

### Module Profil Utilisateur ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Voir son profil | ✅ | 🔒 USER+ |
| Modifier profil (nom, email) | ✅ | 🔒 USER+ |
| Changer de numéro (OTP) | ✅ | 🔒 USER+ |
| Voir ses sessions actives | ✅ | 🔒 USER+ |
| Révoquer une session | ✅ | 🔒 USER+ |
| Révoquer toutes les sessions | ✅ | 🔒 USER+ |

> **Changement de numéro** : les tontines/memberships sont liés à l'`id` UUID de l'utilisateur,
> pas au numéro. Le changement ne casse aucune relation. Seul le username Keycloak + `users.phone`
> sont mis à jour. Toutes les sessions sont révoquées → re-login obligatoire.

### Module Tontine 🔶 (entités OK – services à créer)

| Feature | Statut | Accès |
|---------|--------|-------|
| Créer une tontine | 🔲 TODO | 🔒 ADMIN+ |
| Lister mes tontines | 🔲 TODO | 🔒 MEMBER+ |
| Détail tontine | 🔲 TODO | 🔒 MEMBER+ (si membre) |
| Modifier tontine | 🔲 TODO | 🔒 ADMIN de la tontine |
| Activer tontine | 🔲 TODO | 🔒 ADMIN de la tontine |
| Clôturer tontine | 🔲 TODO | 🔒 ADMIN+ |
| Liste toutes (admin) | 🔲 TODO | 🔒 SUPER_ADMIN |

### Module Membre 🔶

| Feature | Statut | Accès |
|---------|--------|-------|
| Ajouter membre | 🔲 TODO | 🔒 ADMIN de la tontine |
| Retirer membre | 🔲 TODO | 🔒 ADMIN de la tontine |
| Modifier ordre jackpot | 🔲 TODO | 🔒 ADMIN de la tontine |
| Promouvoir en admin | 🔲 TODO | 🔒 SUPER_ADMIN |
| Liste membres | 🔲 TODO | 🔒 MEMBER+ |

### Module Cotisation 🔶

| Feature | Statut | Accès |
|---------|--------|-------|
| Enregistrer cotisation | 🔲 TODO | 🔒 ADMIN de la tontine |
| Valider cotisation | 🔲 TODO | 🔒 ADMIN de la tontine |
| Rejeter cotisation | 🔲 TODO | 🔒 ADMIN de la tontine |
| Historique (pageable) | 🔲 TODO | 🔒 MEMBER+ |
| Cotisations en retard | 🔲 TODO | 🔒 ADMIN+ |

### Module Cycles & Jackpots 🔶

| Feature | Statut | Accès |
|---------|--------|-------|
| Démarrer un cycle | 🔲 TODO | 🔒 ADMIN de la tontine |
| Cycle en cours | 🔲 TODO | 🔒 MEMBER+ |
| Confirmer remise jackpot | 🔲 TODO | 🔒 ADMIN de la tontine |
| Historique cycles | 🔲 TODO | 🔒 MEMBER+ |

### Module Dashboard Admin 🔲

| Feature | Statut | Accès |
|---------|--------|-------|
| Dashboard global (stats) | 🔲 TODO | 🔒 SUPER_ADMIN |
| Liste tous les utilisateurs | 🔲 TODO | 🔒 SUPER_ADMIN |
| Désactiver/activer compte | 🔲 TODO | 🔒 SUPER_ADMIN |
| Modifier rôles utilisateur | 🔲 TODO | 🔒 SUPER_ADMIN |
| Journal d'audit complet | 🔲 TODO | 🔒 SUPER_ADMIN |
| Dashboard mes tontines | 🔲 TODO | 🔒 ADMIN+ |

### Module Notification 🔲

| Feature | Statut | Accès |
|---------|--------|-------|
| SMS OTP | ✅ (mock dev) | AUTO |
| Rappel cotisation | 🔲 TODO | AUTO (scheduler) |
| Annonce bénéficiaire jackpot | 🔲 TODO | AUTO |
| Alerte retard | 🔲 TODO | AUTO |

---

## Infrastructure locale (Docker)

```
docker/docker-compose.yml   # PostgreSQL, Keycloak, pgAdmin, Dozzle
```

### Ports

| Service | Port hôte |
|---------|-----------|
| Backend API | `8081` |
| PostgreSQL | `5434` |
| Keycloak | `8280` |
| pgAdmin | `5051` |
| Dozzle | `8889` |

```bash
# Infrastructure seule
cd docker && docker compose up -d

# Dev IDE
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Prérequis Keycloak (1 seule fois)

1. Aller sur http://localhost:8280
2. Créer le realm `dinthialma`
3. Créer le client `dinthialma-client` (confidential, service accounts enabled)
4. Créer les rôles realm : `DINTHIALMA_SUPER_ADMIN`, `DINTHIALMA_ADMIN`, `DINTHIALMA_MEMBER`
5. Dans User Profile (realm settings) : ajouter l'attribut `phone`
6. Copier le client secret → `docker/.env` → `KEYCLOAK_CLIENT_SECRET`

---

## Variables d'environnement clés

| Variable | Défaut local | Description |
|----------|-------------|-------------|
| `KEYCLOAK_CLIENT_ID` | – | **Requis** |
| `KEYCLOAK_CLIENT_SECRET` | – | **Requis** |
| `KEYCLOAK_ADMIN_USER` | `admin` | Admin realm master |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Admin realm master |
| `BOOTSTRAP_ENABLED` | `true` | Créer SUPER_ADMIN au 1er démarrage |
| `BOOTSTRAP_SUPER_ADMIN_PHONE` | `221783703310` | Sans le + |
| `TOKEN_ENCRYPTION_KEY` | *(dev default)* | Clé AES-256 pour refresh tokens |

---

## Prochaines étapes recommandées

```
✦ Sprint actuel : Auth + Profil (terminé)
  → refresh token ✅
  → pin_created_at + expiration 90j ✅
  → changement de numéro ✅
  → profil (GET/PUT) ✅

✦ Sprint suivant : Tontine core
  1. TontineService + TontineController
  2. MembreService + MembreController
  3. CotisationService + CotisationController

✦ Sprint 3 : Cycles & Dashboard
  1. CycleService (prochain bénéficiaire, ordre jackpot)
  2. DashboardService (métriques SUPER_ADMIN)
  3. AdminController (gestion utilisateurs)

✦ Sprint 4 : Notifications & Audit
  1. SchedulerService (rappels SMS)
  2. AuditService (logger automatiquement les mutations)
```
