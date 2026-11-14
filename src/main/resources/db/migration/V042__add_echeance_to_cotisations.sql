-- ===================================================================
-- V042 – Cotisations par échéance (B32, lot 2)
--
-- 1. Chaque cotisation est rattachée à une échéance de son tour.
-- 2. Reprise : les cotisations existantes vont sur l'échéance du tour dont la
--    période contient leur date (paiement, sinon déclaration), à défaut sur la
--    première échéance active du tour. Avant B32, un tour = une échéance : la
--    reprise est sans ambiguïté.
-- 3. L'unicité « une cotisation active par membre » porte désormais sur
--    l'échéance (et non plus sur le tour) ; une cotisation REFUSE ou supprimée
--    ne bloque toujours pas une nouvelle déclaration.
-- ===================================================================

ALTER TABLE dinthialma.cotisations ADD COLUMN IF NOT EXISTS echeance_id UUID;

ALTER TABLE dinthialma.cotisations DROP CONSTRAINT IF EXISTS fk_cot_echeance;
ALTER TABLE dinthialma.cotisations
    ADD CONSTRAINT fk_cot_echeance FOREIGN KEY (echeance_id) REFERENCES dinthialma.echeances_cycle (id);

UPDATE dinthialma.cotisations c
SET echeance_id = (SELECT e.id
                   FROM dinthialma.echeances_cycle e
                   WHERE e.cycle_id = c.cycle_id
                     AND e.deleted_at IS NULL
                   ORDER BY CASE
                                WHEN e.date_debut <= CAST(COALESCE(c.date_paiement, c.created_at) AS DATE)
                                    THEN 0
                                ELSE 1 END,
                            CASE
                                WHEN e.date_debut <= CAST(COALESCE(c.date_paiement, c.created_at) AS DATE)
                                    THEN -e.numero
                                ELSE e.numero END
                   LIMIT 1)
WHERE c.echeance_id IS NULL;

DROP INDEX IF EXISTS dinthialma.uq_cot_cycle_membre_actif;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cot_echeance_membre_actif
    ON dinthialma.cotisations (echeance_id, membre_id)
    WHERE deleted_at IS NULL AND statut <> 'REFUSE';

CREATE INDEX IF NOT EXISTS idx_cot_echeance ON dinthialma.cotisations (echeance_id);
