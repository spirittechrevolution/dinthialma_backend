-- ===================================================================
-- V005 – Table tontines
-- ===================================================================

CREATE TABLE dinthialma.tontines
(
    id                   UUID                     NOT NULL,
    nom                  VARCHAR(150)             NOT NULL,
    description          TEXT,
    montant              DECIMAL(12, 2)           NOT NULL,
    frequence            VARCHAR(50)              NOT NULL,
    ordre_beneficiaire   VARCHAR(50)              NOT NULL,
    mode_cycle           VARCHAR(20)              NOT NULL,
    date_debut           DATE                     NOT NULL,
    nombre_membres       INTEGER                  NOT NULL,
    statut               VARCHAR(30)              NOT NULL,
    cree_par             UUID                     NOT NULL,
    deleted_at           TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tontines PRIMARY KEY (id),
    CONSTRAINT fk_tontines_cree_par FOREIGN KEY (cree_par) REFERENCES dinthialma.users (id)
);

CREATE INDEX idx_tontines_cree_par ON dinthialma.tontines (cree_par);
CREATE INDEX idx_tontines_statut   ON dinthialma.tontines (statut);