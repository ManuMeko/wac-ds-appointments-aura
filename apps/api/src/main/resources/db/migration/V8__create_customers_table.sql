CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,

    business_id BIGINT NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(150),

    email VARCHAR(180),

    phone VARCHAR(40),

    notes TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customers_business
        FOREIGN KEY (business_id)
        REFERENCES businesses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_customers_business_id
    ON customers(
        business_id
    );

CREATE INDEX idx_customers_business_active
    ON customers(
        business_id,
        active
    );

CREATE INDEX idx_customers_business_email
    ON customers(
        business_id,
        email
    );

CREATE INDEX idx_customers_business_phone
    ON customers(
        business_id,
        phone
    );

CREATE UNIQUE INDEX uq_customers_business_email
    ON customers(
        business_id,
        LOWER(email)
    )
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX uq_customers_business_phone
    ON customers(
        business_id,
        phone
    )
    WHERE phone IS NOT NULL;