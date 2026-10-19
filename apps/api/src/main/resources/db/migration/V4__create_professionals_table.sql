CREATE TABLE professionals (
    id BIGSERIAL PRIMARY KEY,

    business_id BIGINT NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(150),

    display_name VARCHAR(180) NOT NULL,

    email VARCHAR(180),

    phone VARCHAR(40),

    bio TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    booking_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_professionals_business
        FOREIGN KEY (business_id)
        REFERENCES businesses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_professionals_business_id
    ON professionals(business_id);

CREATE INDEX idx_professionals_business_active
    ON professionals(
        business_id,
        active
    );

CREATE INDEX idx_professionals_business_booking_enabled
    ON professionals(
        business_id,
        booking_enabled
    );

CREATE UNIQUE INDEX uq_professionals_business_email
    ON professionals(
        business_id,
        LOWER(email)
    )
    WHERE email IS NOT NULL;