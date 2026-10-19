CREATE TABLE schedule_exceptions (
    id BIGSERIAL PRIMARY KEY,

    professional_id BIGINT NOT NULL,

    exception_date DATE NOT NULL,

    type VARCHAR(30) NOT NULL,

    start_time TIME,

    end_time TIME,

    reason VARCHAR(500),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_exceptions_professional
        FOREIGN KEY (professional_id)
        REFERENCES professionals(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_schedule_exceptions_type
        CHECK (
            type IN (
                'UNAVAILABLE',
                'CUSTOM_HOURS'
            )
        ),

    CONSTRAINT chk_schedule_exceptions_time_configuration
        CHECK (
            (
                type = 'UNAVAILABLE'
                AND start_time IS NULL
                AND end_time IS NULL
            )
            OR
            (
                type = 'CUSTOM_HOURS'
                AND start_time IS NOT NULL
                AND end_time IS NOT NULL
                AND start_time < end_time
            )
        )
);

CREATE INDEX idx_schedule_exceptions_professional_id
    ON schedule_exceptions(
        professional_id
    );

CREATE INDEX idx_schedule_exceptions_professional_date
    ON schedule_exceptions(
        professional_id,
        exception_date
    );

CREATE INDEX idx_schedule_exceptions_professional_active
    ON schedule_exceptions(
        professional_id,
        active
    );

CREATE UNIQUE INDEX uq_schedule_exceptions_active_unavailable
    ON schedule_exceptions(
        professional_id,
        exception_date
    )
    WHERE
        active = TRUE
        AND type = 'UNAVAILABLE';