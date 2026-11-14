-- ===================================================================
-- V040 – Journal des rappels d'échéance envoyés (B26)
--
-- Un rappel (veille, N heures avant, cotisations manquantes) n'est envoyé
-- qu'une fois par échéance, destinataire et canal : la ligne est insérée
-- AVANT l'envoi, l'index unique arbitre entre plusieurs instances du
-- backend. Un envoi WhatsApp différé (heures calmes 21h–8h) n'a pas encore
-- sa ligne : il part au premier passage autorisé tant que la fenêtre du
-- rappel reste ouverte.
-- ===================================================================

CREATE TABLE IF NOT EXISTS dinthialma.rappels_envoyes
(
    id          UUID                     NOT NULL,
    echeance_id UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    nature      VARCHAR(30)              NOT NULL,
    canal       VARCHAR(20)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_rappels_envoyes PRIMARY KEY (id),
    CONSTRAINT fk_rappel_echeance FOREIGN KEY (echeance_id) REFERENCES dinthialma.echeances_cycle (id),
    CONSTRAINT fk_rappel_user     FOREIGN KEY (user_id)     REFERENCES dinthialma.users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rappel_echeance_user_nature_canal
    ON dinthialma.rappels_envoyes (echeance_id, user_id, nature, canal);
