-- ===================================================================
-- V038 – Date de paiement d'une cotisation (S01, ponctualité)
--
-- Renseignée quand le gestionnaire encaisse un paiement remis en main
-- propre (POST /cotisations/admin) : c'est elle qui compte pour la
-- ponctualité. Sinon, la date de déclaration (created_at) fait foi.
-- ===================================================================

ALTER TABLE dinthialma.cotisations ADD COLUMN IF NOT EXISTS date_paiement TIMESTAMP WITH TIME ZONE;
