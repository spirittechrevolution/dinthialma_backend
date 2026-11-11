-- ===================================================================
-- V027 – Refus d'une cotisation déclarée (B18)
--
-- 1. Colonnes de traçabilité du refus (motif, auteur, date).
-- 2. L'unicité « une cotisation par membre et par cycle » ne porte plus que
--    sur les cotisations actives : une cotisation REFUSE (ou supprimée) ne
--    bloque plus une nouvelle déclaration du membre sur le même cycle.
-- ===================================================================

ALTER TABLE dinthialma.cotisations ADD COLUMN IF NOT EXISTS motif_refus VARCHAR(500);
ALTER TABLE dinthialma.cotisations ADD COLUMN IF NOT EXISTS refuse_par  UUID;
ALTER TABLE dinthialma.cotisations ADD COLUMN IF NOT EXISTS date_refus  TIMESTAMP WITH TIME ZONE;

ALTER TABLE dinthialma.cotisations DROP CONSTRAINT IF EXISTS fk_cot_refuse_par;
ALTER TABLE dinthialma.cotisations
    ADD CONSTRAINT fk_cot_refuse_par FOREIGN KEY (refuse_par) REFERENCES dinthialma.users (id);

ALTER TABLE dinthialma.cotisations DROP CONSTRAINT IF EXISTS uq_cot_cycle_membre;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cot_cycle_membre_actif
    ON dinthialma.cotisations (cycle_id, membre_id)
    WHERE deleted_at IS NULL AND statut <> 'REFUSE';
