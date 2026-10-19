CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,

    business_id BIGINT NOT NULL,

    customer_id BIGINT NOT NULL,

    professional_id BIGINT NOT NULL,

    service_id BIGINT NOT NULL,

    start_at TIMESTAMPTZ NOT NULL,

    end_at TIMESTAMPTZ NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_business
        FOREIGN KEY (business_id)
        REFERENCES businesses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_appointments_professional
        FOREIGN KEY (professional_id)
        REFERENCES professionals(id),

    CONSTRAINT fk_appointments_service
        FOREIGN KEY (service_id)
        REFERENCES services(id),

    CONSTRAINT chk_appointments_time_range
        CHECK (
            start_at < end_at
        ),

    CONSTRAINT chk_appointments_status
        CHECK (
            status IN (
                'PENDING',
                'CONFIRMED',
                'COMPLETED',
                'CANCELLED',
                'NO_SHOW'
            )
        )
);

CREATE INDEX idx_appointments_business_id
    ON appointments(
        business_id
    );

CREATE INDEX idx_appointments_customer_id
    ON appointments(
        customer_id
    );

CREATE INDEX idx_appointments_professional_id
    ON appointments(
        professional_id
    );

CREATE INDEX idx_appointments_service_id
    ON appointments(
        service_id
    );

CREATE INDEX idx_appointments_professional_start
    ON appointments(
        professional_id,
        start_at
    );

CREATE INDEX idx_appointments_business_start
    ON appointments(
        business_id,
        start_at
    );

CREATE INDEX idx_appointments_status
    ON appointments(
        status
    );