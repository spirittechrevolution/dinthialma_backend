-- ===================================================================
-- V021 – Compte pré-inscrit : support utilisateurs sans Keycloak
--
-- Contexte : un gestionnaire de tontine peut ajouter un membre
-- uniquement via son numéro de téléphone, même si ce dernier n'a
-- pas encore créé de compte sur l'application.
-- Un utilisateur pré-inscrit (PRE_ENROLLED) n'a pas de keycloak_id
-- ni de mot de passe. Il pourra activer son compte plus tard en
-- s'inscrivant avec le même numéro.
-- ===================================================================

-- keycloak_id devient nullable pour les comptes pré-inscrits
ALTER TABLE dinthialma.users ALTER COLUMN keycloak_id DROP NOT NULL;

-- Statut du compte : ACTIVE (compte normal) | PRE_ENROLLED (créé par un admin, sans accès app)
ALTER TABLE dinthialma.users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_users_account_status
    ON dinthialma.users (account_status)
    WHERE account_status IS NOT NULL;
