-- ===================================================================
-- V025 – Tontine événementielle : nouveaux champs
-- ===================================================================

ALTER TABLE dinthialma.tontines
    ADD COLUMN IF NOT EXISTS tontine_type    VARCHAR(30)    NOT NULL DEFAULT 'ROTATIVE',
    ADD COLUMN IF NOT EXISTS date_echeance   DATE,
    ADD COLUMN IF NOT EXISTS nom_evenement   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS montant_libre   BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS montant_minimum NUMERIC(12, 2);

-- ordre_beneficiaire devient nullable (non applicable aux tontines événementielles)
ALTER TABLE dinthialma.tontines ALTER COLUMN ordre_beneficiaire DROP NOT NULL;
