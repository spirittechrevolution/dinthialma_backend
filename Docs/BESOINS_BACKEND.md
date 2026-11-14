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
| B26 | Rythme libre (« tous les 3 jours ») + jour et heure d'échéance + rappels avant échéance | Créer, Modifier, Réglages, Fiche, Tours | P1 |
| B27 | Amendes de retard (tontines rotatives), avec validation du gestionnaire | Réglages, À valider, Fiche, Payer, Mes tontines | P1 |
| B28 | Prêts sur la cagnotte (tontines événementielles) | Fiche, Demande de prêt, À valider, Distribution | P1 |
| B29 | Caisses de la tontine (commune, amendes, frais de prêt) et mouvements | Fiche (rubrique Caisse), Réglages | P1 (socle de B27/B28) |
| B30 | Commission du gestionnaire enregistrée dans la création | Créer une tontine | P1 |
| B31 | Canaux de notification : in-app, push, WhatsApp (+ SMS pour les jackpots) | Toutes les alertes | P1 |
| B32 | Rythme du jackpot distinct du rythme de cotisation (N cotisations par tour, rotatives) | Créer, Modifier, Réglages, Fiche, Tours, Payer, Encaisser, À valider, Invitation, Accueil, Mes jackpots | P1 (après B26) |
| B33 | Cotisation séparée de la remise : retards régularisables, options de remise du jackpot, clôture de la tontine | Tours, Remise du jackpot, Payer, Encaisser, À valider, Réglages, Fiche | P1 (avec B32) |
| B34 | Tontine groupée : achat groupé d'un produit unique, remis à la fin (sans commission) | Créer, Fiche, Membres, Payer, Encaisser, À valider, Mon espace | P1 (après B33) |
| B35 | Plusieurs parts par membre (rotatives, groupées si configuré) : parts max par adhérent, personnes requises pour démarrer, retrait encadré par le gestionnaire | Créer, Réglages, Membres, Invitation, Payer, Encaisser, Mes jackpots | P1 (après B33) |
| S01 | Socle : ponctualité d'une cotisation (à temps / en retard / non déclarée), distincte du statut | Récap tour, Je cotise, Amendes | P1 (prérequis B26/B27) |
| S02 | Socle : montants en francs entiers, partages exacts (aucun franc perdu) | Clôture, Distribution, Caisses | P1 (prérequis B27/B28/B29) |

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
- **Ajout (2026-11-12)** : champ facultatif `"derniereTontineGeree": "uuid | null"` — dernière tontine choisie dans l'espace Gestionnaire de l'accueil (l'accueil gestionnaire a toujours une tontine sélectionnée, dont il affiche le QR d'invitation).
  - accepté en `PUT` et renvoyé en `GET` ; absent ou null = aucune ;
  - **contrôle** : si l'identifiant ne correspond pas à une tontine dont l'utilisateur est gestionnaire, le serveur l'enregistre à null (ou renvoie null en lecture) plutôt que de refuser tout l'objet ;
  - le `PUT` remplace l'objet complet : tant que ce champ n'est pas accepté, le front le garde seulement sur l'appareil (il ne l'envoie pas, pour ne pas faire échouer la sauvegarde des autres préférences).
  - ✅ **Livré** (branche `feature/socles-ponctualite-montants`) : gestionnaire = créateur de la tontine, tontine non supprimée.

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
5. **Lot « réalité des tontines » (B26 à B31)**, dans cet ordre de dépendance :
   0. S01 et S02 (socles, voir 6.1) : ponctualité et montants entiers, **avant tout le reste** ;
   1. B30 (petit, indépendant) et B31 (canaux, utilisé par tous les suivants) ;
   2. B26 (rythme + échéance) : **prérequis de B27** (l'amende se déclenche à l'échéance) ;
   3. B29 (caisses) : **prérequis de B27 et B28** (destination des amendes et des frais de prêt) ;
   4. B27 (amendes) ;
   5. B28 (prêts).
6. **Ajouts du 2026-11-13** : B32 (N cotisations par tour) puis B33 (cotisation séparée de la remise, retards régularisables, options de remise, clôture de la tontine), **après B26 et avant B29/B27** : B27 compte une amende par échéance manquée (B32), B29 reçoit les régularisations en option D (B33). Puis B34 (tontine groupée), qui réutilise B26, B32 et B33, B35 (plusieurs parts par membre) passe **avant** B34, qui en a besoin pour les parts en groupée.

---

## 6. Lot « réalité des tontines » (B26 à B31)

> Décisions prises avec le porteur du projet le 2026-11-12 :
> - les **prêts** concernent uniquement les tontines **événementielles** pour l'instant ; pas de garant pour l'instant ;
> - un prêt n'est visible que par **le gestionnaire et l'emprunteur** ; les autres membres voient seulement les **totaux** ;
> - les **amendes** concernent les tontines **rotatives** ; **une seule amende par tour manqué** ; application **avec validation du gestionnaire** ;
> - l'argent des amendes et des frais de prêt va, **au choix du gestionnaire**, dans la **caisse commune** ou dans une **caisse dédiée** (caisse amendes / caisse frais de prêt) ;
> - canaux : **in-app, push et WhatsApp** ; **pas de SMS**, sauf pour annoncer la **réception d'un jackpot** ou d'une distribution (à confirmer, voir B31).
>
> Règles communes à tout le lot :
> - **montants en francs CFA entiers** (pas de décimales), arrondis au franc le plus proche ;
> - **fuseau horaire** : `Africa/Dakar` (UTC+0, sans heure d'été) pour toutes les échéances, rappels et détections ;
> - **droits** : seul le **gestionnaire de la tontine** (son créateur) configure, valide, accorde, annule ; le super-admin lit tout ; un membre ne voit que ce qui le concerne + les totaux ;
> - **traçabilité** : chaque action enregistre son auteur et sa date ; rien n'est supprimé physiquement (annulation = statut + motif) ;
> - **règles connues avant d'adhérer** : les règles (rythme, échéance, amendes, prêts) sont renvoyées dans l'aperçu d'invitation (B16) et dans `TontineResponse` ;
> - **changement de règles en cours de tontine** : il ne s'applique qu'aux tours **pas encore commencés** ; chaque élément (amende, prêt) garde une **copie de la règle** en vigueur au moment où il est créé ; les membres reçoivent une notification `REGLES_MODIFIEES` ;
> - **codes d'erreur** stables au format B02 (listés dans chaque besoin).

### 6.1 Socles décidés le 2026-11-12 (sans rien casser)

Principe : **ajouter, jamais modifier**. Aucun champ, statut ni code n'est renommé ou retiré ; aucune
donnée passée n'est recalculée.

#### S01 — Ponctualité d'une cotisation (distincte du statut)
- **Constat** : `statut = EN_RETARD` signifie aujourd'hui « **non validée à la clôture du tour** »
  (clôture : jackpot calculé sur les VALIDE, puis EN_ATTENTE → EN_RETARD). Ce sens est utilisé par la
  validation, le refus, la correction, les tableaux de bord, le fil d'activité et les filtres. Il est
  **conservé tel quel**.
- **Ajout** : `ponctualite` = `A_TEMPS` | `EN_RETARD` | `NON_DECLAREE`, calculée et non stockée :
  `datePaiement` (si renseignée, sinon date de déclaration `createdAt`) comparée à l'échéance du tour
  (`echeanceAt` de B26 ; en attendant : `dateFin` à 23h59, fuseau `Africa/Dakar`) + délai de tolérance
  (B27). Une déclaration refusée ne compte pas (la redéclaration crée une nouvelle ligne datée).
- **Base** : une seule colonne `cotisations.date_paiement` (facultative), renseignée lors d'un
  encaissement par le gestionnaire (`POST /cotisations/admin`, champ `datePaiement` : ≤ maintenant).
- **API** (champs ajoutés uniquement) : `ponctualite` dans `CotisationResponse`, dans le récapitulatif
  par tour (`recap/{cycleId}`, `NON_DECLAREE` si aucune cotisation) et dans `cotisationDue` de
  `/v1/me/summary`.
- **Notification** `PAIEMENT_EN_RETARD` : type inchangé ; texte corrigé quand le paiement avait été
  déclaré à temps (« déclaré à temps, non validé avant la clôture » au lieu de « n'a pas cotisé »).
- **Front** : afficher `statut = EN_RETARD` comme « non validée à la clôture » et `ponctualite`
  comme « à temps / en retard ».
- **Estimation** : 1,5 j.

#### S02 — Montants en francs entiers et partages exacts
- **Constat** : calculs à 2 décimales ; un net de 1 000 F partagé entre 3 gagnants donne
  333,33 × 3 = 999,99 F (un centime perdu) ; même défaut sur la répartition au prorata de la
  distribution événementielle. Bloquant pour le contrôle de cohérence des caisses (B29).
- **Ajout** : utilitaire unique `Montants` : `arrondir` (franc le plus proche), `repartir(total, n)`
  (somme exacte, le reste aux premiers dans l'ordre de désignation : 1 000 / 3 → 334 + 333 + 333),
  `repartirAuProrata(total, poids)` (somme exacte, plus forts restes).
- **Application** : commission arrondie au franc, net = jackpot − commission, partage du jackpot,
  distribution finale ; **uniquement pour les tours clôturés après la mise en production**. Pas de
  migration de schéma (colonnes `scale 2` conservées, valeurs entières `1600.00`).
- **Saisies** : les nouveaux endpoints (amendes, prêts, caisses) refusent les décimales ; les
  endpoints existants (cotisation, commission) continuent de les accepter (compatibilité).
- **Effet visible** : au plus 1 F d'écart par bénéficiaire, totaux exacts.
- **Estimation** : 1,5 j.

### 6.2 Estimations et impacts (revue backend du 2026-11-12)

Estimations pour un développeur, tests unitaires et d'intégration compris (seuil JaCoCo), doc à jour.

| Besoin | Estimation | Ce qui pèse |
|---|---|---|
| S01 + S02 | 3 j | voir 6.1 |
| B30 | 0,5 j | réutilise la création de commission dans la même transaction |
| B26 | 6 à 8 j | modèle de rythme, migration, génération des tours, simulation, `echeanceAt`, traitement planifié fin (toutes les 5–15 min, anti-doublon, verrou multi-instance), fuseau `Africa/Dakar` (JVM, Jackson, `@Scheduled(zone)`) |
| B29 | 6 à 8 j | journal de mouvements branché sur cotisations, jackpots, distributions, commissions ; reprise de l'historique ; test de cohérence |
| B27 | 7 à 9 j | détection planifiée, ~12 endpoints, lots, paiement, déduction au jackpot (à la clôture **et** à `PATCH /beneficiaire`, nouvel endpoint de prévisualisation), résumés B04/B05/B09 |
| B28 | 9 à 12 j | éligibilité, 8 statuts, remboursements au prorata, distribution finale (colonnes ajoutées à `distributions_evenementielles`, découpage préalable de `computeFinalDistribution`) |
| B31 | 5 à 7 j + 3 à 4 j (push, B19) + 2 à 4 j (WhatsApp Business) | aiguillage, préférences, heures calmes, regroupement, journal d'envoi |
| **Total** | **≈ 43 à 58 j (9 à 12 semaines)** | |

Points d'attention :
- **WhatsApp** : le backend utilise aujourd'hui **OpenWA** (API non officielle, texte libre). Les modèles
  approuvés par Meta supposent **WhatsApp Business** : changement de fournisseur, vérification
  d'entreprise et approbation des modèles (1 à 3 semaines de délai externe), envoi payant. **Lancer la
  démarche Meta dès maintenant**, en parallèle.
- **Push** : dépend de B19 (abonnements Web Push, clés VAPID par environnement), en stand-by.
- **`REGLES_MODIFIEES`** : nouveau type générique (rythme, amendes, prêts) ; `ORDRE_BENEFICIAIRE_MODIFIE`
  (déjà livré) est conservé.
- **Caisses (B29)** : la reprise de l'historique est exacte pour les rotatives ; pour les événementielles
  clôturées **avant V034**, les parts par membre n'ont pas été enregistrées.
- **`frequence` (B26)** : nouvelle valeur `PERSONNALISEE` ; `calculateEndDate` et les rappels
  événementiels passent au rythme.
- **Migrations** : à partir de **V038**.

### B26 — Rythme libre, jour et heure d'échéance, rappels avant échéance
- **Écrans** : Créer une tontine (étape « rythme »), Modifier, Réglages, Fiche Tontine, Tours, Invitation.
- **Existant** : `frequence` n'accepte que 5 codes (`JOURNALIERE`, `HEBDOMADAIRE`, `BIMENSUEL`, `MENSUEL`, `TRIMESTRIEL`). Impossible de dire « tous les 3 jours », « tous les 10 jours », « tous les 2 mois », « tous les 6 mois », ni « chaque dimanche avant 23h59 » ou « chaque mois le 30 (après le salaire) ». Les tours n'ont qu'une `dateFin` (jour), sans heure limite.
- **Contournement front** : aucun possible (le backend génère les tours, les retards et les rappels).
- **Proposition — nouveaux champs** (dans `CreateTontineRequest`, `UpdateTontineRequest`, `TontineResponse`, `InvitationApercuResponse`, `TontineCotisant`, `TontineStats`) :
  ```json
  "rythmeIntervalle": 3,              // entier ≥ 1
  "rythmeUnite": "JOUR",              // JOUR | SEMAINE | MOIS
  "jourEcheance": "DIMANCHE",         // SEMAINE : LUNDI…DIMANCHE ; MOIS : "1"…"31" ou "DERNIER" ; JOUR : null
  "heureEcheance": "23:59",           // HH:mm, défaut 23:59
  "rappels": { "veille": true, "heuresAvant": 3 }   // défaut : veille + 3 h avant
  ```
  - Bornes : `JOUR` 1 à 90, `SEMAINE` 1 à 26, `MOIS` 1 à 12.
  - **Compatibilité** : `frequence` reste renvoyé (lecture) : code historique si le rythme correspond, sinon `PERSONNALISEE`. En écriture, `frequence` seul reste accepté (traduit selon le tableau ci-dessous) ; si les deux sont envoyés, les nouveaux champs priment.
    | `frequence` historique | `rythmeIntervalle` | `rythmeUnite` |
    |---|---|---|
    | `JOURNALIERE` | 1 | `JOUR` |
    | `HEBDOMADAIRE` | 1 | `SEMAINE` |
    | `BIMENSUEL` | 2 | `SEMAINE` |
    | `MENSUEL` | 1 | `MOIS` |
    | `TRIMESTRIEL` | 3 | `MOIS` |
  - **Migration** des tontines existantes : traduction ci-dessus, `jourEcheance` = null, `heureEcheance` = 23:59, `rappels` par défaut. Aucun tour existant n'est modifié.
- **Génération des tours** (activation, mode automatique et sous-cycles événementiels) :
  - tour *k* : `dateDebut` = lendemain de l'échéance du tour *k−1* (le 1ᵉʳ commence à `dateDebut` de la tontine) ;
  - **échéance** du tour *k* : `dateDebut` du tour + intervalle, puis ramenée au **prochain `jourEcheance`** s'il est défini ; si `jourEcheance` = 29, 30 ou 31 et que le mois est plus court → **dernier jour du mois** ; `DERNIER` = dernier jour du mois ;
  - événementielle : le **dernier sous-cycle se termine sur `dateEcheance`** (règle actuelle conservée), même si l'intervalle n'est pas complet ;
  - mode manuel : à l'ouverture d'un tour, les dates proposées par défaut suivent le même calcul (le gestionnaire peut toujours les changer).
- **Nouveau champ sur les tours** (`CycleResponse`, `CycleEnCours`, `CycleEnCoursInfo`) : `echeanceAt` (date-heure ISO avec fuseau, ex. `2026-11-22T23:59:00Z`) = jour de fin + `heureEcheance`. C'est **la** limite de paiement du tour.
- **Simulation pour l'écran de création** (évite que le front calcule différemment du backend) :
  `GET /v1/tontines/simulation?rythmeIntervalle=3&rythmeUnite=JOUR&jourEcheance=&heureEcheance=23:59&dateDebut=2026-11-12&nombreMembres=10&tontineType=ROTATIVE&dateEcheance=` →
  ```json
  { "nombreTours": 10, "premiereEcheance": "2026-11-14T23:59:00Z", "derniereEcheance": "2026-12-11T23:59:00Z",
    "echeances": ["2026-11-14T23:59:00Z", "…"] }   // 400 échéances maximum
  ```
  Authentifié, sans effet en base.
- **Retard d'une cotisation** (règle unique, utilisée aussi par B27) :
  - une cotisation **n'est pas en retard** si elle a été **déclarée** (statut `EN_ATTENTE` puis `VALIDE`) **avant `echeanceAt`** (+ délai de tolérance de B27 le cas échéant) : c'est la **date de déclaration** qui compte, **pas la date de validation** ;
  - une déclaration **refusée** (B18) ne compte pas : si le refus arrive après l'échéance, le membre est en retard (à partir du refus) ;
  - pour un paiement **encaissé par le gestionnaire** (espèces remises en main propre), ajouter un champ facultatif `datePaiement` (date-heure, ≤ maintenant, ≥ début du tour, défaut maintenant) dans la requête d'encaissement et dans `Cotisation` : c'est elle qui compte pour le retard ;
  - le passage en `EN_RETARD` se fait par un traitement planifié à `echeanceAt` (pas seulement à la lecture), pour pouvoir notifier.
- **Rappels avant échéance** : traitement planifié qui envoie, aux membres **actifs** n'ayant **pas encore déclaré** pour le tour en cours :
  - la **veille** à 18h00 (si `rappels.veille`) ;
  - **N heures avant** `echeanceAt` (si `rappels.heuresAvant` > 0, 1 à 24) ;
  - type `RAPPEL_COTISATION` (existant), canaux selon B31 ; pas d'envoi WhatsApp entre 21h00 et 8h00 (reporté à 8h00) ; jamais deux fois le même rappel pour le même tour.
- **Modification** : rythme (intervalle, unité) modifiable seulement en `BROUILLON` ; `jourEcheance`, `heureEcheance`, `rappels` modifiables quand la tontine est active, **pour les tours pas encore commencés** (notification `REGLES_MODIFIEES`).
- **Exports CSV** (B24) : ajouter le rythme lisible (« tous les 3 jours, avant 23h59 ») et `echeanceAt` par tour.
- **Codes d'erreur** : `RYTHME_INVALIDE` (unité ou intervalle hors bornes), `JOUR_ECHEANCE_INVALIDE` (incohérent avec l'unité), `HEURE_ECHEANCE_INVALIDE`, `RYTHME_NON_MODIFIABLE` (tontine non brouillon).
- **Critères d'acceptation** :
  - « tous les 3 jours » à partir du 12/11, 10 membres → 10 tours, échéances 14/11, 17/11… à 23h59 ;
  - « chaque semaine le dimanche » à partir d'un mercredi → 1ʳᵉ échéance le dimanche suivant ;
  - « chaque mois le 31 » → 30/11, 31/12, 28/02 (ou 29), 31/03 ;
  - une déclaration à 23h58 le jour d'échéance, validée le lendemain → **pas en retard** ;
  - une tontine créée avec l'ancien `frequence: "MENSUEL"` fonctionne comme avant.
- **✅ Livré (2026-11-14)**, en 4 lots : rythme, échéances et simulation (V039) ; échéance et rappels modifiables en cours (`PATCH /tontines/{id}/echeance`, `REGLES_MODIFIEES`) ; rappels veille / N h avant et `COTISATIONS_MANQUANTES` (V040) ; export CSV (colonnes ajoutées en fin de ligne).
  - **Écarts assumés** : à l'échéance, le statut ne passe pas `EN_RETARD` (règle S01 : seulement à la clôture), seule la notification `COTISATIONS_MANQUANTES` part ; avec un jour d'échéance fixe, la 1ʳᵉ échéance est l'occurrence **suivante** (jamais le jour même du démarrage) ; `rappels` n'est exposé que dans `TontineResponse` ; le statut d'une échéance (A_VENIR / EN_COURS / ECHUE) sera calculé avec B32 ; WhatsApp des rappels envoyé hors 21h–8h (aiguillage complet avec B31) ; l'export du fil d'activité (`/me/activite/export`) n'a pas reçu ces colonnes.

### B27 — Amendes de retard (tontines rotatives)
- **Écrans** : Créer une tontine et Réglages (règle d'amende), À valider (section « Retards à traiter »), Fiche Tontine (membre : « À régler »), Payer (payer une amende), Historique, Caisse (B29).
- **Existant** : rien (seul le statut `EN_RETARD` existe).
- **Dépendances** : B26 (échéance `echeanceAt`), B29 (caisse de destination), B31 (notifications).
- **Règle d'amende par tontine** — `reglesAmende` dans `CreateTontineRequest`, `TontineResponse`, aperçu d'invitation, et modifiable par `PUT /v1/tontines/{id}/regles-amende` :
  ```json
  "reglesAmende": {
    "active": true,
    "mode": "FIXE",                     // FIXE (montant en F) | POURCENTAGE (de la cotisation du tour)
    "valeur": 500,                      // F si FIXE ; 1 à 100 si POURCENTAGE
    "delaiToleranceMinutes": 0,         // 0 à 1440, après echeanceAt
    "application": "VALIDATION_GESTIONNAIRE",   // seule valeur pour l'instant (AUTOMATIQUE réservé pour plus tard)
    "caisse": "COMMUNE",                // COMMUNE | AMENDES (voir B29)
    "deductionJackpot": "PROPOSEE"      // PROPOSEE (proposée au gestionnaire au moment du jackpot) | JAMAIS
  }
  ```
  - Refusé sur une tontine événementielle (`AMENDE_NON_DISPONIBLE`).
  - Inactive par défaut (`active: false`).
  - Une modification s'applique aux tours pas encore commencés ; chaque amende garde une copie de la règle (mode, valeur, caisse).
- **Amende** (nouvelle ressource) :
  ```json
  { "id", "tontineId", "membreId", "membre": { "firstName", "lastName", "phone" }, "cycleId", "numeroCycle",
    "montant": 500, "statut": "PROPOSEE", "caisse": "COMMUNE",
    "detecteeLe", "confirmeeLe", "confirmeePar", "motifRejet", "motifAnnulation", "annuleeLe", "annuleePar",
    "paiement": { "statut": "EN_ATTENTE|VALIDE|REFUSE", "methodePaiement", "reference", "declareLe", "valideLe", "motifRefus" },
    "deduiteDuJackpotCycleId": null }
  ```
  - **Statuts** :
    - `PROPOSEE` : détectée par le système, en attente de décision du gestionnaire ; le membre **ne la voit pas** ;
    - `REJETEE` : le gestionnaire l'a écartée (ex. il avait reçu l'argent en main propre) ; pas visible du membre ;
    - `CONFIRMEE` : due par le membre ;
    - `PAYEE` : paiement validé (ou déduite du jackpot) ;
    - `ANNULEE` : exonération après confirmation (motif **obligatoire** : maladie, deuil…), visible du membre.
  - **Unicité** : une seule amende par (membre, tour), quel que soit le statut sauf `REJETEE`.
  - Montant `POURCENTAGE` : calculé sur le montant de cotisation du tour, arrondi au franc.
- **Détection** (traitement planifié à `echeanceAt` + `delaiToleranceMinutes`, et au moment d'un refus de paiement après l'échéance) :
  - pour chaque membre **actif** du tour, **inscrit avant le début du tour**, sans cotisation déclarée à temps (règle de retard B26) → amende `PROPOSEE` ;
  - le bénéficiaire du tour est traité comme les autres (il doit cotiser) ;
  - si le membre déclare ou paie plus tard, l'amende **reste proposée** (le gestionnaire décide) ;
  - notification au gestionnaire **groupée par tontine et par tour** (« 3 retards sur le tour 5 ») : `AMENDE_DETECTEE`.
- **Endpoints gestionnaire** :
  - `GET /v1/tontines/{id}/amendes?statut=&membreId=&cycleId=&page=&size=` ;
  - `GET /v1/me/gestion/amendes?statut=PROPOSEE` : file de toutes ses tontines (alimente « À valider » et le compteur d'accueil B05) ;
  - `PUT /v1/tontines/{id}/amendes/{amendeId}/confirmer` ;
  - `PUT /v1/tontines/{id}/amendes/{amendeId}/rejeter` `{ "motif"?: string }` ;
  - `POST /v1/tontines/{id}/amendes/confirmer-lot` `{ "ids": [...] }` et `…/rejeter-lot` (comme B14) ;
  - `PUT /v1/tontines/{id}/amendes/{amendeId}/annuler` `{ "motif": string }` (exonération d'une amende confirmée, non payée) ;
  - `POST /v1/tontines/{id}/amendes` `{ "membreId", "cycleId" }` : amende manuelle pour un tour échu (cas où le système n'a pas pu détecter), créée directement `CONFIRMEE` ;
  - `POST /v1/tontines/{id}/amendes/{amendeId}/encaisser` `{ "methodePaiement", "reference"?, "datePaiement"? }` : paiement reçu en main propre, directement `PAYEE`.
- **Endpoints membre** :
  - `GET /v1/me/amendes?tontineId=&statut=` : ses amendes `CONFIRMEE`, `PAYEE`, `ANNULEE` (jamais `PROPOSEE` ni `REJETEE`) ;
  - `POST /v1/tontines/{id}/amendes/{amendeId}/paiement` `{ "methodePaiement", "reference"? }` : déclare le paiement (**à tout moment**, y compris avant le tour suivant) ; le gestionnaire le valide ou le refuse :
    - `PUT /v1/tontines/{id}/amendes/{amendeId}/paiement/valider` ;
    - `PUT /v1/tontines/{id}/amendes/{amendeId}/paiement/refuser` `{ "motif" }`.
  - Le paiement de l'amende est **séparé** de la cotisation, en **une fois** (montant total de l'amende).
- **Règle fondamentale — ne jamais bloquer** : aucun endpoint de cotisation (déclarer, encaisser, valider) ne vérifie les amendes. Un membre avec des amendes impayées **cotise normalement** les tours suivants et **reste éligible** au jackpot.
- **Au moment du jackpot du membre** (désignation du bénéficiaire / clôture du tour où il reçoit) :
  - si `deductionJackpot` = `PROPOSEE` et que le bénéficiaire a des amendes `CONFIRMEE` non payées, la réponse de prévisualisation / clôture renvoie `amendesImpayees: { nombre, montant }` ;
  - la requête de clôture accepte `deduireAmendes: true` → les amendes passent `PAYEE` (`methodePaiement: "DEDUCTION_JACKPOT"`, `deduiteDuJackpotCycleId`), le montant remis au bénéficiaire est diminué d'autant (`BeneficiaireResponse.montantDeduitAmendes`), mouvement de caisse (B29) ;
  - sans `deduireAmendes`, rien n'est déduit (jamais de déduction silencieuse).
- **Fin de tontine** : les amendes confirmées non payées restent dues (listées dans `GET /v1/tontines/{id}/amendes?statut=CONFIRMEE`), visibles du gestionnaire et du membre.
- **Confidentialité** : un membre ne voit que **ses** amendes ; la caisse (B29) montre aux autres le **total** des amendes encaissées, sans nom.
- **Résumés et fil** :
  - B04 (Je cotise) : par tontine et au total, `amendesDues: { nombre, montant }` ;
  - B05 (Je gère) : `amendesAConfirmer`, `paiementsAmendeAValider` ;
  - B09 (activité) : nouveaux types `AMENDE_INFLIGEE`, `AMENDE_PAYEE`, `AMENDE_ANNULEE` (membre) ; `AMENDE_DETECTEE`, `AMENDE_PAIEMENT_DECLARE` (gestionnaire).
- **Notifications** (canaux B31) :
  - `AMENDE_DETECTEE` → gestionnaire (groupée) ;
  - `AMENDE_INFLIGEE` → membre, à la confirmation : « Vous n'avez pas cotisé avant dimanche 23h59 (tour 5) : amende de 500 F. Vous pouvez continuer à cotiser normalement. » ;
  - `AMENDE_ANNULEE` → membre ;
  - `AMENDE_PAIEMENT_DECLARE` → gestionnaire ;
  - `AMENDE_PAYEE` / `AMENDE_PAIEMENT_REFUSE` → membre.
- **Export** : `GET /v1/tontines/{id}/amendes/export?format=csv&statut=`.
- **Codes d'erreur** : `AMENDE_NON_DISPONIBLE`, `AMENDE_DEJA_EXISTANTE`, `AMENDE_STATUT_INVALIDE` (ex. payer une amende proposée), `AMENDE_TOUR_NON_ECHU`, `AMENDE_MOTIF_REQUIS`.
- **Critères d'acceptation** :
  - échéance dimanche 23h59, aucun paiement déclaré → lundi 00h00, amende `PROPOSEE`, gestionnaire notifié, membre **non** notifié ;
  - le gestionnaire confirme → membre notifié, amende dans « À régler » ;
  - le membre déclare sa cotisation du tour suivant sans payer l'amende → **accepté** ;
  - au tour où il reçoit le jackpot, le gestionnaire choisit de déduire → jackpot diminué, amende payée ;
  - un deuxième passage du traitement planifié ne crée pas de doublon.

### B28 — Prêts sur la cagnotte (tontines événementielles)
- **Écrans** : Fiche Tontine (« Emprunter », « Mon prêt »), Demande de prêt, À valider (section « Demandes de prêt » et « Remboursements »), Caisse (B29), Distribution finale.
- **Existant** : rien.
- **Dépendances** : B29 (caisse, disponible), B31 (notifications), distribution finale existante (`distributionParMembre`).
- **Règle de prêt par tontine** — `reglesPret` dans `CreateTontineRequest`, `TontineResponse`, aperçu d'invitation, et `PUT /v1/tontines/{id}/regles-pret` :
  ```json
  "reglesPret": {
    "active": true,
    "fraisPourcentage": 0,              // 0 par défaut (point sensible : tontine religieuse) ; 0 à 50 ; frais fixes sur le montant accordé
    "caisseFrais": "COMMUNE",           // COMMUNE | FRAIS_PRET (voir B29)
    "plafondMultipleCotise": 1.0,       // prêt ≤ X fois ce que le membre a déjà cotisé (validé) ; null = pas de plafond
    "plafondMontant": null,             // plafond absolu en F ; null = aucun
    "partMaxCagnottePct": 50,           // part maximum de la cagnotte prêtée en même temps (1 à 100)
    "dureeMaxJours": 60,                // durée maximum d'un prêt
    "margeAvantEvenementJours": 7       // tout prêt doit être remboursé au plus tard X jours avant dateEcheance
  }
  ```
  - Refusé sur une tontine rotative (`PRET_NON_DISPONIBLE`). Inactif par défaut.
- **Éligibilité d'un membre** : tontine `ACTIVE` ; membre `ACTIF` ; **à jour** (aucune cotisation `EN_RETARD` sur les sous-cycles échus) ; **aucun prêt** `DEMANDE`, `ACCORDE` ou `EN_COURS` ; aujourd'hui + 1 jour ≤ `dateEcheance` − `margeAvantEvenementJours`.
  - `GET /v1/tontines/{id}/prets/eligibilite` (membre) →
    ```json
    { "eligible": false, "motifs": ["COTISATION_EN_RETARD"], "montantMax": 40000, "dateLimiteMax": "2027-05-20",
      "fraisPourcentage": 0, "exemple": { "montant": 20000, "frais": 0, "aRembourser": 20000 } }
    ```
  - `montantMax` = minimum de (plafond multiple × cotisé validé, `plafondMontant`, **disponible** de la caisse commune − réserve liée à `partMaxCagnottePct`).
- **Prêt** (nouvelle ressource) :
  ```json
  { "id", "tontineId", "membreId", "membre": {…},
    "montantDemande", "montantAccorde", "fraisPourcentage", "montantFrais", "montantDu", "montantRembourse", "resteDu",
    "motif", "dureeSouhaiteeJours", "dateLimiteRemboursement",
    "statut": "DEMANDE", "enRetard": false,
    "demandeLe", "decideLe", "decidePar", "motifRefus",
    "remise": { "methodePaiement", "reference", "remisLe", "remisPar" },
    "caisseFrais": "COMMUNE", "soldeLe", "deduitDistributionLe" }
  ```
  - `montantFrais` = arrondi(`montantAccorde` × `fraisPourcentage` / 100) ; `montantDu` = accordé + frais ; `resteDu` = dû − remboursé (validé).
  - **Statuts** :
    - `DEMANDE` : envoyée par le membre ;
    - `REFUSE` : refusée par le gestionnaire (motif facultatif) ;
    - `ANNULE` : retirée par le membre avant la remise de l'argent ;
    - `ACCORDE` : acceptée, argent pas encore remis (le montant est **réservé** dans le disponible) ;
    - `EN_COURS` : argent remis ;
    - `REMBOURSE` : `resteDu` = 0 ;
    - `DEDUIT` : soldé par déduction sur la part de l'emprunteur à la distribution finale ;
    - `DEFAUT` : part insuffisante à la distribution, un reste dû subsiste.
  - `enRetard` = `EN_COURS` et date du jour > `dateLimiteRemboursement` (pas de pénalité pour l'instant).
- **Endpoints membre** :
  - `POST /v1/tontines/{id}/prets` `{ "montant", "dureeJours"?, "motif"? }` : crée une `DEMANDE` (contrôle d'éligibilité) ;
  - `GET /v1/me/prets?tontineId=&statut=` ;
  - `PUT /v1/tontines/{id}/prets/{pretId}/annuler` (seulement `DEMANDE` ou `ACCORDE`) ;
  - `POST /v1/tontines/{id}/prets/{pretId}/remboursements` `{ "montant", "methodePaiement", "reference"? }` : remboursement **partiel ou total** (≤ `resteDu`), statut `EN_ATTENTE`.
- **Endpoints gestionnaire** :
  - `GET /v1/tontines/{id}/prets?statut=&page=&size=` ;
  - `GET /v1/me/gestion/prets?statut=DEMANDE` et `GET /v1/me/gestion/remboursements?statut=EN_ATTENTE` : files de toutes ses tontines (« À valider », compteur B05) ;
  - `PUT /v1/tontines/{id}/prets/{pretId}/accorder` `{ "montantAccorde"?, "dateLimiteRemboursement"? }` : peut réduire le montant et fixer la date (≤ limite de la règle) ; refusé si le disponible ne suffit pas (`PRET_FONDS_INSUFFISANTS`) ;
  - `PUT /v1/tontines/{id}/prets/{pretId}/refuser` `{ "motif"? }` ;
  - `PUT /v1/tontines/{id}/prets/{pretId}/remettre` `{ "methodePaiement", "reference"?, "dateRemise"? }` : argent remis → `EN_COURS` ;
  - `PUT /v1/tontines/{id}/prets/{pretId}/remboursements/{rembId}/valider` et `…/refuser` `{ "motif" }` ;
  - `POST /v1/tontines/{id}/prets/{pretId}/remboursements/encaisser` `{ "montant", "methodePaiement", "reference"?, "datePaiement"? }` : reçu en main propre, directement validé.
- **Répartition d'un remboursement validé** : au prorata entre capital et frais (capital = montant × accordé / dû ; frais = le reste). Le capital retourne à la **caisse commune** ; les frais vont à la caisse choisie (`caisseFrais`). Mouvements B29.
- **Distribution finale** (clôture du dernier sous-cycle) — compléter `distributionParMembre` :
  ```json
  { "montantCotise", "montantCommission", "partFraisPret", "resteDuPret", "montantNet" }
  ```
  - `partFraisPret` : si `caisseFrais` = `COMMUNE`, les frais encaissés sont répartis entre **tous** les membres au prorata de leur `montantCotise` ;
  - `montantNet` = cotisé − commission + part des frais − reste dû du prêt ;
  - si `montantNet` < 0 : `montantNet` = 0, le prêt passe `DEFAUT` avec `resteDu` = la différence ; sinon il passe `DEDUIT` ;
  - **avant clôture**, la prévisualisation renvoie `pretsEnCours: [{ membre, resteDu }]` pour que le gestionnaire confirme en connaissance de cause (la clôture n'est pas bloquée).
- **Confidentialité** :
  - la liste et le détail d'un prêt ne sont accessibles qu'au **gestionnaire** et à **l'emprunteur** (et au super-admin) ; tout autre membre reçoit 403 ;
  - les autres membres voient seulement, dans la caisse (B29), `montantPrete` (total en cours) et `disponible`, **sans nom** ;
  - dans `distributionParMembre`, un membre ne voit que **sa** ligne détaillée (le gestionnaire voit tout).
- **Rappels et retards** (traitement planifié, canaux B31) :
  - `PRET_RAPPEL_ECHEANCE` à J−3 et J−1 avant `dateLimiteRemboursement` → emprunteur ;
  - `PRET_EN_RETARD` le lendemain de la date limite → emprunteur et gestionnaire ;
  - rappel au gestionnaire 7 jours avant `dateEcheance` s'il reste des prêts en cours.
- **Résumés et fil** :
  - B04 : `pret: { resteDu, dateLimiteRemboursement, enRetard }` par tontine ;
  - B05 : `pretsADecider`, `remboursementsAValider`, `montantPrete` ;
  - B09 : types `PRET_DEMANDE`, `PRET_ACCORDE`, `PRET_REFUSE`, `PRET_REMIS`, `REMBOURSEMENT_DECLARE`, `REMBOURSEMENT_VALIDE`, `PRET_SOLDE`, `PRET_DEDUIT` (chacun visible seulement du gestionnaire et de l'emprunteur).
- **Notifications** : mêmes types que ci-dessus ; `PRET_DEMANDE` et `REMBOURSEMENT_DECLARE` → gestionnaire ; les autres → emprunteur.
- **Export** : `GET /v1/tontines/{id}/prets/export?format=csv` (gestionnaire).
- **Codes d'erreur** : `PRET_NON_DISPONIBLE`, `PRET_NON_ELIGIBLE` (+ `motifs`), `PRET_DEJA_EN_COURS`, `PRET_MONTANT_TROP_ELEVE` (+ `montantMax`), `PRET_DATE_TROP_TARDIVE` (+ `dateLimiteMax`), `PRET_FONDS_INSUFFISANTS`, `PRET_STATUT_INVALIDE`, `REMBOURSEMENT_SUPERIEUR_AU_RESTE_DU`.
- **Critères d'acceptation** :
  - cagnotte de 500 000 F, part max 50 % → au plus 250 000 F prêtés en même temps ;
  - membre ayant cotisé 30 000 F, plafond ×1 → `montantMax` = 30 000 F ;
  - prêt de 20 000 F à 10 % → 22 000 F à rembourser ; remboursements de 11 000 F + 11 000 F → `REMBOURSE`, 2 000 F de frais dans la caisse choisie ;
  - prêt non remboursé à la distribution : reste dû 8 000 F, part nette 50 000 F → part versée 42 000 F, prêt `DEDUIT` ;
  - un autre membre qui appelle `GET …/prets/{pretId}` → 403.

### B29 — Caisses de la tontine et mouvements
- **Écrans** : Fiche Tontine (rubrique « Caisse » : solde et mouvements), Réglages (choix des caisses), Distribution finale.
- **Existant** : aucun suivi des entrées et sorties hors cotisations et jackpots.
- **Proposition** :
  - Chaque tontine a une caisse **`COMMUNE`** (toujours), et selon les choix du gestionnaire une caisse **`AMENDES`** (B27) et une caisse **`FRAIS_PRET`** (B28).
  - **Mouvement de caisse** (écrit **dans la même transaction** que l'opération qui le provoque, jamais modifié ensuite ; une correction = un mouvement inverse) :
    ```json
    { "id", "tontineId", "caisse": "COMMUNE", "sens": "ENTREE", "type": "COTISATION", "montant": 5000,
      "date", "membreId", "referenceType": "COTISATION", "referenceId", "libelle", "creePar" }
    ```
    Types :
    | Type | Sens | Caisse |
    |---|---|---|
    | `COTISATION` | entrée | commune |
    | `JACKPOT` / `DISTRIBUTION` | sortie | commune |
    | `COMMISSION_GESTIONNAIRE` | sortie | commune |
    | `PRET_REMIS` | sortie | commune |
    | `REMBOURSEMENT_CAPITAL` | entrée | commune |
    | `FRAIS_PRET` | entrée | commune ou frais de prêt |
    | `AMENDE` | entrée (quand payée) | commune ou amendes |
    | `DEDUCTION_JACKPOT` | (écriture interne : amende payée par déduction) | commune ou amendes |
    | `SORTIE_MANUELLE` | sortie | amendes ou frais de prêt |
    | `REDISTRIBUTION` | sortie | amendes ou frais de prêt |
    | `AJUSTEMENT` | entrée ou sortie | toutes (super-admin) |
  - `GET /v1/tontines/{id}/caisse` (tous les membres) →
    ```json
    { "commune":  { "solde", "totalEntrees", "totalSorties", "montantPrete", "disponible" },
      "amendes":  { "solde", "totalEntrees", "totalSorties" },
      "fraisPret":{ "solde", "totalEntrees", "totalSorties" } }
    ```
    (`amendes` et `fraisPret` absents si non utilisées.) `disponible` = solde commune − prêts accordés non encore remis.
  - `GET /v1/tontines/{id}/caisse/mouvements?caisse=&type=&dateDebut=&dateFin=&page=&size=` (tous les membres) : **anonymisé** pour les membres qui ne sont ni gestionnaire ni concernés : pour `PRET_*`, `REMBOURSEMENT_*` et `AMENDE`, `membreId` = null et `libelle` générique (« Prêt accordé », « Amende encaissée »).
  - `POST /v1/tontines/{id}/caisse/sorties` (gestionnaire) `{ "caisse": "AMENDES", "montant", "motif" }` : utilisation d'une caisse dédiée (ex. achat pour l'événement) ; refusé sur la caisse commune (`CAISSE_SORTIE_INTERDITE`) ; refusé si le solde ne suffit pas (`CAISSE_SOLDE_INSUFFISANT`) ; notification `CAISSE_SORTIE` à **tous** les membres (transparence).
  - `POST /v1/tontines/{id}/caisse/{caisse}/redistribuer` (gestionnaire, caisse dédiée) : répartit le solde entre les membres actifs au prorata de leurs cotisations validées → un mouvement `REDISTRIBUTION` par membre ; possible à tout moment, proposé à la clôture de la tontine.
  - **Choix des caisses** : `caisse` dans `reglesAmende` et `caisseFrais` dans `reglesPret`. Changer de caisse s'applique aux nouvelles amendes et aux nouveaux prêts ; le solde déjà dans une caisse dédiée y reste.
  - **Migration** : générer les mouvements des tontines existantes à partir des cotisations validées, des jackpots, des distributions et des commissions, pour que les soldes soient justes dès la mise en production.
  - **Contrôle de cohérence** : solde commune = cotisations validées − jackpots/distributions − commissions − prêts remis + capital remboursé (+ frais et amendes si caisse commune) ; un test automatique doit le vérifier.
- **Codes d'erreur** : `CAISSE_INCONNUE`, `CAISSE_SORTIE_INTERDITE`, `CAISSE_SOLDE_INSUFFISANT`.

### B30 — Commission du gestionnaire dans la création
- **Écran** : Créer une tontine (étape Récapitulatif, déjà faite côté front).
- **Existant** : la commission se crée après coup par `POST /v1/tontines/{id}/commissions`. Le front enchaîne les deux appels ; si le second échoue, la tontine est créée sans commission (message à l'utilisateur).
- **Proposition** : `CreateTontineRequest` accepte `"commissions": [{ "type", "valeur", "description"? }]` (0 ou plusieurs ; mêmes règles que `POST …/commissions`), enregistrées **dans la même transaction**. Absent ou vide = aucune commission (0).
- **Codes d'erreur** : ceux de la création de commission (valeur ≤ 0, pourcentage > 100…).
- **✅ Livré (2026-11-13)** : tout est vérifié avant d'enregistrer la tontine ; si une commission est refusée, rien n'est créé.
  - valeur ≤ 0 → 400 `VALIDATION_ERROR` ;
  - type en double dans la liste → 409 `COMMISSION_ALREADY_EXISTS` (même code pour `POST …/commissions` si le type existe déjà) ;
  - `POURCENTAGE_JACKPOT` > 100 → 400 `COMMISSION_PERCENTAGE_TOO_HIGH` (nouveau plafond, appliqué aussi à `POST` et `PUT …/commissions`).
  - La réponse de création reste un `TontineResponse` ; les commissions se relisent par `GET /v1/tontines/{id}/commissions`.

### B31 — Canaux de notification
- **Écrans** : toutes les alertes ; Paramètres (préférences de canaux).
- **Existant** : notifications in-app ; rappels WhatsApp mentionnés dans la documentation ; push prévu en P2 (B19).
- **Proposition** :
  - **Canaux** : `IN_APP`, `PUSH` (Web Push, abonnements de B19), `WHATSAPP` (API WhatsApp Business), `SMS`.
  - **SMS réservé** aux annonces de **réception d'argent** : `JACKPOT_DISTRIBUE` (le membre reçoit le jackpot) et `DISTRIBUTION_FINALE` (part versée). *À confirmer avec le porteur du projet : SMS en plus des autres canaux pour ces deux types.*
  - **Canaux par défaut par type** :
    | Types | In-app | Push | WhatsApp | SMS |
    |---|---|---|---|---|
    | `RAPPEL_COTISATION`, `PAIEMENT_EN_RETARD` | ✓ | ✓ | ✓ | — |
    | `AMENDE_INFLIGEE`, `AMENDE_ANNULEE`, `AMENDE_PAYEE`, `AMENDE_PAIEMENT_REFUSE` | ✓ | ✓ | ✓ | — |
    | `AMENDE_DETECTEE`, `AMENDE_PAIEMENT_DECLARE` (gestionnaire) | ✓ | ✓ | — | — |
    | `PRET_*`, `REMBOURSEMENT_*` | ✓ | ✓ | ✓ | — |
    | `CAISSE_SORTIE`, `REGLES_MODIFIEES` | ✓ | ✓ | ✓ | — |
    | `JACKPOT_DISTRIBUE`, `DISTRIBUTION_FINALE` | ✓ | ✓ | ✓ | ✓ |
    | autres types existants | ✓ | ✓ | — | — |
  - **Préférences** : `GET/PUT /v1/me/preferences/notifications` → `{ "whatsapp": true, "push": true }` (l'utilisateur peut couper WhatsApp ou push ; in-app toujours actif ; SMS de jackpot non désactivable).
  - **WhatsApp** :
    - modèles de messages à faire **approuver par Meta** à l'avance : un par type ci-dessus, en français, avec montant, tontine, tour et lien vers l'app ;
    - pas d'envoi entre **21h00 et 8h00** (reporté à 8h00), sauf annonces de jackpot ;
    - regroupement : au plus **un message WhatsApp par tontine et par jour** pour un même membre (les rappels du jour sont fusionnés).
  - Chaque notification garde le lien précis vers l'écran concerné (B19), ex. `/tontine/{id}?amende={amendeId}`, `/tontine/{id}?pret={pretId}`.
  - Journal d'envoi par canal (envoyé / échec / motif) consultable par le super-admin.

### B32 — Rythme du jackpot distinct du rythme de cotisation (tontines rotatives)
- **Écrans** : Créer une tontine, Modifier, Réglages, Fiche Tontine, Tours, Payer, Encaisser, À valider, Invitation, Accueil (bandeaux), Mes jackpots.
- **Existant** : 1 cycle = 1 tour = 1 jackpot = une seule cotisation par membre (`POST /tontines/{id}/cotisations` : une 2ᵉ déclaration sur le même cycle → 409 `COTISATION_ALREADY_EXISTS`). Impossible de cotiser chaque semaine et de verser le jackpot chaque mois.
- **Contournement front** : aucun (échéances, retards et rappels sont gérés par le backend).
- **Dépendance** : B26 (rythme, `jourEcheance`, `echeanceAt`). Non bloquante : B32 fonctionne aussi avec les rythmes actuels.
- **Décision produit (2026-11-13)** : le rythme du jackpot s'exprime en **nombre de cotisations par tour**, pas en durée calendaire. Le jackpot reste fixe (ex. 5 000 F × 10 membres × 4 = 200 000 F), sans le problème des mois à 4 ou 5 semaines.
- **Cas à couvrir** (une rotative combine librement rythme de cotisation, nombre de cotisations par tour, déclenchement de la remise et mode de désignation) :
  | Cas | Cotisation | Jackpot | Paramétrage |
  |---|---|---|---|
  | Classique | chaque mois, avant le 30 | à chaque cotisation, une fois que tous ont payé | rythme 1 `MOIS` + `jourEcheance: "30"`, `cotisationsParTour: 1`, `declenchementJackpot: TOUS_PAYES` |
  | Hebdo / mensuel | chaque semaine, le dimanche | toutes les 4 cotisations (≈ 1 mois) | rythme 1 `SEMAINE` + `jourEcheance: "DIMANCHE"`, `cotisationsParTour: 4`, `declenchementJackpot: FIN_DU_TOUR` |
  | Journalière | chaque jour (marchés) | toutes les 10 cotisations | rythme 1 `JOUR`, `cotisationsParTour: 10` |
  | Manuel | selon le gestionnaire | à la date choisie par le gestionnaire | `modeCycle: MANUEL`, date de fin proposée ou choisie (voir plus bas) |

  Dans tous les cas, les gagnants sont désignés selon `ordreBeneficiaire` (ordre, tirage, choix, hybride) et les priorités de jackpot déjà livrées.
- **Nouveaux champs** (`CreateTontineRequest`, `UpdateTontineRequest`, `TontineResponse`, `InvitationApercuResponse`, `TontineCotisant`, `TontineStats`) :
  ```json
  "cotisationsParTour": 4,                 // entier ≥ 1, défaut 1 (= fonctionnement actuel)
  "declenchementJackpot": "FIN_DU_TOUR"    // FIN_DU_TOUR | TOUS_PAYES, défaut FIN_DU_TOUR
  ```
  - réservés aux ROTATIVE ; pour une EVENEMENTIELLE, `cotisationsParTour` = 1 (sinon `COTISATIONS_PAR_TOUR_INVALIDE`) ;
  - **pas de borne** (décision 2026-11-13) : ni maximum sur `cotisationsParTour`, ni limite de durée d'un tour (N × rythme) ; le front affiche la durée résultante via la simulation B26 ;
  - modifiables seulement en `BROUILLON` (`COTISATIONS_PAR_TOUR_NON_MODIFIABLE`).
- **Jackpot visé** (ajout 2026-11-13) : au lieu de `cotisationsParTour`, le gestionnaire peut saisir `jackpotVise` (montant **brut**, avant commission). L'app en déduit N = `jackpotVise` ÷ (`montant` × membres ÷ `nombreGagnants`) :
  - ex. 10 membres × 5 000 F/semaine = 50 000 F par semaine → 500 000 F visés = **10 cotisations par tour** (≈ 10 semaines) ; avec 20 % de commission, le bénéficiaire reçoit 400 000 F ;
  - division inexacte : l'app propose le N le plus proche et affiche le jackpot réel (la simulation B26 renvoie `cotisationsParTour` et `jackpotParTour`) ;
  - seul `cotisationsParTour` est enregistré ; si les deux sont envoyés, `cotisationsParTour` prime.
- **`declenchementJackpot`** :
  - `FIN_DU_TOUR` : remise possible (mode automatique : faite) à l'échéance de la dernière cotisation du tour, même si des membres n'ont pas payé (ils passent `EN_RETARD`, comme aujourd'hui) ;
  - `TOUS_PAYES` : remise possible dès que tous les membres actifs ont une cotisation validée sur toutes les échéances du tour (même avant la dernière échéance) ; mode automatique : clôture déclenchée à la dernière validation ; sinon le tour reste ouvert après l'échéance et le gestionnaire est notifié (`COTISATIONS_MANQUANTES`, B26) ; il peut toujours clôturer lui-même.
- **Modèle : un tour contient N échéances de cotisation**
  - nouvelle entité **échéance** rattachée à un tour : `id`, `cycleId`, `numero` (1 à N), `dateDebut`, `echeanceAt`, `statut` (`A_VENIR` | `EN_COURS` | `ECHUE`) ;
  - génération : chaque échéance commence le lendemain de la précédente et suit la règle de B26 (intervalle, `jourEcheance`, `heureEcheance`) ; la `dateFin` du tour = jour de sa dernière échéance ;
  - le nombre de tours ne change pas (`nombreMembres` / `nombreGagnants`) ; seule la durée d'un tour change (N × rythme).
- **Paiement : uniquement l'échéance en cours** (décision 2026-11-13)
  - un membre ne déclare que l'échéance en cours (pas d'avance sur plusieurs échéances) ; une échéance passée non payée reste déclarable (régularisation) ; une échéance future → `ECHEANCE_NON_OUVERTE` ;
  - une cotisation par membre **et par échéance** : le 409 `COTISATION_ALREADY_EXISTS` porte sur l'échéance ; une déclaration refusée (B18) peut être refaite ;
  - le gestionnaire qui encaisse (`/cotisations/admin`) suit la même règle, avec `echeanceId` explicite pour régulariser une échéance passée.
- **Mode manuel : date de fin flexible** (décision 2026-11-13)
  - à l'ouverture d'un tour, l'app propose une date de fin (dernière des N échéances depuis `dateDebut`) : `GET /v1/tontines/{id}/cycles/proposition?dateDebut=` → `{ "dateDebut", "dateFin", "echeances": [...] }` ;
  - `OpenCycleRequest.dateFin` devient facultatif (absent = date proposée) ;
  - date choisie : les échéances sont celles du rythme qui tombent dans [`dateDebut`, `dateFin`], la dernière est ramenée sur `dateFin` ; le nombre réel peut différer de N et le jackpot prévu suit ce nombre (`CycleResponse.nombreEcheances`) ; seule contrainte : au moins une échéance, sans limite de durée.
- **Jackpot et commission** : jackpot du tour = somme des cotisations validées de toutes ses échéances ; jackpot prévu = montant × membres actifs × nombre d'échéances (÷ `nombreGagnants`) ; `FRAIS_FIXES_PAR_CYCLE` prélevé **une fois par tour** ; `POURCENTAGE_JACKPOT` sur le jackpot du tour.
- **Retards, rappels, amendes : par échéance** : ponctualité, rappels veille / N heures avant, `COTISATIONS_MANQUANTES` calculés à chaque échéance (règles B26) ; B27 : une amende = une échéance manquée.
- **Changements d'API** :
  - `RecordCotisationRequest` / `AdminRecordCotisationRequest` : `echeanceId` (facultatif ; défaut = échéance en cours) ;
  - `CotisationResponse` : `echeanceId`, `numeroEcheance`, `echeanceAt` ;
  - `CycleResponse`, `CycleEnCours`, `CycleEnCoursInfo` : `nombreEcheances`, `echeances[]`, `echeanceEnCours`, `montantJackpotPrevu`, `tousPayes` ;
  - `CotisationDue` (`/me/summary`) : sur l'échéance en cours + `numeroEcheance`, `nombreEcheances`, `echeanceAt`, `echeancesEnRetard` ;
  - `GET /cotisations/recap/{cycleId}` : par membre `echeancesPayees` / `echeancesDues` + détail par échéance ; filtre `?echeanceId=` ;
  - `GET /membres?aPaye=` : accepte `echeanceId` en plus de `cycleId` ;
  - `/admin/my-dashboard` (collecté / attendu) et `/me/jackpots` (montant prévu) : sur le tour complet (× nombre d'échéances) ;
  - simulation B26 (`GET /tontines/simulation`) : paramètre `cotisationsParTour`, réponse avec les tours et leurs échéances.
- **Migration** : tontines existantes → `cotisationsParTour: 1`, `declenchementJackpot: FIN_DU_TOUR` ; chaque cycle existant reçoit une échéance reprenant ses dates ; les cotisations existantes y sont rattachées. Aucun changement de comportement.
- **Codes d'erreur** : `COTISATIONS_PAR_TOUR_INVALIDE`, `COTISATIONS_PAR_TOUR_NON_MODIFIABLE`, `ECHEANCE_INTROUVABLE`, `ECHEANCE_NON_OUVERTE`, `COTISATION_ALREADY_EXISTS` (désormais par échéance).
- **Critères d'acceptation** :
  - chaque semaine le dimanche, N = 4, 10 membres, 5 000 F → tour de 4 échéances, jackpot prévu 200 000 F, 10 tours ;
  - 2ᵉ déclaration sur la même semaine → 409 ; semaine suivante avant son ouverture → `ECHEANCE_NON_OUVERTE` ; régularisation de la semaine 2 en semaine 3 → acceptée, ponctualité `EN_RETARD` ;
  - chaque mois avant le 30, N = 1, `TOUS_PAYES` → dès la 10ᵉ validation, le tour peut être clôturé et le gagnant désigné ; le 30 passé avec un membre non payé → tour toujours ouvert, gestionnaire notifié ;
  - mode manuel : ouverture sans `dateFin` → date proposée ; avec une `dateFin` plus courte → moins d'échéances, jackpot prévu ajusté ;
  - commission fixe 2 000 F → prélevée une seule fois à la clôture du tour ;
  - tontine existante → fonctionne exactement comme avant.

### B33 — Cotisation séparée de la remise : retards régularisables, options de remise, clôture de la tontine
- **Écrans** : Tours, Remise du jackpot (nouvel aperçu), Payer, Encaisser, À valider, Réglages, Fiche Tontine.
- **Principe (porteur du projet, 2026-11-13)** : le gestionnaire a la main sur les dates de cotisation, les échéances, la fermeture d'un tour et la clôture de la tontine ; l'app l'accompagne (configuration automatique, aperçus, rappels) sans décider à sa place.
- **Constat (code au 2026-11-13)** :
  1. un retardataire ne peut plus payer une fois le tour clôturé (déclaration et encaissement réservés au tour `EN_COURS`) ; une déclaration `EN_ATTENTE` à la clôture passe `EN_RETARD` et **ne peut plus être validée** ;
  2. un seul tour `EN_COURS` à la fois : tant que le gestionnaire attend un retardataire, **personne ne peut payer l'échéance suivante** ;
  3. aucune clôture de tontine : rien ne la fait passer `TERMINEE`.
- **Hors plateforme** : les litiges entre gestionnaire et membres (retards, refus de continuer après avoir reçu le jackpot, tontines événementielles ou rotatives) se règlent **en dehors de Dinthialma** et n'engagent en aucun cas la plateforme. Les montants « reste à recevoir » et « avance à rembourser » sont des **informations de suivi**, pas des obligations de Dinthialma ; l'app ne détient aucun fonds. Clause à prévoir dans les CGU (affichée côté front).

#### 1. La cotisation ne dépend plus de la remise
- La période de cotisation d'un tour (ses échéances, B32) **s'ouvre à sa date**, que le tour précédent soit remis ou non : plusieurs tours peuvent être ouverts en même temps.
- **Remettre le jackpot** = clôturer le tour. Le gestionnaire décide quand (bouton « Remettre »), ou l'app le fait si `declenchementJackpot: TOUS_PAYES` (B32) et que le jackpot prévu est réuni.
- « Tour en cours » dans les résumés (`/me/summary`, `my-dashboard`, rappels) = tour dont la période contient aujourd'hui ; les tours précédents non remis apparaissent à part (`toursARemettre`).
- Mode manuel : le gestionnaire peut ouvrir le tour suivant sans avoir remis le précédent (`CYCLE_ALREADY_IN_PROGRESS` ne s'applique plus qu'à deux tours de même période).

#### 2. Régularisation d'un retard
- Un membre peut déclarer, et le gestionnaire encaisser, une **échéance passée**, y compris sur un tour déjà remis ; une déclaration restée en attente à la clôture peut être **validée** (`EN_RETARD` → `VALIDE`). La ponctualité reste `EN_RETARD` (S01).
- Un retard ne bloque jamais les échéances suivantes.
- La destination de l'argent dépend de l'option de remise retenue pour ce tour (voir 3).

#### 3. Options de remise du jackpot
Le gestionnaire choisit une **option par défaut** dans les réglages (`modeRemise`), et peut **en choisir une autre à chaque remise**. Exemple : 10 membres × 5 000 F × 10 semaines = jackpot prévu 500 000 F **brut**, commission 20 % ; un membre n'a pas payé 2 semaines → collecté 490 000 F, manque 10 000 F.

| `modeRemise` | À la remise | Régularisation des 10 000 F |
|---|---|---|
| `ATTENDRE` | refusée tant que le jackpot prévu n'est pas réuni (`REMISE_JACKPOT_INCOMPLETE`) ; relances aux retardataires | la remise devient possible |
| `DISPONIBLE` | commission 98 000 F (20 % de 490 000) ; bénéficiaire **392 000 F** ; reste à recevoir 8 000 F | commission 2 000 F ; complément de 8 000 F à remettre au bénéficiaire (total 400 000 F) |
| `AVANCE_COMMISSION` | le gestionnaire comble le manque sur sa commission : bénéficiaire **400 000 F** ; commission perçue 90 000 F | les 10 000 F reviennent au gestionnaire (total 100 000 F) |
| `AVANCE_GESTIONNAIRE` | manque supérieur à la commission : le gestionnaire avance la différence de sa poche (tracée) ; bénéficiaire jackpot net complet | les régularisations remboursent d'abord l'avance |
| `CAISSE` *(avec B29)* | le manque est pris dans la caisse commune | les régularisations reviennent à la caisse |

- `AVANCE_COMMISSION` n'est proposé que si la commission du tour couvre le manque ; au-delà, `AVANCE_GESTIONNAIRE`. `CAISSE` → `MODE_REMISE_INDISPONIBLE` tant que B29 n'existe pas.
- **Aperçu avant remise** : `GET /v1/tontines/{id}/cycles/{cycleId}/remise/apercu` → `jackpotPrevu`, `collecte`, `manquant`, `retardataires[]` (membre, échéances manquées, montant), `options[]` (`modeRemise`, `possible`, `montantBeneficiaire`, `commission`, `avanceGestionnaire`, `resteARecevoir`).
- **Remise** : `PUT …/cycles/{cycleId}/cloturer` accepte `modeRemise` (facultatif ; défaut = réglage de la tontine).
- **Affectation d'une régularisation sur un tour remis** : rembourse d'abord l'avance en cours du gestionnaire, puis complète le bénéficiaire (part nette), la commission correspondante revenant au gestionnaire. Notification au gestionnaire (`COMPLEMENT_A_REMETTRE`) et au bénéficiaire.

#### 4. Commission
- Toujours **perçue par le gestionnaire**, quel que soit le type de tontine (rotative, événementielle, groupée).
- Calculée sur ce qui est **réellement remis** : à la remise, puis sur chaque complément (S02 : francs entiers, aucun franc perdu). `FRAIS_FIXES_PAR_CYCLE` : une fois par tour, à la première remise.

#### 5. Clôture de la tontine
- `PUT /v1/tontines/{id}/cloturer` (créateur, SUPER_ADMIN) → `TERMINEE`. S'il reste des tours non remis, des retards ou des compléments à remettre → 409 `TONTINE_CLOTURE_A_CONFIRMER` avec la liste dans `data` ; le gestionnaire confirme avec `forcer: true`.
- Quand le dernier tour est remis, l'app **suggère** la clôture (notification `TONTINE_PRETE_A_CLOTURER`) sans la faire.
- Après clôture, l'encaissement des retards reste possible (suivi uniquement).

- **Migration** : `modeRemise: DISPONIBLE` pour les tontines existantes (= fonctionnement actuel : remise de ce qui est validé) ; seule différence, les retards deviennent régularisables.
- **Codes d'erreur** : `REMISE_JACKPOT_INCOMPLETE`, `MODE_REMISE_INDISPONIBLE`, `TONTINE_CLOTURE_A_CONFIRMER`.
- **Critères d'acceptation** :
  - 10 membres × 5 000 F/semaine, jackpot visé 500 000 F → 10 cotisations par tour ; commission 20 % → bénéficiaire 400 000 F si tout est payé ;
  - un membre manque 2 semaines : `ATTENDRE` → remise refusée ; `DISPONIBLE` → 392 000 F puis complément de 8 000 F à la régularisation ; `AVANCE_COMMISSION` → 400 000 F, commission 90 000 F puis 100 000 F après régularisation ;
  - le gestionnaire n'a pas remis le tour 1 : les membres paient quand même la 1ʳᵉ échéance du tour 2 ;
  - déclaration en attente à la clôture → validable ensuite ;
  - clôture de la tontine avec un retard en cours → 409 `TONTINE_CLOTURE_A_CONFIRMER`, puis `forcer: true` → `TERMINEE`.

### B34 — Tontine groupée (achat groupé d'un produit)
- **Écrans** : Créer une tontine (type « Groupée »), Fiche Tontine, Membres, Payer, Encaisser, À valider, Mon espace.
- **Besoin** : un vendeur (le gestionnaire) réunit des membres qui cotisent selon un rythme (journalier, hebdomadaire, quinzaine, mensuel ou libre, B26) ; **à la fin de la durée prévue, chaque membre reçoit le produit**. Ex. un lot de vêtements à 50 000 F par personne, 10 membres, 5 000 F par semaine → 10 semaines.
- **Périmètre (2026-11-13)** : collectif + cotisations récurrentes + argent qui circule en cotisations (mobile money) → dans le périmètre. **Hors périmètre** : suivi de créances grossiste → détaillant (relation à deux, crédit commercial), catalogue, stock, livraison. La version « un produit par tour » existe déjà : c'est la ROTATIVE.
- **Décisions (porteur du projet, 2026-11-13)** :
  - **un produit unique**, identique pour tous les membres (même prix, même cotisation) ;
  - **remise à la fin** de la durée prévue ; le gestionnaire peut aussi remettre plus tôt à un membre qui a tout payé ;
  - la **remise se fait hors de l'app** : l'app l'enregistre mais ne la bloque jamais ; remettre à un membre qui n'a pas tout payé est la décision du gestionnaire (l'app affiche le reste dû) ;
  - le **vendeur est le gestionnaire** de la tontine ; **aucune commission** (sa marge est dans le prix).
- **Création** (`tontineType: GROUPEE`) :
  ```json
  "produit": { "nom": "Lot de 3 boubous", "description": "Bazin, tailles au choix", "prix": 50000 },
  "montant": 5000                       // ou "nombreEcheances": 10
  ```
  - rythme et échéances : B26 ; un seul tour de N échéances (modèle B32), N = `prix` ÷ `montant` (arrondi au-dessus, dernière échéance = reste) ou `montant` déduit de `nombreEcheances` (`Montants.repartir` : somme exacte) ; la simulation B26 renvoie N, le montant de chaque échéance et la date de fin ;
  - `ordreBeneficiaire`, `nombreGagnants`, commissions : non applicables (commission → 400 `COMMISSION_NON_APPLICABLE`, y compris `commissions` à la création B30) ;
  - `produit` obligatoire pour ce type (`PRODUIT_OBLIGATOIRE`) ; prix en francs entiers (S02).
- **Paiements et retards** : règles B32/B33 (échéance en cours, régularisation d'une échéance passée, ponctualité S01, rappels B26) ; « tout payé » = toutes les échéances validées.
- **Remise du produit** (suivi uniquement) :
  - `PUT /v1/tontines/{id}/membres/{membreId}/remise-produit` (créateur, SUPER_ADMIN) : `{ "dateRemise"?, "note"? }` → enregistre la remise (auteur, date) ; réponse avec `resteDu` ; aucune vérification bloquante ;
  - `DELETE` du même chemin : annule une remise saisie par erreur (motif, tracé) ;
  - `PUT /v1/tontines/{id}/remise-produit/confirmer` (le membre) : confirme avoir reçu le produit (trace utile en cas de désaccord, qui reste hors plateforme) ;
  - notifications : `PRODUIT_REMIS` au membre ; `PRODUIT_A_REMETTRE` au gestionnaire quand un membre a tout payé et à la fin de la durée prévue.
- **Lecture** :
  - `TontineResponse` et aperçu d'invitation : `produit` (nom, description, prix), `nombreEcheances`, date de fin ;
  - `GET /membres` : par membre `montantPaye`, `resteDu`, `toutPaye`, `produitRemis` (date), `remiseConfirmee` ; filtre `?produitRemis=true|false` ;
  - `my-dashboard` : `produitsARemettre` (tout payé, non remis), `produitsRemis`, `resteDuTotal` ;
  - `/me/summary` : mon reste dû, mes échéances, produit remis ou non.
- **Fin** : quand tous les produits sont remis, l'app suggère la clôture (B33, `TONTINE_PRETE_A_CLOTURER`).
- **Migration** : colonnes `tontines.produit_nom`, `produit_description`, `produit_prix` ; `tontine_membres.produit_remis_le`, `produit_remis_par`, `produit_remise_note`, `produit_remise_confirmee_le` ; valeur `GROUPEE` de `TontineType`. Aucun effet sur les tontines existantes.
- **Dépendances** : B26 (rythme), B32 (échéances d'un tour), B33 (régularisation, clôture). À faire après B33.
- **Codes d'erreur** : `PRODUIT_OBLIGATOIRE`, `COMMISSION_NON_APPLICABLE`.
- **Critères d'acceptation** :
  - produit à 50 000 F, 5 000 F par semaine le dimanche, 10 membres → 10 échéances, fin au 10ᵉ dimanche, collecte prévue 500 000 F ;
  - produit à 50 000 F, 3 échéances → 16 667 + 16 667 + 16 666 F ;
  - membre ayant tout payé → `PRODUIT_A_REMETTRE` au gestionnaire ; remise saisie → `PRODUIT_REMIS` au membre, qui peut confirmer ;
  - remise à un membre avec 10 000 F de reste dû → acceptée, `resteDu` = 10 000 F affiché ;
  - commission sur une tontine groupée → 400 `COMMISSION_NON_APPLICABLE`.

### B35 — Plusieurs parts par membre (rotatives et groupées)
- **Écrans** : Créer une tontine, Réglages, Membres (ajout, parts), Invitation / demande d'adhésion, Payer, Encaisser, Fiche Tontine, Mes jackpots.
- **Besoin (porteur du projet, 2026-11-13)** : dans une rotative, un membre peut prendre **plusieurs parts**. Ex. jackpot de 1 000 000 F à la fin du mois, part de 100 000 F → 10 parts ; 8 membres à 1 part + 1 membre à 2 parts (200 000 F par mois) = 9 personnes, 10 parts. C'est fréquent.
- **Paramètres demandés à la création** : `partsMaxParMembre` (nombre de parts maximum pour un adhérent) et `membresMinimum` (nombre de personnes requis pour démarrer).
- **Changement de modèle** : l'unité de la rotative devient la **part**, plus la personne.
  - nombre de tours = parts ÷ `nombreGagnants` (aujourd'hui : membres ÷ gagnants) ; jackpot = `montant` (par part) × parts ;
  - cotisation due par un membre = `montant` × ses parts, à chaque échéance (une seule cotisation par membre et par échéance) ;
  - un membre à 2 parts occupe **2 places** dans l'ordre de passage et peut recevoir **2 jackpots** ; `aRecuJackpot` devient « parts servies / parts détenues » ;
  - champs proposés : `tontines.nombre_parts` (parts attendues, remplace `nombreMembres` comme base du calcul des tours), `parts_max_par_membre`, `membres_minimum` ; `tontine_membres.nombre_parts` (défaut 1) ; une ligne par part servie (tour, montant) ;
  - **activation** : refusée tant que les parts attribuées ≠ `nombreParts` ou que les personnes < `membresMinimum` (codes à définir) ;
  - **migration** : chaque membre existant = 1 part, `nombreParts` = `nombreMembres` → aucun changement de comportement.
- **Impacts** : désignation des gagnants (ordre, tirage, choix, hybride), priorités de jackpot (par part), clôture et remise (B33), jackpot visé (B32 : calcul sur les parts), demandes d'adhésion (B17 : nombre de parts souhaité, places restantes en parts), récapitulatifs, `/me/summary`, `/me/jackpots`, tableaux de bord, ponctualité (par membre), amendes (B27).
- **Périmètre** : collectif + cotisations récurrentes + plus de cotisations qui circulent → dans le périmètre.
- **Décisions (porteur du projet, 2026-11-13)** :
  1. **un seul jackpot à la fois** : un membre à plusieurs parts ne reçoit jamais deux jackpots au même tour ; ses parts sont servies à des tours différents (y compris quand `nombreGagnants` > 1 : un membre ne peut pas être deux fois gagnant d'un même tour ; tirage, choix et priorités le respectent) ;
  2. **pas de demi-part** dans l'app : quand deux personnes se partagent une part, une seule est inscrite et en porte la responsabilité (le partage se règle hors de l'app) ;
  3. **parts figées à l'activation** (modifiables en BROUILLON seulement) ;
  4. **paiement partiel = cotisation en retard** : l'app ne sait pas quelle part est payée ; la cotisation due est `montant` × parts, une déclaration d'un autre montant est refusée (`AMOUNT_MISMATCH`, comme le montant fixe actuel) et la cotisation reste due.
- **Retrait d'un membre en cours de rotative** (règle générale, avec ou sans parts) :
  - **n'a pas encore reçu de jackpot** : le gestionnaire choisit le remboursement de ses cotisations validées : **à la fin de la tontine** ou **sans attendre** ; l'app enregistre le montant dû, le mode choisi, puis la date de remboursement déclarée par le gestionnaire (suivi uniquement : le remboursement se fait hors de l'app) ;
  - **a déjà reçu un jackpot** : **litige, géré hors de l'app** (l'app passe le membre SORTI et affiche ses échéances restantes, sans autre action) ;
  - endpoint : `DELETE /membres/{membreId}` (ou `PATCH …/statut` SORTI) accepte `remboursement: FIN_DE_TONTINE | IMMEDIAT` quand le membre n'a reçu aucun jackpot ; `PUT /membres/{membreId}/remboursement` enregistre le remboursement effectué ; notifications au membre.
- **Décisions complémentaires (2026-11-13)** :
  - **retrait réservé au gestionnaire** : une tontine en cours ne propose **jamais** au membre de se retirer lui-même (sécurité) ; seul le gestionnaire (ou le SUPER_ADMIN) passe un membre SORTI. *Déjà le cas dans le code : à conserver.*
  - **plusieurs gagnants par tour** : le gestionnaire fixe `nombreGagnants` dans les réglages de création (existe déjà, défaut 1, modifiable en BROUILLON) ; l'app impose des limites cohérentes (voir point 8) ; avec les parts : nombre de tours = parts ÷ gagnants, et un membre n'est jamais deux fois gagnant d'un même tour ;
  - **parts en tontine groupée** (B34) : **oui, si le gestionnaire l'a configuré** (`partsMaxParMembre` > 1) : un membre à 2 parts achète 2 produits, cotise 2 × le montant et reçoit 2 produits → B35 passe **avant** B34 ;
  - **parts non servies d'un membre sorti** : **au choix du gestionnaire**, au moment du retrait : (a) un nouveau membre les reprend, (b) le gestionnaire les reprend, (c) le nombre de tours diminue (jackpots suivants recalculés) ; l'app montre l'effet de chaque choix avant validation ;
  - **reprise par un nouveau membre** (et plus largement **adhésion à une rotative en cours**) : par défaut, le nouveau membre **paie les cotisations des échéances déjà passées** pour suivre le flux (rattrapage, régularisation B33) ; le gestionnaire peut choisir de ne pas l'exiger ;
  - **remboursement** = cotisations **validées par le gestionnaire** ; si le gestionnaire a configuré une **pénalité d'abandon** (nouveau type de commission `PENALITE_ABANDON`, en **montant fixe ou en pourcentage** des cotisations remboursées, au choix du gestionnaire), elle est déduite et lui revient ;
  - **limites du nombre de gagnants** : `1 ≤ nombreGagnants ≤ parts ÷ 2` (au moins 2 tours) ; si les parts ne se divisent pas exactement (ex. 10 parts, 3 gagnants), le **dernier tour a moins de gagnants** (comportement actuel conservé), affiché par la simulation.
- **Codes d'erreur** (à nommer à l'implémentation) : parts au-delà de `partsMaxParMembre`, activation sans toutes les parts ou sous `membresMinimum`, `nombreGagnants` hors limites.
- **Dépendances** : B32/B33 (échéances, régularisation, remise) ; **avant B34**.
