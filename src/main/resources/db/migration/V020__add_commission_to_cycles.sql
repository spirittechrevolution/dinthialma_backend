-- ===================================================================
-- V020 – Ajout montant_commission et montant_net sur cycles_tontine
--
-- montant_jackpot  : somme brute des cotisations validées (existant)
-- montant_commission : total prélevé par le gestionnaire
-- montant_net      : montant_jackpot - montant_commission → remis au bénéficiaire
-- ===================================================================

ALTER TABLE dinthialma.cycles_tontine
    ADD COLUMN IF NOT EXISTS montant_commission DECIMAL(12, 2),
    ADD COLUMN IF NOT EXISTS montant_net        DECIMAL(12, 2);
