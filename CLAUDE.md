# CLAUDE.md – dinthialma-backend

## Vue d'ensemble

**dinthialma-backend** est le backend de la plateforme Dinthialma : application de gestion de tontines et d'épargne collective permettant à des groupes de gérer leurs cotisations, bénéficiaires et historiques de manière numérique et transparente.

- **Stack** : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak
- **Port** : `8081` (local) · Context path : `/api`
- **Realm Keycloak** : `dinthialma`
- **Swagger UI** : `http://localhost:8081/api/swagger-ui.html`
- **Schéma DB** : `dinthialma`

---

## Architecture des modules

```
src/main/java/com/africa/dinthialma_backend/
├── auth/           ✅ Auth, inscription OTP, Keycloak, PIN
│   ├── codeList/       UserRole (SUPER_ADMIN, ADMIN, MEMBER, USER), ClientType (WEB, MOBILE)
│   ├── config/         KeycloakClientConfig, KeycloakProperties
│   ├── controller/     AuthController (login, logout, register, forgot-password, PIN)
│   ├── dto/            LoginRequest/Response, RegisterCompleteRequest/Response,
│   │                   SendOtpRequest, VerifyOtpRequest, LogoutRequest,
│   │                   ForgotPasswordRequest, ResetPasswordByPhoneRequest, RequestUser,
│   │                   PinSetupRequest, PinLoginRequest, PinResetRequest
│   ├── entity/         User, OtpVerification, UserRoleAssignment, UserSession
│   ├── repository/     UserRepository, OtpVerificationRepository,
│   │                   UserRoleAssignmentRepository, UserSessionRepository
│   └── service/
│       ├── interfaces/ KeycloakAuthService, RegistrationService,
│       │               PasswordResetService, PinService, UserSessionService
│       └── impl/       KeycloakAuthServiceImpl, RegistrationServiceImpl,
│                       PasswordResetServiceImpl, PinServiceImpl, UserSessionServiceImpl
├── tontine/        🔲 Gestion des tontines (entités créées)
│   ├── codeList/       TontineStatut, ModeCycle, CycleStatut
│   └── entity/         Tontine, CycleTontine
├── member/         🔲 Membres des tontines (entités créées)
│   ├── codeList/       MembreRole, MembreStatut
│   └── entity/         TontineMembre
├── contribution/   🔲 Cotisations (entités créées)
│   ├── codeList/       CotisationStatut
│   └── entity/         Cotisation
├── notification/   ✅ SMS (LAfricaMobile)
│   └── service/        SmsService
├── common/         ✅ BaseEntity, exceptions, CustomResponse, utils, CodeList, Audit
│   ├── audit/          ✅ TontineAuditLog, AuditAction
│   ├── base/           BaseEntity (id UUID, createdAt, updatedAt)
│   ├── codelist/       ✅ LaCodeList — entité, DTO, repo, service, controller
│   │   ├── controller/     LaCodeListController (GET public /type/{type}, CRUD admin)
│   │   ├── dto/            LaCodeListDto (from() + toEntity())
│   │   ├── entity/         LaCodeList (type, value, description, isSystemAssign)
│   │   ├── repository/     LaCodeListRepository (CrudRepository + findAllByType)
│   │   └── service/
│   │       ├── interfaces/ LaCodeListService
│   │       └── impl/       LaCodeListServiceImpl
│   ├── constants/      Constants, ResponseMessageConstants
│   ├── exception/      CustomException, ApiExceptionHandler + hiérarchie
│   ├── response/       CustomResponse
│   └── util/           RoleGuard, RequestHeaderParser (+ extractKeycloakId()),
│                       KeycloakJwtRolesConverter, OtpUtils, TokenEncryptionUtil
└── config/         ✅ SecurityConfig, OpenApiConfig, CustomEntryPoint/AccessDenied
```

---

## Sécurité & Rôles

### Deux filter chains Spring Security

| Ordre | Périmètre | Comportement |
|-------|-----------|--------------|
| 1 | Routes publiques (`WHITELIST`) | Aucun JWT requis |
| 2 | Toutes les autres routes | JWT Keycloak obligatoire |

Routes publiques : `/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/v1/auth/**` (sauf reset-password forcé admin).

### Rôles Keycloak (`UserRole` enum) — Realm `dinthialma`

| UserRole | Realm Keycloak | Périmètre |
|----------|----------------|-----------|
| `SUPER_ADMIN` | `DINTHIALMA_SUPER_ADMIN` | Accès total, administration plateforme |
| `ADMIN` | `DINTHIALMA_ADMIN` | Administrateur de groupe / tontine |
| `MEMBER` | `DINTHIALMA_MEMBER` | Membre d'une tontine |

### Vérification des rôles dans les contrôleurs

```java
// Exiger un rôle spécifique
RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.ADMIN);

// Exiger ADMIN ou SUPER_ADMIN
RequestUser caller = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

// Super admin uniquement
RequestUser caller = RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
```

---

## Conventions de code

- **Formatage** : Google Java Format via `fmt-maven-plugin` – `./mvnw fmt:apply` en local, `fmt:check` en CI
- **Lombok** : `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder` sur les entités ; `@RequiredArgsConstructor` sur les services et controllers
- **Entités** : toutes étendent `BaseEntity` (id UUID auto-généré, createdAt, updatedAt) via `@SuperBuilder`
- **Soft delete** : champ `deletedAt` (`LocalDateTime`, null = actif) – jamais de suppression physique
- **Réponses API** : toujours `CustomResponse(status, statusCode, message, data)`
- **Exceptions** : hiérarchie `CustomException` capturée par `ApiExceptionHandler` → réponse JSON normalisée
- **Services** : interface dans `service/interfaces/` + implémentation dans `service/impl/` ; `@Transactional` sur les méthodes d'écriture

### Pattern contrôleur type

```java
@RestController
@RequestMapping("/v1/module")
@RequiredArgsConstructor
@Tag(name = "...", description = "...")
public class ModuleController {

  private final ModuleService moduleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(summary = "...", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateRequest request,
      HttpServletRequest httpRequest) throws CustomException {

    RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.ADMIN);
    ModuleResponse response = moduleService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.CREATED,
            ResponseMessageConstants.MODULE_CREATE_SUCCESS,
            response));
  }
}
```

---

## Base de données

- **Schéma** : `dinthialma`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` — Flyway est **la seule** source de vérité pour le schéma
- Toute nouvelle table ou colonne = nouvelle migration versionnée

### Migrations actuelles

| Version | Fichier | Tables / contenu |
|---------|---------|-----------------|
| V001 | `create_schema.sql` | Schéma `dinthialma` |
| V002 | `create_users.sql` | `users` |
| V003 | `create_otp_verifications.sql` | `otp_verifications` |
| V004 | `create_la_code_list.sql` | `la_code_list` (table seule, sans seeds) |
| V005 | `create_tontines.sql` | `tontines` (statut, mode_cycle, cree_par) |
| V006 | `create_tontine_membres.sql` | `tontine_membres` (cotisants uniquement, sans colonne role) |
| V007 | `create_cycles_tontine.sql` | `cycles_tontine` (beneficiaire_id, montant_jackpot, date_remise) |
| V008 | `create_cotisations.sql` | `cotisations` (statut EN_ATTENTE→VALIDE, valide_par) |
| V009 | `create_tontine_audit_log.sql` | `tontine_audit_log` (table_name, record_id, action, champ, ancienne_val, nouvelle_val) |
| V010 | `seed_frequence_tontine.sql` | Seeds FREQUENCE_TONTINE (5 valeurs, UUIDs fixes) |
| V011 | `seed_methode_paiement.sql` | Seeds METHODE_PAIEMENT (4 valeurs, UUIDs fixes) |
| V012 | `seed_statut_cotisation.sql` | Seeds STATUT_COTISATION (3 valeurs, UUIDs fixes) |
| V013 | `seed_ordre_beneficiaire.sql` | Seeds ORDRE_BENEFICIAIRE (3 valeurs, UUIDs fixes) |
| V014 | `create_user_roles.sql` | `user_roles` (rôles multiples, synced_to_keycloak) |
| V015 | `create_tontine_commissions.sql` | `tontine_commissions` (POURCENTAGE_JACKPOT, FRAIS_FIXES_PAR_CYCLE, FRAIS_ADHESION) |
| V016 | `create_user_sessions.sql` | `user_sessions` (refresh_token_hash, device_info, last_used_at, expires_at) |
| V017 | `alter_user_sessions_token_column.sql` | `user_sessions.refresh_token_hash` → TEXT (stocke AES-256-GCM chiffré) |

Prochaine version disponible : **V018**

### Règle seeds codelist
Chaque type de codelist = **1 migration dédiée**. Les UUIDs sont fournis manuellement (pas de gen_random_uuid). Template :
```sql
INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('<uuid>', 'MON_TYPE', 'MA_VALEUR', 'Mon libellé', TRUE);
```

---

## Infrastructure locale (Docker)

```
docker/docker-compose.yml   # PostgreSQL, Keycloak, pgAdmin, Dozzle
                            # + profil full-stack  : Backend
                            # + profil monitoring  : Prometheus, Grafana
```

### Ports (évitent les conflits avec ubax-platform)

| Service | Port hôte |
|---------|-----------|
| Backend API | `8081` |
| PostgreSQL | `5434` |
| Keycloak | `8280` |
| pgAdmin | `5051` |
| Dozzle | `8889` |
| Prometheus | `9091` |
| Grafana | `3002` |

```bash
# Infrastructure seule (PostgreSQL + Keycloak + pgAdmin + Dozzle)
cd docker && docker compose up -d

# Avec le backend compilé
cd docker && docker compose --profile full-stack up -d

# Avec le monitoring
cd docker && docker compose --profile monitoring up -d

# Démarrage IDE (recommandé en dev)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Prérequis Keycloak

Après `docker compose up -d`, configurer le realm dans l'interface Keycloak :
1. Aller sur http://localhost:8280
2. Créer le realm `dinthialma`
3. Créer le client `dinthialma-client` (type `confidential`, `client_credentials`)
4. Créer les rôles realm : `DINTHIALMA_SUPER_ADMIN`, `DINTHIALMA_ADMIN`, `DINTHIALMA_MEMBER`
5. Copier le client secret dans `docker/.env` → `KEYCLOAK_CLIENT_SECRET`

---

## Variables d'environnement clés

| Variable | Défaut local | Description |
|----------|-------------|-------------|
| `KEYCLOAK_CLIENT_ID` | – | **Requis** – client Keycloak |
| `KEYCLOAK_CLIENT_SECRET` | – | **Requis** – secret client |
| `BOOTSTRAP_ENABLED` | `true` | Créer le super admin au 1er démarrage |
| `SECURITY_ENABLED` | `true` | Mettre à `false` uniquement en dev |
| `DB_HOST/PORT/NAME` | `localhost/5434/db-dinthialma` | PostgreSQL |

Voir `docker/.env.example` pour la liste complète.

---

## CI/CD (GitHub Actions)

| Workflow | Déclencheur | Stages |
|----------|-------------|--------|
| `ci-branches.yml` | Push `feature/**`, `hotfix/**`, `bugfix/**` | Format → Test |
| `docker-publish.yml` | Push `main`, `develop` | Format → Test → Build → Push Docker Hub → Deploy VPS → Health Check |

### Secrets GitHub requis

```
DOCKER_HUB_USERNAME   · DOCKER_HUB_TOKEN
VPS_HOST              · VPS_USER          · VPS_SSH_PRIVATE_KEY
```

---

## Fonctionnalités MVP à développer

| Module | Statut | Description |
|--------|--------|-------------|
| Auth | ✅ Complet | Login, inscription OTP, reset mot de passe, PIN WEB+MOBILE |
| Tontine | 🔶 Entités OK | CRUD tontines, activation, génération cycles |
| Membre | 🔶 Entités OK | Ajout/retrait membres, ordre bénéficiaires |
| Cotisation | 🔶 Entités OK | Enregistrement, validation manuelle, retards |
| Jackpot (Cycles) | 🔶 Entités OK | Prochain bénéficiaire, historique remises |
| Audit | 🔶 Entités OK | Traçabilité des modifications (AuditService à créer) |
| Notification | 🔲 TODO | Rappels SMS, annonce bénéficiaire |

### Prochaines étapes recommandées (V010+)
1. Repositories JPA pour chaque entité
2. Services : TontineService (créer, activer, générer cycles)
3. Services : MembreService (ajouter, gérer ordre jackpot)
4. Services : CotisationService (enregistrer, valider, marquer retard)
5. Controllers + DTOs pour chaque module
6. AuditService (logger automatiquement les changements de statut)
