-- V018 : Ajout de pin_created_at sur users pour gérer l'expiration du PIN (90 jours)
ALTER TABLE dinthialma.users
    ADD COLUMN IF NOT EXISTS pin_created_at TIMESTAMP;
