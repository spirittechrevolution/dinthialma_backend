-- ===================================================================
-- V030 – Suppression logique des valeurs de liste (B23)
--
-- Une valeur supprimée disparaît des listes mais reste en base : les données
-- qui la référencent (ex. méthode de paiement d'une cotisation) restent
-- lisibles. La contrainte (type, value) est conservée : recréer une valeur
-- supprimée la restaure.
-- ===================================================================

ALTER TABLE dinthialma.la_code_list ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
