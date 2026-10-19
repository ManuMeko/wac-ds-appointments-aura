CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,

    business_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,

    slug VARCHAR(180) NOT NULL,

    description TEXT,

    duration_minutes INTEGER NOT NULL,

    price NUMERIC(10, 2) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    booking_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_services_business
        FOREIGN KEY (business_id)
        REFERENCES businesses(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_services_business_slug
        UNIQUE (business_id, slug),

    CONSTRAINT chk_services_duration_positive
        CHECK (duration_minutes > 0),

    CONSTRAINT chk_services_price_non_negative
        CHECK (price >= 0)
);

CREATE INDEX idx_services_business_id
    ON services(business_id);

CREATE INDEX idx_services_active
    ON services(active);

CREATE INDEX idx_services_booking_enabled
    ON services(booking_enabled);