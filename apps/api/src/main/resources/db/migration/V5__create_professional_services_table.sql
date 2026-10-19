CREATE TABLE professional_services (
    professional_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_professional_services
        PRIMARY KEY (
            professional_id,
            service_id
        ),

    CONSTRAINT fk_professional_services_professional
        FOREIGN KEY (
            professional_id
        )
        REFERENCES professionals(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_professional_services_service
        FOREIGN KEY (
            service_id
        )
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_professional_services_professional_id
    ON professional_services(
        professional_id
    );

CREATE INDEX idx_professional_services_service_id
    ON professional_services(
        service_id
    );