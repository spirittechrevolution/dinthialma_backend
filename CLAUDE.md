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
| `USER` | `DINTHIALMA_USER` | Tout compte inscrit (rôle de base) |

> **Règle** : les rôles se cumulent (ex. ADMIN + USER, MEMBER + USER).  
> La logique d'accès par tontine est applicative : être ADMIN ne suffit pas, il faut être le **créateur de la tontine** concernée.

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
| `/pin/reset/send-otp` | POST | ✅ | – | – | – | – | |
| `/pin/reset/verify-otp` | POST | ✅ | – | – | – | – | |
| `/pin/reset` | POST | ✅ | – | – | – | – | |

### 👤 Profil utilisateur (`/v1/profile`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 voir son propre profil |
| `/` | PUT | ✅ | ✅ | ✅ | ✅ | 🔒 firstName, lastName, email |
| `/phone/request-change` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 OTP envoyé au nouveau numéro |
| `/phone/verify` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 vérifie OTP + change partout |

### 🏦 Tontines (`/v1/tontines`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 tout auth peut créer → devient ADMIN |
| `/` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 SUPER_ADMIN voit tout ; autres voient les leurs |
| `/{id}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 ¹membre de la tontine ²créateur |
| `/{id}` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 BROUILLON seulement |
| `/{id}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 soft delete, BROUILLON seulement |
| `/{id}/activer` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 BROUILLON/SUSPENDUE → ACTIVE |
| `/{id}/suspendre` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 ACTIVE → SUSPENDUE |

### 👥 Membres (`/v1/tontines/{tontineId}/membres`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 ajouter un cotisant → rôle MEMBER attribué |
| `/{membreId}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 soft delete, statut → SORTI |
| `/{membreId}/statut` | PATCH | ❌ | ❌ | ✅² | ✅ | 🔒 ACTIF / SUSPENDU / SORTI |

### 💰 Cotisations (`/v1/tontines/{tontineId}/cotisations`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 ¹MEMBER voit les siennes seulement |
| `/{id}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/` | POST | ❌ | ✅¹ | ❌ | ✅ | 🔒 le cotisant enregistre son paiement |
| `/{id}/valider` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 EN_ATTENTE → VALIDE |

### 🔄 Cycles (`/v1/tontines/{tontineId}/cycles`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/{cycleId}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 mode MANUEL uniquement |
| `/{cycleId}/cloturer` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 EN_COURS → TERMINE, calcul jackpot |

> ¹ = membre de cette tontine · ² = créateur de cette tontine

### 📊 Dashboard Admin (`/v1/admin`)

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
| `/my-dashboard` | GET | ✅ | ✅ | Métriques de l'admin pour ses tontines |

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

---

## Architecture des modules

```
src/main/java/com/africa/dinthialma_backend/
├── auth/           ✅ Auth, inscription OTP, Keycloak, PIN, profil
│   ├── codeList/       UserRole, ClientType
│   ├── config/         KeycloakClientConfig, KeycloakProperties
│   ├── controller/     AuthController, UserProfileController
│   ├── dto/            LoginRequest/Response, RegisterCompleteRequest/Response,
│   │                   RefreshTokenRequest, UserProfileResponse, UpdateProfileRequest,
│   │                   PhoneChangeRequest, PhoneChangeVerifyRequest, …
│   ├── entity/         User, OtpVerification, UserRoleAssignment, UserSession,
│   │                   PhoneChangeRequestEntity
│   ├── repository/     UserRepository, OtpVerificationRepository,
│   │                   UserRoleAssignmentRepository, UserSessionRepository,
│   │                   PhoneChangeRequestRepository
│   └── service/
│       ├── interfaces/ KeycloakAuthService, LoginService, RegistrationService,
│       │               PasswordResetService, PinService, UserSessionService,
│       │               UserProfileService
│       └── impl/       (implémentations correspondantes)
├── tontine/        ✅ Complet (entités + repo + service + controller)
│   ├── codeList/       TontineStatut, ModeCycle, CycleStatut, CommissionType
│   ├── controller/     TontineController, CycleController
│   ├── dto/            CreateTontineRequest, UpdateTontineRequest, TontineResponse,
│   │                   CycleResponse, OpenCycleRequest
│   ├── entity/         Tontine, CycleTontine, TontineCommission
│   ├── repository/     TontineRepository, CycleTontineRepository,
│   │                   TontineCommissionRepository
│   └── service/
│       ├── interfaces/ TontineService, CycleService
│       └── impl/       TontineServiceImpl, CycleServiceImpl
├── member/         ✅ Complet (entités + repo + service + controller)
│   ├── codeList/       MembreStatut
│   ├── controller/     MembreController
│   ├── dto/            AddMembreRequest, MembreResponse, UpdateMembreStatutRequest
│   ├── entity/         TontineMembre
│   ├── repository/     TontineMembreRepository
│   └── service/
│       ├── interfaces/ MembreService
│       └── impl/       MembreServiceImpl
├── contribution/   ✅ Complet (entités + repo + service + controller)
│   ├── codeList/       CotisationStatut
│   ├── controller/     CotisationController
│   ├── dto/            RecordCotisationRequest, CotisationResponse
│   ├── entity/         Cotisation
│   ├── repository/     CotisationRepository
│   └── service/
│       ├── interfaces/ CotisationService
│       └── impl/       CotisationServiceImpl
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
`/v1/auth/login`, `/v1/auth/logout`, `/v1/auth/register/**`,
`/v1/auth/forgot-password/**`, `/v1/auth/login-pin`, `/v1/auth/pin/reset/**`,
`/v1/code-list/type/**`

### Vérification des droits — pattern service

La vérification d'accès se fait dans la couche service (pas le contrôleur) :

```java
// Dans un ServiceImpl :
private void assertIsCreatorOrSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (!tontine.getCreePar().getId().equals(caller.getId()))
        throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
}

private boolean isSuperAdmin(User user) {
    return roleAssignmentRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN);
}
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
- **Phone** : toujours stocké SANS le `+` (normalisé via `PhoneUtils.normalize()` en entrée)

### ⛔ Imports — règle absolue : PAS de wildcard `*`

**Interdit** :
```java
import com.africa.dinthialma_backend.auth.dto.*;       // ❌
import jakarta.persistence.*;                           // ❌
import java.util.*;                                     // ❌
```

**Obligatoire** : chaque classe sur sa propre ligne :
```java
import com.africa.dinthialma_backend.auth.dto.LoginRequest;    // ✅
import jakarta.persistence.Column;                              // ✅
import java.util.List;                                          // ✅
```

**Pourquoi** : les imports `*` créent des conflits de noms silencieux (ex. `PhoneChangeRequest`
en `dto/` et en `entity/`). Java résout l'import explicite en priorité, rendant le comportement
imprévisible. Un conflit réel a déjà cassé la compilation dans ce projet.

### Pattern contrôleur type

```java
@RestController
@RequestMapping("/v1/module")
@RequiredArgsConstructor
@Tag(name = "Module", description = "...")
@SecurityRequirement(name = "bearerAuth")
public class ModuleController {

  private final ModuleService moduleService;
  private final RequestHeaderParser headerParser;

  @PostMapping
  @Operation(summary = "Créer ...", description = "🔒 Rôles : ADMIN, SUPER_ADMIN")
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateRequest request,
      HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    ModuleResponse response = moduleService.create(keycloakId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse("success", 201, MODULE_CREATE_SUCCESS, response));
  }
}
```

---

## Base de données

- **Schéma** : `dinthialma`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` — Flyway est **la seule** source de vérité

### Règles migrations

- PAS de `DEFAULT` ni `CHECK` SQL dans les migrations (géré Java/service)
- PAS de `uniqueConstraints` dans les annotations JPA (contraintes uniquement en SQL)
- UUIDs pour les colonnes de code list : fournis manuellement (pas `gen_random_uuid()`)
- Toujours utiliser `IF NOT EXISTS` pour les `CREATE TABLE` et `ADD COLUMN IF NOT EXISTS`

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

| Feature | Statut |
|---------|--------|
| Login (phone/email + password) | ✅ |
| Login PIN (WEB/MOBILE) | ✅ |
| Inscription OTP 3 étapes | ✅ |
| Reset mot de passe OTP | ✅ |
| Reset PIN OTP | ✅ |
| Refresh token | ✅ |
| Setup PIN | ✅ |
| PIN expiration 90 jours | ✅ |
| Lockout PIN 5 tentatives/30min | ✅ |

### Module Profil ✅ (complet)

| Feature | Statut |
|---------|--------|
| Voir son profil | ✅ |
| Modifier profil (nom, email) | ✅ |
| Changer de numéro (OTP) | ✅ |

> **Changement de numéro** : les tontines/memberships sont liés à l'`id` UUID, pas au numéro.
> Le changement ne casse aucune relation.

### Module Tontine ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Créer une tontine | ✅ | 🔒 tout auth |
| Lister tontines | ✅ | 🔒 SUPER_ADMIN : toutes ; autres : les leurs |
| Détail tontine | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Modifier tontine (BROUILLON) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Supprimer tontine (BROUILLON) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Activer tontine | ✅ | 🔒 créateur + SUPER_ADMIN |
| Suspendre tontine | ✅ | 🔒 créateur + SUPER_ADMIN |
| Génération auto cycles (AUTOMATIQUE) | ✅ | AUTO à l'activation |

### Module Membre ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Ajouter cotisant | ✅ | 🔒 créateur + SUPER_ADMIN |
| Retirer cotisant | ✅ | 🔒 créateur + SUPER_ADMIN |
| Liste cotisants | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Modifier statut (ACTIF/SUSPENDU/SORTI) | ✅ | 🔒 créateur + SUPER_ADMIN |

### Module Cotisation ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Enregistrer cotisation | ✅ | 🔒 cotisant lui-même |
| Valider cotisation | ✅ | 🔒 créateur + SUPER_ADMIN |
| Liste cotisations | ✅ | 🔒 MEMBER voit les siennes ; admin voit tout |
| Détail cotisation | ✅ | 🔒 propriétaire + créateur + SUPER_ADMIN |

### Module Cycle ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Liste cycles | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Détail cycle | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Ouvrir cycle (MANUEL) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Clôturer cycle | ✅ | 🔒 créateur + SUPER_ADMIN |
| Activation cycle suivant (AUTOMATIQUE) | ✅ | AUTO à la clôture |
| Calcul jackpot = somme cotisations VALIDEES | ✅ | AUTO à la clôture |
| EN_ATTENTE → EN_RETARD à la clôture | ✅ | AUTO à la clôture |

### Module Dashboard Admin 🔲 (prochain sprint)

| Feature | Statut | Accès |
|---------|--------|-------|
| Dashboard global (stats plateforme) | 🔲 TODO | 🔒 SUPER_ADMIN |
| Liste tous utilisateurs | 🔲 TODO | 🔒 SUPER_ADMIN |
| Désactiver/activer compte | 🔲 TODO | 🔒 SUPER_ADMIN |
| Modifier rôles | 🔲 TODO | 🔒 SUPER_ADMIN |
| Dashboard mes tontines (résumé admin) | 🔲 TODO | 🔒 ADMIN+ |
| Journal d'audit | 🔲 TODO | 🔒 SUPER_ADMIN |

### Module Notification 🔲

| Feature | Statut |
|---------|--------|
| SMS OTP | ✅ (mock dev) |
| Rappel cotisation | 🔲 TODO |
| Annonce bénéficiaire jackpot | 🔲 TODO |
| Alerte retard | 🔲 TODO |

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
4. Créer les rôles realm : `DINTHIALMA_USER`, `DINTHIALMA_MEMBER`, `DINTHIALMA_ADMIN`, `DINTHIALMA_SUPER_ADMIN`
5. Dans User Profile (realm settings) : ajouter l'attribut `phone`
6. Copier le client secret → `docker/.env` → `KEYCLOAK_CLIENT_SECRET`

---

## Variables d'environnement clés

| Variable | Défaut local | Description |
|----------|-------------|-------------|
| `KEYCLOAK_CLIENT_ID` | – | **Requis** |
| `KEYCLOAK_CLIENT_SECRET` | – | **Requis** |
| `KEYCLOAK_ADMIN_USER` | `admin` | Admin realm master (KeycloakClientConfig) |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Admin realm master |
| `BOOTSTRAP_ENABLED` | `true` | Créer SUPER_ADMIN au 1er démarrage |
| `BOOTSTRAP_SUPER_ADMIN_PHONE` | `221783703310` | Sans le `+` |
| `TOKEN_ENCRYPTION_KEY` | *(dev default)* | Clé AES-256 pour refresh tokens |

---

## Prochaines étapes recommandées

```
✦ Sprint 1 – Auth + Profil   ✅ TERMINÉ
✦ Sprint 2 – Tontine core    ✅ TERMINÉ
  → TontineService + TontineController  ✅
  → MembreService + MembreController    ✅
  → CotisationService + CotisationController ✅
  → CycleService + CycleController      ✅

✦ Sprint 3 – Dashboard Admin
  1. DashboardService (métriques SUPER_ADMIN + métriques admin tontine)
  2. AdminController (/v1/admin/dashboard)
  3. AdminUserController (activer/désactiver/roles)

✦ Sprint 4 – Notifications & Audit
  1. SchedulerService (rappels SMS cotisations, annonce bénéficiaire)
  2. AuditService (TontineAuditLog automatique sur mutations)
  3. TontineCommission (gestion commissions par tontine)
```
