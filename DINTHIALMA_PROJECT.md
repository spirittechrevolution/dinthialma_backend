# Dinthialma – Vision & Positionnement Produit

## Qu'est-ce que Dinthialma ?

**Dinthialma** est une plateforme numérique de gestion de tontines et d'épargne collective,
conçue pour les communautés d'Afrique de l'Ouest — notamment au Sénégal et au sein de la diaspora.

Elle transforme une pratique sociale ancestrale (la tontine) en un outil moderne, transparent
et accessible depuis n'importe quel appareil.

---

## Problèmes résolus

La tontine traditionnelle souffre de plusieurs problèmes structurels que Dinthialma adresse
directement :

| Problème | Solution Dinthialma |
|----------|---------------------|
| Gestion papier → pertes et erreurs | Historique numérique complet et infalsifiable |
| Manque de transparence sur les montants | Jackpot, commissions et montant net affichés à la clôture |
| Disputes sur l'ordre des bénéficiaires | Ordre défini à l'avance (fixe ou aléatoire) et visible par tous |
| Oubli de cotisation → retards | Rappels SMS automatiques chaque matin à 8h |
| Calcul manuel des frais de gestion | Commissions configurées une fois, calculées automatiquement |
| Absent d'historique opposable | Journal d'audit immuable sur chaque action |
| Accès limité au gestionnaire physique | Application accessible depuis le téléphone, 24h/24 |

---

## La solution : fonctionnement global

```
Créateur crée une tontine
  └─ Configure : montant, fréquence, membres attendus, mode cycle, commissions
  └─ Ajoute les membres (chaque membre reçoit l'accès)
  └─ Active la tontine → cycles générés automatiquement (mode AUTO) ou ouverts manuellement

Chaque cycle :
  ├─ Un bénéficiaire désigné (premier de la rotation)
  ├─ Membres cotisent → signalent leur paiement depuis l'app
  ├─ Gestionnaire valide chaque paiement
  ├─ SMS d'annonce envoyé au bénéficiaire désigné
  └─ Clôture → jackpot brut calculé, commissions déduites, montant net affiché

Rappels automatiques :
  └─ Chaque matin à 8h → SMS aux cotisants dont le paiement est EN_ATTENTE
```

---

## Public cible

### Utilisateurs principaux (B2C)

| Profil | Besoin |
|--------|--------|
| **Gestionnaire de tontine** | Digitaliser son groupe existant sans changer les habitudes |
| **Membre d'une tontine** | Voir ses paiements, son tour de jackpot, être rappelé |
| **Diaspora sénégalaise** | Gérer une tontine transnationale (famille dispersée) |
| **Association / coopérative** | Gérer les cotisations régulières d'un groupe |

### Segments secondaires (B2B)

| Profil | Besoin |
|--------|--------|
| **Coopératives d'épargne** | Outil de gestion avec traçabilité |
| **Microfinance** | Complément pour les groupes de solidarité |
| **Employeurs** | Tontine interne pour le bien-être des employés |

---

## Phase gratuite — 12 mois

### Objectifs de la période gratuite (J+0 à J+365)

La plateforme est **100 % gratuite** pendant les 12 premiers mois suivant son lancement public.

**Pourquoi ?**

1. **Construire la base d'utilisateurs** — la tontine est une affaire de confiance. Il faut que
   les groupes expérimentent la plateforme sans friction financière.

2. **Collecter du feedback terrain** — les habitudes de tontine varient par région et par groupe.
   Les 12 mois servent à affiner les fonctionnalités selon les retours réels.

3. **Fidéliser les gestionnaires** — un gestionnaire qui a migré son groupe ne repartira pas
   facilement. L'investissement en data (membres, historique) crée de la rétention naturelle.

4. **Atteindre la masse critique** — les recommandations de bouche à oreille fonctionnent
   mieux quand le produit est gratuit au démarrage.

### Ce qui est offert gratuitement

- Création de tontines illimitées
- Nombre de membres illimité par tontine
- Nombre de cycles illimité
- SMS de rappel et d'annonce (quota mensuel offert)
- Historique complet et journal d'audit
- Support par email

### Indicateurs de succès à 12 mois (KPIs)

| KPI | Cible |
|-----|-------|
| Nombre de tontines actives | ≥ 500 |
| Nombre de membres actifs | ≥ 5 000 |
| Volume de jackpots traités | ≥ 50 000 000 FCFA |
| Taux de rétention mensuelle | ≥ 70 % |
| NPS (satisfaction) | ≥ 40 |

---

## Modèle économique — après 12 mois

Trois leviers de monétisation, par ordre de priorité :

### 1. Commission de plateforme (freemium → payant)

Le gestionnaire configure ses propres commissions de gestion (ex : 4 % du jackpot).
Dinthialma prélèvera une micro-commission sur ces frais pour couvrir ses coûts.

```
Exemple :
Jackpot brut = 500 000 FCFA
Commission gestionnaire (4%) = 20 000 FCFA → perçue par le gestionnaire
Commission Dinthialma (1%) = 5 000 FCFA → prélevée automatiquement
Montant net bénéficiaire = 475 000 FCFA
```

### 2. Abonnement premium

| Plan | Prix / mois | Inclus |
|------|-------------|--------|
| **Gratuit** | 0 FCFA | 3 tontines, 20 membres max, SMS limités |
| **Starter** | 2 000 FCFA | 10 tontines, 50 membres, SMS illimités |
| **Pro** | 5 000 FCFA | Tontines illimitées, API, export Excel, support prioritaire |
| **Entreprise** | Sur devis | White label, intégration comptable, SLA |

### 3. Services à valeur ajoutée

- **Virement intégré** — règlement du jackpot directement via Wave / Orange Money depuis la plateforme
- **Scoring financier** — rapport de fiabilité d'un membre (historique de paiements → crédibilité)
- **Attestation officielle** — PDF certifié de participation à une tontine (usage administratif)

---

## Roadmap après le MVP

```
MVP (Sprint 1-4) ✅
  Auth · Tontines · Membres · Cotisations · Cycles · Commissions · Dashboard · SMS

Sprint 5 – Mobile / UX
  ├─ Application React Native (iOS + Android)
  └─ Push notifications (Firebase)

Sprint 6 – Paiements intégrés
  ├─ Intégration Wave Sénégal
  ├─ Intégration Orange Money
  └─ Règlement jackpot automatique à la clôture

Sprint 7 – Analytics & Rapport
  ├─ Export PDF / Excel des cycles et cotisations
  ├─ Rapport mensuel par email
  └─ Dashboard enrichi (graphiques)

Sprint 8 – Monétisation
  ├─ Système d'abonnements Stripe / Cinetpay
  ├─ Commission plateforme automatique
  └─ Facturation

Sprint 9 – Expansion
  ├─ Multi-devise (XOF, EUR, CAD)
  ├─ Multi-langue (Wolof, Français, Anglais)
  └─ API publique pour partenaires
```

---

## Différenciateurs vs. concurrence

| Critère | Dinthialma | WhatsApp / groupes informels | Solutions génériques |
|---------|------------|-------------------------------|----------------------|
| Gestion des cycles | ✅ Automatique + manuel | ❌ Aucun | ⚠️ Partiel |
| Validation des paiements | ✅ Workflow dédié | ❌ Informel | ⚠️ Manuel |
| Calcul des commissions | ✅ Automatique à la clôture | ❌ Aucun | ❌ Non |
| SMS de rappel | ✅ Quotidien automatique | ❌ Dépend du gestionnaire | ⚠️ Optionnel |
| Journal d'audit | ✅ Immuable, horodaté | ❌ Aucun | ⚠️ Partiel |
| Adapté aux tontines africaines | ✅ Conçu pour ça | ⚠️ Usage détourné | ❌ Non |
| Accès mobile-first | ✅ (roadmap) | ✅ | ⚠️ Variable |

---

## Vision long terme

> **Devenir la référence de confiance de l'épargne collective en Afrique francophone.**

Dinthialma ambitionne de devenir l'infrastructure numérique sur laquelle reposent les pratiques
d'épargne communautaire — des tontines de quartier jusqu'aux coopératives d'épargne structurées —
en apportant transparence, efficacité et confiance là où elles font actuellement défaut.

---

## Informations techniques

- **Backend** : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak
- **Authentification** : OAuth2 / JWT via Keycloak
- **SMS** : LAfricaMobile (Sénégal + sous-région)
- **Infrastructure** : Docker · PostgreSQL 15 · Keycloak 24
- **API** : REST · OpenAPI 3 · Port 8081 · Context path `/api`
- **Swagger UI** : `http://localhost:8081/api/swagger-ui.html`
