-- ===================================================================
-- V012 – Seeds STATUT_COTISATION
-- ===================================================================

INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('fa15bc36-a396-4ed1-9b7f-dad9e6262b1e', 'STATUT_COTISATION', 'EN_ATTENTE', 'En attente de validation', TRUE),
  ('1a21f4e9-a7d2-4808-bf8b-cffa3649f2fe', 'STATUT_COTISATION', 'VALIDE',     'Paiement validé',          TRUE),
  ('6e33de35-81f5-4d4f-bc9d-9f31fbe67d85', 'STATUT_COTISATION', 'EN_RETARD',  'Paiement en retard',       TRUE);
