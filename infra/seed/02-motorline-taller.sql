-- Motorline Taller. Development catalog and demo diary for the workshop.
-- Outside Flyway. Does not create users, passwords, tokens or memberships.
--
-- Idempotent. Existing catalog rows are left untouched.
-- Customers and appointments are inserted only when that email and start
-- time are not already there.
-- Relationships are resolved by business slug, service slug and
-- professional email (professionals have no slug; email is unique per business).
-- Services, team and hours follow apps/web/data/taller.ts.
-- Weekday hours are 08:00-18:00. Saturday 09:00-13:00 stays because the
-- site publishes it. Appointment times are relative to the Monday of the
-- current week in Europe/Madrid, from the previous week through the next one.
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
        ('Andrés', 'Prieto', 'andres.prieto@demo.wacds.local', '+34 612 847 305'),
        ('Elena', 'Navarro', 'elena.navarro@demo.wacds.local', '+34 623 519 748'),
        ('Pablo', 'Herrera', 'pablo.herrera@demo.wacds.local', '+34 634 280 916'),
        ('Marta', 'Gil', 'marta.gil@demo.wacds.local', '+34 645 173 602'),
        ('Hugo', 'Santos', 'hugo.santos@demo.wacds.local', '+34 656 904 231'),
        ('Irene', 'Vázquez', 'irene.vazquez@demo.wacds.local', '+34 667 318 540'),
        ('Raúl', 'Ortega', 'raul.ortega@demo.wacds.local', '+34 678 452 817'),
        ('Diego', 'Lara', 'diego.lara@demo.wacds.local', '+34 689 026 473')
) AS v (first_name, last_name, email, phone) ON TRUE
WHERE b.slug = 'motorline-taller'
  AND NOT EXISTS (
      SELECT 1
      FROM customers AS c
      WHERE c.business_id = b.id
        AND (
            lower(c.email) = lower(v.email)
            OR c.phone = v.phone
        )
  );

-- Notes use the same lines the public booking form writes:
-- "Matrícula: …" and "Modelo: …", then an optional note.
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
        (
            'elena.navarro@demo.wacds.local',
            'javier.ruiz@motorline-taller.example',
            'diagnosis',
            -6, 9, 0,
            'COMPLETED',
            E'Matrícula: 4821 BCD\nModelo: Volkswagen Golf\n\nTestigo del motor encendido desde el lunes.'
        ),
        (
            'pablo.herrera@demo.wacds.local',
            'javier.ruiz@motorline-taller.example',
            'revision-oficial',
            -4, 11, 0,
            'COMPLETED',
            E'Matrícula: 1937 KLM\nModelo: Seat León\n\nRevisión de los 60.000 km.'
        ),
        (
            'marta.gil@demo.wacds.local',
            'javier.ruiz@motorline-taller.example',
            'aceite-filtros',
            2, 16, 0,
            'CONFIRMED',
            E'Matrícula: 7604 HNP\nModelo: Renault Clio'
        ),
        (
            'hugo.santos@demo.wacds.local',
            'javier.ruiz@motorline-taller.example',
            'diagnosis',
            7, 9, 30,
            'PENDING',
            E'Matrícula: 2518 FTR\nModelo: Toyota Corolla\n\nRuido metálico al arrancar en frío.'
        ),
        (
            'irene.vazquez@demo.wacds.local',
            'carlos.mendez@motorline-taller.example',
            'frenos',
            -7, 10, 0,
            'COMPLETED',
            E'Matrícula: 8346 JVZ\nModelo: Peugeot 308\n\nVibra al frenar desde hace dos semanas.'
        ),
        (
            'raul.ortega@demo.wacds.local',
            'carlos.mendez@motorline-taller.example',
            'aceite-filtros',
            -5, 15, 0,
            'NO_SHOW',
            E'Matrícula: 5092 LGS\nModelo: Kia Sportage'
        ),
        (
            'andres.prieto@demo.wacds.local',
            'carlos.mendez@motorline-taller.example',
            'revision-oficial',
            4, 9, 0,
            'CONFIRMED',
            E'Matrícula: 6720 MWN\nModelo: Hyundai Tucson'
        ),
        (
            'diego.lara@demo.wacds.local',
            'carlos.mendez@motorline-taller.example',
            'frenos',
            8, 12, 0,
            'PENDING',
            E'Matrícula: 3185 PRC\nModelo: Citroën C4'
        ),
        (
            'elena.navarro@demo.wacds.local',
            'lucia.torres@motorline-taller.example',
            'neumaticos',
            -3, 8, 30,
            'COMPLETED',
            E'Matrícula: 4821 BCD\nModelo: Volkswagen Golf\n\nDos neumáticos delanteros.'
        ),
        (
            'pablo.herrera@demo.wacds.local',
            'lucia.torres@motorline-taller.example',
            'pre-itv',
            -7, 12, 0,
            'CANCELLED',
            E'Matrícula: 1937 KLM\nModelo: Seat León'
        ),
        (
            'hugo.santos@demo.wacds.local',
            'lucia.torres@motorline-taller.example',
            'pre-itv',
            3, 17, 0,
            'CONFIRMED',
            E'Matrícula: 2518 FTR\nModelo: Toyota Corolla'
        ),
        (
            'marta.gil@demo.wacds.local',
            'lucia.torres@motorline-taller.example',
            'neumaticos',
            9, 10, 0,
            'PENDING',
            E'Matrícula: 7604 HNP\nModelo: Renault Clio\n\nEquilibrado del eje delantero.'
        )
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
WHERE b.slug = 'motorline-taller'
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
