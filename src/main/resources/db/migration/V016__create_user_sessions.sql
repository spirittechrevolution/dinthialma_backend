-- ===================================================================
-- V016 – Table user_sessions
-- Session active d'un utilisateur, valable sur WEB et MOBILE.
-- Utilisée pour la connexion rapide par PIN (6 chiffres) :
--   valider PIN → récupérer session → rafraîchir Keycloak token → retourner JWT.
-- Le TTL de la session est défini par client_type (WEB plus court que MOBILE).
-- ===================================================================

CREATE TABLE dinthialma.user_sessions
(
    id                   UUID                     NOT NULL,
    user_id              UUID                     NOT NULL,
    client_type          VARCHAR(20)              NOT NULL,
    refresh_token_hash   VARCHAR(255)             NOT NULL,
    device_info          VARCHAR(255),
    last_used_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_sessions PRIMARY KEY (id),
    CONSTRAINT fk_us_user FOREIGN KEY (user_id) REFERENCES dinthialma.users (id)
);

CREATE INDEX idx_us_user_id    ON dinthialma.user_sessions (user_id);
CREATE INDEX idx_us_expires    ON dinthialma.user_sessions (expires_at);
CREATE INDEX idx_us_client     ON dinthialma.user_sessions (client_type);

COMMENT ON TABLE dinthialma.user_sessions IS 'Sessions actives PIN – fonctionne sur WEB et MOBILE';
COMMENT ON COLUMN dinthialma.user_sessions.client_type IS 'WEB | MOBILE – détermine le TTL et les politiques de sécurité';
COMMENT ON COLUMN dinthialma.user_sessions.refresh_token_hash IS 'SHA-256 du Keycloak refresh_token – jamais stocké en clair';
COMMENT ON COLUMN dinthialma.user_sessions.device_info IS 'User-agent navigateur (WEB) ou device_id (MOBILE)';
COMMENT ON COLUMN dinthialma.user_sessions.last_used_at IS 'Mis à jour à chaque connexion PIN réussie';
COMMENT ON COLUMN dinthialma.user_sessions.expires_at IS 'TTL : 24h (WEB) ou 30 jours (MOBILE) – renouvelé à chaque usage';
