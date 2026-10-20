-- Motorline Taller. Development catalog for the workshop demo.
-- Outside Flyway. Does not change API code.
--
-- Idempotent. Existing rows are left untouched.
-- Relationships are resolved by business slug, service slug and
-- professional email (professionals have no slug; email is unique per business).
-- business_settings uses the V12 defaults.

BEGIN;

INSERT INTO businesses (
    name,
    slug,
    description,
    email,
    phone,
    website_url,
    address_line_1,
    address_line_2,
    postal_code,
    city,
    state_region,
    country_code,
    timezone,
    locale,
    currency,
    logo_url,
    cover_image_url,
    primary_color,
    secondary_color,
    public_booking_enabled,
    active
)
VALUES (
    'Motorline Taller',
    'motorline-taller',
    'Mecánica de confianza en Madrid. Te decimos qué tiene el coche, qué cuesta y cuándo lo tienes.',
    'hola@motorline-taller.example',
    '+34 910 000 000',
    NULL,
    'Calle del Taller, 8',
    NULL,
    '28045',
    'Madrid',
    'Madrid',
    'ES',
    'Europe/Madrid',
    'es-ES',
    'EUR',
    NULL,
    NULL,
    NULL,
    NULL,
    TRUE,
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
    version
)
SELECT
    b.id,
    120,
    60,
    15,
    0,
    0,
    TRUE,
    TRUE,
    1440,
    TRUE,
    1440,
    TRUE,
    1440,
    FALSE,
    120,
    0
FROM businesses b
WHERE b.slug = 'motorline-taller'
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO services (
    business_id,
    name,
    slug,
    description,
    duration_minutes,
    price,
    active,
    booking_enabled
)
SELECT
    b.id,
    v.name,
    v.slug,
    v.description,
    v.duration_minutes,
    v.price,
    TRUE,
    TRUE
FROM businesses b
JOIN (
    VALUES
        (
            'Revisión oficial',
            'revision-oficial',
            'Puntos de control del fabricante, niveles y un informe claro de lo que está bien y de lo que conviene vigilar.',
            120,
            149.00
        ),
        (
            'Cambio de aceite y filtros',
            'aceite-filtros',
            'Aceite, filtro de aceite y filtro de habitáculo. Te decimos la especificación que corresponde a tu motor.',
            45,
            69.00
        ),
        (
            'Pre-ITV',
            'pre-itv',
            'Revisión previa de luces, frenos, neumáticos y emisiones para llegar a la ITV sin improvisar.',
            30,
            35.00
        ),
        (
            'Neumáticos: montaje y equilibrado',
            'neumaticos',
            'Desmontaje, montaje, equilibrado y apriete. Comprobamos la presión y el desgaste del eje.',
            45,
            40.00
        ),
        (
            'Frenos: pastillas y discos',
            'frenos',
            'Medimos espesor y sustitución de pastillas o discos cuando el desgaste lo pide, no antes.',
            90,
            120.00
        ),
        (
            'Diagnosis electrónica',
            'diagnosis',
            'Lectura de averías, datos en vivo y una explicación en claro de qué significa el testigo.',
            30,
            45.00
        )
) AS v (name, slug, description, duration_minutes, price) ON TRUE
WHERE b.slug = 'motorline-taller'
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
    booking_enabled
)
SELECT
    b.id,
    v.first_name,
    v.last_name,
    v.display_name,
    v.email,
    NULL,
    v.bio,
    TRUE,
    TRUE
FROM businesses b
JOIN (
    VALUES
        (
            'Javier',
            'Ruiz',
            'Javier Ruiz',
            'javier.ruiz@motorline-taller.example',
            'Coordina el taller y se ocupa de la diagnosis. Traduce los códigos de avería a una decisión concreta.'
        ),
        (
            'Carlos',
            'Méndez',
            'Carlos Méndez',
            'carlos.mendez@motorline-taller.example',
            'Mantenimiento periódico y sistema de frenos. Mide antes de sustituir y deja el recambio a la vista.'
        ),
        (
            'Lucía',
            'Torres',
            'Lucía Torres',
            'lucia.torres@motorline-taller.example',
            'Neumáticos, equilibrado y la revisión previa a la ITV. Te dice qué fallaría y qué puede esperar.'
        )
) AS v (first_name, last_name, display_name, email, bio) ON TRUE
WHERE b.slug = 'motorline-taller'
  AND NOT EXISTS (
      SELECT 1
      FROM professionals p
      WHERE p.business_id = b.id
        AND lower(p.email) = lower(v.email)
  );

INSERT INTO professional_services (
    professional_id,
    service_id
)
SELECT
    p.id,
    s.id
FROM businesses b
JOIN (
    VALUES
        ('javier.ruiz@motorline-taller.example', 'revision-oficial'),
        ('javier.ruiz@motorline-taller.example', 'aceite-filtros'),
        ('javier.ruiz@motorline-taller.example', 'diagnosis'),
        ('carlos.mendez@motorline-taller.example', 'revision-oficial'),
        ('carlos.mendez@motorline-taller.example', 'aceite-filtros'),
        ('carlos.mendez@motorline-taller.example', 'frenos'),
        ('lucia.torres@motorline-taller.example', 'neumaticos'),
        ('lucia.torres@motorline-taller.example', 'pre-itv')
) AS v (professional_email, service_slug) ON TRUE
JOIN professionals p
    ON p.business_id = b.id
   AND lower(p.email) = lower(v.professional_email)
JOIN services s
    ON s.business_id = b.id
   AND s.slug = v.service_slug
WHERE b.slug = 'motorline-taller'
ON CONFLICT (professional_id, service_id) DO NOTHING;

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
FROM businesses b
JOIN professionals p
    ON p.business_id = b.id
JOIN (
    VALUES
        ('MONDAY', TIME '08:00', TIME '18:00'),
        ('TUESDAY', TIME '08:00', TIME '18:00'),
        ('WEDNESDAY', TIME '08:00', TIME '18:00'),
        ('THURSDAY', TIME '08:00', TIME '18:00'),
        ('FRIDAY', TIME '08:00', TIME '18:00'),
        ('SATURDAY', TIME '09:00', TIME '13:00')
) AS d (day_of_week, start_time, end_time) ON TRUE
WHERE b.slug = 'motorline-taller'
  AND lower(p.email) IN (
      'javier.ruiz@motorline-taller.example',
      'carlos.mendez@motorline-taller.example',
      'lucia.torres@motorline-taller.example'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM professional_availability a
      WHERE a.professional_id = p.id
        AND a.day_of_week = d.day_of_week
        AND a.start_time = d.start_time
        AND a.end_time = d.end_time
  );

COMMIT;
