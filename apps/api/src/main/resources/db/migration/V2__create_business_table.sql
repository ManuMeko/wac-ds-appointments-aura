CREATE TABLE businesses (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    slug VARCHAR(180) NOT NULL UNIQUE,

    email VARCHAR(180) NOT NULL,

    phone VARCHAR(40),

    timezone VARCHAR(80) NOT NULL DEFAULT 'Europe/Madrid',

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_businesses_slug
    ON businesses(slug);

CREATE INDEX idx_businesses_email
    ON businesses(email);