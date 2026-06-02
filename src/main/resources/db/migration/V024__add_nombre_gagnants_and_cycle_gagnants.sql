-- ===================================================================
-- V024 – Support multi-gagnants par cycle
--
-- Contexte : une tontine peut avoir N gagnants par cycle. Le jackpot
-- est divisé équitablement entre les N gagnants. Par défaut 1 gagnant
-- (comportement actuel conservé). La relation bénéficiaire unique sur
-- cycles_tontine est supprimée et remplacée par la table cycle_gagnants.
-- ===================================================================

-- 1. Ajout du nombre de gagnants par cycle sur la tontine (défaut 1)
ALTER TABLE dinthialma.tontines
    ADD COLUMN IF NOT EXISTS nombre_gagnants INT NOT NULL DEFAULT 1;

-- 2. Table des gagnants par cycle
CREATE TABLE IF NOT EXISTS dinthialma.cycle_gagnants (
    id           UUID          NOT NULL PRIMARY KEY,
    cycle_id     UUID          NOT NULL REFERENCES dinthialma.cycles_tontine(id),
    membre_id    UUID          NOT NULL REFERENCES dinthialma.tontine_membres(id),
    montant_recu NUMERIC(12,2),
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cycle_gagnants_cycle_membre
    ON dinthialma.cycle_gagnants (cycle_id, membre_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_cycle_gagnants_cycle_id
    ON dinthialma.cycle_gagnants (cycle_id);

-- 3. Suppression de l'ancienne colonne beneficiaire_id sur cycles_tontine
ALTER TABLE dinthialma.cycles_tontine
    DROP COLUMN IF EXISTS beneficiaire_id;
