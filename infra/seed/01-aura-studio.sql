-- Aura Studio. Snapshot of the development database on 2026-10-20.
-- Source: businesses, business_settings, services, professionals,
-- professional_services and professional_availability for slug aura-studio.
-- schedule_exceptions had no rows, so it is not seeded.
-- Excluded: users, passwords, tokens, appointments and customers.
--
-- Idempotent. Existing rows are left untouched.
-- Relationships are resolved by business slug, service slug and
-- professional email (professionals have no slug; email is unique per business).

BEGIN;

INSERT INTO businesses (
    name,
    slug,
    email,
    phone,
    timezone,
    active,
    created_at,
    updated_at,
    description,
    website_url,
    address_line_1,
    address_line_2,
    postal_code,
    city,
    state_region,
    country_code,
    locale,
    currency,
    logo_url,
    cover_image_url,
    primary_color,
    secondary_color,
    public_booking_enabled
)
VALUES (
    'Aura Studio',
    'aura-studio',
    'hola@aurastudio.es',
    '+34 611 111 111',
    'Europe/Madrid',
    TRUE,
    '2026-09-26T16:38:46.249741+00:00',
    '2026-10-13T03:59:25.790789+00:00',
    'Estética, belleza y bienestar premium en Madrid. Prueba de regresión final.',
    'https://aurastudio.es',
    'Calle Serrano 100',
    NULL,
    '28006',
    'Madrid',
    'Madrid',
    'ES',
    'es-ES',
    'EUR',
    NULL,
    NULL,
    '#A49C8E',
    '#E59306',
    TRUE
)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO business_settings (
    business_id,
    minimum_booking_notice_minutes,
    booking_horizon_days,
    slot_interval_minutes,
    default_buffer_before_minutes,
    default_buffer_after_minutes,
    auto_confirm_appointments,
    online_cancellation_enabled,
    minimum_cancellation_notice_minutes,
    online_rescheduling_enabled,
    minimum_reschedule_notice_minutes,
    reminders_enabled,
    first_reminder_minutes_before,
    second_reminder_enabled,
    second_reminder_minutes_before,
    version,
    created_at,
    updated_at
)
SELECT
    b.id,
    180,
    90,
    20,
    0,
    10,
    FALSE,
    TRUE,
    1440,
    TRUE,
    1440,
    TRUE,
    1440,
    FALSE,
    120,
    7,
    '2026-10-02T17:09:39.287277+00:00',
    '2026-10-03T08:45:06.056081+00:00'
FROM businesses b
WHERE b.slug = 'aura-studio'
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO services (
    business_id,
    name,
    slug,
    description,
    duration_minutes,
    price,
    active,
    booking_enabled,
    created_at,
    updated_at
)
SELECT
    b.id,
    v.name,
    v.slug,
    v.description,
    v.duration_minutes,
    v.price,
    v.active,
    v.booking_enabled,
    v.created_at,
    v.updated_at
FROM businesses b
JOIN (
    VALUES
        (
            'Tratamiento facial',
            'tratamiento-facial',
            'Ritual personalizado para devolver luminosidad, hidratación y equilibrio a tu piel.',
            45,
            49.00,
            TRUE,
            TRUE,
            TIMESTAMPTZ '2026-09-28T06:24:35.282728+00:00',
            TIMESTAMPTZ '2026-10-08T07:23:10.995899+00:00'
        ),
        (
            'Masaje',
            'masaje',
            'Masaje de prueba para regresión final.',
            50,
            50.00,
            TRUE,
            FALSE,
            TIMESTAMPTZ '2026-10-08T07:27:50.474958+00:00',
            TIMESTAMPTZ '2026-10-12T07:25:45.835876+00:00'
        ),
        (
            'Limpieza facial',
            'limpieza-facial',
            'Limpieza facial profunda para purificar, equilibrar y devolver frescura y luminosidad a la piel.',
            30,
            35.00,
            TRUE,
            TRUE,
            TIMESTAMPTZ '2026-10-12T07:28:53.407356+00:00',
            TIMESTAMPTZ '2026-10-12T07:28:53.407356+00:00'
        )
) AS v (
    name,
    slug,
    description,
    duration_minutes,
    price,
    active,
    booking_enabled,
    created_at,
    updated_at
) ON TRUE
WHERE b.slug = 'aura-studio'
ON CONFLICT (business_id, slug) DO NOTHING;

INSERT INTO professionals (
    business_id,
    first_name,
    last_name,
    display_name,
    email,
    phone,
    bio,
    active,
    booking_enabled,
    created_at,
    updated_at
)
SELECT
    b.id,
    v.first_name,
    v.last_name,
    v.display_name,
    v.email,
    v.phone,
    v.bio,
    v.active,
    v.booking_enabled,
    v.created_at,
    v.updated_at
FROM businesses b
JOIN (
    VALUES
        (
            'Laura',
            'Martín',
            'Laura Martín',
            'laura@aurastudio.es',
            '+34 600 000 001',
            'Especialista en tratamientos faciales y bienestar de la piel.',
            TRUE,
            TRUE,
            TIMESTAMPTZ '2026-09-28T06:43:52.08107+00:00',
            TIMESTAMPTZ '2026-09-28T06:43:52.08107+00:00'
        ),
        (
            'Lucía',
            'Sánchez',
            'Lucía · Especialista facial',
            'lucia@aurastudio.es',
            '600123123',
            'Especialista facial · prueba de regresión final.',
            TRUE,
            FALSE,
            TIMESTAMPTZ '2026-10-08T08:44:13.593761+00:00',
            TIMESTAMPTZ '2026-10-12T07:53:48.027546+00:00'
        ),
        (
            'Sergio',
            'López',
            'Sergio · Masajista',
            'sergio@aurastudio.es',
            '600123124',
            'Profesional de prueba para regresión final.',
            TRUE,
            TRUE,
            TIMESTAMPTZ '2026-10-13T03:52:35.870109+00:00',
            TIMESTAMPTZ '2026-10-13T03:52:35.870109+00:00'
        )
) AS v (
    first_name,
    last_name,
    display_name,
    email,
    phone,
    bio,
    active,
    booking_enabled,
    created_at,
    updated_at
) ON TRUE
WHERE b.slug = 'aura-studio'
  AND NOT EXISTS (
      SELECT 1
      FROM professionals p
      WHERE p.business_id = b.id
        AND lower(p.email) = lower(v.email)
  );

INSERT INTO professional_services (
    professional_id,
    service_id,
    created_at
)
SELECT
    p.id,
    s.id,
    v.created_at
FROM businesses b
JOIN (
    VALUES
        ('laura@aurastudio.es', 'limpieza-facial', TIMESTAMPTZ '2026-10-13T15:58:46.993165+00:00'),
        ('laura@aurastudio.es', 'masaje', TIMESTAMPTZ '2026-10-10T05:46:25.333433+00:00'),
        ('laura@aurastudio.es', 'tratamiento-facial', TIMESTAMPTZ '2026-09-28T07:00:28.810822+00:00')
) AS v (professional_email, service_slug, created_at) ON TRUE
JOIN professionals p
    ON p.business_id = b.id
   AND lower(p.email) = lower(v.professional_email)
JOIN services s
    ON s.business_id = b.id
   AND s.slug = v.service_slug
WHERE b.slug = 'aura-studio'
ON CONFLICT (professional_id, service_id) DO NOTHING;

INSERT INTO professional_availability (
    professional_id,
    day_of_week,
    start_time,
    end_time,
    active,
    created_at,
    updated_at
)
SELECT
    p.id,
    v.day_of_week,
    v.start_time,
    v.end_time,
    TRUE,
    v.created_at,
    v.updated_at
FROM businesses b
JOIN (
    VALUES
        ('laura@aurastudio.es', 'MONDAY', TIME '09:00', TIME '14:00', TIMESTAMPTZ '2026-09-29T15:47:15.425533+00:00', TIMESTAMPTZ '2026-09-29T15:47:15.425533+00:00'),
        ('laura@aurastudio.es', 'MONDAY', TIME '16:00', TIME '19:00', TIMESTAMPTZ '2026-09-29T15:48:34.000195+00:00', TIMESTAMPTZ '2026-09-29T15:48:34.000195+00:00'),
        ('laura@aurastudio.es', 'THURSDAY', TIME '09:00', TIME '17:00', TIMESTAMPTZ '2026-10-10T06:12:22.170906+00:00', TIMESTAMPTZ '2026-10-10T06:12:22.170906+00:00'),
        ('laura@aurastudio.es', 'TUESDAY', TIME '09:00', TIME '13:00', TIMESTAMPTZ '2026-10-10T06:34:34.499845+00:00', TIMESTAMPTZ '2026-10-10T06:34:34.499845+00:00'),
        ('laura@aurastudio.es', 'WEDNESDAY', TIME '10:00', TIME '12:00', TIMESTAMPTZ '2026-10-10T07:53:59.707089+00:00', TIMESTAMPTZ '2026-10-10T07:53:59.707089+00:00')
) AS v (
    professional_email,
    day_of_week,
    start_time,
    end_time,
    created_at,
    updated_at
) ON TRUE
JOIN professionals p
    ON p.business_id = b.id
   AND lower(p.email) = lower(v.professional_email)
WHERE b.slug = 'aura-studio'
  AND NOT EXISTS (
      SELECT 1
      FROM professional_availability a
      WHERE a.professional_id = p.id
        AND a.day_of_week = v.day_of_week
        AND a.start_time = v.start_time
        AND a.end_time = v.end_time
  );

COMMIT;
