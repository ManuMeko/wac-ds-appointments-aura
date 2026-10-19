-- ============================================================
-- V15__support_reminders_after_rescheduling.sql
-- ============================================================
--
-- WAC D.S. - Modelo 01 - Appointments
--
-- Objetivo:
-- Permitir conservar el histórico de recordatorios cuando una
-- Appointment es reprogramada.
--
-- Antes de esta migración existía una unicidad equivalente a:
--
--     UNIQUE (appointment_id, reminder_type)
--
-- Esto impedía crear un nuevo reminder FIRST/SECOND para una cita
-- reprogramada cuando ya existía un reminder del mismo tipo.
--
-- A partir de V15 cada recordatorio queda asociado también al
-- startAt concreto de la Appointment para el cual fue generado.
--
-- Nueva identidad lógica:
--
--     appointment_id
--     + reminder_type
--     + appointment_start_at
--
-- Ejemplo:
--
--     appointment_id = 10
--     reminder_type = FIRST
--     appointment_start_at = 2026-10-19 11:20
--     status = SENT
--
-- Después de reprogramar:
--
--     appointment_id = 10
--     reminder_type = FIRST
--     appointment_start_at = 2026-10-19 16:00
--     status = PENDING
--
-- De esta forma mantenemos histórico sin colisiones.
-- ============================================================


-- ============================================================
-- 1. ADD APPOINTMENT START SNAPSHOT
-- ============================================================

ALTER TABLE appointment_reminders
    ADD COLUMN appointment_start_at TIMESTAMPTZ;


-- ============================================================
-- 2. BACKFILL EXISTING REMINDERS
-- ============================================================
--
-- Los reminders existentes se asociarán al start_at actual
-- de su Appointment.
--
-- A partir de V15, el backend será responsable de guardar
-- explícitamente appointment_start_at al crear cada reminder.
-- ============================================================

UPDATE appointment_reminders ar
SET appointment_start_at = a.start_at
FROM appointments a
WHERE a.id = ar.appointment_id
  AND ar.appointment_start_at IS NULL;


-- ============================================================
-- 3. VALIDATE BACKFILL
-- ============================================================
--
-- No permitimos continuar si existe algún reminder al que
-- no se haya podido asignar appointment_start_at.
-- ============================================================

DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM appointment_reminders
        WHERE appointment_start_at IS NULL
    ) THEN
        RAISE EXCEPTION
            'V15 migration aborted: appointment_reminders contains rows without appointment_start_at';
    END IF;
END
$$;


-- ============================================================
-- 4. ENFORCE NOT NULL
-- ============================================================

ALTER TABLE appointment_reminders
    ALTER COLUMN appointment_start_at SET NOT NULL;


-- ============================================================
-- 5. REMOVE OLD UNIQUE CONSTRAINT
-- ============================================================
--
-- La constraint antigua puede tener un nombre generado
-- automáticamente o uno definido explícitamente en V14.
--
-- Por ello no dependemos de su nombre.
--
-- Buscamos cualquier UNIQUE de appointment_reminders formada
-- EXACTAMENTE por:
--
--     appointment_id
--     reminder_type
--
-- y la eliminamos.
--
-- IMPORTANTE:
--
-- pg_attribute.attname es de tipo PostgreSQL "name".
-- Se convierte explícitamente a text para evitar:
--
--     operator does not exist: name[] = text[]
--
-- ============================================================

DO
$$
DECLARE
    constraint_record RECORD;
BEGIN

    FOR constraint_record IN

        SELECT
            c.conname

        FROM pg_constraint c

        JOIN pg_class t
            ON t.oid = c.conrelid

        JOIN pg_namespace n
            ON n.oid = t.relnamespace

        WHERE c.contype = 'u'

          AND t.relname = 'appointment_reminders'

          AND n.nspname = current_schema()

          AND ARRAY(
                SELECT
                    a.attname::text

                FROM unnest(c.conkey)
                    WITH ORDINALITY AS key_columns(attnum, position)

                JOIN pg_attribute a
                    ON a.attrelid = t.oid
                   AND a.attnum = key_columns.attnum

                ORDER BY
                    a.attname::text
              )
              =
              ARRAY[
                  'appointment_id',
                  'reminder_type'
              ]::text[]

    LOOP

        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            current_schema(),
            'appointment_reminders',
            constraint_record.conname
        );

    END LOOP;

END
$$;


-- ============================================================
-- 6. NEW OCCURRENCE-AWARE UNIQUE CONSTRAINT
-- ============================================================
--
-- Un Appointment puede tener varios FIRST o SECOND históricos,
-- siempre que pertenezcan a diferentes horarios de la cita.
--
-- Seguimos impidiendo duplicar el mismo reminder para:
--
--     misma Appointment
--     mismo tipo
--     mismo startAt
--
-- ============================================================

ALTER TABLE appointment_reminders
    ADD CONSTRAINT uq_appointment_reminders_appointment_type_start
        UNIQUE (
            appointment_id,
            reminder_type,
            appointment_start_at
        );


-- ============================================================
-- 7. INDEX FOR APPOINTMENT REMINDER HISTORY
-- ============================================================
--
-- Optimiza consultas de histórico por Appointment y horario.
-- ============================================================

CREATE INDEX idx_appointment_reminders_appointment_start
    ON appointment_reminders (
        appointment_id,
        appointment_start_at
    );


-- ============================================================
-- V15 COMPLETE
-- ============================================================