-- ===================================================================
-- V013 – Seeds ORDRE_BENEFICIAIRE
-- ===================================================================

INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('73c76a1c-88ef-4845-9287-80e5c5d381ae', 'ORDRE_BENEFICIAIRE', 'ALEATOIRE', 'Tirage au sort',      TRUE),
  ('e201b8b8-057c-429e-b72d-6b416032a253', 'ORDRE_BENEFICIAIRE', 'MANUEL',    'Défini par l''admin', TRUE),
  ('67240d24-1460-47b9-b400-cb45f0e105a6', 'ORDRE_BENEFICIAIRE', 'ROTATION',  'Rotation fixe',       TRUE);
