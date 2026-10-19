CREATE TABLE appointment_reminders (
    id BIGSERIAL PRIMARY KEY,

    appointment_id BIGINT NOT NULL,

    reminder_type VARCHAR(20) NOT NULL,

    channel VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL,

    scheduled_for TIMESTAMPTZ NOT NULL,

    sent_at TIMESTAMPTZ NULL,

    last_attempt_at TIMESTAMPTZ NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    destination VARCHAR(320) NULL,

    provider_message_id VARCHAR(255) NULL,

    last_error VARCHAR(2000) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_appointment_reminders_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_appointment_reminder_appointment_type
        UNIQUE (
            appointment_id,
            reminder_type
        ),

    CONSTRAINT ck_appointment_reminders_type
        CHECK (
            reminder_type IN (
                'FIRST',
                'SECOND'
            )
        ),

    CONSTRAINT ck_appointment_reminders_channel
        CHECK (
            channel IN (
                'EMAIL',
                'SMS',
                'WHATSAPP'
            )
        ),

    CONSTRAINT ck_appointment_reminders_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_appointment_reminders_attempt_count
        CHECK (
            attempt_count >= 0
        )
);

CREATE INDEX idx_appointment_reminders_appointment_id
    ON appointment_reminders (
        appointment_id
    );

CREATE INDEX idx_appointment_reminders_status
    ON appointment_reminders (
        status
    );

CREATE INDEX idx_appointment_reminders_scheduled_for
    ON appointment_reminders (
        scheduled_for
    );

CREATE INDEX idx_appointment_reminders_due
    ON appointment_reminders (
        status,
        scheduled_for
    );