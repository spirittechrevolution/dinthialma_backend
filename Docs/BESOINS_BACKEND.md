# Besoins backend pour le nouveau front (brief v2 « logique Wave »)

> Comparaison entre ce que demande `docs/BRIEF_ECRANS.md` et ce que décrit la spécification `api-docs.json` (OpenAPI du backend, 64 opérations), complétée par l'usage réel du front actuel.
> Chaque besoin indique : écrans concernés · ce qui existe · contournement possible côté front · proposition d'endpoint · priorité.
> Les endpoints proposés sont des **suggestions de contrat** à discuter avec l'équipe backend (noms et formes modifiables).

**Priorités**
- **P0 — bloquant** : sans lui, un écran du brief ne peut pas fonctionner correctement ou de façon fiable.
- **P1 — fortement conseillé** : faisable côté front en contournant, mais lent (beaucoup d'appels sur réseau faible), fragile ou incomplet.
- **P2 — v2 / confort** : fonctionnalité prévue plus tard ou amélioration.

---

## 0. Synthèse

| # | Besoin | Écrans | Priorité |
|---|---|---|---|
| B01 | Mon rôle et mon identifiant de membre dans chaque tontine | Accueil, Fiche Tontine, Payer, toutes | **P0** |
| B02 | Codes d'erreur stables (au lieu de textes à analyser) | Connexion, Code secret, Ajout membre, Payer | **P0** |
| B03 | Encodage UTF-8 correct des réponses (accents) | Toutes | **P0** |
| B04 | Résumé « Je cotise » toutes tontines (total, cotisations dues) | Accueil (bandeau, carte d'action), feuille Montants | **P0** |
| B05 | Résumé « Je gère » : collecté / attendu du tour en cours | Accueil (bandeau), feuille Collecte | **P0** |
| B06 | File « À valider » de toutes mes tontines gérées | À valider, Accueil (carte, badge) | **P0** |
| B07 | Filtre par statut sur les paiements d'une tontine | Fiche Tontine, Historique, À valider | **P0** |
| B08 | Mettre la documentation à jour sur ce que le front utilise déjà | — | **P0** |
| B09 | Fil d'activité agrégé (mes mouvements toutes tontines) | Accueil (fil), Historique | P1 |
| B10 | Mes jackpots : reçus + prochain tour estimé | Mes jackpots, feuille Montants | P1 |
| B11 | Recherche et filtres côté serveur sur les listes | Mes tontines, Membres, Utilisateurs, Historique | P1 |
| B12 | Droits de gestionnaire actifs juste après création d'une tontine | Créer une tontine | P1 (à tester) |
| B13 | Connexion automatique après inscription | Création de compte | P1 |
| B14 | Validation groupée de paiements | À valider (« Tout valider ») | P1 |
| B15 | Identifiant opaque pour le QR membre + résolution d'un QR | Carte QR, Scanner | P1 |
| B16 | Code d'invitation de tontine + aperçu public | Partager, Invitation, Scanner | P2 |
| B17 | Demandes d'adhésion (envoyer, accepter, refuser) | Invitation, À valider | P2 |
| B18 | Refuser un paiement déclaré | À valider, Reçu | P1 |
| B19 | Notifications : liens précis + notifications push | Alertes | P1 / P2 |
| B20 | Appareils connectés (liste, déconnexion à distance) | Paramètres | P2 |
| B21 | Changer de mot de passe connecté (ancien + nouveau) | Paramètres | P2 |
| B22 | Préférences utilisateur synchronisées (favoris, mode, montant masqué) | Accueil, Mes tontines | P2 |
| B23 | Suppression d'une valeur de liste | Listes de valeurs | P2 |
| B24 | Export des paiements (CSV) | Historique | P2 |
| B25 | Canal d'assistance (contact configurable) | Paramètres > Support | P2 |

---

## 1. Fondations (P0)

### B01 — Mon rôle et mon identifiant de membre dans chaque tontine
- **Écrans** : Accueil (bascule Je cotise / Je gère, filtre), Fiche Tontine (vue gestionnaire ou membre), Payer, Mes jackpots.
- **Existant** : `TontineResponse` contient `creePar` mais **ni mon rôle ni mon `membreId`**. `GET /v1/tontines` renvoie, pour un non-super-admin, « les tontines créées + celles où il cotise », sans dire laquelle est laquelle.
- **Contournement actuel** : le front compare le téléphone de `creePar` avec celui du jeton, et retrouve son `membreId` en parcourant la liste des membres, en retirant l'indicatif « 221 » pour comparer. Fragile (formats différents, changement de numéro) et coûteux (1 appel de liste de membres par tontine).
- **Proposition** : ajouter à `TontineResponse` (dans la liste **et** dans le détail) :
  ```json
  "monRole": "GESTIONNAIRE" | "MEMBRE" | "GESTIONNAIRE_ET_MEMBRE" | "LECTURE",
  "monMembreId": "uuid | null",
  "monStatutMembre": "ACTIF | SUSPENDU | SORTI | null"
  ```
  et un filtre `GET /v1/tontines?role=GERE|COTISE`.

### B02 — Codes d'erreur stables
- **Écrans** : Connexion, Déverrouillage, Code SMS, Ajouter un membre, Payer.
- **Existant** : le front lit le **texte** du message d'erreur pour décider quoi faire : « pré-inscrit / gestionnaire / pas encore inscrit » (connexion 403), « session », « expiré », « simple » (code secret), « position / ordre » (ajout membre, 409). Toute reformulation côté backend casse le front.
- **Proposition** : un champ `code` dans `CustomResponse` en cas d'erreur, par exemple :
  | Code | Cas |
  |---|---|
  | `ACCOUNT_PRE_ENROLLED` | Connexion d'un compte créé par un gestionnaire |
  | `INVALID_CREDENTIALS` | Mauvais identifiants |
  | `PIN_INVALID`, `PIN_EXPIRED`, `PIN_TOO_SIMPLE`, `PIN_LOCKED` (+ `retryAfterSeconds`) | Code secret |
  | `PIN_SESSION_EXPIRED` | Session du code secret expirée → connexion complète |
  | `OTP_INVALID`, `OTP_EXPIRED`, `OTP_RATE_LIMITED` | Codes SMS |
  | `PHONE_ALREADY_USED` | Inscription / changement de numéro |
  | `MEMBER_ALREADY_IN_TONTINE`, `JACKPOT_POSITION_TAKEN`, `TONTINE_FULL` | Ajout d'un membre |
  | `COTISATION_ALREADY_EXISTS` | 2ᵉ déclaration sur le même tour (409 actuel) |
  | `NO_CYCLE_IN_PROGRESS`, `AMOUNT_BELOW_MINIMUM` | Payer / Encaisser |
  | `CYCLE_ALREADY_IN_PROGRESS` | Démarrer un tour |

### B03 — Encodage UTF-8 des réponses
- **Existant** : le front contient un correcteur qui répare les noms accentués mal encodés (« AÃ¯cha » → « Aïcha ») dans **toutes** les réponses : le texte est lu en Latin-1 quelque part entre la base et l'API.
- **Proposition** : corriger l'encodage côté backend / base de données (connexion JDBC, collation, en-tête `Content-Type: application/json; charset=UTF-8`) et nettoyer les données existantes. Le correcteur front pourra alors être retiré.

### B04 — Résumé « Je cotise » toutes tontines
- **Écrans** : Accueil (bandeau « TOTAL COTISÉ », carte d'action « À payer 5.000F · Famille »), feuille « Mes montants par tontine ».
- **Existant** : rien d'agrégé. Il faut, pour chaque tontine : `GET /tontines/{id}/cycles` (trouver le tour en cours) + `GET /tontines/{id}/cotisations` (mes paiements) puis additionner côté front → **2 appels × nombre de tontines** à chaque ouverture de l'app, sur réseau faible.
- **Proposition** : `GET /v1/me/summary` (ou `/v1/me/cotisant`) :
  ```json
  {
    "totalCotiseValide": 85000,
    "totalEnAttente": 5000,
    "totalJackpotsRecus": 57000,
    "tontines": [{
      "tontineId": "…", "nom": "Famille", "tontineType": "ROTATIVE",
      "monMembreId": "…", "totalCotiseValide": 20000,
      "toursPayes": 4, "toursTotal": 12,
      "cycleEnCours": { "cycleId": "…", "numeroCycle": 5, "dateFin": "2026-11-30" },
      "cotisationDue": { "montant": 5000, "montantLibre": false, "montantMinimum": null,
                         "dateLimite": "2026-11-30", "statut": "A_PAYER | EN_ATTENTE | VALIDE" },
      "gestionnaire": { "firstName": "Moussa", "lastName": "Diop", "phone": "+22177…" }
    }]
  }
  ```

### B05 — Résumé « Je gère » : collecté et attendu du tour en cours
- **Écrans** : Accueil mode Je gère (bandeau « COLLECTÉ CE TOUR », carte « 4 n'ont pas payé »), feuille « Collecte par tontine ».
- **Existant** : `GET /v1/admin/my-dashboard` donne par tontine `cotisationsEnAttente`, `cotisationsEnRetard`, `montantTotalValide` (toutes périodes) et `cycleEnCours` — mais **pas** le montant collecté sur le tour en cours, le montant attendu, ni le nombre de membres ayant payé.
- **Proposition** : enrichir `TontineStats` (et la racine de `MyDashboardResponse` pour les totaux) :
  ```json
  "cycleEnCours": { …existant…, "montantCollecte": 40000, "montantAttendu": 60000,
                    "membresAyantPaye": 8, "membresActifs": 12 },
  "tontineType": "ROTATIVE", "montant": 5000, "dateEcheance": null,
  "gagnantsADesigner": false
  ```
  Racine : `totalCollecteToursEnCours`, `totalAttenduToursEnCours`, `totalCotisationsEnAttente`, `totalCotisationsEnRetard`.

### B06 — File « À valider » de toutes mes tontines gérées
- **Écrans** : À valider, badge de la case « À valider », carte d'action « 3 paiements à valider ».
- **Existant** : `GET /v1/admin/dashboard/contributions?statut=` existe mais est **réservé au super-admin**. Pour un gestionnaire, il faut appeler `GET /tontines/{id}/cotisations` pour chaque tontine puis filtrer « EN_ATTENTE » côté front (voir B07).
- **Proposition** : `GET /v1/me/gestion/cotisations?statut=EN_ATTENTE&tontineId=&page&size` → paiements de **toutes les tontines que je gère**, chacun avec `tontineId` + `tontineNom`. Même forme que `CotisationResponse`.

### B07 — Filtre par statut sur les paiements d'une tontine
- **Écrans** : Fiche Tontine (pastilles de statut), Historique, À valider.
- **Existant** : `GET /tontines/{tontineId}/cotisations` accepte `cycleId` et `membreId`, **pas `statut`**. Le front filtre donc la page en cours seulement → résultats incomplets dès qu'il y a plus de 20 paiements.
- **Proposition** : paramètre `statut` (multi-valeurs : `statut=EN_ATTENTE&statut=EN_RETARD`) et `q` (nom du membre ou référence).

### B08 — Documentation à aligner avec ce que le front utilise déjà
Des champs ou paramètres sont utilisés par le front mais **absents de `api-docs.json`**. Soit ils existent et la doc est en retard, soit le front se trompe. À vérifier un par un :
| Élément | Utilisé par le front | Dans la doc |
|---|---|---|
| `MyDashboardResponse` : `montantTotalValide`, `totalMembres`, `totalCotisationsEnAttente`, `totalCotisationsEnRetard` | Accueil gestionnaire | ❌ (seulement `nombreTontinesGerees`, `tontines`) |
| `TontineStats` : `tontineType`, `montant`, `dateEcheance`, `nombreMembresCotise` | Accueil gestionnaire | ❌ |
| `GET /admin/dashboard/users?accountStatus=ACTIVE\|PRE_ENROLLED` | Utilisateurs | ❌ (seulement pagination) |
| `AdminUserResponse.accountStatus` | Utilisateurs | ❌ |
| `MembreResponse.dateJackpot` (ou équivalent « a reçu ») | Membres | ❌ |
| Réponse de `POST /auth/register/complete` (`role`) | Inscription | ❌ (schéma non décrit) |
| Réponse de `POST /auth/login` (`access_token`, `refresh_token`, `pin_configured`) | Connexion | ❌ (schéma non décrit) |
| `NotificationResponse` (`type`, `tontineId`, `read`, `title`, `body`) | Alertes | ❌ (schéma non décrit) |
| `POST /tontines/{id}/cycles` : la description parle d'un `beneficiaireId`, absent de `OpenCycleRequest` | Démarrer un tour | ⚠️ incohérence |

---

## 2. Performance et fiabilité (P1)

### B09 — Fil d'activité agrégé
- **Écrans** : Accueil (fil), Historique.
- **Existant** : aucun flux transverse ; il faudrait fusionner les cotisations et les jackpots de chaque tontine côté front.
- **Proposition** : `GET /v1/me/activite?mode=COTISE|GERE&tontineId=&type=&statut=&page&size` → lignes normalisées :
  ```json
  { "id": "…", "type": "COTISATION_DECLAREE | COTISATION_VALIDEE | COTISATION_ENCAISSEE | COTISATION_EN_RETARD | JACKPOT_RECU | TOUR_CLOTURE | MEMBRE_AJOUTE",
    "date": "…", "montant": 5000, "sens": "EPARGNE | GAIN", "statut": "EN_ATTENTE | VALIDE | EN_RETARD",
    "tontineId": "…", "tontineNom": "Famille", "cycleId": "…", "cotisationId": "…", "libelle": "Cotisation Famille" }
  ```

### B10 — Mes jackpots : reçus + prochain tour estimé
- **Écrans** : Mes jackpots, Fiche Tontine (« Mon tour »), bandeau d'info « Votre prochain tour : décembre ».
- **Existant** : `GET /tontines/{id}/cycles/beneficiaires` par tontine (historique) ; `ordreJackpot` du membre. **Aucune date estimée** de mon prochain tour, ni « prochain gagnant » du tour en cours hors `cycleEnCours.beneficiaireNom` (dashboard gestionnaire seulement).
- **Proposition** : `GET /v1/me/jackpots` :
  ```json
  { "totalRecu": 57000,
    "recus": [{ "tontineId": "…", "tontineNom": "Amis", "numeroCycle": 3, "dateRemise": "…", "montantRecu": 57000 }],
    "aVenir": [{ "tontineId": "…", "tontineNom": "Famille", "position": 7, "numeroCycleEstime": 7,
                 "dateEstimee": "2026-12-01", "montantEstime": 60000, "modeAttribution": "ORDRE | TIRAGE | CHOIX" }] }
  ```
  `dateEstimee` = null si l'ordre est tiré au sort ou choisi. Ajouter aussi `prochainBeneficiaire` (nom) dans `CycleResponse` quand il est connu.

### B11 — Recherche et filtres côté serveur
Aujourd'hui les recherches se font sur la page affichée uniquement.
| Liste | Paramètres à ajouter |
|---|---|
| `GET /v1/tontines` | `q` (nom), `statut`, `role` (B01) |
| `GET /v1/tontines/{id}/membres` | `q` (nom, téléphone), `statut`, `aPayeCycleId` (membres ayant / n'ayant pas payé un tour) |
| `GET /v1/admin/dashboard/users` | `q` (nom, téléphone, email), `role`, `accountStatus` (B08) |
| `GET /v1/admin/dashboard/tontines` | `q` |
| `GET /v1/tontines/{id}/cotisations` | `statut`, `q` (B07) |

Le filtre `aPayeCycleId=false` alimente directement l'écran **Encaisser** (« membres pas encore payés en tête ») et la liste **Pas encore payé** de l'écran **Tour**.

### B12 — Droits de gestionnaire juste après la création d'une tontine
- **Écran** : Créer une tontine → passage immédiat en mode « Je gère ».
- **Existant** : `POST /v1/tontines` attribue le rôle `DINTHIALMA_ADMIN`, mais le jeton en cours ne le contient pas ; le front affiche « Reconnectez-vous ».
- **Piste** : appeler `POST /v1/auth/refresh` juste après la création ; si Keycloak renvoie un jeton avec le nouveau rôle, **aucun développement backend** n'est nécessaire. **À tester.** Sinon, renvoyer de nouveaux jetons dans la réponse de création. Avec B01 (`monRole` par tontine), le front n'a d'ailleurs plus besoin du rôle global pour afficher la vue gestionnaire.

### B13 — Connexion automatique après inscription
- **Écran** : Création de compte → Code secret → Accueil, sans repasser par la connexion.
- **Existant** : `POST /auth/register/complete` ne renvoie pas de jetons (d'après l'usage du front) ; l'utilisateur doit se reconnecter.
- **Proposition** : renvoyer `access_token`, `refresh_token`, `pin_configured: false` dans la réponse (paramètres `clientType`, `deviceInfo` dans la requête).

### B14 — Validation groupée
- **Écran** : À valider (« Tout valider (6) »).
- **Existant** : un `PUT …/valider` par paiement.
- **Proposition** : `POST /v1/me/gestion/cotisations/valider` avec `{ "cotisationIds": [...] }` → résultat par identifiant (`VALIDE` / erreur), pour ne pas perdre toute la série en cas d'échec partiel.

### B15 — QR membre : identifiant opaque + résolution
- **Écrans** : Carte QR (mode Je cotise), Scanner.
- **Existant** : `GET /v1/users/search?phone=` permet de retrouver quelqu'un par numéro. Mettre le **numéro en clair** dans le QR fonctionne mais expose le numéro à quiconque photographie l'écran.
- **Proposition** :
  - `GET /v1/me/qr` → `{ "qrToken": "m_8fK2…", "url": "https://<domaine>/q/m_8fK2…" }` (jeton stable, révocable).
  - `GET /v1/qr/{token}` (authentifié) → `{ "type": "MEMBRE", "user": { "id", "firstName", "lastName", "accountStatus" }, "dansMesTontines": [{ "tontineId", "membreId", "aPayeCycleEnCours": false }] }` ; pour un QR tontine (B16) : `{ "type": "TONTINE", "code": "…" }`.

### B18 — Refuser un paiement déclaré
- **Écrans** : À valider, Reçu.
- **Existant** : statuts `EN_ATTENTE / VALIDE / EN_RETARD` uniquement ; un paiement déclaré par erreur ne peut qu'être modifié. Comme le backend n'accepte qu'**une cotisation par membre et par tour** (409), un paiement erroné bloque le membre.
- **Proposition** : `PUT /v1/tontines/{id}/cotisations/{cotisationId}/refuser` avec `{ "motif": "Transfert non reçu" }` → nouveau statut `REFUSE`, notification au membre, et le membre peut déclarer à nouveau.

### B19 — Notifications (P1 : liens ; P2 : push)
- **P1** : ajouter à chaque notification `cycleId`, `cotisationId`, `membreId` quand ils existent, pour ouvrir l'écran exact (reçu, tour, fiche membre) au lieu de la seule tontine. Documenter `NotificationResponse` (B08).
- **P2** : notifications push Web : `POST /v1/me/push-subscriptions` (abonnement Web Push), `DELETE /v1/me/push-subscriptions/{id}`, envoi côté serveur pour les rappels de cotisation, validations et jackpots.

---

## 3. Fonctionnalités v2 (P2)

### B16 — Code d'invitation de tontine + aperçu public
- **Écrans** : Partager (invitation tontine), Invitation, Scanner.
- **Proposition** :
  - `POST /v1/tontines/{id}/invitation` (gestionnaire) → `{ "code": "FAM-7Q2K", "url": "https://<domaine>/q/t/FAM-7Q2K", "expireLe": null }` ; `DELETE` pour révoquer / régénérer.
  - `GET /v1/public/invitations/{code}` (**sans authentification**) → `{ "nom", "tontineType", "montant", "frequence", "montantLibre", "nomEvenement", "dateEcheance", "placesRestantes", "gestionnairePrenom" }` — aucune donnée personnelle des membres.
- **Repli v1 sans backend** : page d'invitation + bouton WhatsApp vers le gestionnaire.

### B17 — Demandes d'adhésion
- **Écrans** : Invitation (« Demander à rejoindre »), À valider (section « Demandes »).
- **Proposition** :
  - `POST /v1/invitations/{code}/demandes` (authentifié) → crée une demande `EN_ATTENTE`, notifie le gestionnaire (nouveau type `DEMANDE_ADHESION`).
  - `GET /v1/me/gestion/demandes?statut=EN_ATTENTE` ; `PUT /v1/tontines/{id}/demandes/{demandeId}/accepter` (avec `ordreJackpot` facultatif) ; `PUT …/refuser`.
  - Remplace aussi la fonctionnalité inachevée « Rejoindre avec un code ».

### B20 — Appareils connectés
- `GET /v1/me/sessions` → `[{ "id", "clientType", "deviceInfo", "derniereActivite", "courante": true }]` ; `DELETE /v1/me/sessions/{id}` (déconnexion à distance).

### B21 — Changer de mot de passe connecté
- `POST /v1/me/password` `{ "ancienMotDePasse", "nouveauMotDePasse" }`. Aujourd'hui on passe par le flux « mot de passe oublié » (SMS), acceptable mais plus long.

### B22 — Préférences utilisateur synchronisées
- `GET/PUT /v1/me/preferences` → `{ "tontinesFavorites": [...], "dernierMode": "COTISE|GERE", "masquerMontant": true, "dernierMoyenPaiement": "WAVE" }`. Sans cela, ces réglages restent propres à chaque appareil (acceptable en v1).

### B23 — Supprimer une valeur de liste
- `DELETE /v1/code-list/{id}` (refusé pour les valeurs système). Annoncé dans la documentation projet, absent de l'API.

### B24 — Export des paiements
- `GET /v1/me/activite/export?format=csv&…` et `GET /v1/tontines/{id}/cotisations/export?format=csv&cycleId=` (gestionnaire).

### B25 — Canal d'assistance
- `GET /v1/public/config` → `{ "supportWhatsapp": "+221…", "supportEmail": "…", "versionMinimaleApp": "…" }` (permet aussi de forcer une mise à jour).

---

## 4. Ce qui existe déjà et suffit (pour mémoire)

Aucun développement backend nécessaire pour : inscription / connexion / code secret / mot de passe oublié par SMS ; changement de numéro ; profil ; création, modification et suppression d'un brouillon, activation, suspension ; ajout (avec pré-inscription + SMS), suspension, retrait de membres ; ouverture, clôture de tours, désignation et tirage des gagnants, répartition finale ; déclaration, encaissement, validation, modification d'un paiement ; récapitulatif par tour et total par membre ; commissions (création, modification, suppression) ; notifications (liste, compteur, lu, tout lu) ; console plateforme (tableau de bord, utilisateurs, rôles, activation, tontines, paiements, listes de valeurs en création / modification) ; invitation à l'app par lien (aucune API) ; QR membre en v1 avec le numéro de téléphone (si on accepte de l'exposer).

---

## 5. Ordre de livraison suggéré

1. **Sprint 1 (P0)** : B01, B02, B03, B07, B08 — corrections et ajouts de champs/paramètres sur des endpoints existants.
2. **Sprint 2 (P0)** : B04, B05, B06 — les 3 agrégats qui alimentent l'accueil.
3. **Sprint 3 (P1)** : B09, B10, B11, B13, B14, B18, B19 (liens) ; tester B12 ; B15.
4. **Plus tard (P2)** : B16 + B17 (invitations), B19 (push), B20 à B25.
