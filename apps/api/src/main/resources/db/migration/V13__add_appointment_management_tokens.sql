CREATE TABLE appointment_management_tokens (
    id BIGSERIAL PRIMARY KEY,

    appointment_id BIGINT NOT NULL,

    token_hash VARCHAR(128) NOT NULL,

    expires_at TIMESTAMPTZ NULL,

    revoked_at TIMESTAMPTZ NULL,

    last_used_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_appointment_management_tokens_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_appointment_management_tokens_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_appointment_management_tokens_appointment_id
    ON appointment_management_tokens (appointment_id);

CREATE INDEX idx_appointment_management_tokens_expires_at
    ON appointment_management_tokens (expires_at);

CREATE INDEX idx_appointment_management_tokens_revoked_at
    ON appointment_management_tokens (revoked_at);

CREATE INDEX idx_appointment_management_tokens_active_lookup
    ON appointment_management_tokens (
        appointment_id,
        revoked_at,
        expires_at
    );