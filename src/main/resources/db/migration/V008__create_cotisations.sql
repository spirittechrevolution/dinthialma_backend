-- ===================================================================
-- V008 – Table cotisations
-- ===================================================================

CREATE TABLE dinthialma.cotisations
(
    id                    UUID                     NOT NULL,
    tontine_id            UUID                     NOT NULL,
    membre_id             UUID                     NOT NULL,
    cycle_id              UUID                     NOT NULL,
    montant               DECIMAL(12, 2)           NOT NULL,
    methode_paiement      VARCHAR(50),
    reference_transaction VARCHAR(100),
    statut                VARCHAR(30)              NOT NULL,
    note                  TEXT,
    valide_par            UUID,
    date_validation       TIMESTAMP WITH TIME ZONE,
    deleted_at            TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cotisations PRIMARY KEY (id),
    CONSTRAINT fk_cot_tontine    FOREIGN KEY (tontine_id) REFERENCES dinthialma.tontines (id),
    CONSTRAINT fk_cot_membre     FOREIGN KEY (membre_id)  REFERENCES dinthialma.tontine_membres (id),
    CONSTRAINT fk_cot_cycle      FOREIGN KEY (cycle_id)   REFERENCES dinthialma.cycles_tontine (id),
    CONSTRAINT fk_cot_valide_par FOREIGN KEY (valide_par) REFERENCES dinthialma.users (id),
    CONSTRAINT uq_cot_cycle_membre UNIQUE (cycle_id, membre_id)
);

CREATE INDEX idx_cot_tontine_id ON dinthialma.cotisations (tontine_id);
CREATE INDEX idx_cot_membre_id  ON dinthialma.cotisations (membre_id);
CREATE INDEX idx_cot_cycle_id   ON dinthialma.cotisations (cycle_id);
CREATE INDEX idx_cot_statut     ON dinthialma.cotisations (statut);
