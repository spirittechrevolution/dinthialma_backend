-- ===================================================================
-- V029 – Jeton opaque du QR membre (B15)
--
-- Le QR d'un membre encode ce jeton aléatoire au lieu de son numéro de
-- téléphone. Généré à la première demande, régénérable (révocation).
-- ===================================================================

ALTER TABLE dinthialma.users ADD COLUMN IF NOT EXISTS qr_token VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_qr_token
    ON dinthialma.users (qr_token)
    WHERE qr_token IS NOT NULL;
