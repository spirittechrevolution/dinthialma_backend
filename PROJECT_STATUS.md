# Dinthialma Backend — État du projet

> Dernière mise à jour : 19 juin 2026
> Stack : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak

---

## Modules implémentés

| Module | Statut | Endpoints |
|--------|--------|-----------|
| Auth & sessions | ✅ Complet | `/v1/auth/**` |
| Profil utilisateur | ✅ Complet | `/v1/profile/**` |
| Utilisateurs (lookup) | ✅ Complet | `/v1/users/search` |
| Tontines | ✅ Complet | `/v1/tontines/**` |
| Membres | ✅ Complet | `/v1/tontines/{id}/membres/**` |
| Cotisations | ✅ Complet | `/v1/tontines/{id}/cotisations/**` |
| Cycles | ✅ Complet | `/v1/tontines/{id}/cycles/**` |
| Commissions | ✅ Complet | `/v1/tontines/{id}/commissions/**` |
| Dashboard admin | ✅ Complet | `/v1/admin/**` |
| Notifications in-app | ✅ Complet | `/v1/notifications/**` |
| SMS / WhatsApp | ✅ Complet | (interne — LAfricaMobile) |
| Scheduler (rappels auto) | ✅ Complet | (tâche cron 8h) |
| Journal d'audit | 🔲 TODO | — |

---

## Auth (`/v1/auth`)

- Login phone/email + password, logout, refresh token
- Inscription OTP 3 étapes (send-otp → verify-otp → complete)
- Reset mot de passe OTP (3 étapes)
- Setup PIN + login PIN (WEB/MOBILE)
- Reset PIN OTP (3 étapes)
- PIN expiration 90 jours · lockout 5 tentatives / 30 min
- Activation automatique compte PRE_ENROLLED à l'inscription
- Blocage login (HTTP 403) si `accountStatus = PRE_ENROLLED`

---

## Profil (`/v1/profile`)

- Voir son profil · Modifier (firstName, lastName, email)
- Changement de numéro de téléphone via OTP (2 étapes)

---

## Tontines (`/v1/tontines`)

### Types supportés

| Type | Description |
|------|-------------|
| `ROTATIVE` | Jackpot rotatif — chaque membre bénéficie à son tour |
| `EVENEMENTIELLE` | Cagnotte commune pour un événement (Tabaski, mariage…) |

### Fonctionnalités

- Créer, lister, détailler, modifier (BROUILLON), supprimer (soft delete, BROUILLON)
- Activer (BROUILLON/SUSPENDUE → ACTIVE) · Suspendre (ACTIVE → SUSPENDUE)
- Génération automatique des cycles à l'activation :
  - ROTATIVE AUTO → cycles par membre selon la fréquence
  - EVENEMENTIELLE → sous-cycles `dateDebut → dateEcheance` par fréquence (JOURNALIERE / HEBDOMADAIRE / BIMENSUEL / MENSUEL / TRIMESTRIEL)

### Accès

- Tout compte authentifié peut créer → devient ADMIN de la tontine
- SUPER_ADMIN voit toutes les tontines ; les autres voient uniquement les leurs

---

## Membres (`/v1/tontines/{tontineId}/membres`)

- Lookup préalable : `GET /v1/users/search?phone=xxx` (couvre ACTIVE + PRE_ENROLLED)
- Ajout par numéro de téléphone (firstName/lastName requis seulement si numéro inconnu)
- Création automatique compte **PRE_ENROLLED** si le numéro est totalement inconnu
- SMS d'invitation envoyé au membre pré-inscrit (non-bloquant)
- Retirer un membre (soft delete, statut → SORTI)
- Modifier statut : ACTIF / SUSPENDU / SORTI
- Liste avec `accountStatus` exposé

---

## Cotisations (`/v1/tontines/{tontineId}/cotisations`)

| Endpoint | Accès | Description |
|----------|-------|-------------|
| `GET /` | Membres, admin, SUPER_ADMIN | Liste paginée — MEMBER voit les siennes seulement |
| `GET /?cycleId=` | Admin, SUPER_ADMIN | Filtre par cycle |
| `GET /?membreId=` | Admin, SUPER_ADMIN | Filtre par membre ✨ |
| `GET /?cycleId=&membreId=` | Admin, SUPER_ADMIN | Combinaison des deux filtres ✨ |
| `GET /{id}` | Propriétaire, admin, SUPER_ADMIN | Détail |
| `POST /` | MEMBER | Enregistrer paiement → statut EN_ATTENTE |
| `POST /admin` | Admin, SUPER_ADMIN | Enregistrer + valider (cash / PRE_ENROLLED) |
| `PUT /{id}/valider` | Admin, SUPER_ADMIN | EN_ATTENTE → VALIDÉ |
| `PATCH /{id}` | Admin, SUPER_ADMIN | Modifier cotisation (EN_ATTENTE ou VALIDÉ cycle EN_COURS) |
| `GET /recap/{cycleId}` | Admin, SUPER_ADMIN | Récap par membre sur un cycle |

**Règles métier :**
- 1 cotisation max par membre par cycle (`uq_cot_cycle_membre`)
- EVENEMENTIELLE `montantLibre=false` → montant exact imposé
- EVENEMENTIELLE `montantLibre=true` → libre mais ≥ `montantMinimum` si défini

---

## Cycles (`/v1/tontines/{tontineId}/cycles`)

- Lister, détailler les cycles
- Ouvrir un cycle (ROTATIVE MANUEL uniquement)
- Clôturer un cycle ROTATIVE → jackpot brut + commissions → `montantNet`
- Clôturer un sous-cycle EVENEMENTIELLE intermédiaire → ferme + active le suivant
- Clôture finale EVENEMENTIELLE → `computeFinalDistribution()` par membre
- EN_ATTENTE → EN_RETARD automatiquement à la clôture
- `distributionParMembre` retourné dans `CycleResponse` (EVENEMENTIELLE)

---

## Commissions (`/v1/tontines/{tontineId}/commissions`)

| Type | Comportement à la clôture |
|------|--------------------------|
| `POURCENTAGE_JACKPOT` | % prélevé sur le jackpot brut |
| `FRAIS_FIXES_PAR_CYCLE` | Montant fixe réparti proportionnellement |
| `FRAIS_ADHESION` | Ignoré dans le calcul jackpot |

- Max 1 commission par type · Soft delete · Modifiable (valeur + description)

---

## Dashboard Admin (`/v1/admin`)

### SUPER_ADMIN

- Statistiques globales plateforme (users, tontines, finances, activité 24h)
- Liste de toutes les tontines + statuts
- Liste paginée de tous les utilisateurs
- Détail utilisateur · Désactiver / réactiver compte · Modifier rôles (replace-all idempotent)
- Toutes les cotisations (filtrables)

### ADMIN

- `GET /my-dashboard` : métriques de ses propres tontines

---

## Notifications in-app (`/v1/notifications`)

| Endpoint | Description |
|----------|-------------|
| `GET /` | Liste paginée (lues + non lues, tri createdAt DESC) |
| `GET /unread-count` | Compteur badge cloche |
| `PATCH /{id}/read` | Marquer une notification lue (idempotent) |
| `PATCH /read-all` | Tout marquer lu (bulk) |

**12 types d'événements notifiés :**

| Type | Déclencheur |
|------|-------------|
| `COTISATION_SOUMISE` | Membre soumet une cotisation |
| `PAIEMENT_RECU` | Admin reçoit une cotisation |
| `COTISATION_VALIDEE` | Admin valide une cotisation |
| `JACKPOT_DISTRIBUE` | Clôture cycle ROTATIVE |
| `DISTRIBUTION_FINALE` | Clôture finale EVENEMENTIELLE |
| `PAIEMENT_EN_RETARD` | Clôture avec retards |
| `RAPPEL_COTISATION` | Scheduler 8h |
| `TOUR_PROCHE` | Jackpot du membre dans 3 jours |
| `CYCLE_BIENTOT_CLOTURE` | Cycle se termine dans 2 jours |
| `CYCLE_OUVERT` | Cycle MANUEL ouvert |
| `INVITATION_TONTINE` | Ajout à une tontine |
| `STATUT_MEMBRE` | Statut changé (suspendu / retiré / réactivé) |

---

## SMS / WhatsApp (LAfricaMobile)

- OTP inscription, reset mot de passe, reset PIN
- Invitation membre pré-inscrit
- Rappels quotidiens 8h (ROTATIVE + EVENEMENTIELLE avec compte à rebours J-X)
- Rappels EVENEMENTIELLE jours clés : J-30, J-7, J-3, J-1
- Annonce jackpot ROTATIVE à la clôture
- Distribution finale EVENEMENTIELLE : message individuel par membre + résumé admin

---

## Base de données

- Schéma : `dinthialma` · Migrations Flyway (V001 → V026)
- Soft delete partout (`deletedAt`) · UUIDs · `ddl-auto: none`
- Prochaine migration disponible : **V027**

### Migrations clés récentes

| Version | Contenu |
|---------|---------|
| V021 | `account_status` + `keycloak_id` nullable (PRE_ENROLLED) |
| V024 | `nombre_gagnants` + table `cycle_gagnants` |
| V025 | Champs tontine EVENEMENTIELLE (type, dateEcheance, montantLibre…) |
| V026 | Table `user_notifications` (notifications in-app) |

---

## Rôles

| Rôle | Realm Keycloak | Description |
|------|----------------|-------------|
| `SUPER_ADMIN` | `DINTHIALMA_SUPER_ADMIN` | Accès total plateforme |
| `ADMIN` | `DINTHIALMA_ADMIN` | Créateur/gestionnaire d'une tontine |
| `MEMBER` | `DINTHIALMA_MEMBER` | Cotisant membre d'une tontine |
| `USER` | `DINTHIALMA_USER` | Tout compte inscrit (rôle de base) |

> Les rôles se cumulent. L'accès par tontine est applicatif : être ADMIN ne suffit pas, il faut être le **créateur** de la tontine concernée.

---

## Sprints

| Sprint | Contenu | Statut |
|--------|---------|--------|
| 1 | Auth + Profil | ✅ |
| 2 | Tontine · Membres · Cotisations · Cycles | ✅ |
| 3 | Dashboard Admin | ✅ |
| 4 | Scheduler · Audit · Commissions · Pagination | ✅ |
| 5 | Compte PRE_ENROLLED (gestion hors-ligne) | ✅ |
| 6 | Tontine Événementielle | ✅ |
| 7 | Notifications in-app + WhatsApp EVENEMENTIELLE | ✅ |
| 8 | Journal d'audit (SUPER_ADMIN) | 🔲 |
