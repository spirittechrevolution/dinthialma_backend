-- V019 : Table de demandes de changement de numéro de téléphone
-- Flow : user demande → OTP envoyé au NOUVEAU numéro → vérification → mise à jour Keycloak + DB
CREATE TABLE IF NOT EXISTS dinthialma.phone_change_requests (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES dinthialma.users (id),
    old_phone    VARCHAR(20)  NOT NULL,
    new_phone    VARCHAR(20)  NOT NULL,
    otp_code     VARCHAR(6)   NOT NULL,
    verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at   TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    CONSTRAINT pk_phone_change_requests PRIMARY KEY (id)
);

CREATE INDEX idx_phone_change_requests_user_id ON dinthialma.phone_change_requests (user_id);
CREATE INDEX idx_phone_change_requests_new_phone ON dinthialma.phone_change_requests (new_phone);
