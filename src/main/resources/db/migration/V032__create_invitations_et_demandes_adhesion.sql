-- ===================================================================
-- V032 – Invitation par code et demandes d'adhésion (B16, B17)
-- ===================================================================

-- Code d'invitation partageable (lien, QR « t_<code> ») ; null = aucune invitation active.
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS code_invitation VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tontines_code_invitation
    ON dinthialma.tontines (code_invitation)
    WHERE code_invitation IS NOT NULL;

-- Demandes d'adhésion envoyées via un code d'invitation.
CREATE TABLE IF NOT EXISTS dinthialma.demandes_adhesion
(
    id              UUID                     NOT NULL,
    tontine_id      UUID                     NOT NULL,
    user_id         UUID                     NOT NULL,
    statut          VARCHAR(20)              NOT NULL,
    message         VARCHAR(500),
    motif_refus     VARCHAR(500),
    traitee_par     UUID,
    date_traitement TIMESTAMP WITH TIME ZONE,
    membre_id       UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_demandes_adhesion PRIMARY KEY (id),
    CONSTRAINT fk_dem_tontine     FOREIGN KEY (tontine_id)  REFERENCES dinthialma.tontines (id),
    CONSTRAINT fk_dem_user        FOREIGN KEY (user_id)     REFERENCES dinthialma.users (id),
    CONSTRAINT fk_dem_traitee_par FOREIGN KEY (traitee_par) REFERENCES dinthialma.users (id),
    CONSTRAINT fk_dem_membre      FOREIGN KEY (membre_id)   REFERENCES dinthialma.tontine_membres (id)
);

CREATE INDEX IF NOT EXISTS idx_dem_tontine_statut ON dinthialma.demandes_adhesion (tontine_id, statut);
CREATE INDEX IF NOT EXISTS idx_dem_user           ON dinthialma.demandes_adhesion (user_id);

-- Une seule demande EN_ATTENTE par personne et par tontine.
CREATE UNIQUE INDEX IF NOT EXISTS uq_dem_tontine_user_en_attente
    ON dinthialma.demandes_adhesion (tontine_id, user_id)
    WHERE statut = 'EN_ATTENTE' AND deleted_at IS NULL;
