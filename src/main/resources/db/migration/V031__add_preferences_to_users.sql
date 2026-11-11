-- ===================================================================
-- V031 – Préférences utilisateur synchronisées entre appareils (B22)
--
-- Objet JSON libre côté base (favoris, dernier mode, montant masqué,
-- dernier moyen de paiement…) : ajouter une préférence ne demande pas de
-- migration. Validation et valeurs par défaut gérées côté Java.
-- ===================================================================

ALTER TABLE dinthialma.users ADD COLUMN IF NOT EXISTS preferences JSONB;
