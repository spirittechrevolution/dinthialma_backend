CREATE TABLE IF NOT EXISTS dinthialma.user_notifications
(
    id         UUID                        NOT NULL,
    user_id    UUID                        NOT NULL,
    type       VARCHAR(50)                 NOT NULL,
    title      VARCHAR(200)                NOT NULL,
    body       VARCHAR(500)                NOT NULL,
    is_read    BOOLEAN                     NOT NULL DEFAULT FALSE,
    tontine_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_user_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES dinthialma.users (id)
);

CREATE INDEX IF NOT EXISTS idx_notif_user_read ON dinthialma.user_notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_created ON dinthialma.user_notifications (created_at DESC);
