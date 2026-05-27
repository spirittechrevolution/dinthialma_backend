-- ===================================================================
-- V002 – Table users
-- ===================================================================

CREATE TABLE dinthialma.users
(
    id           UUID                     NOT NULL,
    keycloak_id  VARCHAR(255)             NOT NULL,
    first_name   VARCHAR(100)             NOT NULL,
    last_name    VARCHAR(100)             NOT NULL,
    phone        VARCHAR(20)              NOT NULL,
    email        VARCHAR(255),
    avatar_url        TEXT,
    is_active         BOOLEAN                  NOT NULL,
    -- ─── PIN connexion rapide (mobile) ──────────────────────────────
    pin_hash          VARCHAR(255),
    pin_attempts      INTEGER                  NOT NULL,
    pin_locked_until  TIMESTAMP WITH TIME ZONE,
    -- ────────────────────────────────────────────────────────────────
    deleted_at        TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE INDEX idx_users_keycloak_id ON dinthialma.users (keycloak_id);
CREATE INDEX idx_users_phone ON dinthialma.users (phone);
CREATE INDEX idx_users_email ON dinthialma.users (email) WHERE email IS NOT NULL;

