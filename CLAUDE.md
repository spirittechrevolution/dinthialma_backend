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

### Statut de compte (`AccountStatus`)

| Statut | Keycloak | Peut se connecter | Déclenché par |
|--------|----------|-------------------|---------------|
| `ACTIVE` | ✅ compte existant | ✅ | Inscription normale ou activation PRE_ENROLLED |
| `PRE_ENROLLED` | ❌ pas de compte | ❌ (403 avec message explicite) | Admin tontine via `POST /membres` avec numéro inconnu |

> Un compte **PRE_ENROLLED** est créé automatiquement quand un gestionnaire ajoute un membre par numéro de téléphone et que ce numéro n'est pas encore inscrit sur la plateforme. Il est visible dans la tontine immédiatement. Quand la personne s'inscrit avec ce numéro, le compte est **activé** (rôles USER + MEMBER attribués, keycloakId renseigné) sans perte des memberships existants.

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
| `/register/complete` | POST | ✅ | – | – | – | – | finalise inscription ; avec `clientType` (+`deviceInfo`) : connexion automatique, jetons à plat dans la réponse comme `/login` |
| `/forgot-password/send-otp` | POST | ✅ | – | – | – | – | anti-énumération |
| `/forgot-password/verify-otp` | POST | ✅ | – | – | – | – | |
| `/forgot-password/reset` | POST | ✅ | – | – | – | – | |
| `/pin/setup` | POST | ❌ | ✅ | ✅ | ✅ | ✅ | 🔒 JWT requis |
| `/login-pin` | POST | ✅ | ✅ | ✅ | ✅ | ✅ | nécessite session active |
| `/pin/reset/send-otp` | POST | ✅ | – | – | – | – | |
| `/pin/reset/verify-otp` | POST | ✅ | – | – | – | – | |
| `/pin/reset` | POST | ✅ | – | – | – | – | |

### 🔍 Utilisateurs (`/v1/users`)

| Endpoint | Méthode | Public | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|--------|------|--------|-------|-------------|-------|
| `/search?phone=xxx` | GET | ❌ | ✅ | ✅ | ✅ | ✅ | 🔒 couvre ACTIVE + PRE_ENROLLED, exclut soft-deleted ; 404 si inconnu |

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
| `/` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 SUPER_ADMIN voit tout ; autres voient les leurs ; `?role=GERE\|COTISE` ; chaque tontine porte `monRole`, `monMembreId`, `monStatutMembre` |
| `/{id}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 ¹membre de la tontine ²créateur |
| `/{id}` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 BROUILLON seulement |
| `/{id}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 soft delete, BROUILLON seulement |
| `/{id}/activer` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 BROUILLON/SUSPENDUE → ACTIVE |
| `/{id}/suspendre` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 ACTIVE → SUSPENDUE |

### 👥 Membres (`/v1/tontines/{tontineId}/membres`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 filtres `?statut=`, `?q=` (nom, téléphone), `?cycleId=&aPaye=true\|false` (cotisation active ou non sur le cycle ; cycle hors tontine → 404) |
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 ajout par **phone** (+ firstName/lastName si numéro inconnu) **ou** par **qrToken** `m_…` (QR scanné, compte existant, numéro jamais exposé) ; exactement un des deux (`MEMBER_IDENTIFIER_REQUIRED`) |
| `/{membreId}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 soft delete, statut → SORTI |
| `/{membreId}/statut` | PATCH | ❌ | ❌ | ✅² | ✅ | 🔒 ACTIF / SUSPENDU / SORTI |

### 💰 Cotisations (`/v1/tontines/{tontineId}/cotisations`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 ¹MEMBER voit les siennes seulement ; filtres `?cycleId=`, `?membreId=` (admin uniquement, ignoré pour MEMBER), `?statut=` (multi) et `?q=` (nom, téléphone, référence) — filtre tontine toujours appliqué |
| `/{id}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/` | POST | ❌ | ✅¹ | ❌ | ✅ | 🔒 le cotisant enregistre son paiement |
| `/admin` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 saisie admin pour n'importe quel membre (y compris PRE_ENROLLED), créée directement VALIDE |
| `/{id}` | PATCH | ❌ | ❌ | ✅² | ✅ | 🔒 correction (champs non-null) : EN_ATTENTE ou VALIDE + cycle EN_COURS ; refusé si cycle TERMINE ou EN_RETARD |
| `/{id}/valider` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 EN_ATTENTE → VALIDE |
| `/{id}/refuser` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 EN_ATTENTE → REFUSE, `motif` obligatoire ; notifie le membre (`COTISATION_REFUSEE`) ; le membre peut redéclarer sur le même cycle |
| `/recap/{cycleId}` | GET | ❌ | ❌ | ✅² | ✅ | 🔒 statut de cotisation par membre pour un cycle (pageable, SORTI exclus) |
| `/recap-total` | GET | ❌ | ❌ | ✅² | ✅ | 🔒 total VALIDÉ cumulé par membre, tous cycles (pageable, SORTI exclus) |

### 🔄 Cycles (`/v1/tontines/{tontineId}/cycles`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/{cycleId}` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/beneficiaires` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 historique des cycles TERMINE avec gagnants (pageable, tri numeroCycle ASC) |
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 mode MANUEL uniquement |
| `/{cycleId}/cloturer` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 EN_COURS → TERMINE, calcul jackpot + commissions |
| `/{cycleId}/beneficiaire` | PATCH | ❌ | ❌ | ✅² | ✅ | 🔒 ordreBeneficiaire MANUEL + cycle TERMINE sans gagnant ; `membreIds` vide = tirage aléatoire de `nombreGagnants` ; refuse doublons, membres non ACTIF et membres `aRecuJackpot` |

### 💸 Commissions (`/v1/tontines/{tontineId}/commissions`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | POST | ❌ | ❌ | ✅² | ✅ | 🔒 max 1 par type |
| `/` | GET | ❌ | ✅¹ | ✅² | ✅ | 🔒 |
| `/{commissionId}` | PUT | ❌ | ❌ | ✅² | ✅ | 🔒 valeur + description |
| `/{commissionId}` | DELETE | ❌ | ❌ | ✅² | ✅ | 🔒 soft delete |

> ¹ = membre de cette tontine · ² = créateur de cette tontine

### 📊 Dashboard Admin (`/v1/admin`)

| Endpoint | Méthode | ADMIN | SUPER_ADMIN | Description |
|----------|---------|-------|-------------|-------------|
| `/dashboard` | GET | ❌ | ✅ | Statistiques globales plateforme |
| `/dashboard/tontines` | GET | ❌ | ✅ | Toutes tontines + statuts (pageable, filtres `?statut=`, `?q=`) |
| `/dashboard/users` | GET | ❌ | ✅ | Tous les utilisateurs (pageable, filtres `?accountStatus=ACTIVE|PRE_ENROLLED` (ACTIVE inclut NULL), `?role=`, `?q=` prénom/nom/téléphone/email) |
| `/dashboard/users/{id}` | GET | ❌ | ✅ | Détail utilisateur |
| `/dashboard/users/{id}/disable` | POST | ❌ | ✅ | Désactiver un compte |
| `/dashboard/users/{id}/enable` | POST | ❌ | ✅ | Réactiver un compte |
| `/dashboard/users/{id}/roles` | PUT | ❌ | ✅ | Modifier les rôles |
| `/dashboard/contributions` | GET | ❌ | ✅ | Toutes cotisations (pageable, filtres cumulables `?statut=` `?tontineId=`) |
| `/my-dashboard` | GET | ✅ | ✅ | Métriques de l'admin pour ses tontines (`tontines` = `PagedModel`) + agrégats globaux : `nombreTontinesGerees`, `montantTotalValide`, `totalMembres`, `totalCotisationsEnAttente/EnRetard`, `totalCollecteToursEnCours`, `totalAttenduToursEnCours` ; par tontine : `gagnantsADesigner` + `cycleEnCours` (`montantCollecte`, `montantEnAttente`, `montantAttendu`, `membresAyantPaye`, `membresEnAttente`, `membresActifs`) |

### 🙋 Mon espace (`/v1/me`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/summary` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 résumé « Je cotise » : par tontine où je cotise (membership, totaux, tours, tour EN_COURS, `cotisationDue` A_PAYER/EN_ATTENTE/VALIDE, gestionnaire) + totaux et jackpots reçus ; liste non paginée |
| `/gestion/cotisations` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 paiements de **toutes les tontines que j'ai créées** (file « À valider »), filtres `?statut=` (multi), `?tontineId=`, `?q=` ; pageable |
| `/gestion/cotisations/valider` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 validation groupée `{cotisationIds}` (1–100) ; chaque cotisation dans sa propre transaction, résultat par id avec `code` d'échec ; toujours 200 |
| `/jackpots` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 jackpots reçus + `aVenir` (ROTATION : rang, tour et date estimés ; TIRAGE / CHOIX : sans estimation) |
| `/activite` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 fil cotisations + jackpots (UNION native, tri date DESC imposé), `?mode=COTISE\|GERE`, `?tontineId=`, `?type=`, `?statut=` |
| `/qr` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 mon QR : jeton opaque stable `m_…` + url (pas de téléphone) |
| `/qr/regenerer` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 nouveau jeton, l'ancien → 404 |
| `/activite/export` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 export CSV du fil (mêmes filtres), `;` + BOM UTF-8, 10 000 lignes max |
| `/password` | POST | ✅ | ✅ | ✅ | ✅ | 🔒 changer de mot de passe (ancien vérifié) ; `deconnecterAutresAppareils` ; codes `CURRENT_PASSWORD_INVALID`, `PASSWORD_UNCHANGED` |
| `/sessions` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 appareils connectés (1 session par clientType), `courante` via le `sid` du JWT |
| `/sessions/{id}` | DELETE | ✅ | ✅ | ✅ | ✅ | 🔒 déconnexion à distance (révocation Keycloak + suppression) ; 404 si pas à moi |
| `/preferences` | GET / PUT | ✅ | ✅ | ✅ | ✅ | 🔒 préférences synchronisées (JSONB) : favoris (50 max), dernierMode, masquerMontant, dernierMoyenPaiement |
| `/gestion/demandes` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 demandes d'adhésion reçues sur mes tontines (`?statut=`, `?tontineId=`), avec téléphone |
| `/demandes` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 mes demandes d'adhésion envoyées |

### ✉️ Invitations et demandes d'adhésion

| Endpoint | Méthode | Accès | Notes |
|----------|---------|-------|-------|
| `/v1/tontines/{id}/invitation` | POST | créateur, SUPER_ADMIN | code `XXXX-XXXX` + url + qrToken `t_<code>` ; `?regenerer=true` ; tontine BROUILLON/ACTIVE |
| `/v1/tontines/{id}/invitation` | DELETE | créateur, SUPER_ADMIN | révoque (lien et QR → 404) |
| `/v1/public/invitations/{code}` | GET | 🌐 public | aperçu minimal sans données des membres ; `placesRestantes`, `ouverte` |
| `/v1/invitations/{code}/demandes` | POST | tout auth | demande EN_ATTENTE ; `DEMANDE_DEJA_EN_ATTENTE`, `MEMBER_ALREADY_IN_TONTINE`, `TONTINE_FULL`, `TONTINE_NOT_OPEN` |
| `/v1/tontines/{id}/demandes/{demandeId}/accepter` | PUT | créateur, SUPER_ADMIN | réutilise l'ajout de membre (`ordreJackpot` optionnel) ; `DEMANDE_NOT_PENDING` |
| `/v1/tontines/{id}/demandes/{demandeId}/refuser` | PUT | créateur, SUPER_ADMIN | motif optionnel, notification `ADHESION_REFUSEE` |

### 🌐 Public / divers

| Endpoint | Méthode | Accès | Notes |
|----------|---------|-------|-------|
| `/v1/public/config` | GET | 🌐 public | `supportWhatsapp`, `supportEmail`, `versionMinimaleApp` (bloquante), `versionRecommandeeApp` (bandeau) — env `SUPPORT_WHATSAPP`, `SUPPORT_EMAIL`, `APP_VERSION_MINIMALE`, `APP_VERSION_RECOMMANDEE` ; champ absent si non configuré ; versions en SemVer strict `X.Y.Z`, sinon le backend refuse de démarrer |
| `/v1/tontines/{id}/cotisations/export` | GET | créateur, SUPER_ADMIN | export CSV (`format=csv`, `cycleId`, `statut`) |
| `/v1/code-list/{id}` | DELETE | SUPER_ADMIN | suppression logique ; valeurs système refusées (`CODE_LIST_SYSTEM_VALUE`) ; recréer restaure |

> `GET /v1/qr/{token}` (tout auth) : résout un QR membre → identité sans téléphone + `dansMesTontines` (tontines gérées par l'appelant : membreId, statut, `aPayeCycleEnCours`). Préfixe `t_` réservé aux invitations (B16).

### 🔔 Notifications (`/v1/notifications`)

| Endpoint | Méthode | USER | MEMBER | ADMIN | SUPER_ADMIN | Notes |
|----------|---------|------|--------|-------|-------------|-------|
| `/` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 notifications paginées (lues + non lues, tri createdAt DESC) ; liens `tontineId`, `cycleId`, `cotisationId`, `membreId` quand ils existent |
| `/unread-count` | GET | ✅ | ✅ | ✅ | ✅ | 🔒 compteur badge cloche |
| `/{id}/read` | PATCH | ✅ | ✅ | ✅ | ✅ | 🔒 marquer une notification lue (idempotent) |
| `/read-all` | PATCH | ✅ | ✅ | ✅ | ✅ | 🔒 tout marquer lu en une opération (bulk) |

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
│   ├── codeList/       UserRole, ClientType, AccountStatus
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
│   ├── codeList/       TontineStatut, ModeCycle, CycleStatut, CommissionType, TontineType
│   ├── controller/     TontineController, CycleController, CommissionController
│   ├── dto/            CreateTontineRequest, UpdateTontineRequest, TontineResponse,
│   │                   CycleResponse (+ MembreDistributionInfo), OpenCycleRequest,
│   │                   CreateCommissionRequest, UpdateCommissionRequest, CommissionResponse
│   ├── entity/         Tontine, CycleTontine, TontineCommission
│   ├── repository/     TontineRepository, CycleTontineRepository,
│   │                   TontineCommissionRepository
│   └── service/
│       ├── interfaces/ TontineService, CycleService, CommissionService
│       └── impl/       TontineServiceImpl, CycleServiceImpl, CommissionServiceImpl
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
├── notification/   ✅ SMS (LAfricaMobile) + notifications in-app
│   ├── codeList/       NotificationType (15 valeurs)
│   ├── controller/     NotificationController
│   ├── dto/            NotificationResponse
│   ├── entity/         UserNotification
│   ├── repository/     NotificationRepository
│   └── service/
│       ├── interfaces/ NotificationService
│       ├── impl/       NotificationServiceImpl
│       └── (directs)   SmsService, WhatsappService, SchedulerService
├── me/             ✅ Vues transverses de l'utilisateur connecté (/v1/me, /v1/qr)
│   ├── codeList/       StatutCotisationDue, ModeActivite, TypeActivite
│   ├── controller/     MeController, QrController, CompteController
│   ├── dto/            MeSummaryResponse, MesJackpotsResponse, ActiviteResponse,
│   │                   ValidationGroupeeRequest/Response, MonQrResponse, QrResolutionResponse
│   ├── repository/     ActiviteRepository (requête native UNION)
│   └── service/        MeService(+Impl), QrService(+Impl)
├── adhesion/       ✅ Invitations par code + demandes d'adhésion (B16/B17)
│   ├── codeList/       StatutDemande
│   ├── controller/     AdhesionController
│   ├── dto/            InvitationResponse, InvitationApercuResponse,
│   │                   DemandeAdhesionResponse, DemandeRequests
│   ├── entity/         DemandeAdhesion
│   ├── repository/     DemandeAdhesionRepository
│   └── service/        AdhesionService(+Impl)
├── admin/          ✅ Complet (GlobalDashboard + gestion users + MyDashboard)
│   ├── controller/     AdminDashboardController
│   ├── dto/            GlobalDashboardResponse, AdminUserResponse,
│   │                   UpdateUserRolesRequest, MyDashboardResponse
│   └── service/
│       ├── interfaces/ AdminDashboardService
│       └── impl/       AdminDashboardServiceImpl
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
`/v1/auth/login`, `/v1/auth/logout`, `/v1/auth/refresh`, `/v1/auth/register/**`,
`/v1/auth/forgot-password/**`, `/v1/auth/login-pin`, `/v1/auth/pin/reset/**`,
`/v1/code-list/type/**`, `/v1/public/config`, `/v1/public/invitations/**`

> ⚠️ Les routes de `WHITELIST` (`SecurityConfig`) doivent porter le préfixe complet `/v1/...`
> (le context path `/api` est exclu). Une entrée sans préfixe est silencieusement inopérante.

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

Pour une simple exigence de rôle (sans notion de tontine), utiliser le composant
`common/security/AccessControl` : `exigerUnRole(keycloakId, UserRole...)` (403 sinon),
`utilisateur(keycloakId)` (404), `estSuperAdmin(user)`.

### Contrôleurs — aucun traitement

Un contrôleur extrait le `keycloakId`, appelle **un** service et enveloppe sa réponse dans
`CustomResponse`. Aucun `if`, `try`, accès repository, mapping ou contrôle de droits dans
un contrôleur. Exports CSV : le service renvoie un `FichierExport`, le contrôleur répond
avec `CsvExport.reponse(fichier)`.

---

## Conventions de code

- **Formatage** : Google Java Format via `fmt-maven-plugin` – `./mvnw fmt:apply`
- **Lombok** : `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder` entités ; `@RequiredArgsConstructor` services/controllers
- **Entités** : toutes étendent `BaseEntity` (id UUID, createdAt, updatedAt)
- **Soft delete** : `deletedAt` (`LocalDateTime`, null = actif) – jamais de suppression physique
- **Réponses API** : toujours `CustomResponse(status, statusCode, message, data)`
- **Exceptions** : hiérarchie `CustomException` → `ApiExceptionHandler` → JSON normalisé
- **Codes d'erreur** : toute erreur porte `code` (enum `ErrorCode`), absent sur les succès.
  Pour un cas que le front doit distinguer, lever l'exception avec un code métier
  (`new ConflictException(msg, ErrorCode.COTISATION_ALREADY_EXISTS)`) ; sinon le handler
  déduit un code générique du statut (`NOT_FOUND`, `FORBIDDEN`…). Détails éventuels dans
  `data` (ex. `PIN_LOCKED` → `data.retryAfterSeconds`). On **ajoute** des valeurs à
  `ErrorCode`, on n'en renomme ni n'en supprime jamais.
- **Services** : interface dans `service/interfaces/` + impl dans `service/impl/` ; `@Transactional` sur écriture
- **Mapping** : MapStruct uniquement, un mapper par module dans `<module>/mapper/` (bean Spring
  injecté dans le service). Pas de méthode statique `from()` / `toEntity()` dans les DTOs.
  `unmappedTargetPolicy=ERROR` : tout champ de DTO non alimenté fait échouer la compilation —
  le mapper ou `@Mapping(target = "…", ignore = true)` doit être explicite. Enrichissement après
  mapping : `@Builder(toBuilder = true)` sur le DTO et méthode `default` dans le mapper.
- **Phone** : toujours stocké SANS le `+` (normalisé via `PhoneUtils.normalize()` en entrée)
- **JPQL** : pas de `SELECT DISTINCT` dans les repositories — PostgreSQL gère la déduplication nativement

### Pagination — pattern standard

Tous les endpoints de liste retournent `Page<T>` (Spring Data) :
- Contrôleur : `@PageableDefault(size = 20, ...)` avec le tri naturel du domaine
- Service interface : `Page<T>` en retour, `Pageable` en paramètre
- Repository : surcharge `Page<T> findBy...(... Pageable)` à côté de la version `List<T>` conservée pour usage interne

**Defaults par domaine :**

| Domaine | Sort par défaut | Direction |
|---------|----------------|-----------|
| Tontines | `createdAt` | DESC |
| Cycles | `numeroCycle` | ASC |
| Membres | `ordreJackpot` | ASC |
| Cotisations | `createdAt` | DESC |
| Récaps cotisations par membre | `ordreJackpot`, puis `createdAt` | ASC, ASC |
| Dashboard admin (`my-dashboard`, `/dashboard/tontines`, `/dashboard/contributions`) | `createdAt` | DESC |
| Commissions | `createdAt` | ASC |
| Users (admin) | `createdAt` | DESC |
| Notifications | `createdAt` | DESC |

Query params client : `?page=0&size=20&sort=createdAt,desc`

Sérialisation : `PageSerializationMode.VIA_DTO` → `{ content: [...], page: { size, number, totalElements, totalPages } }`.
Une page imbriquée dans un DTO doit être typée `PagedModel<T>` (pas `Page<T>`) pour que Springdoc documente le vrai format.

> ⚠️ **Booléens préfixés** (`aRecuJackpot`…) : le getter Lombok `isARecuJackpot()` est sérialisé `arecuJackpot`.
> Déclarer un getter explicite annoté `@JsonProperty("aRecuJackpot")` (cf. `MembreResponse`).
> Même piège pour tout champ commençant par « a » + majuscule (`aVenir`, `aPayeCycleEnCours`).

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

## Tests

Arborescence imposée de `src/test/java/com/africa/dinthialma_backend/` — **trois répertoires
à plat, aucun sous-répertoire** :

```
support/       commun aux deux types : Fixtures, AbstractIntegrationTest, ApiClient, TestDataFactory
integration/   tous les tests d'intégration   (*IT.java)   → ./mvnw verify (Docker requis)
service/       tous les tests unitaires       (*Test.java) → ./mvnw test
```

**`./mvnw clean install`** lance tout : unitaires, puis intégration, puis seuil JaCoCo. **Docker
obligatoire** : sans Docker démarré, le build échoue (les `*IT` ne sont jamais sautés en silence).
`-DskipITs` = unitaires seuls · `-DskipTests` = aucun test (Dockerfile).
**CI GitHub** (`ci-branches.yml`, `docker-publish.yml`) : `./mvnw verify -DskipITs` → tests unitaires
+ seuil JaCoCo à chaque push ; les tests d'intégration ne tournent qu'en local.

- **Unitaires** (`service/`, Mockito + AssertJ) : un fichier par service, dépendances simulées,
  mappers MapStruct réels (`Mappers.getMapper(...)` en `@Spy`), jeux de données `support/Fixtures`.
  Les tests étant hors du paquet du service, on ne teste que l'API publique (pas de méthode
  package-private). Les appels HTTP sortants (Keycloak, WhatsApp, SMS) sont testés contre un
  faux serveur `HttpServer` local.
- **Intégration** (`integration/`, Testcontainers PostgreSQL 17 + Keycloak 26) : hériter de
  `support/AbstractIntegrationTest` (conteneurs partagés, profil `test`, SMS/WhatsApp simulés) ;
  appels via `ApiClient`, données via `TestDataFactory`. Realm importé depuis
  `src/test/resources/keycloak/`.
- Après un déplacement de tests, lancer `./mvnw clean verify` (sinon les anciennes classes
  compilées de `target/test-classes` sont rejouées en double).
- **Couverture (JaCoCo)** : `./mvnw verify` échoue si un paquet `*.service`, `*.service.impl`
  ou `*.security` passe sous **90 % d'instructions** ou **80 % de branches** (tests unitaires).
  Rapport : `target/site/jacoco/index.html`.
- Tout nouveau service ou nouvelle règle métier arrive avec ses tests unitaires.

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
| V020 | `add_commission_to_cycles.sql` | `cycles_tontine.montant_commission`, `montant_net` |
| V021 | `add_account_status_to_users.sql` | `users.account_status`, `keycloak_id` nullable |
| V022 | `add_enregistre_par_to_cotisations.sql` | `cotisations.enregistre_par` |
| V023 | `add_jackpot_tracking_to_membres.sql` | `tontine_membres.a_recu_jackpot`, `date_jackpot` |
| V024 | `add_nombre_gagnants_and_cycle_gagnants.sql` | `tontines.nombre_gagnants`, table `cycle_gagnants` |
| V025 | `add_evenementielle_fields_to_tontines.sql` | `tontines.tontine_type`, `date_echeance`, `nom_evenement`, `montant_libre`, `montant_minimum` ; `ordre_beneficiaire` nullable |
| V026 | `create_user_notifications.sql` | `user_notifications` (id, user_id FK, type, title, body, is_read, tontine_id, timestamps, deleted_at) ; index sur (user_id, is_read) et createdAt DESC |
| V027 | `add_refus_to_cotisations.sql` | `cotisations.motif_refus`, `refuse_par`, `date_refus` ; `uq_cot_cycle_membre` remplacée par l'index unique partiel `uq_cot_cycle_membre_actif` (`WHERE deleted_at IS NULL AND statut <> 'REFUSE'`) |
| V028 | `add_liens_to_user_notifications.sql` | `user_notifications.cycle_id`, `cotisation_id`, `membre_id` (liens précis, sans FK) |
| V029 | `add_qr_token_to_users.sql` | `users.qr_token` + index unique partiel (QR membre opaque) |
| V030 | `add_deleted_at_to_la_code_list.sql` | `la_code_list.deleted_at` (suppression logique) |
| V031 | `add_preferences_to_users.sql` | `users.preferences` JSONB |
| V032 | `create_invitations_et_demandes_adhesion.sql` | `tontines.code_invitation` (unique) + table `demandes_adhesion` (1 EN_ATTENTE max par personne/tontine) |

Prochaine version disponible : **V033**

---

## Fonctionnalités & Statut

### Module Auth ✅ (complet)

| Feature | Statut |
|---------|--------|
| Login (phone/email + password) | ✅ |
| Login PIN (WEB/MOBILE) | ✅ |
| Inscription OTP 3 étapes | ✅ |
| Activation compte PRE_ENROLLED à l'inscription | ✅ |
| Blocage login si PRE_ENROLLED (message d'invite) | ✅ |
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
| Créer une tontine (ROTATIVE ou EVENEMENTIELLE) | ✅ | 🔒 tout auth |
| Lister tontines | ✅ | 🔒 SUPER_ADMIN : toutes ; autres : les leurs |
| Détail tontine | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Modifier tontine (BROUILLON) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Supprimer tontine (BROUILLON) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Activer tontine | ✅ | 🔒 créateur + SUPER_ADMIN |
| Suspendre tontine | ✅ | 🔒 créateur + SUPER_ADMIN |
| Génération auto cycles (ROTATIVE AUTOMATIQUE) | ✅ | AUTO à l'activation |
| Génération auto sous-cycles (EVENEMENTIELLE) | ✅ | AUTO à l'activation – dateDebut → dateEcheance par fréquence |

### Module Membre ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Recherche utilisateur par téléphone (lookup) | ✅ | 🔒 tout auth — `GET /v1/users/search?phone=xxx` |
| Ajout membre par téléphone (+ nom si inconnu) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Validation conditionnelle : firstName/lastName requis seulement si numéro inconnu | ✅ | SERVICE |
| Création automatique compte PRE_ENROLLED si numéro inconnu | ✅ | AUTO |
| Complétion du nom si PRE_ENROLLED existant avec nom vide | ✅ | AUTO |
| SMS invitation envoyé au membre pré-inscrit | ✅ | AUTO (non-bloquant) |
| Retirer cotisant | ✅ | 🔒 créateur + SUPER_ADMIN |
| Liste cotisants (avec accountStatus, `aRecuJackpot`, `dateJackpot` exposés) | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Modifier statut (ACTIF/SUSPENDU/SORTI) | ✅ | 🔒 créateur + SUPER_ADMIN |

### Module Cotisation ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Enregistrer cotisation | ✅ | 🔒 cotisant lui-même |
| Saisie admin directe (VALIDE, membres PRE_ENROLLED inclus) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Modifier cotisation (PATCH, selon statut / cycle) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Valider cotisation | ✅ | 🔒 créateur + SUPER_ADMIN |
| Refuser cotisation (motif, redéclaration possible) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Liste cotisations (filtres cycle + membre) | ✅ | 🔒 MEMBER voit les siennes ; admin voit tout |
| Détail cotisation | ✅ | 🔒 propriétaire + créateur + SUPER_ADMIN |
| Récapitulatif par membre pour un cycle (paginé) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Total cumulé validé par membre (paginé) | ✅ | 🔒 créateur + SUPER_ADMIN |

### Module Cycle ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Liste cycles | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Détail cycle | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Ouvrir cycle (ROTATIVE MANUEL uniquement) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Clôturer cycle (ROTATIVE) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Désigner les gagnants (MANUEL ou tirage aléatoire) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Historique des bénéficiaires (paginé) | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Clôturer sous-cycle (EVENEMENTIELLE) | ✅ | 🔒 créateur + SUPER_ADMIN – activation sous-cycle suivant |
| Clôture finale EVENEMENTIELLE | ✅ | AUTO détection dernier cycle – `distributionParMembre` calculée |
| Calcul jackpot brut = somme cotisations VALIDEES | ✅ | AUTO à la clôture |
| Déduction commissions → montantNet | ✅ | AUTO à la clôture |
| Commission proportionnelle par membre (EVENEMENTIELLE) | ✅ | AUTO à la clôture finale |
| EN_ATTENTE → EN_RETARD à la clôture | ✅ | AUTO à la clôture |
| Notification WhatsApp distribution finale | ✅ | AUTO – un message par membre + résumé admin |

### Module Commission ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Créer une commission (max 1 par type) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Lister commissions actives | ✅ | 🔒 membres + créateur + SUPER_ADMIN |
| Modifier commission (valeur + description) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Supprimer commission (soft delete) | ✅ | 🔒 créateur + SUPER_ADMIN |
| Calcul automatique à la clôture du cycle | ✅ | AUTO — FRAIS_ADHESION exclus du jackpot |

### Module Dashboard Admin ✅ (complet)

| Feature | Statut | Accès |
|---------|--------|-------|
| Dashboard global (stats plateforme) | ✅ | 🔒 SUPER_ADMIN |
| Liste tous utilisateurs (pageable) | ✅ | 🔒 SUPER_ADMIN |
| Liste toutes tontines (pageable, filtre statut) | ✅ | 🔒 SUPER_ADMIN |
| Liste toutes cotisations (pageable, filtres statut + tontine) | ✅ | 🔒 SUPER_ADMIN |
| Détail utilisateur | ✅ | 🔒 SUPER_ADMIN |
| Désactiver compte | ✅ | 🔒 SUPER_ADMIN |
| Réactiver compte | ✅ | 🔒 SUPER_ADMIN |
| Modifier rôles (replace-all idempotent) | ✅ | 🔒 SUPER_ADMIN |
| Dashboard mes tontines (résumé admin, paginé) | ✅ | 🔒 ADMIN+ |
| Journal d'audit | 🔲 TODO | 🔒 SUPER_ADMIN |

### Module Notification ✅ (complet)

#### SMS / WhatsApp

| Feature | Statut |
|---------|--------|
| SMS OTP | ✅ (mock dev) |
| SMS invitation membre pré-inscrit | ✅ |
| Rappel cotisation ROTATIVE (scheduler 8h) | ✅ |
| Rappels EVENEMENTIELLE J-X avec compte à rebours | ✅ |
| Rappels EVENEMENTIELLE jours clés J-30/7/3/1 (tous membres) | ✅ |
| Annonce jackpot ROTATIVE (à la clôture du cycle) | ✅ |
| Distribution finale EVENEMENTIELLE (WhatsApp individuel + résumé admin) | ✅ |

#### Notifications in-app (`/v1/notifications`)

| Feature | Statut | Accès |
|---------|--------|-------|
| Persistance `UserNotification` en base (table `user_notifications`) | ✅ | AUTO |
| Lister mes notifications (paginées, lues + non lues) | ✅ | 🔒 tout auth |
| Compteur notifications non lues (badge cloche) | ✅ | 🔒 tout auth |
| Marquer une notification lue (idempotent) | ✅ | 🔒 tout auth |
| Marquer tout lu (bulk `@Modifying`) | ✅ | 🔒 tout auth |
| `notify()` non-bloquant (try/catch interne, jamais de rollback) | ✅ | INFRA |

#### Mapping événements → `NotificationType` (15 valeurs)

| Événement | Type | Destinataire |
|-----------|------|--------------|
| Membre soumet une cotisation | `PAIEMENT_RECU` | Admin de la tontine |
| Membre soumet une cotisation | `COTISATION_SOUMISE` | Membre lui-même |
| Cotisation validée par admin | `COTISATION_VALIDEE` | Membre cotisant |
| Cotisation refusée par admin (avec motif) | `COTISATION_REFUSEE` | Membre cotisant |
| Demande d'adhésion reçue (code d'invitation) | `DEMANDE_ADHESION` | Gestionnaire |
| Demande d'adhésion refusée | `ADHESION_REFUSEE` | Demandeur |
| Jackpot distribué (ROTATIVE) | `JACKPOT_DISTRIBUE` | Gagnant + admin |
| Distribution finale (EVENEMENTIELLE) | `DISTRIBUTION_FINALE` | Chaque membre |
| Cycle clôturé avec retards | `PAIEMENT_EN_RETARD` | Admin |
| Scheduler 8h – cotisation non payée ou période non cotisée | `RAPPEL_COTISATION` | Membre concerné |
| Jackpot du membre dans 3 jours (ROTATIVE ROTATION) | `TOUR_PROCHE` | Futur bénéficiaire |
| Cycle se termine dans 2 jours | `CYCLE_BIENTOT_CLOTURE` | Admin |
| Cycle MANUEL ouvert | `CYCLE_OUVERT` | Membres actifs |
| Ajout à une tontine | `INVITATION_TONTINE` | Membre ajouté |
| Statut changé (suspendu / retiré / réactivé) | `STATUT_MEMBRE` | Membre concerné |

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

## Roadmap des sprints

```
✦ Sprint 1 – Auth + Profil          ✅ TERMINÉ
✦ Sprint 2 – Tontine core           ✅ TERMINÉ
  → TontineService + TontineController        ✅
  → MembreService + MembreController          ✅
  → CotisationService + CotisationController  ✅
  → CycleService + CycleController            ✅

✦ Sprint 3 – Dashboard Admin        ✅ TERMINÉ
  → AdminDashboardService (GlobalDashboard + MyDashboard)  ✅
  → AdminDashboardController (9 endpoints)                 ✅
  → Gestion utilisateurs (disable/enable/roles)            ✅

✦ Sprint 4 – Notifications & Audit  ✅ TERMINÉ
  4.1 SchedulerService (rappel 8h + annonce bénéficiaire)                ✅ FAIT
  4.2 AuditService (TontineAuditLog sur 14 mutations)                    ✅ FAIT
  4.3 TontineCommission CRUD + calcul commission à la clôture            ✅ FAIT
  4.4 Pagination Page<T> sur tous les endpoints de liste                 ✅ FAIT

✦ Sprint 5 – Gestion tontine hors-ligne (contexte africain)  ✅ TERMINÉ
  5.1 Compte PRE_ENROLLED : ajout membre par téléphone sans compte app   ✅ FAIT
      → AddMembreRequest : userId → phone (+ firstName/lastName optionnels)
      → GET /v1/users/search?phone=xxx : lookup avant ajout (UserController)
      → MembreServiceImpl : validation conditionnelle + createPreEnrolledUser()
      → RegistrationServiceImpl : activation PRE_ENROLLED à l'inscription
      → LoginServiceImpl : blocage PRE_ENROLLED avec message d'invite
      → SmsService.sendTontineInvite()
      → Migration V021 : keycloak_id nullable + account_status

✦ Sprint 6 – Tontine Événementielle  ✅ TERMINÉ
  6.1 Nouveau type TontineType : ROTATIVE | EVENEMENTIELLE              ✅ FAIT
  6.2 Migration V025 : 5 champs + ordre_beneficiaire nullable            ✅ FAIT
  6.3 Tontine.java : dateEcheance, nomEvenement, montantLibre,          ✅ FAIT
      montantMinimum, tontineType
  6.4 CreateTontineRequest : validation conditionnelle par type          ✅ FAIT
  6.5 TontineServiceImpl : generateEvenementielleSubCycles()             ✅ FAIT
      → JOURNALIERE supportée dans calculateEndDate
      → Dernier sous-cycle tronqué à dateEcheance
  6.6 CotisationServiceImpl : validateMontantCotisation()               ✅ FAIT
      → montant fixe strict si montantLibre=false
      → montantMinimum enforced si montantLibre=true
  6.7 CycleServiceImpl : closeCycleEvenementielle()                     ✅ FAIT
      → sous-cycles intermédiaires : ferme + active suivant
      → dernier cycle : computeFinalDistribution() + WhatsApp individuel
  6.8 CycleResponse : MembreDistributionInfo + distributionParMembre    ✅ FAIT

✦ Sprint 7 – Notifications in-app  ✅ TERMINÉ
  7.1 WhatsApp EVENEMENTIELLE personnalisés avec J-X (SchedulerService) ✅ FAIT
      → sendEvenementielleRappelAttente / RappelPeriode / Urgent
      → processEvenementielleReminders() : jours clés J-30/7/3/1
  7.2 UserNotification entity + repository + service + controller       ✅ FAIT
      → Migration V026 : table user_notifications
      → NotificationType enum (12 valeurs, 13 depuis B18)
      → notify() non-bloquant (try/catch interne)
      → 4 endpoints : GET / · GET /unread-count · PATCH /{id}/read · PATCH /read-all
      → Bulk markAllAsRead via @Modifying @Query (non itératif)
  7.3 Câblage in-app sur tous les événements métier                     ✅ FAIT
      → CotisationServiceImpl : COTISATION_SOUMISE (membre) + PAIEMENT_RECU (admin)
      → CotisationServiceImpl : COTISATION_VALIDEE (membre)
      → CycleServiceImpl : CYCLE_OUVERT (membres) + JACKPOT_DISTRIBUE + PAIEMENT_EN_RETARD
      → CycleServiceImpl : DISTRIBUTION_FINALE (chaque membre EVENEMENTIELLE)
      → MembreServiceImpl : INVITATION_TONTINE + STATUT_MEMBRE
      → SchedulerService : RAPPEL_COTISATION sur 4 chemins (ROTATIVE + 3 EVENEMENTIELLE)
      → SchedulerService : CYCLE_BIENTOT_CLOTURE (J-2) + TOUR_PROCHE (J-3 ROTATION)

✦ Sprint 8 – Besoins front v2, lot A (cf. Docs/BESOINS_BACKEND.md)  🔄 EN REVUE
  B01 TontineResponse : monRole / monMembreId / monStatutMembre + ?role=GERE|COTISE  ✅
  B07 Cotisations : ?statut= (multi) + ?q= via CotisationSpecifications            ✅
      → correctif : cycleId d'une autre tontine ne fuit plus
  B06 GET /v1/me/gestion/cotisations (file « À valider »)                         ✅
  B05 my-dashboard : collecté / attendu du tour en cours + gagnantsADesigner       ✅
  B04 GET /v1/me/summary (résumé « Je cotise »)                                    ✅
  B08 Docs OpenAPI : schémas login / refresh / login-pin / register / notifications ✅
  B02 Codes d'erreur stables (champ `code`, enum ErrorCode)                     ✅
      → fix : /v1/auth/refresh et /v1/auth/pin/reset/** réellement publics
      → fix : 400/404/405 au lieu de 500 (JSON illisible, route inconnue…)
  B18 Refus de paiement : PUT /cotisations/{id}/refuser + V027 (index partiel)  ✅
  B03 charset=UTF-8 forcé sur toutes les réponses (données en base saines)       ✅
  B11 Recherche/filtres serveur : tontines, membres (aPaye), users, console      ✅
  B13 Connexion automatique après inscription (clientType optionnel)            ✅
  B14 Validation groupée POST /v1/me/gestion/cotisations/valider                ✅
  B19 Liens précis des notifications (V028)                                     ✅
  B10 GET /v1/me/jackpots (reçus + estimation du prochain tour)                 ✅
  B15 QR membre opaque + résolution GET /v1/qr/{token} (V029)                   ✅
  B09 GET /v1/me/activite (fil cotisations + jackpots)                          ✅
      → fix : POST /cycles (MANUEL) renvoyait 500 (@Builder.Default manquant)
  P2 :
  B21 POST /v1/me/password · B20 GET/DELETE /v1/me/sessions                    ✅
  B23 DELETE /v1/code-list/{id} (V030, SUPER_ADMIN)                             ✅
      → fix : RoleGuard renvoyait 401 au lieu de 403 sur rôle insuffisant
  B25 GET /v1/public/config · B24 exports CSV                                   ✅
  B22 GET/PUT /v1/me/preferences (V031, JSONB)                                  ✅
  B16/B17 invitation par code + demandes d'adhésion (V032)                      ✅
  ⏸ B19 notifications push : EN STAND-BY (non bloquant – in-app + WhatsApp couvrent le
    besoin ; à reprendre avec une lib Web Push + clés VAPID par environnement)

✦ Sprint 9 – Qualité (branche feature/refactor-mapstruct-tests)  🔄 EN REVUE
  9.1 Contrôleurs sans traitement (AccessControl, InscriptionService, FichierExport)   ✅
  9.2 Tests d'intégration Testcontainers (37 scénarios)                                ✅
      → fix : « mot de passe oublié » ne trouvait jamais l'utilisateur
  9.3 MapStruct pour toutes les conversions entité ↔ DTO                               ✅
  9.4 Tests unitaires de tous les services (238) + seuil JaCoCo 90 % / 80 %            ✅
      → fix : unicité de l'e-mail du profil contournée par des espaces
      → fix : ajout sans nom d'un PRE_ENROLLED au nom vide -> 500
      → fix : SMS encodé deux fois (service non encore branché)
```

---

## Tontine Événementielle — règles de fonctionnement

### Création (`POST /v1/tontines` avec `tontineType: EVENEMENTIELLE`)

Champs obligatoires spécifiques :

| Champ | Règle |
|---|---|
| `dateEcheance` | Obligatoire, doit être > `dateDebut` |
| `frequence` | Obligatoire (comme ROTATIVE) – structure les sous-cycles + rappels |
| `montant` | Obligatoire si `montantLibre=false` |
| `nomEvenement` | Optionnel (ex : "Tabaski 2026") |
| `montantLibre` | `false` = montant fixe imposé, `true` = libre |
| `montantMinimum` | Optionnel, plancher si `montantLibre=true` |

Champs non applicables (ignorés) : `ordreBeneficiaire`, `nombreMembres`, `modeCycle`.

### Activation → génération des sous-cycles

À l'activation, `generateEvenementielleSubCycles()` découpe la période en sous-cycles :
- `JOURNALIERE` → 1 sous-cycle/jour
- `HEBDOMADAIRE` → 1 sous-cycle/semaine (7 jours)
- `BIMENSUEL` → 1 sous-cycle/15 jours
- `MENSUEL` → 1 sous-cycle/mois
- `TRIMESTRIEL` → 1 sous-cycle/3 mois

Le dernier sous-cycle se termine exactement sur `dateEcheance` (pas de dépassement).

### Cotisations

- `montantLibre=false` : montant de la cotisation doit être exactement égal à `tontine.montant`.
- `montantLibre=true` : montant libre, mais ≥ `montantMinimum` si défini.
- Contrainte `uq_cot_cycle_membre` respectée : 1 cotisation max par membre par sous-cycle.
- Un sous-cycle manqué = aucune cotisation pour cette période (aucune pénalité).

### Clôture sous-cycles intermédiaires

Flux : EN_COURS → TERMINE, EN_ATTENTE → EN_RETARD, sous-cycle suivant → EN_COURS.
Aucune distribution. `montantJackpot` = somme des VALIDE du sous-cycle. Commission = 0.

### Clôture finale (dernier sous-cycle)

Détection automatique : aucun cycle EN_ATTENTE restant.

Calcul `computeFinalDistribution()` :
1. Somme des cotisations VALIDEES par membre (tous sous-cycles confondus)
2. Commission `POURCENTAGE_JACKPOT` : prélevée proportionnellement à la mise de chaque membre
3. Commission `FRAIS_FIXES_PAR_CYCLE` : répartie proportionnellement au montant cotisé
4. Commission `FRAIS_ADHESION` : ignorée (ne touche pas le jackpot)
5. `montantNet` du dernier cycle = cagnotte totale - commission totale (mise à jour)

Réponse : `CycleResponse.distributionParMembre` (liste `MembreDistributionInfo` : montantCotise, montantCommission, montantNet).

Notifications WhatsApp : un message individuel par membre + résumé admin.

---

## Compte PRE_ENROLLED — règles de fonctionnement

### Lookup avant ajout (`GET /v1/users/search?phone=xxx`)

Couvre **ACTIVE et PRE_ENROLLED**, exclut les soft-deleted.

| Résultat | Signification | Action frontend |
|----------|--------------|-----------------|
| 200 — ACTIVE | Utilisateur inscrit normalement | Afficher sa fiche, proposer ajout direct |
| 200 — PRE_ENROLLED | Déjà ajouté par un autre admin, profil connu | Afficher sa fiche, proposer ajout direct |
| 404 | Numéro totalement inconnu | Afficher formulaire firstName + lastName |

### Création

Un compte PRE_ENROLLED est créé dans `MembreServiceImpl.createPreEnrolledUser()` quand :
- `GET /v1/users/search?phone=xxx` retourne 404, et
- `POST /v1/tontines/{id}/membres` est appelé avec un `phone` inconnu.

`AddMembreRequest` : `phone` obligatoire ; `firstName` + `lastName` obligatoires **seulement si le numéro est inconnu** (validation conditionnelle dans le service, pas dans le DTO) ; `ordreJackpot` optionnel.

Champs initiaux : `keycloakId = null`, `firstName`/`lastName` fournis par l'admin, `active = true`, `accountStatus = PRE_ENROLLED`.

**Cas numéro PRE_ENROLLED déjà existant avec nom vide** : `enrichPreEnrolledIfNeeded()` complète le nom. Un compte ACTIVE n'est jamais modifié.

### Activation

Déclenchée dans `RegistrationServiceImpl.completeRegistration()` quand :
- L'étape 1 (`sendOtp`) accepte un numéro PRE_ENROLLED (ne lève pas de 409).
- L'étape 3 (`completeRegistration`) détecte le compte PRE_ENROLLED et appelle `activatePreEnrolledUser()`.

Résultat : `keycloakId` renseigné, `firstName/lastName` mis à jour, rôles `USER + MEMBER` attribués, `accountStatus → ACTIVE`. Les memberships tontine existants sont conservés.

### Restrictions

- Login bloqué (HTTP 403) tant que `accountStatus = PRE_ENROLLED`.
- Le rôle MEMBER n'est **pas** assigné à Keycloak lors de la création PRE_ENROLLED (pas de keycloakId). Il l'est lors de l'activation.
- `keycloak_id` est nullable en base depuis V021 (contrainte UNIQUE conservée — PostgreSQL autorise plusieurs NULL distincts).
