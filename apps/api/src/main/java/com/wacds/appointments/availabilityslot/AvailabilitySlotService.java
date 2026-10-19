
package com.wacds.appointments.availabilityslot;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.availability.ProfessionalAvailability;
import com.wacds.appointments.availability.ProfessionalAvailabilityRepository;
import com.wacds.appointments.availabilityslot.dto.AvailabilitySlotResponse;
import com.wacds.appointments.availabilityslot.dto.DailyAvailabilityResponse;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.business.settings.BusinessSettingsRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentId;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentRepository;
import com.wacds.appointments.scheduleexception.ScheduleException;
import com.wacds.appointments.scheduleexception.ScheduleExceptionRepository;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Motor de calculo de disponibilidad.
 *
 * RESPONSABILIDADES:
 *
 * - Resolver negocio, profesional y servicio.
 * - Validar aislamiento multitenant.
 * - Validar asignacion profesional-servicio.
 * - Aplicar BusinessSettings.
 * - Consultar horarios semanales.
 * - Aplicar excepciones de agenda.
 * - Excluir citas que ocupan horarios.
 * - Aplicar buffers operativos.
 * - Aplicar antelacion minima.
 * - Aplicar horizonte de reservas.
 * - Generar horarios disponibles.
 *
 * PRIORIDAD DE HORARIOS:
 *
 * 1. UNAVAILABLE
 * 2. CUSTOM_HOURS
 * 3. Horario semanal habitual
 *
 * CONCURRENCIA:
 *
 * Este servicio calcula disponibilidad.
 *
 * No adquiere bloqueos exclusivos.
 *
 * Las operaciones que crean o modifican citas
 * deben bloquear previamente la agenda mediante
 * AppointmentRepository.lockProfessionalSchedule().
 *
 * PublicBookingService utiliza este motor
 * dentro de su transaccion de reserva.
 *
 * LIMITACION ACTUAL:
 *
 * Las franjas de trabajo no pueden cruzar
 * la medianoche.
 *
 * Los horarios locales ambiguos o inexistentes
 * por cambios de hora no se ofrecen como slots.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AvailabilitySlotService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalRepository professionalRepository;

    private final ServiceRepository serviceRepository;

    private final ProfessionalServiceAssignmentRepository
        assignmentRepository;

    private final ProfessionalAvailabilityRepository
        availabilityRepository;

    private final ScheduleExceptionRepository
        scheduleExceptionRepository;

    private final AppointmentRepository appointmentRepository;

    private final BusinessSettingsRepository
        businessSettingsRepository;

    // =========================================================
    // DAILY AVAILABILITY
    // =========================================================

    /**
     * Calcula los horarios disponibles de un profesional
     * para un servicio y una fecha.
     *
     * Esta consulta es de solo lectura.
     *
     * Su resultado no constituye una reserva
     * ni mantiene un bloqueo exclusivo.
     */
    @Transactional(readOnly = true)
    public DailyAvailabilityResponse getDailyAvailability(
        Long professionalId,
        Long serviceId,
        LocalDate date
    ) {

        validateProfessionalId(professionalId);
        validateServiceId(serviceId);
        validateDate(date);

        Professional professional =
            findProfessional(professionalId);

        Service service =
            findService(serviceId);

        validateProfessionalAndService(
            professional,
            service
        );

        validateAssignment(
            professionalId,
            serviceId
        );

        Business business =
            requireProfessionalBusiness(professional);

        Long businessId = business.getId();

        BusinessSettings settings =
            requireBusinessSettings(businessId);

        ZoneId businessZone =
            resolveBusinessZone(business);

        LocalDate today =
            LocalDate.now(businessZone);

        // -----------------------------------------------------
        // PAST DATES
        // -----------------------------------------------------

        if (date.isBefore(today)) {
            return buildResponse(
                professional,
                service,
                date,
                businessZone,
                List.of()
            );
        }

        // -----------------------------------------------------
        // BOOKING HORIZON
        // -----------------------------------------------------

        LocalDate latestBookableDate =
            today.plusDays(
                settings.getBookingHorizonDays()
            );

        if (date.isAfter(latestBookableDate)) {
            return buildResponse(
                professional,
                service,
                date,
                businessZone,
                List.of()
            );
        }

        // -----------------------------------------------------
        // WORKING RANGES
        // -----------------------------------------------------

        List<TimeRange> workingRanges =
            resolveWorkingRanges(
                professional,
                date
            );

        if (workingRanges.isEmpty()) {
            return buildResponse(
                professional,
                service,
                date,
                businessZone,
                List.of()
            );
        }

        // -----------------------------------------------------
        // EXISTING APPOINTMENTS
        // -----------------------------------------------------

        List<Appointment> existingAppointments =
            appointmentRepository
                .findAllByBusinessIdAndProfessionalIdOrderByStartAtDesc(
                    businessId,
                    professionalId
                );

        validateAppointmentsBelongToContext(
            existingAppointments,
            businessId,
            professionalId
        );

        List<Appointment> occupyingAppointments =
            existingAppointments.stream()
                .filter(appointment ->
                    appointment.getStatus()
                        != Appointment.AppointmentStatus.CANCELLED
                )
                .toList();

        // -----------------------------------------------------
        // GENERATE AVAILABLE SLOTS
        // -----------------------------------------------------

        List<AvailabilitySlotResponse> slots =
            generateSlots(
                date,
                businessZone,
                service.getDurationMinutes(),
                workingRanges,
                occupyingAppointments,
                settings
            );

        return buildResponse(
            professional,
            service,
            date,
            businessZone,
            slots
        );
    }

    // =========================================================
    // ENTITY LOOKUPS
    // =========================================================

    private Professional findProfessional(
        Long professionalId
    ) {

        return professionalRepository.findById(
            professionalId
        ).orElseThrow(
            () -> new ResourceNotFoundException(
                "No existe ningún profesional con id: "
                    + professionalId
            )
        );
    }

    private Service findService(
        Long serviceId
    ) {

        return serviceRepository.findById(
            serviceId
        ).orElseThrow(
            () -> new ResourceNotFoundException(
                "No existe ningún servicio con id: "
                    + serviceId
            )
        );
    }

    // =========================================================
    // BUSINESS SETTINGS
    // =========================================================

    private BusinessSettings requireBusinessSettings(
        Long businessId
    ) {

        if (
            businessId == null
                || businessId <= 0
        ) {
            throw new IllegalArgumentException(
                "businessId debe ser válido para recuperar "
                    + "la configuración operativa."
            );
        }

        BusinessSettings settings =
            businessSettingsRepository
                .findByBusinessId(businessId)
                .orElseThrow(
                    () -> new ConflictException(
                        "El negocio "
                            + businessId
                            + " no tiene configuración operativa."
                    )
                );

        validateBusinessSettings(
            settings,
            businessId
        );

        return settings;
    }

    private void validateBusinessSettings(
        BusinessSettings settings,
        Long expectedBusinessId
    ) {

        if (
            settings == null
                || settings.getId() == null
        ) {
            throw new IllegalStateException(
                "BusinessSettings no está persistido."
            );
        }

        if (
            settings.getBusiness() == null
                || settings.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "BusinessSettings no tiene un negocio válido."
            );
        }

        if (
            !Objects.equals(
                settings.getBusiness().getId(),
                expectedBusinessId
            )
        ) {
            throw new IllegalStateException(
                "BusinessSettings pertenece a otro negocio."
            );
        }

        if (
            settings.getMinimumBookingNoticeMinutes() < 0
                || settings.getMinimumBookingNoticeMinutes() > 525600
        ) {
            throw new IllegalStateException(
                "minimumBookingNoticeMinutes no es válido."
            );
        }

        if (
            settings.getBookingHorizonDays() < 1
                || settings.getBookingHorizonDays() > 730
        ) {
            throw new IllegalStateException(
                "bookingHorizonDays no es válido."
            );
        }

        if (
            settings.getSlotIntervalMinutes() < 5
                || settings.getSlotIntervalMinutes() > 240
        ) {
            throw new IllegalStateException(
                "slotIntervalMinutes no es válido."
            );
        }

        if (
            settings.getDefaultBufferBeforeMinutes() < 0
                || settings.getDefaultBufferBeforeMinutes() > 240
                || settings.getDefaultBufferAfterMinutes() < 0
                || settings.getDefaultBufferAfterMinutes() > 240
        ) {
            throw new IllegalStateException(
                "Los buffers operativos no son válidos."
            );
        }
    }

    // =========================================================
    // PROFESSIONAL / SERVICE VALIDATION
    // =========================================================

    private void validateProfessionalAndService(
        Professional professional,
        Service service
    ) {

        if (professional == null) {
            throw new IllegalArgumentException(
                "El profesional es obligatorio."
            );
        }

        if (service == null) {
            throw new IllegalArgumentException(
                "El servicio es obligatorio."
            );
        }

        Business professionalBusiness =
            requireProfessionalBusiness(professional);

        Business serviceBusiness =
            requireServiceBusiness(service);

        if (
            !Objects.equals(
                professionalBusiness.getId(),
                serviceBusiness.getId()
            )
        ) {
            throw new ConflictException(
                "El profesional y el servicio "
                    + "no pertenecen al mismo negocio."
            );
        }

        if (
            !professionalBusiness.isActive()
                || !serviceBusiness.isActive()
        ) {
            throw new ConflictException(
                "El negocio no está activo."
            );
        }

        if (!professional.isActive()) {
            throw new ConflictException(
                "El profesional no está activo."
            );
        }

        if (!professional.isBookingEnabled()) {
            throw new ConflictException(
                "El profesional no está habilitado para reservas."
            );
        }

        if (!service.isActive()) {
            throw new ConflictException(
                "El servicio no está activo."
            );
        }

        if (!service.isBookingEnabled()) {
            throw new ConflictException(
                "El servicio no está habilitado para reservas."
            );
        }

        if (
            service.getDurationMinutes() == null
                || service.getDurationMinutes() <= 0
        ) {
            throw new ConflictException(
                "El servicio no tiene una duración válida."
            );
        }
    }

    // =========================================================
    // BUSINESS VALIDATION
    // =========================================================

    private Business requireProfessionalBusiness(
        Professional professional
    ) {

        if (
            professional == null
                || professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El profesional no tiene un negocio válido asociado."
            );
        }

        return professional.getBusiness();
    }

    private Business requireServiceBusiness(
        Service service
    ) {

        if (
            service == null
                || service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El servicio no tiene un negocio válido asociado."
            );
        }

        return service.getBusiness();
    }

    // =========================================================
    // ASSIGNMENT VALIDATION
    // =========================================================

    private void validateAssignment(
        Long professionalId,
        Long serviceId
    ) {

        ProfessionalServiceAssignmentId assignmentId =
            new ProfessionalServiceAssignmentId(
                professionalId,
                serviceId
            );

        if (
            !assignmentRepository.existsById(assignmentId)
        ) {
            throw new ConflictException(
                "El servicio "
                    + serviceId
                    + " no está asignado al profesional "
                    + professionalId
            );
        }
    }

    // =========================================================
    // WORKING RANGES
    // =========================================================

    /**
     * Prioridad:
     *
     * UNAVAILABLE -> sin disponibilidad.
     *
     * CUSTOM_HOURS -> sustituye el horario semanal.
     *
     * En ausencia de excepciones se utiliza
     * el horario semanal habitual.
     */
    private List<TimeRange> resolveWorkingRanges(
        Professional professional,
        LocalDate date
    ) {

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalArgumentException(
                "El profesional es obligatorio."
            );
        }

        Long professionalId = professional.getId();

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findActiveByProfessionalIdAndExceptionDate(
                    professionalId,
                    date
                );

        validateScheduleExceptionsBelongToProfessional(
            exceptions,
            professionalId
        );

        boolean unavailable = exceptions.stream()
            .anyMatch(exception ->
                exception.getType()
                    == ScheduleException.ExceptionType.UNAVAILABLE
            );

        if (unavailable) {
            return List.of();
        }

        List<TimeRange> customHours =
            exceptions.stream()
                .filter(exception ->
                    exception.getType()
                        == ScheduleException.ExceptionType.CUSTOM_HOURS
                )
                .map(exception ->
                    new TimeRange(
                        exception.getStartTime(),
                        exception.getEndTime()
                    )
                )
                .sorted(
                    Comparator.comparing(TimeRange::start)
                )
                .toList();

        customHours.forEach(this::validateTimeRange);

        if (!customHours.isEmpty()) {
            return customHours;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<ProfessionalAvailability> availabilities =
            availabilityRepository
                .findAllByProfessionalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                    professionalId
                );

        validateAvailabilitiesBelongToProfessional(
            availabilities,
            professionalId
        );

        List<TimeRange> weeklyRanges =
            availabilities.stream()
                .filter(availability ->
                    availability.getDayOfWeek() == dayOfWeek
                )
                .map(availability ->
                    new TimeRange(
                        availability.getStartTime(),
                        availability.getEndTime()
                    )
                )
                .sorted(
                    Comparator.comparing(TimeRange::start)
                )
                .toList();

        weeklyRanges.forEach(this::validateTimeRange);

        return weeklyRanges;
    }

    // =========================================================
    // SLOT GENERATION
    // =========================================================

    /**
     * Genera horarios a partir de franjas laborales.
     *
     * La rejilla empieza siempre en el comienzo
     * de cada franja.
     *
     * bufferBefore no desplaza esa rejilla.
     *
     * Se comprueba que la ocupacion total
     * quede dentro del horario laboral.
     *
     * No se generan slots duplicados.
     */
    private List<AvailabilitySlotResponse> generateSlots(
        LocalDate date,
        ZoneId businessZone,
        int durationMinutes,
        List<TimeRange> workingRanges,
        List<Appointment> existingAppointments,
        BusinessSettings settings
    ) {

        if (date == null) {
            throw new IllegalArgumentException(
                "La fecha es obligatoria."
            );
        }

        if (businessZone == null) {
            throw new IllegalArgumentException(
                "La zona horaria es obligatoria."
            );
        }

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                "La duración del servicio debe ser mayor que 0."
            );
        }

        if (settings == null) {
            throw new IllegalArgumentException(
                "BusinessSettings es obligatorio."
            );
        }

        if (workingRanges == null) {
            throw new IllegalStateException(
                "Las franjas laborales no pueden ser nulas."
            );
        }

        if (existingAppointments == null) {
            throw new IllegalStateException(
                "La colección de citas no puede ser nula."
            );
        }

        int slotIntervalMinutes =
            settings.getSlotIntervalMinutes();

        int bufferBeforeMinutes =
            settings.getDefaultBufferBeforeMinutes();

        int bufferAfterMinutes =
            settings.getDefaultBufferAfterMinutes();

        int minimumBookingNoticeMinutes =
            settings.getMinimumBookingNoticeMinutes();

        if (slotIntervalMinutes <= 0) {
            throw new IllegalStateException(
                "slotIntervalMinutes debe ser mayor que 0."
            );
        }

        if (
            bufferBeforeMinutes < 0
                || bufferAfterMinutes < 0
        ) {
            throw new IllegalStateException(
                "Los buffers no pueden ser negativos."
            );
        }

        if (minimumBookingNoticeMinutes < 0) {
            throw new IllegalStateException(
                "minimumBookingNoticeMinutes no puede ser negativo."
            );
        }

        ZonedDateTime now =
            ZonedDateTime.now(businessZone);

        ZonedDateTime earliestBookingStart =
            now.plusMinutes(
                minimumBookingNoticeMinutes
            );

        List<AvailabilitySlotResponse> slots =
            new ArrayList<>();

        Set<OffsetDateTime> generatedStarts =
            new HashSet<>();

        for (TimeRange workingRange : workingRanges) {

            validateTimeRange(workingRange);

            /*
             * Trabajamos con tiempos locales para que
             * la rejilla siga los minutos de apertura.
             */
            LocalDateTime workingStartLocal =
                LocalDateTime.of(
                    date,
                    workingRange.start()
                );

            LocalDateTime workingEndLocal =
                LocalDateTime.of(
                    date,
                    workingRange.end()
                );

            if (
                !workingEndLocal.isAfter(
                    workingStartLocal
                )
            ) {
                throw new ConflictException(
                    "La franja de disponibilidad no es válida."
                );
            }

            LocalDateTime currentStartLocal =
                workingStartLocal;

            while (
                currentStartLocal.isBefore(
                    workingEndLocal
                )
            ) {

                LocalDateTime currentEndLocal =
                    currentStartLocal.plusMinutes(
                        durationMinutes
                    );

                LocalDateTime occupiedStartLocal =
                    currentStartLocal.minusMinutes(
                        bufferBeforeMinutes
                    );

                LocalDateTime occupiedEndLocal =
                    currentEndLocal.plusMinutes(
                        bufferAfterMinutes
                    );

                /*
                 * La ocupacion debe caber dentro
                 * de la franja laboral.
                 */
                if (
                    currentEndLocal.isAfter(workingEndLocal)
                        || occupiedEndLocal.isAfter(
                            workingEndLocal
                        )
                ) {
                    break;
                }

                boolean bufferBeforeOutsideWorkingRange =
                    occupiedStartLocal.isBefore(
                        workingStartLocal
                    );

                if (!bufferBeforeOutsideWorkingRange) {

                    /*
                     * No resolvemos automaticamente horas
                     * inexistentes o ambiguas.
                     *
                     * Evitamos ofrecer slots ambiguos
                     * durante cambios de hora.
                     */
                    if (
                        isUnambiguousLocalTime(
                            currentStartLocal,
                            businessZone
                        )
                            && isUnambiguousLocalTime(
                                currentEndLocal,
                                businessZone
                            )
                            && isUnambiguousLocalTime(
                                occupiedStartLocal,
                                businessZone
                            )
                            && isUnambiguousLocalTime(
                                occupiedEndLocal,
                                businessZone
                            )
                            && !crossesTimezoneTransition(
                                occupiedStartLocal,
                                occupiedEndLocal,
                                businessZone
                            )
                    ) {

                        ZonedDateTime currentStart =
                            resolveZonedDateTime(
                                currentStartLocal,
                                businessZone
                            );

                        ZonedDateTime currentEnd =
                            resolveZonedDateTime(
                                currentEndLocal,
                                businessZone
                            );

                        if (
                            currentEnd.isAfter(currentStart)
                                && !currentStart.isBefore(
                                    earliestBookingStart
                                )
                        ) {

                            OffsetDateTime occupiedStart =
                                resolveZonedDateTime(
                                    occupiedStartLocal,
                                    businessZone
                                ).toOffsetDateTime();

                            OffsetDateTime occupiedEnd =
                                resolveZonedDateTime(
                                    occupiedEndLocal,
                                    businessZone
                                ).toOffsetDateTime();

                            boolean occupied =
                                existingAppointments.stream()
                                    .anyMatch(appointment ->
                                        overlapsAppointmentWithBuffers(
                                            occupiedStart,
                                            occupiedEnd,
                                            appointment,
                                            bufferBeforeMinutes,
                                            bufferAfterMinutes
                                        )
                                    );

                            OffsetDateTime slotStart =
                                currentStart.toOffsetDateTime();

                            OffsetDateTime slotEnd =
                                currentEnd.toOffsetDateTime();

                            if (
                                !occupied
                                    && generatedStarts.add(
                                        slotStart
                                    )
                            ) {
                                slots.add(
                                    new AvailabilitySlotResponse(
                                        currentStart.toLocalTime(),
                                        currentEnd.toLocalTime(),
                                        slotStart,
                                        slotEnd
                                    )
                                );
                            }
                        }
                    }
                }

                currentStartLocal =
                    currentStartLocal.plusMinutes(
                        slotIntervalMinutes
                    );
            }
        }

        /*
         * Orden cronologico estable.
         */
        slots.sort(
            Comparator.comparing(
                AvailabilitySlotResponse::startAt
            )
        );

        return List.copyOf(slots);
    }

    // =========================================================
    // APPOINTMENT OVERLAP WITH BUFFERS
    // =========================================================

    /**
     * Compara intervalos de ocupacion semiabiertos.
     *
     * Cada cita tiene:
     *
     * startAt - bufferBefore
     *
     * endAt + bufferAfter
     *
     * Si dos intervalos se tocan exactamente
     * en sus extremos, no se solapan.
     */
    private boolean overlapsAppointmentWithBuffers(
        OffsetDateTime candidateOccupiedStart,
        OffsetDateTime candidateOccupiedEnd,
        Appointment appointment,
        int bufferBeforeMinutes,
        int bufferAfterMinutes
    ) {

        if (appointment == null) {
            throw new IllegalStateException(
                "La colección de citas contiene una cita nula."
            );
        }

        if (
            appointment.getStartAt() == null
                || appointment.getEndAt() == null
                || !appointment.getEndAt().isAfter(
                    appointment.getStartAt()
                )
        ) {
            throw new IllegalStateException(
                "Existe una cita con un intervalo "
                    + "temporal inválido."
            );
        }

        OffsetDateTime appointmentOccupiedStart =
            appointment.getStartAt()
                .minusMinutes(bufferBeforeMinutes);

        OffsetDateTime appointmentOccupiedEnd =
            appointment.getEndAt()
                .plusMinutes(bufferAfterMinutes);

        return overlaps(
            candidateOccupiedStart,
            candidateOccupiedEnd,
            appointmentOccupiedStart,
            appointmentOccupiedEnd
        );
    }

    // =========================================================
    // TIMEZONE UTILITIES
    // =========================================================

    /**
     * Una hora local debe corresponder exactamente
     * a un offset de la zona horaria.
     *
     * 0 offsets: hora inexistente.
     * 1 offset: hora univoca.
     * 2 offsets: hora ambigua.
     */
    private boolean isUnambiguousLocalTime(
        LocalDateTime localDateTime,
        ZoneId businessZone
    ) {

        ZoneRules rules = businessZone.getRules();

        return rules.getValidOffsets(
            localDateTime
        ).size() == 1;
    }

    /**
     * Excluye intervalos de ocupacion que atraviesan
     * una transicion de offset.
     *
     * De este modo no mezclamos duraciones reales
     * con duraciones locales durante el cambio
     * de horario de verano/invierno.
     */
    private boolean crossesTimezoneTransition(
        LocalDateTime start,
        LocalDateTime end,
        ZoneId businessZone
    ) {

        ZonedDateTime startZoned =
            resolveZonedDateTime(
                start,
                businessZone
            );

        ZonedDateTime endZoned =
            resolveZonedDateTime(
                end,
                businessZone
            );

        return !startZoned.getOffset().equals(
            endZoned.getOffset()
        );
    }

    private ZonedDateTime resolveZonedDateTime(
        LocalDateTime localDateTime,
        ZoneId businessZone
    ) {

        if (
            localDateTime == null
                || businessZone == null
        ) {
            throw new IllegalArgumentException(
                "Fecha, hora y zona horaria son obligatorias."
            );
        }

        if (
            !isUnambiguousLocalTime(
                localDateTime,
                businessZone
            )
        ) {
            throw new ConflictException(
                "El horario solicitado coincide con "
                    + "un cambio de hora y no puede "
                    + "reservarse de forma inequívoca."
            );
        }

        try {
            return localDateTime.atZone(businessZone);
        } catch (DateTimeException exception) {
            throw new ConflictException(
                "No se ha podido resolver el horario "
                    + "en la zona horaria del negocio."
            );
        }
    }

    // =========================================================
    // OVERLAP
    // =========================================================

    /**
     * Intervalos semiabiertos [start, end).
     */
    private boolean overlaps(
        OffsetDateTime startA,
        OffsetDateTime endA,
        OffsetDateTime startB,
        OffsetDateTime endB
    ) {

        if (
            startA == null
                || endA == null
                || startB == null
                || endB == null
        ) {
            throw new IllegalStateException(
                "No se pueden comparar intervalos nulos."
            );
        }

        if (
            !endA.isAfter(startA)
                || !endB.isAfter(startB)
        ) {
            throw new IllegalStateException(
                "No se pueden comparar intervalos inválidos."
            );
        }

        return startA.isBefore(endB)
            && endA.isAfter(startB);
    }

    // =========================================================
    // APPOINTMENT CONTEXT VALIDATION
    // =========================================================

    private void validateAppointmentsBelongToContext(
        List<Appointment> appointments,
        Long expectedBusinessId,
        Long expectedProfessionalId
    ) {

        if (appointments == null) {
            throw new IllegalStateException(
                "La consulta de citas ha devuelto "
                    + "una colección nula."
            );
        }

        for (Appointment appointment : appointments) {

            if (appointment == null) {
                throw new IllegalStateException(
                    "La consulta de citas ha devuelto una cita nula."
                );
            }

            if (
                appointment.getBusiness() == null
                    || appointment.getBusiness().getId() == null
            ) {
                throw new IllegalStateException(
                    "Una cita no tiene un negocio válido asociado."
                );
            }

            if (
                !Objects.equals(
                    appointment.getBusiness().getId(),
                    expectedBusinessId
                )
            ) {
                throw new IllegalStateException(
                    "La disponibilidad ha recuperado "
                        + "una cita perteneciente a otro negocio."
                );
            }

            if (
                appointment.getProfessional() == null
                    || appointment.getProfessional().getId() == null
            ) {
                throw new IllegalStateException(
                    "Una cita no tiene un profesional válido asociado."
                );
            }

            if (
                !Objects.equals(
                    appointment.getProfessional().getId(),
                    expectedProfessionalId
                )
            ) {
                throw new IllegalStateException(
                    "La disponibilidad ha recuperado "
                        + "una cita perteneciente a otro profesional."
                );
            }

            if (appointment.getStatus() == null) {
                throw new IllegalStateException(
                    "Una cita existente no tiene estado."
                );
            }

            if (
                appointment.getStatus()
                    != Appointment.AppointmentStatus.CANCELLED
                    && (
                        appointment.getStartAt() == null
                            || appointment.getEndAt() == null
                            || !appointment.getEndAt().isAfter(
                                appointment.getStartAt()
                            )
                    )
            ) {
                throw new IllegalStateException(
                    "Una cita existente tiene un intervalo inválido."
                );
            }
        }
    }

    // =========================================================
    // WEEKLY AVAILABILITY CONTEXT VALIDATION
    // =========================================================

    private void validateAvailabilitiesBelongToProfessional(
        List<ProfessionalAvailability> availabilities,
        Long expectedProfessionalId
    ) {

        if (availabilities == null) {
            throw new IllegalStateException(
                "La consulta de horarios ha devuelto null."
            );
        }

        for (
            ProfessionalAvailability availability :
            availabilities
        ) {

            if (availability == null) {
                throw new IllegalStateException(
                    "Se ha recuperado una disponibilidad nula."
                );
            }

            if (
                availability.getProfessional() == null
                    || availability.getProfessional().getId() == null
            ) {
                throw new IllegalStateException(
                    "Una disponibilidad no tiene "
                        + "un profesional válido."
                );
            }

            if (
                !Objects.equals(
                    availability.getProfessional().getId(),
                    expectedProfessionalId
                )
            ) {
                throw new IllegalStateException(
                    "Se ha recuperado disponibilidad "
                        + "de otro profesional."
                );
            }
        }
    }

    // =========================================================
    // SCHEDULE EXCEPTION CONTEXT VALIDATION
    // =========================================================

    private void validateScheduleExceptionsBelongToProfessional(
        List<ScheduleException> exceptions,
        Long expectedProfessionalId
    ) {

        if (exceptions == null) {
            throw new IllegalStateException(
                "La consulta de excepciones ha devuelto null."
            );
        }

        for (ScheduleException exception : exceptions) {

            if (exception == null) {
                throw new IllegalStateException(
                    "Se ha recuperado una excepción de horario nula."
                );
            }

            if (
                exception.getProfessional() == null
                    || exception.getProfessional().getId() == null
            ) {
                throw new IllegalStateException(
                    "Una excepción de horario no tiene "
                        + "un profesional válido."
                );
            }

            if (
                !Objects.equals(
                    exception.getProfessional().getId(),
                    expectedProfessionalId
                )
            ) {
                throw new IllegalStateException(
                    "Se ha recuperado una excepción de horario "
                        + "de otro profesional."
                );
            }
        }
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private DailyAvailabilityResponse buildResponse(
        Professional professional,
        Service service,
        LocalDate date,
        ZoneId businessZone,
        List<AvailabilitySlotResponse> slots
    ) {

        Business business =
            requireProfessionalBusiness(professional);

        Business serviceBusiness =
            requireServiceBusiness(service);

        if (
            !Objects.equals(
                business.getId(),
                serviceBusiness.getId()
            )
        ) {
            throw new IllegalStateException(
                "No se puede construir disponibilidad "
                    + "con profesional y servicio "
                    + "de negocios diferentes."
            );
        }

        List<AvailabilitySlotResponse> safeSlots =
            slots == null
                ? List.of()
                : List.copyOf(slots);

        return new DailyAvailabilityResponse(
            business.getId(),
            professional.getId(),
            professional.getDisplayName(),
            service.getId(),
            service.getName(),
            service.getDurationMinutes(),
            date,
            businessZone.getId(),
            !safeSlots.isEmpty(),
            safeSlots.size(),
            safeSlots
        );
    }

    // =========================================================
    // BUSINESS TIMEZONE
    // =========================================================

    private ZoneId resolveBusinessZone(
        Business business
    ) {

        if (
            business == null
                || business.getId() == null
        ) {
            throw new ConflictException(
                "El negocio no es válido."
            );
        }

        String timezone = business.getTimezone();

        if (
            timezone == null
                || timezone.isBlank()
        ) {
            throw new ConflictException(
                "El negocio "
                    + business.getId()
                    + " no tiene una zona horaria configurada."
            );
        }

        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new ConflictException(
                "La zona horaria del negocio no es válida."
            );
        }
    }

    // =========================================================
    // TIME RANGE VALIDATION
    // =========================================================

    private void validateTimeRange(
        TimeRange range
    ) {

        if (
            range == null
                || range.start() == null
                || range.end() == null
        ) {
            throw new ConflictException(
                "Existe una franja horaria no válida."
            );
        }

        /*
         * No admitimos franjas nocturnas
         * que crucen al dia siguiente.
         */
        if (
            !range.end().isAfter(range.start())
        ) {
            throw new ConflictException(
                "Existe una franja horaria cuyo final "
                    + "no es posterior al inicio."
            );
        }
    }

    // =========================================================
    // INPUT VALIDATION
    // =========================================================

    private void validateProfessionalId(
        Long professionalId
    ) {

        if (professionalId == null) {
            throw new IllegalArgumentException(
                "professionalId es obligatorio."
            );
        }

        if (professionalId <= 0) {
            throw new IllegalArgumentException(
                "professionalId debe ser mayor que 0."
            );
        }
    }

    private void validateServiceId(
        Long serviceId
    ) {

        if (serviceId == null) {
            throw new IllegalArgumentException(
                "serviceId es obligatorio."
            );
        }

        if (serviceId <= 0) {
            throw new IllegalArgumentException(
                "serviceId debe ser mayor que 0."
            );
        }
    }

    private void validateDate(
        LocalDate date
    ) {

        if (date == null) {
            throw new IllegalArgumentException(
                "La fecha es obligatoria."
            );
        }
    }

    // =========================================================
    // INTERNAL TYPES
    // =========================================================

    private record TimeRange(
        LocalTime start,
        LocalTime end
    ) {
    }
}
