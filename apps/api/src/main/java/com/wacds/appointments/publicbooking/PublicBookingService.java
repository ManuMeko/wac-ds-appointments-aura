
package com.wacds.appointments.publicbooking;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.availabilityslot.AvailabilitySlotService;
import com.wacds.appointments.availabilityslot.dto.AvailabilitySlotResponse;
import com.wacds.appointments.availabilityslot.dto.DailyAvailabilityResponse;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.business.settings.BusinessSettingsRepository;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.customer.CustomerService;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentId;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentRepository;
import com.wacds.appointments.publicbooking.dto.PublicBookingRequest;
import com.wacds.appointments.publicbooking.dto.PublicBookingResponse;
import com.wacds.appointments.publicbooking.management.AppointmentManagementTokenService;
import com.wacds.appointments.reminder.AppointmentReminderService;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * WAC D.S. - Modelo 01 - Appointments.
 *
 * Servicio de creacion de reservas publicas.
 *
 * SEGURIDAD:
 * - Validacion de negocio y pertenencia multitenant.
 * - Validacion de servicio y profesional.
 * - Validacion de asignacion profesional-servicio.
 * - Disponibilidad recalculada dentro de la transaccion.
 * - Bloqueo pesimista de agenda por profesional.
 * - Comprobacion final de solapamientos.
 * - Token de gestion generado por servicio especializado.
 *
 * CONCURRENCIA:
 * Todas las operaciones que escriban sobre la agenda
 * deben utilizar el mismo bloqueo de profesional.
 *
 * El bloqueo se mantiene hasta COMMIT o ROLLBACK.
 *
 * IMPORTANTE:
 * La integridad concurrente depende tambien de que
 * otras rutas de creacion y reprogramacion respeten
 * el mismo protocolo de bloqueo.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PublicBookingService {

    private static final String BOOKING_CREATED_MESSAGE =
        "Reserva creada correctamente.";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessRepository businessRepository;

    private final BusinessSettingsRepository businessSettingsRepository;

    private final ProfessionalRepository professionalRepository;

    private final ServiceRepository serviceRepository;

    private final ProfessionalServiceAssignmentRepository
        assignmentRepository;

    private final AppointmentRepository appointmentRepository;

    private final AvailabilitySlotService availabilitySlotService;

    private final CustomerService customerService;

    private final AppointmentReminderService appointmentReminderService;

    private final AppointmentManagementTokenService
        appointmentManagementTokenService;

    private final ApplicationEventPublisher applicationEventPublisher;

    // =========================================================
    // CREATE PUBLIC BOOKING
    // =========================================================

    /**
     * Crea una reserva dentro de una unica transaccion.
     *
     * La agenda del profesional se bloquea antes
     * de recalcular disponibilidad y solapamientos.
     */
    @Transactional
    public PublicBookingResponse createBooking(
        PublicBookingRequest request
    ) {

        validateRequest(request);

        Long businessId = request.businessId();
        Long professionalId = request.professionalId();
        Long serviceId = request.serviceId();

        // -----------------------------------------------------
        // BUSINESS
        // -----------------------------------------------------

        Business business = findBusiness(businessId);

        validateBusiness(business);

        ZoneId businessZone = resolveBusinessZone(business);

        // -----------------------------------------------------
        // EXCLUSIVE PROFESSIONAL SCHEDULE LOCK
        // -----------------------------------------------------

        /*
         * Todas las operaciones de escritura sobre la agenda
         * deben coordinarse utilizando este mismo bloqueo.
         *
         * PostgreSQL mantiene FOR UPDATE durante toda
         * la transaccion.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        /*
         * Revalidar el negocio despues de adquirir el bloqueo.
         *
         * Conservamos la instancia gestionada y comprobamos
         * nuevamente las condiciones operativas.
         */
        validateBusiness(business);

        // -----------------------------------------------------
        // SETTINGS
        // -----------------------------------------------------

        BusinessSettings settings =
            requireBusinessSettings(businessId);

        // -----------------------------------------------------
        // PROFESSIONAL
        // -----------------------------------------------------

        Professional professional =
            findProfessional(professionalId);

        validateProfessional(professional);

        // -----------------------------------------------------
        // SERVICE
        // -----------------------------------------------------

        Service service = findService(serviceId);

        validateService(service);

        // -----------------------------------------------------
        // MULTITENANCY
        // -----------------------------------------------------

        validateTenantRelations(
            business,
            professional,
            service
        );

        // -----------------------------------------------------
        // SERVICE ASSIGNMENT
        // -----------------------------------------------------

        validateProfessionalProvidesService(
            professionalId,
            serviceId
        );

        // -----------------------------------------------------
        // REQUESTED TIME
        // -----------------------------------------------------

        OffsetDateTime requestedStartAt = request.startAt();

        validateFutureStart(requestedStartAt);

        validateBookingWindow(
            requestedStartAt,
            businessZone,
            settings
        );

        LocalDate businessLocalDate = requestedStartAt
            .atZoneSameInstant(businessZone)
            .toLocalDate();

        // -----------------------------------------------------
        // AVAILABILITY UNDER LOCK
        // -----------------------------------------------------

        DailyAvailabilityResponse availability =
            availabilitySlotService.getDailyAvailability(
                professionalId,
                serviceId,
                businessLocalDate
            );

        validateAvailabilityBelongsToBusiness(
            availability,
            businessId
        );

        AvailabilitySlotResponse selectedSlot =
            findRequestedSlot(
                availability,
                requestedStartAt
            );

        OffsetDateTime startAt = selectedSlot.startAt();
        OffsetDateTime endAt = selectedSlot.endAt();

        validateSlotDuration(
            service,
            startAt,
            endAt
        );

        /*
         * Comprobamos nuevamente que el inicio sigue
         * siendo futuro y respeta la ventana operativa.
         */
        validateFutureStart(startAt);

        validateBookingWindow(
            startAt,
            businessZone,
            settings
        );

        // -----------------------------------------------------
        // FINAL OVERLAP VALIDATION
        // -----------------------------------------------------

        /*
         * Segunda barrera de seguridad:
         *
         * - Se ejecuta bajo el bloqueo del profesional.
         * - Incluye las citas pendientes y confirmadas.
         * - Excluye las citas canceladas.
         * - Considera los buffers operativos.
         */
        validateNoFinalOverlap(
            businessId,
            professionalId,
            startAt,
            endAt,
            settings
        );

        // -----------------------------------------------------
        // CUSTOMER
        // -----------------------------------------------------

        Customer customer =
            customerService.findOrCreateForPublicBooking(
                businessId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone()
            );

        validateCustomerBelongsToBusiness(
            customer,
            businessId
        );

        // -----------------------------------------------------
        // INITIAL STATUS
        // -----------------------------------------------------

        Appointment.AppointmentStatus initialStatus =
            settings.isAutoConfirmAppointments()
                ? Appointment.AppointmentStatus.CONFIRMED
                : Appointment.AppointmentStatus.PENDING;

        // -----------------------------------------------------
        // APPOINTMENT
        // -----------------------------------------------------

        Appointment appointment = Appointment.builder()
            .business(business)
            .customer(customer)
            .professional(professional)
            .service(service)
            .startAt(startAt)
            .endAt(endAt)
            .status(initialStatus)
            .notes(normalizeNullableText(request.notes()))
            .build();

        Appointment saved =
            appointmentRepository.saveAndFlush(appointment);

        validatePersistedAppointment(saved);

        // -----------------------------------------------------
        // REMINDERS
        // -----------------------------------------------------

        /*
         * Sincroniza la programacion persistente.
         * El envio externo no debe ejecutarse aqui.
         */
        appointmentReminderService
            .synchronizeForAppointment(saved);

        // -----------------------------------------------------
        // MANAGEMENT TOKEN
        // -----------------------------------------------------

        /*
         * El servicio especializado almacena el hash.
         * El token en claro se entrega al crear la reserva.
         */
        String managementToken =
            appointmentManagementTokenService.issueToken(saved);

        validateManagementToken(managementToken);

        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        PublicBookingResponse response = buildResponse(
            saved,
            businessZone,
            managementToken
        );

        // -----------------------------------------------------
        // AFTER-COMMIT EMAIL EVENT
        // -----------------------------------------------------

        /*
         * Se publica dentro de la transaccion.
         * El listener procesa el evento despues del COMMIT.
         */
        publishBookingCreatedEvent(
            saved,
            businessZone,
            managementToken
        );

        return response;
    }

    // =========================================================
    // BOOKING CREATED EVENT
    // =========================================================

    /**
     * Publica un evento interno para la confirmacion
     * por correo electronico.
     *
     * No envia emails directamente.
     *
     * El token es secreto y no debe incluirse en logs.
     */
    private void publishBookingCreatedEvent(
        Appointment appointment,
        ZoneId businessZone,
        String managementToken
    ) {

        validatePersistedAppointment(appointment);
        validateManagementToken(managementToken);

        if (businessZone == null) {
            throw new IllegalStateException(
                "No existe una zona horaria válida "
                    + "para publicar el evento de reserva."
            );
        }

        Customer customer = appointment.getCustomer();

        /*
         * Las reservas pueden utilizar email,
         * telefono o ambos.
         *
         * Si no hay email, no publicamos este evento.
         */
        if (
            customer.getEmail() == null
                || customer.getEmail().isBlank()
        ) {
            return;
        }

        OffsetDateTime eventStartAt =
            appointment.getStartAt()
                .atZoneSameInstant(businessZone)
                .toOffsetDateTime();

        OffsetDateTime eventEndAt =
            appointment.getEndAt()
                .atZoneSameInstant(businessZone)
                .toOffsetDateTime();

        applicationEventPublisher.publishEvent(
            new PublicBookingCreatedEvent(
                appointment.getId(),
                appointment.getBusiness().getId(),
                appointment.getBusiness().getName(),
                customer.getId(),
                customer.getFullName(),
                customer.getEmail().trim(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getProfessional().getId(),
                appointment.getProfessional().getDisplayName(),
                eventStartAt,
                eventEndAt,
                businessZone.getId(),
                managementToken.trim()
            )
        );
    }

    // =========================================================
    // PROFESSIONAL SCHEDULE LOCK
    // =========================================================

    /**
     * Bloquea la fila del profesional en PostgreSQL.
     *
     * El repositorio debe usar SELECT FOR UPDATE.
     *
     * El bloqueo se mantiene mientras continue
     * abierta la transaccion de createBooking().
     */
    private void lockProfessionalSchedule(
        Long businessId,
        Long professionalId
    ) {

        validatePositiveId(businessId, "businessId");
        validatePositiveId(professionalId, "professionalId");

        Long lockedProfessionalId =
            appointmentRepository.lockProfessionalSchedule(
                businessId,
                professionalId
            ).orElseThrow(
                () -> new ResourceNotFoundException(
                    "No existe el profesional "
                        + professionalId
                        + " dentro del negocio "
                        + businessId
                        + "."
                )
            );

        if (
            !Objects.equals(
                lockedProfessionalId,
                professionalId
            )
        ) {
            throw new IllegalStateException(
                "El bloqueo de agenda devolvió "
                    + "un profesional inesperado."
            );
        }
    }

    // =========================================================
    // BUSINESS SETTINGS
    // =========================================================

    private BusinessSettings requireBusinessSettings(
        Long businessId
    ) {

        BusinessSettings settings =
            businessSettingsRepository.findByBusinessId(
                businessId
            ).orElseThrow(
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
                "La configuración operativa del negocio no es válida."
            );
        }

        if (
            settings.getBusiness() == null
                || settings.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "BusinessSettings no tiene un negocio válido asociado."
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
        ) {
            throw new IllegalStateException(
                "minimumBookingNoticeMinutes no es válido."
            );
        }

        if (settings.getBookingHorizonDays() < 1) {
            throw new IllegalStateException(
                "bookingHorizonDays no es válido."
            );
        }

        if (settings.getSlotIntervalMinutes() <= 0) {
            throw new IllegalStateException(
                "slotIntervalMinutes no es válido."
            );
        }

        if (
            settings.getDefaultBufferBeforeMinutes() < 0
                || settings.getDefaultBufferAfterMinutes() < 0
        ) {
            throw new IllegalStateException(
                "Los buffers operativos no son válidos."
            );
        }

        if (
            settings.getMinimumCancellationNoticeMinutes() < 0
        ) {
            throw new IllegalStateException(
                "minimumCancellationNoticeMinutes no es válido."
            );
        }

        if (
            settings.getMinimumRescheduleNoticeMinutes() < 0
        ) {
            throw new IllegalStateException(
                "minimumRescheduleNoticeMinutes no es válido."
            );
        }

        if (
            settings.getFirstReminderMinutesBefore() <= 0
        ) {
            throw new IllegalStateException(
                "firstReminderMinutesBefore no es válido."
            );
        }

        if (
            settings.getSecondReminderMinutesBefore() <= 0
        ) {
            throw new IllegalStateException(
                "secondReminderMinutesBefore no es válido."
            );
        }

        if (
            settings.isSecondReminderEnabled()
                && !settings.isRemindersEnabled()
        ) {
            throw new IllegalStateException(
                "El segundo recordatorio no puede estar activo "
                    + "si los recordatorios generales están deshabilitados."
            );
        }

        if (
            settings.isSecondReminderEnabled()
                && settings.getSecondReminderMinutesBefore()
                    >= settings.getFirstReminderMinutesBefore()
        ) {
            throw new IllegalStateException(
                "El segundo recordatorio debe estar más cerca "
                    + "de la cita que el primero."
            );
        }
    }

    // =========================================================
    // BOOKING WINDOW
    // =========================================================

    private void validateBookingWindow(
        OffsetDateTime requestedStartAt,
        ZoneId businessZone,
        BusinessSettings settings
    ) {

        if (
            requestedStartAt == null
                || businessZone == null
                || settings == null
        ) {
            throw new IllegalArgumentException(
                "No se puede validar la ventana de reserva."
            );
        }

        ZonedDateTime now =
            ZonedDateTime.now(businessZone);

        ZonedDateTime requestedStart =
            requestedStartAt.atZoneSameInstant(businessZone);

        ZonedDateTime earliestAllowedStart =
            now.plusMinutes(
                settings.getMinimumBookingNoticeMinutes()
            );

        if (
            requestedStart.isBefore(
                earliestAllowedStart
            )
        ) {
            throw new ConflictException(
                "La reserva requiere una antelación mínima de "
                    + settings.getMinimumBookingNoticeMinutes()
                    + " minutos."
            );
        }

        LocalDate latestAllowedDate =
            now.toLocalDate()
                .plusDays(
                    settings.getBookingHorizonDays()
                );

        if (
            requestedStart.toLocalDate()
                .isAfter(latestAllowedDate)
        ) {
            throw new ConflictException(
                "La fecha solicitada supera el horizonte máximo "
                    + "de reserva de "
                    + settings.getBookingHorizonDays()
                    + " días."
            );
        }
    }

    // =========================================================
    // FINAL OVERLAP VALIDATION
    // =========================================================

    /**
     * Comprueba posibles ocupaciones bajo el bloqueo
     * exclusivo del profesional.
     *
     * Incluye buffers operativos.
     *
     * No ignora citas con intervalos invalidos:
     * ante datos inconsistentes se rechaza la reserva.
     */
    private void validateNoFinalOverlap(
        Long businessId,
        Long professionalId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        BusinessSettings settings
    ) {

        validatePositiveId(businessId, "businessId");
        validatePositiveId(professionalId, "professionalId");

        if (
            startAt == null
                || endAt == null
                || !endAt.isAfter(startAt)
        ) {
            throw new IllegalArgumentException(
                "El intervalo de la reserva no es válido."
            );
        }

        if (settings == null) {
            throw new IllegalArgumentException(
                "BusinessSettings es obligatorio "
                    + "para comprobar solapamientos."
            );
        }

        int bufferBeforeMinutes =
            settings.getDefaultBufferBeforeMinutes();

        int bufferAfterMinutes =
            settings.getDefaultBufferAfterMinutes();

        if (
            bufferBeforeMinutes < 0
                || bufferAfterMinutes < 0
        ) {
            throw new IllegalStateException(
                "Los buffers de agenda no son válidos."
            );
        }

        OffsetDateTime candidateOccupiedStart =
            startAt.minusMinutes(bufferBeforeMinutes);

        OffsetDateTime candidateOccupiedEnd =
            endAt.plusMinutes(bufferAfterMinutes);

        List<Appointment> existingAppointments =
            appointmentRepository
                .findAllByBusinessIdAndProfessionalIdOrderByStartAtDesc(
                    businessId,
                    professionalId
                );

        validateAppointmentContext(
            existingAppointments,
            businessId,
            professionalId
        );

        if (
            existingAppointments == null
                || existingAppointments.isEmpty()
        ) {
            return;
        }

        for (Appointment existing : existingAppointments) {

            if (
                existing.getStatus()
                    == Appointment.AppointmentStatus.CANCELLED
            ) {
                continue;
            }

            if (existing.getStatus() == null) {
                throw new IllegalStateException(
                    "Una cita existente no tiene un estado válido."
                );
            }

            OffsetDateTime existingStart =
                existing.getStartAt();

            OffsetDateTime existingEnd =
                existing.getEndAt();

            if (
                existingStart == null
                    || existingEnd == null
                    || !existingEnd.isAfter(existingStart)
            ) {
                throw new IllegalStateException(
                    "Una cita existente contiene "
                        + "un intervalo temporal inválido."
                );
            }

            OffsetDateTime existingOccupiedStart =
                existingStart.minusMinutes(
                    bufferBeforeMinutes
                );

            OffsetDateTime existingOccupiedEnd =
                existingEnd.plusMinutes(
                    bufferAfterMinutes
                );

            if (
                overlaps(
                    candidateOccupiedStart,
                    candidateOccupiedEnd,
                    existingOccupiedStart,
                    existingOccupiedEnd
                )
            ) {
                throw new ConflictException(
                    "El horario seleccionado ya no está disponible. "
                        + "Selecciona otro horario."
                );
            }
        }
    }

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
            throw new IllegalArgumentException(
                "No se pueden comparar intervalos nulos."
            );
        }

        if (
            !endA.isAfter(startA)
                || !endB.isAfter(startB)
        ) {
            throw new IllegalArgumentException(
                "No se pueden comparar intervalos inválidos."
            );
        }

        return startA.isBefore(endB)
            && endA.isAfter(startB);
    }

    private void validateAppointmentContext(
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
                    "Se ha recuperado una cita perteneciente "
                        + "a otro negocio."
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
                    "Se ha recuperado una cita perteneciente "
                        + "a otro profesional."
                );
            }
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        PublicBookingRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de reserva es obligatoria."
            );
        }

        validatePositiveId(
            request.businessId(),
            "businessId"
        );

        validatePositiveId(
            request.professionalId(),
            "professionalId"
        );

        validatePositiveId(
            request.serviceId(),
            "serviceId"
        );

        if (request.startAt() == null) {
            throw new IllegalArgumentException(
                "startAt es obligatorio."
            );
        }

        if (
            request.firstName() == null
                || request.firstName().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El nombre del cliente es obligatorio."
            );
        }

        if (
            isBlank(request.email())
                && isBlank(request.phone())
        ) {
            throw new IllegalArgumentException(
                "La reserva debe incluir al menos "
                    + "un email o un teléfono."
            );
        }
    }

    // =========================================================
    // BUSINESS
    // =========================================================

    private Business findBusiness(
        Long businessId
    ) {

        return businessRepository.findById(
            businessId
        ).orElseThrow(
            () -> new ResourceNotFoundException(
                "No existe ningún negocio con id: "
                    + businessId
            )
        );
    }

    private void validateBusiness(
        Business business
    ) {

        if (
            business == null
                || business.getId() == null
        ) {
            throw new IllegalStateException(
                "El negocio no es válido."
            );
        }

        if (!business.canAcceptPublicBookings()) {
            throw new ConflictException(
                "Las reservas públicas están deshabilitadas "
                    + "para este negocio."
            );
        }
    }

    // =========================================================
    // PROFESSIONAL
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

    private void validateProfessional(
        Professional professional
    ) {

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no es válido."
            );
        }

        if (!professional.isActive()) {
            throw new ConflictException(
                "El profesional no está activo."
            );
        }

        if (!professional.isBookingEnabled()) {
            throw new ConflictException(
                "El profesional no acepta reservas."
            );
        }

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no tiene un negocio válido asociado."
            );
        }
    }

    // =========================================================
    // SERVICE
    // =========================================================

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

    private void validateService(
        Service service
    ) {

        if (
            service == null
                || service.getId() == null
        ) {
            throw new IllegalStateException(
                "El servicio no es válido."
            );
        }

        if (!service.isActive()) {
            throw new ConflictException(
                "El servicio no está activo."
            );
        }

        if (!service.isBookingEnabled()) {
            throw new ConflictException(
                "El servicio no está disponible para reservas."
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

        if (
            service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El servicio no tiene un negocio válido asociado."
            );
        }
    }

    // =========================================================
    // MULTITENANCY
    // =========================================================

    private void validateTenantRelations(
        Business business,
        Professional professional,
        Service service
    ) {

        Long businessId = business.getId();

        Long professionalBusinessId =
            professional.getBusiness().getId();

        Long serviceBusinessId =
            service.getBusiness().getId();

        if (
            !Objects.equals(
                businessId,
                professionalBusinessId
            )
        ) {
            throw new ConflictException(
                "El profesional no pertenece al negocio solicitado."
            );
        }

        if (
            !Objects.equals(
                businessId,
                serviceBusinessId
            )
        ) {
            throw new ConflictException(
                "El servicio no pertenece al negocio solicitado."
            );
        }
    }

    // =========================================================
    // PROFESSIONAL-SERVICE ASSIGNMENT
    // =========================================================

    private void validateProfessionalProvidesService(
        Long professionalId,
        Long serviceId
    ) {

        ProfessionalServiceAssignmentId assignmentId =
            new ProfessionalServiceAssignmentId(
                professionalId,
                serviceId
            );

        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ConflictException(
                "El servicio "
                    + serviceId
                    + " no está asignado al profesional "
                    + professionalId
                    + "."
            );
        }
    }

    // =========================================================
    // DATE VALIDATION
    // =========================================================

    private void validateFutureStart(
        OffsetDateTime startAt
    ) {

        if (startAt == null) {
            throw new IllegalArgumentException(
                "La fecha y hora de la reserva es obligatoria."
            );
        }

        if (
            !startAt.isAfter(
                OffsetDateTime.now()
            )
        ) {
            throw new IllegalArgumentException(
                "La fecha y hora de la reserva "
                    + "debe estar en el futuro."
            );
        }
    }

    // =========================================================
    // AVAILABILITY
    // =========================================================

    private void validateAvailabilityBelongsToBusiness(
        DailyAvailabilityResponse availability,
        Long businessId
    ) {

        if (availability == null) {
            throw new ConflictException(
                "No se ha podido calcular la disponibilidad."
            );
        }

        if (availability.businessId() == null) {
            throw new IllegalStateException(
                "La disponibilidad calculada no contiene businessId."
            );
        }

        if (
            !Objects.equals(
                availability.businessId(),
                businessId
            )
        ) {
            throw new IllegalStateException(
                "La disponibilidad calculada no pertenece "
                    + "al negocio solicitado."
            );
        }
    }

    private AvailabilitySlotResponse findRequestedSlot(
        DailyAvailabilityResponse availability,
        OffsetDateTime requestedStartAt
    ) {

        if (availability == null) {
            throw new ConflictException(
                "No se ha podido calcular la disponibilidad."
            );
        }

        if (requestedStartAt == null) {
            throw new IllegalArgumentException(
                "La fecha de reserva solicitada es obligatoria."
            );
        }

        List<AvailabilitySlotResponse> slots =
            availability.slots();

        if (
            slots == null
                || slots.isEmpty()
        ) {
            throw new ConflictException(
                "No hay horarios disponibles "
                    + "para la fecha seleccionada."
            );
        }

        return slots.stream()
            .filter(Objects::nonNull)
            .filter(slot -> slot.startAt() != null)
            .filter(
                slot -> slot.startAt().isEqual(
                    requestedStartAt
                )
            )
            .findFirst()
            .orElseThrow(
                () -> new ConflictException(
                    "El horario seleccionado ya no está disponible."
                )
            );
    }

    private void validateSlotDuration(
        Service service,
        OffsetDateTime startAt,
        OffsetDateTime endAt
    ) {

        if (service == null) {
            throw new IllegalStateException(
                "El servicio seleccionado no es válido."
            );
        }

        if (
            service.getDurationMinutes() == null
                || service.getDurationMinutes() <= 0
        ) {
            throw new IllegalStateException(
                "El servicio no tiene una duración válida."
            );
        }

        if (
            startAt == null
                || endAt == null
        ) {
            throw new IllegalStateException(
                "El slot seleccionado no contiene "
                    + "un intervalo temporal válido."
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalStateException(
                "El final del slot debe ser posterior al inicio."
            );
        }

        OffsetDateTime expectedEndAt =
            startAt.plusMinutes(
                service.getDurationMinutes()
            );

        if (!expectedEndAt.isEqual(endAt)) {
            throw new IllegalStateException(
                "La duración del slot no coincide "
                    + "con la duración del servicio."
            );
        }
    }

    // =========================================================
    // CUSTOMER VALIDATION
    // =========================================================

    private void validateCustomerBelongsToBusiness(
        Customer customer,
        Long businessId
    ) {

        if (
            customer == null
                || customer.getId() == null
        ) {
            throw new IllegalStateException(
                "No se ha podido resolver un cliente válido."
            );
        }

        if (
            customer.getBusinessId() == null
                || !Objects.equals(
                    customer.getBusinessId(),
                    businessId
                )
        ) {
            throw new IllegalStateException(
                "El cliente resuelto no pertenece "
                    + "al negocio solicitado."
            );
        }

        if (!customer.isActive()) {
            throw new ConflictException(
                "El cliente no está activo."
            );
        }
    }

    // =========================================================
    // PERSISTED APPOINTMENT
    // =========================================================

    private void validatePersistedAppointment(
        Appointment appointment
    ) {

        if (
            appointment == null
                || appointment.getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no se ha persistido correctamente."
            );
        }

        if (
            appointment.getBusiness() == null
                || appointment.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva persistida no tiene un Business válido."
            );
        }

        if (
            appointment.getCustomer() == null
                || appointment.getCustomer().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva persistida no tiene un Customer válido."
            );
        }

        if (
            appointment.getProfessional() == null
                || appointment.getProfessional().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva persistida no tiene un Professional válido."
            );
        }

        if (
            appointment.getService() == null
                || appointment.getService().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva persistida no tiene un Service válido."
            );
        }

        if (appointment.getStatus() == null) {
            throw new IllegalStateException(
                "La reserva persistida no tiene estado."
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
                "La reserva persistida no tiene "
                    + "un intervalo temporal válido."
            );
        }
    }

    // =========================================================
    // MANAGEMENT TOKEN
    // =========================================================

    private void validateManagementToken(
        String managementToken
    ) {

        if (
            managementToken == null
                || managementToken.isBlank()
        ) {
            throw new IllegalStateException(
                "No se ha podido generar la credencial "
                    + "de gestión de la reserva."
            );
        }

        String normalized = managementToken.trim();

        if (
            normalized.length() < 32
                || normalized.length() > 256
        ) {
            throw new IllegalStateException(
                "La credencial de gestión generada "
                    + "no tiene una longitud válida."
            );
        }
    }

    // =========================================================
    // PUBLIC BOOKING RESPONSE
    // =========================================================

    private PublicBookingResponse buildResponse(
        Appointment appointment,
        ZoneId businessZone,
        String managementToken
    ) {

        validatePersistedAppointment(appointment);
        validateManagementToken(managementToken);

        if (businessZone == null) {
            throw new IllegalStateException(
                "No existe una zona horaria válida "
                    + "para construir la respuesta."
            );
        }

        Business business = appointment.getBusiness();
        Customer customer = appointment.getCustomer();
        Professional professional = appointment.getProfessional();
        Service service = appointment.getService();

        OffsetDateTime responseStartAt =
            appointment.getStartAt()
                .atZoneSameInstant(businessZone)
                .toOffsetDateTime();

        OffsetDateTime responseEndAt =
            appointment.getEndAt()
                .atZoneSameInstant(businessZone)
                .toOffsetDateTime();

        return new PublicBookingResponse(
            appointment.getId(),
            appointment.getStatus(),
            business.getId(),
            business.getName(),
            service.getId(),
            service.getName(),
            service.getDurationMinutes(),
            professional.getId(),
            professional.getDisplayName(),
            customer.getFullName(),
            responseStartAt,
            responseEndAt,
            businessZone.getId(),
            managementToken,
            BOOKING_CREATED_MESSAGE
        );
    }

    // =========================================================
    // BUSINESS TIMEZONE
    // =========================================================

    private ZoneId resolveBusinessZone(
        Business business
    ) {

        if (business == null) {
            throw new IllegalArgumentException(
                "El negocio es obligatorio."
            );
        }

        String timezone = business.getTimezone();

        if (
            timezone == null
                || timezone.isBlank()
        ) {
            throw new ConflictException(
                "El negocio no tiene una zona horaria configurada."
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
    // BASIC VALIDATION
    // =========================================================

    private void validatePositiveId(
        Long id,
        String fieldName
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                fieldName + " es obligatorio."
            );
        }

        if (id <= 0) {
            throw new IllegalArgumentException(
                fieldName + " debe ser mayor que 0."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeNullableText(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    // =========================================================
    // PUBLIC BOOKING CREATED EVENT
    // =========================================================

    /**
     * Evento interno generado al crear una reserva
     * publica con correo electronico.
     *
     * El listener debe procesarlo AFTER_COMMIT.
     *
     * El managementToken es secreto:
     * - No registrar en logs.
     * - No persistir en texto plano.
     * - No incluir en metricas.
     * - No incluir en errores.
     */
    public record PublicBookingCreatedEvent(
        Long appointmentId,
        Long businessId,
        String businessName,
        Long customerId,
        String customerName,
        String customerEmail,
        Long serviceId,
        String serviceName,
        Long professionalId,
        String professionalName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String timezone,
        String managementToken
    ) {
    }
}
