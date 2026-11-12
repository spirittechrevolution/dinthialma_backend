-- ===================================================================
-- V035 – Ordre des bénéficiaires HYBRIDE
--
-- À chaque clôture de tour, aucun gagnant n'est désigné automatiquement :
-- le gestionnaire choisit, tour par tour, entre un tirage au sort et un
-- choix manuel des gagnants (PATCH /cycles/{cycleId}/beneficiaire).
-- ===================================================================

INSERT INTO dinthialma.la_code_list (id, type, value, description, is_system_assign)
SELECT 'ac806541-25c1-4786-831d-08201086d95e', 'ORDRE_BENEFICIAIRE', 'HYBRIDE',
       'Tirage au sort ou choix du gestionnaire, à chaque tour', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM dinthialma.la_code_list
    WHERE type = 'ORDRE_BENEFICIAIRE' AND value = 'HYBRIDE');
