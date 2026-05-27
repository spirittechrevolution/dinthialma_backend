-- ===================================================================
-- V014 – Table user_roles
-- Un utilisateur peut avoir plusieurs rôles (ex : MEMBER + ADMIN).
-- Chaque rôle est synchronisé avec le realm Keycloak.
-- ===================================================================

CREATE TABLE dinthialma.user_roles
(
    id                   UUID                     NOT NULL,
    user_id              UUID                     NOT NULL,
    role                 VARCHAR(50)              NOT NULL,
    synced_to_keycloak   BOOLEAN                  NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT fk_ur_user    FOREIGN KEY (user_id) REFERENCES dinthialma.users (id),
    CONSTRAINT uq_ur_user_role UNIQUE (user_id, role)
);

CREATE INDEX idx_ur_user_id ON dinthialma.user_roles (user_id);
CREATE INDEX idx_ur_role    ON dinthialma.user_roles (role);
