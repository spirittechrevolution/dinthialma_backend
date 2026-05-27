-- ===================================================================
-- V006 – Table tontine_membres
-- ===================================================================

CREATE TABLE dinthialma.tontine_membres
(
    id              UUID                     NOT NULL,
    tontine_id      UUID                     NOT NULL,
    user_id         UUID                     NOT NULL,
    ordre_jackpot   INTEGER,
    statut          VARCHAR(30)              NOT NULL,
    date_adhesion   DATE                     NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tontine_membres PRIMARY KEY (id),
    CONSTRAINT fk_tm_tontine FOREIGN KEY (tontine_id) REFERENCES dinthialma.tontines (id),
    CONSTRAINT fk_tm_user    FOREIGN KEY (user_id)    REFERENCES dinthialma.users (id),
    CONSTRAINT uq_tm_tontine_user  UNIQUE (tontine_id, user_id),
    CONSTRAINT uq_tm_tontine_ordre UNIQUE (tontine_id, ordre_jackpot)
);

CREATE INDEX idx_tm_tontine_id ON dinthialma.tontine_membres (tontine_id);
CREATE INDEX idx_tm_user_id    ON dinthialma.tontine_membres (user_id);
CREATE INDEX idx_tm_statut     ON dinthialma.tontine_membres (statut);