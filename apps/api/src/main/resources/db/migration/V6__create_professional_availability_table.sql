CREATE TABLE professional_availability (
    id BIGSERIAL PRIMARY KEY,

    professional_id BIGINT NOT NULL,

    day_of_week VARCHAR(20) NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_professional_availability_professional
        FOREIGN KEY (professional_id)
        REFERENCES professionals(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_professional_availability_time_range
        CHECK (start_time < end_time),

    CONSTRAINT chk_professional_availability_day_of_week
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        )
);

CREATE INDEX idx_professional_availability_professional_id
    ON professional_availability(
        professional_id
    );

CREATE INDEX idx_professional_availability_professional_day
    ON professional_availability(
        professional_id,
        day_of_week
    );

CREATE INDEX idx_professional_availability_active
    ON professional_availability(
        professional_id,
        active
    );