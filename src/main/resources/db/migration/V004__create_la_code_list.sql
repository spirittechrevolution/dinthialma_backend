-- ===================================================================
-- V004 – Table la_code_list (référentiels de valeurs)
-- ===================================================================

CREATE TABLE dinthialma.la_code_list
(
    id               UUID                     NOT NULL,
    type             VARCHAR(100)             NOT NULL,
    value            VARCHAR(100)             NOT NULL,
    description      VARCHAR(500)             NOT NULL,
    is_system_assign BOOLEAN                  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_la_code_list PRIMARY KEY (id),
    CONSTRAINT uq_la_code_list_type_value UNIQUE (type, value)
);

CREATE INDEX idx_la_code_list_type ON dinthialma.la_code_list (type);