-- ===================================================================
-- V015 – Table tontine_commissions
-- L'admin peut définir 0, 1 ou plusieurs types de commission par tontine.
-- Contrainte : un seul enregistrement par (tontine_id, type).
-- ===================================================================

CREATE TABLE dinthialma.tontine_commissions
(
    id           UUID                     NOT NULL,
    tontine_id   UUID                     NOT NULL,
    type         VARCHAR(50)              NOT NULL,
    valeur       DECIMAL(10, 2)           NOT NULL,
    description  TEXT,
    deleted_at   TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tontine_commissions PRIMARY KEY (id),
    CONSTRAINT fk_tc_tontine          FOREIGN KEY (tontine_id) REFERENCES dinthialma.tontines (id),
    CONSTRAINT uq_tc_tontine_type     UNIQUE (tontine_id, type)
);

CREATE INDEX idx_tc_tontine_id ON dinthialma.tontine_commissions (tontine_id);