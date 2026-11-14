-- ===================================================================
-- V041 – Rythme du jackpot distinct du rythme de cotisation (B32)
--
-- `cotisations_par_tour` : nombre d'échéances de cotisation par tour
-- (1 = une cotisation par jackpot, fonctionnement historique).
-- `declenchement_jackpot` : FIN_DU_TOUR | TOUS_PAYES.
-- Reprise : 1 et FIN_DU_TOUR pour les tontines existantes ; chaque tour
-- existant a déjà sa seule échéance (V039). Aucun changement de comportement.
-- ===================================================================

ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS cotisations_par_tour  INTEGER;
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS declenchement_jackpot VARCHAR(20);

UPDATE dinthialma.tontines SET cotisations_par_tour = 1 WHERE cotisations_par_tour IS NULL;
UPDATE dinthialma.tontines SET declenchement_jackpot = 'FIN_DU_TOUR' WHERE declenchement_jackpot IS NULL;

ALTER TABLE dinthialma.tontines ALTER COLUMN cotisations_par_tour  SET NOT NULL;
ALTER TABLE dinthialma.tontines ALTER COLUMN declenchement_jackpot SET NOT NULL;
