-- ============================================================
-- V10 - Authentication and business memberships
-- ============================================================

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(150) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,

    last_login_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_users_email_lower
    ON users (LOWER(email));

CREATE INDEX ix_users_active
    ON users (active);


-- ============================================================
-- BUSINESS MEMBERSHIPS
-- ============================================================

CREATE TABLE business_memberships (
    id BIGSERIAL PRIMARY KEY,

    business_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    role VARCHAR(30) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_business_memberships_business
        FOREIGN KEY (business_id)
        REFERENCES businesses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_business_memberships_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_business_memberships_role
        CHECK (
            role IN (
                'OWNER',
                'ADMIN',
                'STAFF'
            )
        ),

    CONSTRAINT ux_business_membership
        UNIQUE (business_id, user_id)
);

CREATE INDEX ix_business_memberships_business
    ON business_memberships (business_id);

CREATE INDEX ix_business_memberships_user
    ON business_memberships (user_id);

CREATE INDEX ix_business_memberships_role
    ON business_memberships (role);

CREATE INDEX ix_business_memberships_active
    ON business_memberships (active);


-- ============================================================
-- REFRESH TOKENS
-- ============================================================

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    revoked_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX ix_refresh_tokens_user
    ON refresh_tokens (user_id);

CREATE INDEX ix_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

CREATE INDEX ix_refresh_tokens_revoked_at
    ON refresh_tokens (revoked_at);