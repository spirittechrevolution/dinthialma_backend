-- ===================================================================
-- V034 – Distribution finale des tontines ÉVÉNEMENTIELLES, par membre
--
-- À la clôture du dernier sous-cycle, chaque membre récupère sa mise
-- (somme de ses cotisations VALIDÉES) diminuée de sa part de commission.
-- Jusqu'ici ce calcul n'était renvoyé qu'une fois (réponse de clôture,
-- WhatsApp) : il est désormais conservé pour l'historique du membre
-- (résumé « Je cotise », mes jackpots, fil d'activité).
--
-- Une seule distribution active par membre et par tontine.
-- Pas de DEFAULT ni de CHECK (valeurs fournies par l'application).
-- ===================================================================

CREATE TABLE IF NOT EXISTS dinthialma.distributions_evenementielles (
    id                 UUID          NOT NULL PRIMARY KEY,
    tontine_id         UUID          NOT NULL REFERENCES dinthialma.tontines(id),
    cycle_id           UUID          NOT NULL REFERENCES dinthialma.cycles_tontine(id),
    membre_id          UUID          NOT NULL REFERENCES dinthialma.tontine_membres(id),
    montant_cotise     NUMERIC(12,2) NOT NULL,
    montant_commission NUMERIC(12,2) NOT NULL,
    montant_net        NUMERIC(12,2) NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    deleted_at         TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_distrib_evt_tontine_membre
    ON dinthialma.distributions_evenementielles (tontine_id, membre_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_distrib_evt_membre_id
    ON dinthialma.distributions_evenementielles (membre_id);
