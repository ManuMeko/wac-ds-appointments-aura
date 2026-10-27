-- Aura Studio. Development catalog and demo diary for slug aura-studio.
-- Outside Flyway. Does not create users, passwords, tokens or memberships.
--
-- Idempotent. Catalog rows are inserted when missing. Regression copy,
-- inactive booking flags and partial hours of the three demo professionals
-- are corrected in place. Customers and appointments are inserted only
-- when that email and start time are not already there.
-- Relationships are resolved by business slug, service slug and
-- professional email (professionals have no slug; email is unique per business).
-- Appointment times are relative to the Monday of the current week
-- in Europe/Madrid, from the previous week through the next one.

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
    'Estética, belleza y bienestar desde una forma más personal de entender el cuidado.',
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

UPDATE businesses
SET
    description = 'Estética, belleza y bienestar desde una forma más personal de entender el cuidado.',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'aura-studio'
  AND description ILIKE '%regres%';

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
            'Una experiencia de desconexión completa para liberar tensión y recuperar el bienestar físico.',
            50,
            50.00,
            TRUE,
            TRUE,
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

UPDATE services AS s
SET
    description = 'Una experiencia de desconexión completa para liberar tensión y recuperar el bienestar físico.',
    active = TRUE,
    booking_enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
FROM businesses AS b
WHERE s.business_id = b.id
  AND b.slug = 'aura-studio'
  AND s.slug = 'masaje'
  AND (
      s.description ILIKE '%regres%'
      OR s.active IS DISTINCT FROM TRUE
      OR s.booking_enabled IS DISTINCT FROM TRUE
  );

UPDATE services AS s
SET
    active = TRUE,
    booking_enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
FROM businesses AS b
WHERE s.business_id = b.id
  AND b.slug = 'aura-studio'
  AND s.slug IN ('tratamiento-facial', 'limpieza-facial', 'masaje')
  AND (
      s.active IS DISTINCT FROM TRUE
      OR s.booking_enabled IS DISTINCT FROM TRUE
  );

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
            'Especializada en diagnóstico estético y tratamientos faciales personalizados.',
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
            'Especialista en limpieza facial y en protocolos adaptados a cada piel.',
            TRUE,
            TRUE,
            TIMESTAMPTZ '2026-10-08T08:44:13.593761+00:00',
            TIMESTAMPTZ '2026-10-12T07:53:48.027546+00:00'
        ),
        (
            'Sergio',
            'López',
            'Sergio · Masajista',
            'sergio@aurastudio.es',
            '600123124',
            'Masajista. Trabaja la tensión con un ritmo tranquilo y explica qué está soltando.',
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

UPDATE professionals AS p
SET
    bio = v.bio,
    active = TRUE,
    booking_enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
FROM businesses AS b
JOIN (
    VALUES
        (
            'laura@aurastudio.es',
            'Especializada en diagnóstico estético y tratamientos faciales personalizados.'
        ),
        (
            'lucia@aurastudio.es',
            'Especialista en limpieza facial y en protocolos adaptados a cada piel.'
        ),
        (
            'sergio@aurastudio.es',
            'Masajista. Trabaja la tensión con un ritmo tranquilo y explica qué está soltando.'
        )
) AS v (email, bio) ON TRUE
WHERE p.business_id = b.id
  AND b.slug = 'aura-studio'
  AND lower(p.email) = lower(v.email)
  AND (
      p.bio IS DISTINCT FROM v.bio
      OR p.active IS DISTINCT FROM TRUE
      OR p.booking_enabled IS DISTINCT FROM TRUE
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
        ('laura@aurastudio.es', 'tratamiento-facial', TIMESTAMPTZ '2026-09-28T07:00:28.810822+00:00'),
        ('lucia@aurastudio.es', 'limpieza-facial', TIMESTAMPTZ '2026-10-13T15:58:46.993165+00:00'),
        ('lucia@aurastudio.es', 'tratamiento-facial', TIMESTAMPTZ '2026-10-13T15:58:46.993165+00:00'),
        ('sergio@aurastudio.es', 'masaje', TIMESTAMPTZ '2026-10-13T15:58:46.993165+00:00')
) AS v (professional_email, service_slug, created_at) ON TRUE
JOIN professionals p
    ON p.business_id = b.id
   AND lower(p.email) = lower(v.professional_email)
JOIN services s
    ON s.business_id = b.id
   AND s.slug = v.service_slug
WHERE b.slug = 'aura-studio'
ON CONFLICT (professional_id, service_id) DO NOTHING;

-- Opening hours from the Aura site: Monday-Friday 10:00-20:00, Saturday 10:00-15:00.
-- Drop leftover partial ranges for these three professionals, then insert the canonical week.
DELETE FROM professional_availability AS a
USING professionals AS p, businesses AS b
WHERE a.professional_id = p.id
  AND p.business_id = b.id
  AND b.slug = 'aura-studio'
  AND lower(p.email) IN (
      'laura@aurastudio.es',
      'lucia@aurastudio.es',
      'sergio@aurastudio.es'
  )
  AND NOT (
      a.active
      AND (
          (
              a.day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')
              AND a.start_time = TIME '10:00'
              AND a.end_time = TIME '20:00'
          )
          OR (
              a.day_of_week = 'SATURDAY'
              AND a.start_time = TIME '10:00'
              AND a.end_time = TIME '15:00'
          )
      )
  );

INSERT INTO professional_availability (
    professional_id,
    day_of_week,
    start_time,
    end_time,
    active
)
SELECT
    p.id,
    d.day_of_week,
    d.start_time,
    d.end_time,
    TRUE
FROM businesses AS b
JOIN professionals AS p
    ON p.business_id = b.id
JOIN (
    VALUES
        ('MONDAY', TIME '10:00', TIME '20:00'),
        ('TUESDAY', TIME '10:00', TIME '20:00'),
        ('WEDNESDAY', TIME '10:00', TIME '20:00'),
        ('THURSDAY', TIME '10:00', TIME '20:00'),
        ('FRIDAY', TIME '10:00', TIME '20:00'),
        ('SATURDAY', TIME '10:00', TIME '15:00')
) AS d (day_of_week, start_time, end_time) ON TRUE
WHERE b.slug = 'aura-studio'
  AND lower(p.email) IN (
      'laura@aurastudio.es',
      'lucia@aurastudio.es',
      'sergio@aurastudio.es'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM professional_availability AS existing
      WHERE existing.professional_id = p.id
        AND existing.day_of_week = d.day_of_week
        AND existing.start_time = d.start_time
        AND existing.end_time = d.end_time
  );

INSERT INTO customers (
    business_id,
    first_name,
    last_name,
    email,
    phone,
    active
)
SELECT
    b.id,
    v.first_name,
    v.last_name,
    v.email,
    v.phone,
    TRUE
FROM businesses AS b
JOIN (
    VALUES
        ('Claudia', 'Morales', 'claudia.morales@demo.wacds.local', '+34 611 284 903'),
        ('Marina', 'Ruiz', 'marina.ruiz@demo.wacds.local', '+34 622 475 160'),
        ('Elena', 'Vázquez', 'elena.vazquez@demo.wacds.local', '+34 633 908 274'),
        ('Nuria', 'Soler', 'nuria.soler@demo.wacds.local', '+34 644 156 382'),
        ('Alba', 'Campos', 'alba.campos@demo.wacds.local', '+34 655 327 841'),
        ('Paula', 'Herrero', 'paula.herrero@demo.wacds.local', '+34 671 819 205'),
        ('Inés', 'Delgado', 'ines.delgado@demo.wacds.local', '+34 682 540 736'),
        ('Carmen', 'Ortiz', 'carmen.ortiz@demo.wacds.local', '+34 693 714 058')
) AS v (first_name, last_name, email, phone) ON TRUE
WHERE b.slug = 'aura-studio'
  AND NOT EXISTS (
      SELECT 1
      FROM customers AS c
      WHERE c.business_id = b.id
        AND (
            lower(c.email) = lower(v.email)
            OR c.phone = v.phone
        )
  );

-- day_offset is relative to Monday of the current week in Europe/Madrid.
INSERT INTO appointments (
    business_id,
    customer_id,
    professional_id,
    service_id,
    start_at,
    end_at,
    status,
    notes
)
SELECT
    b.id,
    c.id,
    p.id,
    s.id,
    timezone(
        'Europe/Madrid',
        date_trunc('week', timezone('Europe/Madrid', now()))
            + make_interval(days => v.day_offset, hours => v.hour_of_day, mins => v.minute_of_hour)
    ),
    timezone(
        'Europe/Madrid',
        date_trunc('week', timezone('Europe/Madrid', now()))
            + make_interval(days => v.day_offset, hours => v.hour_of_day, mins => v.minute_of_hour)
    ) + make_interval(mins => s.duration_minutes),
    v.status,
    v.notes
FROM businesses AS b
JOIN (
    VALUES
        ('claudia.morales@demo.wacds.local', 'laura@aurastudio.es', 'tratamiento-facial', -6, 10, 0, 'COMPLETED', 'Piel apagada y deshidratación. Prefiere un ritual corto.'::text),
        ('marina.ruiz@demo.wacds.local', 'laura@aurastudio.es', 'limpieza-facial', -4, 12, 0, 'COMPLETED', 'Primera visita. Piel mixta.'::text),
        ('elena.vazquez@demo.wacds.local', 'laura@aurastudio.es', 'tratamiento-facial', 2, 17, 0, 'CONFIRMED', 'Quiere mantener la hidratación de la última sesión.'::text),
        ('nuria.soler@demo.wacds.local', 'laura@aurastudio.es', 'limpieza-facial', 7, 11, 0, 'PENDING', 'Viene recomendada por una clienta.'::text),
        ('alba.campos@demo.wacds.local', 'lucia@aurastudio.es', 'limpieza-facial', -7, 16, 0, 'COMPLETED', 'Zona T con brillos.'::text),
        ('paula.herrero@demo.wacds.local', 'lucia@aurastudio.es', 'tratamiento-facial', -5, 18, 0, 'NO_SHOW', 'No se presentó.'::text),
        ('ines.delgado@demo.wacds.local', 'lucia@aurastudio.es', 'limpieza-facial', 4, 10, 30, 'CONFIRMED', 'Sensibilidad en las mejillas.'::text),
        ('carmen.ortiz@demo.wacds.local', 'lucia@aurastudio.es', 'tratamiento-facial', 8, 15, 0, 'PENDING', NULL::text),
        ('claudia.morales@demo.wacds.local', 'sergio@aurastudio.es', 'masaje', -3, 11, 0, 'COMPLETED', 'Espalda cargada después del trabajo.'::text),
        ('marina.ruiz@demo.wacds.local', 'sergio@aurastudio.es', 'masaje', -7, 11, 0, 'CANCELLED', 'Aviso de cancelación la tarde anterior.'::text),
        ('elena.vazquez@demo.wacds.local', 'sergio@aurastudio.es', 'masaje', 3, 18, 0, 'CONFIRMED', 'Prefiere presión media.'::text),
        ('alba.campos@demo.wacds.local', 'sergio@aurastudio.es', 'masaje', 9, 12, 0, 'PENDING', 'Sesión de mantenimiento.'::text)
) AS v (
    customer_email,
    professional_email,
    service_slug,
    day_offset,
    hour_of_day,
    minute_of_hour,
    status,
    notes
) ON TRUE
JOIN customers AS c
    ON c.business_id = b.id
   AND lower(c.email) = lower(v.customer_email)
JOIN professionals AS p
    ON p.business_id = b.id
   AND lower(p.email) = lower(v.professional_email)
JOIN services AS s
    ON s.business_id = b.id
   AND s.slug = v.service_slug
WHERE b.slug = 'aura-studio'
  AND NOT EXISTS (
      SELECT 1
      FROM appointments AS a
      WHERE a.business_id = b.id
        AND a.customer_id = c.id
        AND a.professional_id = p.id
        AND a.start_at = timezone(
            'Europe/Madrid',
            date_trunc('week', timezone('Europe/Madrid', now()))
                + make_interval(days => v.day_offset, hours => v.hour_of_day, mins => v.minute_of_hour)
        )
  );

COMMIT;
