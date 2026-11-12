-- ===================================================================
-- V037 – Priorités de jackpot (tontines ROTATIVE)
--
-- Un membre dans le besoin demande à recevoir le prochain jackpot, ou le
-- gestionnaire lui attribue directement cette priorité après accord du
-- groupe. Une priorité ACCEPTEE passe avant la règle convenue
-- (ordreBeneficiaire) au prochain tour où des gagnants sont désignés,
-- puis devient SERVIE ; la règle reprend ensuite pour les autres membres.
-- ===================================================================

CREATE TABLE IF NOT EXISTS dinthialma.priorites_jackpot
(
    id              UUID                     NOT NULL,
    tontine_id      UUID                     NOT NULL,
    membre_id       UUID                     NOT NULL,
    statut          VARCHAR(20)              NOT NULL,
    origine         VARCHAR(20)              NOT NULL,
    motif           VARCHAR(500),
    motif_refus     VARCHAR(500),
    traitee_par     UUID,
    date_traitement TIMESTAMP WITH TIME ZONE,
    cycle_id        UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_priorites_jackpot PRIMARY KEY (id),
    CONSTRAINT fk_prio_tontine     FOREIGN KEY (tontine_id)  REFERENCES dinthialma.tontines (id),
    CONSTRAINT fk_prio_membre      FOREIGN KEY (membre_id)   REFERENCES dinthialma.tontine_membres (id),
    CONSTRAINT fk_prio_traitee_par FOREIGN KEY (traitee_par) REFERENCES dinthialma.users (id),
    CONSTRAINT fk_prio_cycle       FOREIGN KEY (cycle_id)    REFERENCES dinthialma.cycles_tontine (id)
);

CREATE INDEX IF NOT EXISTS idx_prio_tontine_statut ON dinthialma.priorites_jackpot (tontine_id, statut);

-- Une seule priorité active (EN_ATTENTE ou ACCEPTEE) par membership.
CREATE UNIQUE INDEX IF NOT EXISTS uq_prio_membre_active
    ON dinthialma.priorites_jackpot (membre_id)
    WHERE statut IN ('EN_ATTENTE', 'ACCEPTEE') AND deleted_at IS NULL;
