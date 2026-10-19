-- ============================================================
-- V11__expand_business_profile.sql
-- WAC D.S. Appointments
--
-- Amplía la tabla businesses para soportar:
--
-- - descripción
-- - datos de contacto ampliados
-- - dirección
-- - regionalización
-- - branding
-- - control global de reservas públicas
--
-- IMPORTANTE:
--
-- - No modifica migraciones anteriores.
-- - Mantiene compatibilidad con businesses ya existentes.
-- - Inicializa valores seguros para Aura Studio.
-- ============================================================


-- ============================================================
-- DESCRIPTION
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN description TEXT;


-- ============================================================
-- CONTACT
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN website_url VARCHAR(500);


-- ============================================================
-- ADDRESS
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN address_line_1 VARCHAR(180);

ALTER TABLE businesses
    ADD COLUMN address_line_2 VARCHAR(180);

ALTER TABLE businesses
    ADD COLUMN postal_code VARCHAR(30);

ALTER TABLE businesses
    ADD COLUMN city VARCHAR(120);

ALTER TABLE businesses
    ADD COLUMN state_region VARCHAR(120);

ALTER TABLE businesses
    ADD COLUMN country_code VARCHAR(2);


-- ============================================================
-- REGIONAL SETTINGS
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN locale VARCHAR(20);

ALTER TABLE businesses
    ADD COLUMN currency VARCHAR(3);


-- ============================================================
-- BRANDING
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN logo_url VARCHAR(500);

ALTER TABLE businesses
    ADD COLUMN cover_image_url VARCHAR(500);

ALTER TABLE businesses
    ADD COLUMN primary_color VARCHAR(20);

ALTER TABLE businesses
    ADD COLUMN secondary_color VARCHAR(20);


-- ============================================================
-- PUBLIC BOOKING
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN public_booking_enabled BOOLEAN;


-- ============================================================
-- BACKFILL EXISTING DATA
-- ============================================================
--
-- Inicializamos todos los businesses existentes antes de
-- convertir las columnas obligatorias a NOT NULL.
--
-- ============================================================

UPDATE businesses
SET
    country_code = 'ES',
    locale = 'es-ES',
    currency = 'EUR',
    public_booking_enabled = TRUE
WHERE
    country_code IS NULL
    OR locale IS NULL
    OR currency IS NULL
    OR public_booking_enabled IS NULL;


-- ============================================================
-- AURA STUDIO · DEVELOPMENT PROFILE
-- ============================================================
--
-- Valores iniciales conocidos del business demo.
--
-- No sobreescribimos valores no nulos que pudieran haberse
-- configurado manualmente.
--
-- ============================================================

UPDATE businesses
SET
    city = COALESCE(city, 'Madrid'),
    state_region = COALESCE(state_region, 'Madrid'),
    country_code = COALESCE(country_code, 'ES'),
    locale = COALESCE(locale, 'es-ES'),
    currency = COALESCE(currency, 'EUR'),
    public_booking_enabled = COALESCE(public_booking_enabled, TRUE)
WHERE slug = 'aura-studio';


-- ============================================================
-- REQUIRED CONSTRAINTS
-- ============================================================

ALTER TABLE businesses
    ALTER COLUMN country_code SET NOT NULL;

ALTER TABLE businesses
    ALTER COLUMN locale SET NOT NULL;

ALTER TABLE businesses
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE businesses
    ALTER COLUMN public_booking_enabled SET NOT NULL;


-- ============================================================
-- DATABASE DEFAULTS
-- ============================================================
--
-- Aunque la entidad Java también contiene defaults,
-- mantenemos defaults en PostgreSQL como defensa adicional.
--
-- ============================================================

ALTER TABLE businesses
    ALTER COLUMN country_code SET DEFAULT 'ES';

ALTER TABLE businesses
    ALTER COLUMN locale SET DEFAULT 'es-ES';

ALTER TABLE businesses
    ALTER COLUMN currency SET DEFAULT 'EUR';

ALTER TABLE businesses
    ALTER COLUMN public_booking_enabled SET DEFAULT TRUE;


-- ============================================================
-- CHECK CONSTRAINTS
-- ============================================================

ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_country_code_length
        CHECK (char_length(country_code) = 2);

ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_currency_length
        CHECK (char_length(currency) = 3);

ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_primary_color
        CHECK (
            primary_color IS NULL
            OR primary_color ~ '^#[0-9A-Fa-f]{6}$'
        );

ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_secondary_color
        CHECK (
            secondary_color IS NULL
            OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'
        );


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_businesses_active
    ON businesses (active);

CREATE INDEX idx_businesses_city
    ON businesses (city);

CREATE INDEX idx_businesses_country_code
    ON businesses (country_code);