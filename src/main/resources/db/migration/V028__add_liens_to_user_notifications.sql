-- ===================================================================
-- V028 – Liens précis des notifications in-app (B19)
--
-- Permet au front d'ouvrir l'écran exact (reçu, tour, fiche membre) au lieu
-- de la seule tontine. Colonnes facultatives, renseignées quand elles existent.
-- Pas de clé étrangère : une notification reste lisible même si l'objet lié
-- est supprimé (soft delete) plus tard.
-- ===================================================================

ALTER TABLE dinthialma.user_notifications ADD COLUMN IF NOT EXISTS cycle_id      UUID;
ALTER TABLE dinthialma.user_notifications ADD COLUMN IF NOT EXISTS cotisation_id UUID;
ALTER TABLE dinthialma.user_notifications ADD COLUMN IF NOT EXISTS membre_id     UUID;
