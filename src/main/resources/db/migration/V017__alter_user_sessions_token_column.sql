-- ===================================================================
-- V017 – Aggrandisement du type refresh_token_hash → TEXT
-- Justification : le token Keycloak chiffré (AES-256-GCM + Base64)
--   dépasse systématiquement VARCHAR(255).
--   La colonne stocke dorénavant le token chiffré (jamais en clair).
-- ===================================================================

ALTER TABLE dinthialma.user_sessions
    ALTER COLUMN refresh_token_hash TYPE TEXT;

COMMENT ON COLUMN dinthialma.user_sessions.refresh_token_hash
    IS 'Token Keycloak chiffré AES-256-GCM – jamais stocké en clair';
