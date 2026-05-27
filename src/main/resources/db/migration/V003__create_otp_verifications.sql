-- ===================================================================
-- V003 – Table otp_verifications
-- ===================================================================

CREATE TABLE dinthialma.otp_verifications
(
    id         UUID                     NOT NULL,
    phone      VARCHAR(20)              NOT NULL,
    code       VARCHAR(6)               NOT NULL,
    purpose    VARCHAR(30)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used       BOOLEAN                  NOT NULL,
    verified   BOOLEAN                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_otp_verifications PRIMARY KEY (id)
);

CREATE INDEX idx_otp_phone_purpose ON dinthialma.otp_verifications (phone, purpose);