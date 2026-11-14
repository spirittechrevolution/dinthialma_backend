-- ===================================================================
-- V039 – Rythme libre, jour et heure d'échéance, rappels (B26)
--
-- Tontine : rythme « tous les N JOUR | SEMAINE | MOIS », jour d'échéance
-- facultatif, heure limite et rappels. `frequence` est conservée : code
-- historique si le rythme y correspond, sinon PERSONNALISEE.
-- Tour : limite de paiement `echeance_at` + table des échéances (une par
-- tour aujourd'hui ; plusieurs cotisations par tour avec B32).
--
-- Reprise : traduction des fréquences historiques, heure 23:59, rappel la
-- veille + 3 h avant ; chaque tour existant reçoit une échéance à 23:59 le
-- jour de sa date de fin. Aucune date de tour n'est modifiée.
-- ===================================================================

ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS rythme_intervalle   INTEGER;
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS rythme_unite        VARCHAR(10);
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS jour_echeance       VARCHAR(10);
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS heure_echeance      TIME;
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS rappel_veille       BOOLEAN;
ALTER TABLE dinthialma.tontines ADD COLUMN IF NOT EXISTS rappel_heures_avant INTEGER;

UPDATE dinthialma.tontines
SET rythme_intervalle = CASE UPPER(frequence)
                            WHEN 'BIMENSUEL' THEN 2
                            WHEN 'TRIMESTRIEL' THEN 3
                            ELSE 1
                        END,
    rythme_unite      = CASE UPPER(frequence)
                            WHEN 'JOURNALIERE' THEN 'JOUR'
                            WHEN 'HEBDOMADAIRE' THEN 'SEMAINE'
                            WHEN 'BIMENSUEL' THEN 'SEMAINE'
                            ELSE 'MOIS'
                        END
WHERE rythme_unite IS NULL;

UPDATE dinthialma.tontines SET heure_echeance = TIME '23:59' WHERE heure_echeance IS NULL;
UPDATE dinthialma.tontines SET rappel_veille = TRUE WHERE rappel_veille IS NULL;
UPDATE dinthialma.tontines SET rappel_heures_avant = 3 WHERE rappel_heures_avant IS NULL;

ALTER TABLE dinthialma.tontines ALTER COLUMN rythme_intervalle   SET NOT NULL;
ALTER TABLE dinthialma.tontines ALTER COLUMN rythme_unite        SET NOT NULL;
ALTER TABLE dinthialma.tontines ALTER COLUMN heure_echeance      SET NOT NULL;
ALTER TABLE dinthialma.tontines ALTER COLUMN rappel_veille       SET NOT NULL;
ALTER TABLE dinthialma.tontines ALTER COLUMN rappel_heures_avant SET NOT NULL;

-- ─── Tours : limite de paiement ─────────────────────────────────────

ALTER TABLE dinthialma.cycles_tontine ADD COLUMN IF NOT EXISTS echeance_at TIMESTAMP WITH TIME ZONE;

UPDATE dinthialma.cycles_tontine
SET echeance_at = (date_fin + TIME '23:59') AT TIME ZONE 'Africa/Dakar'
WHERE echeance_at IS NULL;

-- ─── Échéances de cotisation ────────────────────────────────────────

CREATE TABLE IF NOT EXISTS dinthialma.echeances_cycle
(
    id          UUID                     NOT NULL,
    cycle_id    UUID                     NOT NULL,
    numero      INTEGER                  NOT NULL,
    date_debut  DATE                     NOT NULL,
    echeance_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_echeances_cycle PRIMARY KEY (id),
    CONSTRAINT fk_ech_cycle FOREIGN KEY (cycle_id) REFERENCES dinthialma.cycles_tontine (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ech_cycle_numero
    ON dinthialma.echeances_cycle (cycle_id, numero)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ech_echeance_at ON dinthialma.echeances_cycle (echeance_at);

INSERT INTO dinthialma.echeances_cycle (id, cycle_id, numero, date_debut, echeance_at, created_at, updated_at)
SELECT gen_random_uuid(), c.id, 1, c.date_debut, c.echeance_at, NOW(), NOW()
FROM dinthialma.cycles_tontine c
WHERE NOT EXISTS (SELECT 1 FROM dinthialma.echeances_cycle e WHERE e.cycle_id = c.id);

-- ─── Fréquence : valeur des rythmes sans code historique ────────────

INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign)
SELECT '9b3f2c4e-6a1d-4e8b-b7c2-5d0e1f2a3b4c', 'FREQUENCE_TONTINE', 'PERSONNALISEE',
       'Rythme libre (tous les N jours / semaines / mois)', TRUE
WHERE NOT EXISTS (SELECT 1 FROM dinthialma.la_code_list
                  WHERE type = 'FREQUENCE_TONTINE' AND value = 'PERSONNALISEE');
