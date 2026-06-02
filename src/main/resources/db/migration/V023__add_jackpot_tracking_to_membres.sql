-- ===================================================================
-- V023 – Suivi réception jackpot sur les membres de tontine
--
-- Contexte : en mode MANUEL, après clôture d'un cycle, l'admin
-- sélectionne le bénéficiaire du jackpot (manuellement ou aléatoire).
-- Ce membre est alors marqué comme ayant reçu le jackpot afin de ne
-- pas être éligible lors des prochains cycles.
--
-- a_recu_jackpot : true si le membre a déjà bénéficié d'un jackpot.
-- date_jackpot   : date à laquelle le jackpot lui a été remis.
-- ===================================================================

ALTER TABLE dinthialma.tontine_membres
    ADD COLUMN IF NOT EXISTS a_recu_jackpot BOOLEAN;

UPDATE dinthialma.tontine_membres
SET a_recu_jackpot = FALSE
WHERE a_recu_jackpot IS NULL;

ALTER TABLE dinthialma.tontine_membres
    ALTER COLUMN a_recu_jackpot SET NOT NULL;

ALTER TABLE dinthialma.tontine_membres
    ADD COLUMN IF NOT EXISTS date_jackpot TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_membres_a_recu_jackpot
    ON dinthialma.tontine_membres (tontine_id, a_recu_jackpot);
