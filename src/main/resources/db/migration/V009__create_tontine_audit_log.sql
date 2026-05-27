-- ===================================================================
-- V009 – Table tontine_audit_log
-- ===================================================================

CREATE TABLE dinthialma.tontine_audit_log
(
    id            UUID                     NOT NULL,
    table_name    VARCHAR(100)             NOT NULL,
    record_id     UUID                     NOT NULL,
    action        VARCHAR(20)              NOT NULL,
    champ         VARCHAR(100),
    ancienne_val  TEXT,
    nouvelle_val  TEXT,
    fait_par      UUID,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tontine_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_fait_par FOREIGN KEY (fait_par) REFERENCES dinthialma.users (id)
);

CREATE INDEX idx_audit_record     ON dinthialma.tontine_audit_log (table_name, record_id);
CREATE INDEX idx_audit_fait_par   ON dinthialma.tontine_audit_log (fait_par);
CREATE INDEX idx_audit_created_at ON dinthialma.tontine_audit_log (created_at DESC);