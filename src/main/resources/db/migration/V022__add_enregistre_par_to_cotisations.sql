-- ===================================================================
-- V022 – Traçabilité de saisie sur cotisations
--
-- Contexte : une cotisation peut être saisie par deux acteurs distincts :
--   • Le membre lui-même (paiement mobile money, il fournit la référence)
--   • L'admin de la tontine (encaissement cash, il coche la liste)
--
-- enregistre_par : référence l'utilisateur (users.id) qui a créé la ligne,
--   indépendamment de celui qui la valide (valide_par).
--   Nullable sur les anciennes lignes (migration sans valeur par défaut).
-- ===================================================================

ALTER TABLE dinthialma.cotisations
    ADD COLUMN IF NOT EXISTS enregistre_par UUID;

ALTER TABLE dinthialma.cotisations
    ADD CONSTRAINT fk_cot_enregistre_par
        FOREIGN KEY (enregistre_par) REFERENCES dinthialma.users (id);

CREATE INDEX IF NOT EXISTS idx_cot_enregistre_par
    ON dinthialma.cotisations (enregistre_par);
