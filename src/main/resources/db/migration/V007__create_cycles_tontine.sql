-- ===================================================================
-- V007 – Table cycles_tontine
-- ===================================================================

CREATE TABLE dinthialma.cycles_tontine
(
    id                UUID                     NOT NULL,
    tontine_id        UUID                     NOT NULL,
    numero_cycle      INTEGER                  NOT NULL,
    beneficiaire_id   UUID,
    date_debut        DATE                     NOT NULL,
    date_fin          DATE                     NOT NULL,
    montant_jackpot   DECIMAL(12, 2),
    statut            VARCHAR(30)              NOT NULL,
    date_remise       DATE,
    deleted_at        TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cycles_tontine PRIMARY KEY (id),
    CONSTRAINT fk_ct_tontine      FOREIGN KEY (tontine_id)      REFERENCES dinthialma.tontines (id),
    CONSTRAINT fk_ct_beneficiaire FOREIGN KEY (beneficiaire_id) REFERENCES dinthialma.tontine_membres (id),
    CONSTRAINT uq_ct_tontine_cycle UNIQUE (tontine_id, numero_cycle)
);

CREATE INDEX idx_ct_tontine_id   ON dinthialma.cycles_tontine (tontine_id);
CREATE INDEX idx_ct_beneficiaire ON dinthialma.cycles_tontine (beneficiaire_id);
CREATE INDEX idx_ct_statut       ON dinthialma.cycles_tontine (statut);