-- ===================================================================
-- V036 – Moyen de paiement PI-SPI
--
-- PI-SPI : plateforme interopérable du Système de Paiement Instantané
-- de la BCEAO (virements instantanés entre banques et e-money, UEMOA).
-- ===================================================================

INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign)
SELECT 'c631fda9-da0d-43cf-accf-08e9b987457c', 'METHODE_PAIEMENT', 'PI_SPI', 'PI-SPI', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM dinthialma.la_code_list
    WHERE type = 'METHODE_PAIEMENT' AND value = 'PI_SPI');
