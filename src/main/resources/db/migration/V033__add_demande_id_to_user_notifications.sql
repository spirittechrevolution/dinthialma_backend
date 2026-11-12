-- ===================================================================
-- V033 – Lien vers la demande d'adhésion dans les notifications in-app
--
-- DEMANDE_ADHESION (gestionnaire) et ADHESION_REFUSEE (demandeur) portent
-- l'identifiant de la demande pour cibler directement la bonne ligne.
-- Colonne facultative, sans clé étrangère (même règle que V028).
-- ===================================================================

ALTER TABLE dinthialma.user_notifications ADD COLUMN IF NOT EXISTS demande_id UUID;
