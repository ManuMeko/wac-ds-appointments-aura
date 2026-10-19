package com.wacds.appointments.appointment;

import com.wacds.appointments.appointment.dto.AppointmentRequest;
import com.wacds.appointments.appointment.dto.AppointmentRescheduleRequest;
import com.wacds.appointments.appointment.dto.AppointmentResponse;
import com.wacds.appointments.appointment.dto.AppointmentStatusUpdateRequest;
import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.availability.ProfessionalAvailability;
import com.wacds.appointments.availability.ProfessionalAvailabilityRepository;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.customer.CustomerRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentId;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentRepository;
import com.wacds.appointments.reminder.AppointmentReminderService;
import com.wacds.appointments.publicbooking.management.AppointmentManagementToken;
import com.wacds.appointments.publicbooking.management.AppointmentManagementTokenService;
import com.wacds.appointments.scheduleexception.ScheduleException;
import com.wacds.appointments.scheduleexception.ScheduleExceptionRepository;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final long MAX_AGENDA_RANGE_DAYS =
        366;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentRepository appointmentRepository;

    private final BusinessRepository businessRepository;

    private final CustomerRepository customerRepository;

    private final ProfessionalRepository professionalRepository;

    private final ServiceRepository serviceRepository;

    private final ProfessionalServiceAssignmentRepository
        assignmentRepository;

    private final ProfessionalAvailabilityRepository
        availabilityRepository;

    private final ScheduleExceptionRepository
        scheduleExceptionRepository;

    private final AppointmentMapper appointmentMapper;

    private final BusinessMembershipService
        businessMembershipService;

    private final AppointmentReminderService
        appointmentReminderService;

    private final AppointmentManagementTokenService
        appointmentManagementTokenService;

    private final ApplicationEventPublisher
        applicationEventPublisher;

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public AppointmentResponse create(
        Long businessId,
        AppointmentRequest request
    ) {

        validateBusinessId(
            businessId
        );

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de creación de cita es obligatoria."
            );
        }

        /*
         * La autorización operativa se ejecuta ANTES de cargar
         * recursos relacionados con el negocio.
         *
         * OWNER, ADMIN y STAFF pueden trabajar con citas, pero
         * ningún usuario puede utilizar esta capa para operar
         * sobre un tenant distinto al de su sesión.
         */
        requireOperationalAccess(
            businessId
        );

        Business business =
            findBusinessEntity(
                businessId
            );

        /*
         * Serializamos todas las escrituras de agenda del mismo
         * profesional. El bloqueo PostgreSQL FOR UPDATE se mantiene
         * hasta el COMMIT o ROLLBACK de esta transacción.
         *
         * Debe adquirirse antes de comprobar disponibilidad y
         * solapamientos para que create(), reschedule() y el booking
         * público compartan exactamente la misma barrera de
         * concurrencia.
         */
        lockProfessionalSchedule(
            businessId,
            request.professionalId()
        );

        Customer customer =
            customerRepository
                .findByIdAndBusinessId(
                    request.customerId(),
                    businessId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe el cliente "
                            + request.customerId()
                            + " dentro del negocio "
                            + businessId
                    )
                );

        Professional professional =
            professionalRepository
                .findById(
                    request.professionalId()
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún profesional con id: "
                            + request.professionalId()
                    )
                );

        Service service =
            serviceRepository
                .findById(
                    request.serviceId()
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún servicio con id: "
                            + request.serviceId()
                    )
                );

        validateBusinessRelations(
            business,
            customer,
            professional,
            service
        );

        validateActors(
            business,
            customer,
            professional,
            service
        );

        validateCreationStatus(
            request.status()
        );

        validateProfessionalProvidesService(
            professional.getId(),
            service.getId()
        );

        OffsetDateTime startAt =
            request.startAt();

        validateFutureStart(
            startAt
        );

        OffsetDateTime endAt =
            startAt.plusMinutes(
                service.getDurationMinutes()
            );

        validateAvailability(
            business,
            professional,
            startAt,
            endAt
        );

        boolean overlaps =
            appointmentRepository
                .existsOverlappingAppointmentInBusiness(
                    business.getId(),
                    professional.getId(),
                    startAt,
                    endAt,
                    Appointment.AppointmentStatus.CANCELLED
                );

        if (overlaps) {
            throw new ConflictException(
                "El profesional "
                    + professional.getId()
                    + " ya tiene otra cita que se solapa "
                    + "con el horario solicitado."
            );
        }

        Appointment appointment =
            appointmentMapper.toEntity(
                request,
                business,
                customer,
                professional,
                service,
                endAt
            );

        Appointment saved =
            appointmentRepository.save(
                appointment
            );

        AppointmentResponse response =
            appointmentMapper.toResponse(
                saved
            );

        String businessName =
            business.getName();

        /*
         * Misma sincronización que PublicBookingService:
         * la respuesta se materializa antes porque una
         * cancelación masiva limpia el contexto JPA.
         */
        synchronizeReminders(
            saved
        );

        notifyCustomer(
            AppointmentCustomerEmailKind.CONFIRMATION,
            saved.getId(),
            saved.getStatus(),
            response,
            businessName
        );

        return response;
    }

    // =========================================================
    // STATUS
    // =========================================================

    @Transactional
    public AppointmentResponse updateStatus(
        Long appointmentId,
        AppointmentStatusUpdateRequest request
    ) {

        validateAppointmentId(
            appointmentId
        );

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de cambio de estado es obligatoria."
            );
        }

        Appointment appointment =
            findAppointmentEntityForCurrentBusiness(
                appointmentId
            );

        Appointment.AppointmentStatus currentStatus =
            appointment.getStatus();

        Appointment.AppointmentStatus newStatus =
            request.status();

        if (newStatus == null) {
            throw new IllegalArgumentException(
                "El nuevo estado de la cita es obligatorio."
            );
        }

        if (currentStatus == newStatus) {
            return appointmentMapper.toResponse(
                appointment
            );
        }

        validateStatusTransition(
            currentStatus,
            newStatus
        );

        appointment.setStatus(
            newStatus
        );

        Appointment saved =
            appointmentRepository.save(
                appointment
            );

        AppointmentResponse response =
            appointmentMapper.toResponse(
                saved
            );

        String businessName =
            saved.getBusiness().getName();

        /*
         * PENDING -> CONFIRMED mantiene o actualiza reminders.
         * CANCELLED, COMPLETED y NO_SHOW cancelan los activos
         * y conservan los ya enviados, igual que la cancelación
         * pública.
         */
        synchronizeReminders(
            saved
        );

        if (
            newStatus
                == Appointment.AppointmentStatus.CANCELLED
        ) {

            notifyCustomer(
                AppointmentCustomerEmailKind.CANCELLATION,
                saved.getId(),
                saved.getStatus(),
                response,
                businessName
            );
        }

        return response;
    }

    // =========================================================
    // RESCHEDULE
    // =========================================================

    @Transactional
    public AppointmentResponse reschedule(
        Long appointmentId,
        AppointmentRescheduleRequest request
    ) {

        validateAppointmentId(
            appointmentId
        );

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de reprogramación es obligatoria."
            );
        }

        Appointment appointment =
            findAppointmentEntityForCurrentBusiness(
                appointmentId
            );

        validateReschedulableStatus(
            appointment.getStatus()
        );

        Business business =
            appointment.getBusiness();

        Customer customer =
            appointment.getCustomer();

        Professional professional =
            appointment.getProfessional();

        Service service =
            appointment.getService();

        validateBusinessRelations(
            business,
            customer,
            professional,
            service
        );

        validateActors(
            business,
            customer,
            professional,
            service
        );

        validateProfessionalProvidesService(
            professional.getId(),
            service.getId()
        );

        /*
         * La reprogramación modifica la agenda del profesional, por
         * lo que debe utilizar el mismo lock que create() y que el
         * flujo público. El bloqueo se adquiere antes de volver a
         * validar disponibilidad y solapamientos.
         */
        lockProfessionalSchedule(
            business.getId(),
            professional.getId()
        );

        OffsetDateTime newStartAt =
            request.startAt();

        validateFutureStart(
            newStartAt
        );

        OffsetDateTime newEndAt =
            newStartAt.plusMinutes(
                service.getDurationMinutes()
            );

        validateAvailability(
            business,
            professional,
            newStartAt,
            newEndAt
        );

        boolean overlaps =
            appointmentRepository
                .existsOverlappingAppointmentInBusinessExcludingId(
                    business.getId(),
                    professional.getId(),
                    appointment.getId(),
                    newStartAt,
                    newEndAt,
                    Appointment.AppointmentStatus.CANCELLED
                );

        if (overlaps) {
            throw new ConflictException(
                "El profesional "
                    + professional.getId()
                    + " ya tiene otra cita que se solapa "
                    + "con el nuevo horario solicitado."
            );
        }

        appointment.setStartAt(
            newStartAt
        );

        appointment.setEndAt(
            newEndAt
        );

        Appointment saved =
            appointmentRepository.save(
                appointment
            );

        AppointmentResponse response =
            appointmentMapper.toResponse(
                saved
            );

        String businessName =
            business.getName();

        /*
         * Igual que la reprogramación pública: cancela los
         * reminders activos del horario anterior y prepara
         * los de la nueva ocurrencia. SENT se conserva.
         */
        synchronizeReminders(
            saved
        );

        notifyCustomer(
            AppointmentCustomerEmailKind.RESCHEDULE,
            saved.getId(),
            saved.getStatus(),
            response,
            businessName
        );

        return response;
    }

    // =========================================================
    // REMINDERS
    // =========================================================

    /**
     * Deja los recordatorios alineados con el estado actual
     * de la cita, reutilizando AppointmentReminderService.
     *
     * PENDING y CONFIRMED se sincronizan.
     * CANCELLED, COMPLETED y NO_SHOW solo cancelan activos.
     */
    private void synchronizeReminders(
        Appointment appointment
    ) {

        if (
            appointment.getStatus()
                == Appointment.AppointmentStatus.PENDING
                || appointment.getStatus()
                == Appointment.AppointmentStatus.CONFIRMED
        ) {

            appointmentReminderService
                .synchronizeForAppointment(
                    appointment
                );

            return;
        }

        appointmentReminderService
            .cancelActiveReminders(
                appointment.getId()
            );
    }

    // =========================================================
    // CUSTOMER EMAIL
    // =========================================================

    /**
     * Publica el aviso al cliente dentro de la transacción
     * que ya guardó la cita.
     *
     * El envío real ocurre AFTER_COMMIT, en
     * AppointmentCustomerEmailListener, con el mismo
     * ResendEmailReminderDeliveryService que la reserva
     * pública. Un fallo posterior del correo no revierte
     * esta operación.
     *
     * El token en claro solo existe en el momento de
     * emitirlo. No se puede reconstruir desde el hash, así
     * que el enlace se incluye cuando esta operación emite
     * la credencial. Si ya hay una vigente, se conserva y
     * no se rota: el aviso sale igual, sin repetir el secreto.
     */
    private void notifyCustomer(
        AppointmentCustomerEmailKind kind,
        Long appointmentId,
        Appointment.AppointmentStatus status,
        AppointmentResponse response,
        String businessName
    ) {

        if (
            response == null
                || response.customerEmail() == null
                || response.customerEmail().isBlank()
        ) {
            return;
        }

        String managementToken =
            resolveManagementToken(
                kind,
                appointmentId,
                status
            );

        applicationEventPublisher.publishEvent(
            new AppointmentCustomerEmailEvent(
                kind,
                response.id(),
                businessName,
                response.customerName(),
                response.customerEmail().trim(),
                response.serviceName(),
                response.professionalName(),
                response.startAt(),
                response.timezone(),
                managementToken
            )
        );
    }

    private String resolveManagementToken(
        AppointmentCustomerEmailKind kind,
        Long appointmentId,
        Appointment.AppointmentStatus status
    ) {

        if (
            kind
                == AppointmentCustomerEmailKind.CANCELLATION
                || status
                == Appointment.AppointmentStatus.CANCELLED
                || appointmentId == null
        ) {
            return null;
        }

        List<AppointmentManagementToken> usable =
            appointmentManagementTokenService
                .findUsableByAppointmentId(
                    appointmentId
                );

        if (
            usable != null
                && !usable.isEmpty()
        ) {
            return null;
        }

        Appointment appointment =
            appointmentRepository
                .findById(
                    appointmentId
                )
                .orElseThrow(() ->
                    new IllegalStateException(
                        "No se pudo preparar la credencial "
                            + "de gestión de la cita "
                            + appointmentId
                            + "."
                    )
                );

        return appointmentManagementTokenService
            .issueToken(
                appointment
            );
    }

    // =========================================================
    // BASIC QUERIES
    // =========================================================

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAllByBusiness(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireOperationalAccess(
            businessId
        );

        validateBusinessExists(
            businessId
        );

        List<AppointmentResponse> appointments =
            appointmentRepository
                .findAllByBusinessIdOrderByStartAtDesc(
                    businessId
                )
                .stream()
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        validateResponsesBelongToBusiness(
            appointments,
            businessId
        );

        return appointments;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAllByProfessional(
        Long professionalId
    ) {

        validateProfessionalId(
            professionalId
        );

        Professional professional =
            findProfessionalEntity(
                professionalId
            );

        Long businessId =
            extractProfessionalBusinessId(
                professional
            );

        requireOperationalAccess(
            businessId
        );

        List<AppointmentResponse> appointments =
            appointmentRepository
                .findAllByBusinessIdAndProfessionalIdOrderByStartAtDesc(
                    businessId,
                    professionalId
                )
                .stream()
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        validateResponsesBelongToBusiness(
            appointments,
            businessId
        );

        return appointments;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAllByCustomer(
        Long customerId
    ) {

        validateCustomerId(
            customerId
        );

        /*
         * El tenant nunca se deriva del customerId solicitado.
         *
         * Primero obtenemos el Business autenticado y validamos
         * el acceso operativo. Después buscamos el cliente
         * exclusivamente dentro de ese Business.
         *
         * De esta forma, un customerId perteneciente a otro tenant
         * se comporta como inexistente para la sesión actual.
         */
        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );

        Customer customer =
            customerRepository
                .findByIdAndBusinessId(
                    customerId,
                    businessId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe el cliente "
                            + customerId
                            + " dentro del negocio actual."
                    )
                );

        if (
            customer.getBusiness() == null
                || customer.getBusiness().getId() == null
                || !Objects.equals(
                    customer.getBusiness().getId(),
                    businessId
                )
        ) {
            throw new AccessDeniedException(
                "El cliente solicitado no pertenece "
                    + "al negocio de la sesión actual."
            );
        }

        List<AppointmentResponse> appointments =
            appointmentRepository
                .findAllByBusinessIdAndCustomerIdOrderByStartAtDesc(
                    businessId,
                    customerId
                )
                .stream()
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        validateResponsesBelongToBusiness(
            appointments,
            businessId
        );

        return appointments;
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(
        Long appointmentId
    ) {

        validateAppointmentId(
            appointmentId
        );

        Appointment appointment =
            findAppointmentEntityForCurrentBusiness(
                appointmentId
            );

        return appointmentMapper.toResponse(
            appointment
        );
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findByBusinessAndId(
        Long businessId,
        Long appointmentId
    ) {

        validateBusinessId(
            businessId
        );

        validateAppointmentId(
            appointmentId
        );

        requireOperationalAccess(
            businessId
        );

        validateBusinessExists(
            businessId
        );

        Appointment appointment =
            appointmentRepository
                .findByIdAndBusinessId(
                    appointmentId,
                    businessId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe la cita "
                            + appointmentId
                            + " dentro del negocio "
                            + businessId
                    )
                );

        return appointmentMapper.toResponse(
            appointment
        );
    }

    // =========================================================
    // BUSINESS AGENDA
    // =========================================================

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findBusinessAgendaForDay(
        Long businessId,
        LocalDate date,
        Appointment.AppointmentStatus status
    ) {

        validateBusinessId(
            businessId
        );

        requireOperationalAccess(
            businessId
        );

        Business business =
            findBusinessEntity(
                businessId
            );

        if (date == null) {
            throw new IllegalArgumentException(
                "La fecha de la agenda es obligatoria."
            );
        }

        DateTimeRange range =
            resolveDateRange(
                business,
                date,
                date
            );

        List<Appointment> appointments;

        if (status == null) {
            appointments =
                appointmentRepository
                    .findBusinessAppointmentsInRange(
                        businessId,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        } else {
            appointments =
                appointmentRepository
                    .findBusinessAppointmentsInRangeByStatus(
                        businessId,
                        status,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        }

        List<AppointmentResponse> responses =
            mapAppointments(
                appointments
            );

        validateResponsesBelongToBusiness(
            responses,
            businessId
        );

        return responses;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findBusinessAgendaInRange(
        Long businessId,
        LocalDate from,
        LocalDate to,
        Appointment.AppointmentStatus status
    ) {

        validateBusinessId(
            businessId
        );

        requireOperationalAccess(
            businessId
        );

        Business business =
            findBusinessEntity(
                businessId
            );

        validateAgendaDateRange(
            from,
            to
        );

        DateTimeRange range =
            resolveDateRange(
                business,
                from,
                to
            );

        List<Appointment> appointments;

        if (status == null) {
            appointments =
                appointmentRepository
                    .findBusinessAppointmentsInRange(
                        businessId,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        } else {
            appointments =
                appointmentRepository
                    .findBusinessAppointmentsInRangeByStatus(
                        businessId,
                        status,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        }

        List<AppointmentResponse> responses =
            mapAppointments(
                appointments
            );

        validateResponsesBelongToBusiness(
            responses,
            businessId
        );

        return responses;
    }

    // =========================================================
    // PROFESSIONAL AGENDA
    // =========================================================

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findProfessionalAgendaInRange(
        Long professionalId,
        LocalDate from,
        LocalDate to,
        Appointment.AppointmentStatus status
    ) {

        validateProfessionalId(
            professionalId
        );

        Professional professional =
            findProfessionalEntity(
                professionalId
            );

        Long businessId =
            extractProfessionalBusinessId(
                professional
            );

        requireOperationalAccess(
            businessId
        );

        validateAgendaDateRange(
            from,
            to
        );

        Business business =
            professional.getBusiness();

        DateTimeRange range =
            resolveDateRange(
                business,
                from,
                to
            );

        List<Appointment> appointments;

        if (status == null) {
            appointments =
                appointmentRepository
                    .findProfessionalAppointmentsInRangeByBusiness(
                        businessId,
                        professionalId,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        } else {
            appointments =
                appointmentRepository
                    .findProfessionalAppointmentsInRangeByBusinessAndStatus(
                        businessId,
                        professionalId,
                        status,
                        range.fromInclusive(),
                        range.toExclusive()
                    );
        }

        List<AppointmentResponse> responses =
            mapAppointments(
                appointments
            );

        validateResponsesBelongToBusiness(
            responses,
            businessId
        );

        return responses;
    }

    // =========================================================
    // UPCOMING
    // =========================================================

    @Transactional(readOnly = true)
    public List<AppointmentResponse>
        findUpcomingBusinessAppointments(
            Long businessId
        ) {

        validateBusinessId(
            businessId
        );

        requireOperationalAccess(
            businessId
        );

        Business business =
            findBusinessEntity(
                businessId
            );

        ZoneId businessZone =
            resolveBusinessZone(
                business
            );

        OffsetDateTime now =
            OffsetDateTime.now(
                businessZone
            );

        List<AppointmentResponse> appointments =
            appointmentRepository
                .findUpcomingBusinessAppointments(
                    businessId,
                    now,
                    Appointment.AppointmentStatus.CANCELLED
                )
                .stream()
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        validateResponsesBelongToBusiness(
            appointments,
            businessId
        );

        return appointments;
    }

    // =========================================================
    // SCHEDULING · CONCURRENCY LOCK
    // =========================================================

    private void lockProfessionalSchedule(
        Long businessId,
        Long professionalId
    ) {

        validateBusinessId(
            businessId
        );

        validateProfessionalId(
            professionalId
        );

        Long lockedProfessionalId =
            appointmentRepository
                .lockProfessionalSchedule(
                    businessId,
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
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
    // SECURITY · CURRENT AUTHENTICATED USER
    // =========================================================

    private AuthenticatedUser requireAuthenticatedUser() {

        Authentication authentication =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (
            authentication == null
                || !authentication.isAuthenticated()
        ) {
            throw new AccessDeniedException(
                "No existe una sesión autenticada válida."
            );
        }

        Object principal =
            authentication.getPrincipal();

        if (
            !(principal
                instanceof AuthenticatedUser authenticatedUser)
        ) {
            throw new AccessDeniedException(
                "El principal autenticado no es válido."
            );
        }

        if (authenticatedUser.getUserId() == null) {
            throw new AccessDeniedException(
                "La sesión no contiene un userId válido."
            );
        }

        if (authenticatedUser.getBusinessId() == null) {
            throw new AccessDeniedException(
                "La sesión no contiene un businessId válido."
            );
        }

        if (authenticatedUser.getRole() == null) {
            throw new AccessDeniedException(
                "La sesión no contiene un rol válido."
            );
        }

        return authenticatedUser;
    }

    /**
     * Segunda barrera de autorización del módulo de citas.
     *
     * Todas las operaciones privadas de Appointment pertenecen
     * al ámbito operativo diario del negocio.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     * - STAFF
     *
     * Además de comprobar el rol, BusinessMembershipService
     * vuelve a validar:
     *
     * - usuario autenticado
     * - businessId activo de la sesión
     * - membresía activa
     * - negocio activo
     * - usuario activo
     * - rol persistido actual
     *
     * De esta forma AppointmentService no depende únicamente
     * de que el controller haya protegido correctamente la ruta.
     */
    private void requireOperationalAccess(
        Long businessId
    ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );
    }

    // =========================================================
    // SECURE ENTITY LOOKUPS
    // =========================================================

    private Appointment findAppointmentEntityForCurrentBusiness(
        Long appointmentId
    ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );

        return appointmentRepository
            .findByIdAndBusinessId(
                appointmentId,
                businessId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe ninguna cita con id: "
                        + appointmentId
                        + " dentro del negocio actual."
                )
            );
    }

    // =========================================================
    // ENTITY LOOKUPS
    // =========================================================

    private Business findBusinessEntity(
        Long businessId
    ) {

        return businessRepository
            .findById(
                businessId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe ningún negocio con id: "
                        + businessId
                )
            );
    }

    private Professional findProfessionalEntity(
        Long professionalId
    ) {

        return professionalRepository
            .findById(
                professionalId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe ningún profesional con id: "
                        + professionalId
                )
            );
    }

    private Customer findCustomerEntity(
        Long customerId
    ) {

        return customerRepository
            .findById(
                customerId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe ningún cliente con id: "
                        + customerId
                )
            );
    }

    // =========================================================
    // TENANT RELATION HELPERS
    // =========================================================

    private Long extractProfessionalBusinessId(
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

        return professional
            .getBusiness()
            .getId();
    }

    private Long extractCustomerBusinessId(
        Customer customer
    ) {

        if (
            customer == null
                || customer.getBusiness() == null
                || customer.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El cliente no tiene un negocio válido asociado."
            );
        }

        return customer
            .getBusiness()
            .getId();
    }

    private void validateResponsesBelongToBusiness(
        List<AppointmentResponse> appointments,
        Long expectedBusinessId
    ) {

        if (
            appointments == null
                || appointments.isEmpty()
        ) {
            return;
        }

        for (
            AppointmentResponse appointment :
            appointments
        ) {

            if (appointment == null) {
                throw new IllegalStateException(
                    "La consulta de citas ha devuelto "
                        + "un elemento nulo."
                );
            }

            if (
                appointment.businessId() == null
                    || !Objects.equals(
                        appointment.businessId(),
                        expectedBusinessId
                    )
            ) {
                throw new AccessDeniedException(
                    "Se ha detectado una cita perteneciente "
                        + "a otro negocio."
                );
            }
        }
    }

    // =========================================================
    // AGENDA RANGE HELPERS
    // =========================================================

    private void validateAgendaDateRange(
        LocalDate from,
        LocalDate to
    ) {

        if (from == null) {
            throw new IllegalArgumentException(
                "La fecha inicial 'from' es obligatoria."
            );
        }

        if (to == null) {
            throw new IllegalArgumentException(
                "La fecha final 'to' es obligatoria."
            );
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                "La fecha final 'to' no puede ser anterior "
                    + "a la fecha inicial 'from'."
            );
        }

        long days =
            ChronoUnit.DAYS.between(
                from,
                to
            );

        if (days > MAX_AGENDA_RANGE_DAYS) {
            throw new IllegalArgumentException(
                "El rango máximo permitido para consultar "
                    + "la agenda es de "
                    + MAX_AGENDA_RANGE_DAYS
                    + " días."
            );
        }
    }

    private DateTimeRange resolveDateRange(
        Business business,
        LocalDate from,
        LocalDate to
    ) {

        ZoneId businessZone =
            resolveBusinessZone(
                business
            );

        OffsetDateTime fromInclusive =
            from
                .atStartOfDay(
                    businessZone
                )
                .toOffsetDateTime();

        OffsetDateTime toExclusive =
            to
                .plusDays(1)
                .atStartOfDay(
                    businessZone
                )
                .toOffsetDateTime();

        return new DateTimeRange(
            fromInclusive,
            toExclusive
        );
    }

    private ZoneId resolveBusinessZone(
        Business business
    ) {

        if (business == null) {
            throw new IllegalArgumentException(
                "El negocio es obligatorio."
            );
        }

        String timezone =
            business.getTimezone();

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
            return ZoneId.of(
                timezone
            );
        } catch (Exception exception) {
            throw new ConflictException(
                "La zona horaria '"
                    + timezone
                    + "' del negocio "
                    + business.getId()
                    + " no es válida."
            );
        }
    }

    private List<AppointmentResponse> mapAppointments(
        List<Appointment> appointments
    ) {

        return appointments
            .stream()
            .map(
                appointmentMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // STATUS VALIDATION
    // =========================================================

    private void validateStatusTransition(
        Appointment.AppointmentStatus currentStatus,
        Appointment.AppointmentStatus newStatus
    ) {

        if (currentStatus == null) {
            throw new ConflictException(
                "La cita no tiene un estado actual válido."
            );
        }

        if (newStatus == null) {
            throw new IllegalArgumentException(
                "El nuevo estado es obligatorio."
            );
        }

        boolean validTransition =
            switch (currentStatus) {

                case PENDING ->
                    newStatus
                        == Appointment.AppointmentStatus.CONFIRMED
                        || newStatus
                        == Appointment.AppointmentStatus.CANCELLED;

                case CONFIRMED ->
                    newStatus
                        == Appointment.AppointmentStatus.COMPLETED
                        || newStatus
                        == Appointment.AppointmentStatus.CANCELLED
                        || newStatus
                        == Appointment.AppointmentStatus.NO_SHOW;

                case COMPLETED,
                     CANCELLED,
                     NO_SHOW ->
                    false;
            };

        if (!validTransition) {
            throw new ConflictException(
                "No se puede cambiar una cita del estado "
                    + currentStatus
                    + " al estado "
                    + newStatus
                    + "."
            );
        }
    }

    private void validateReschedulableStatus(
        Appointment.AppointmentStatus status
    ) {

        if (status == null) {
            throw new ConflictException(
                "La cita no tiene un estado válido."
            );
        }

        if (
            status
                != Appointment.AppointmentStatus.PENDING
                && status
                != Appointment.AppointmentStatus.CONFIRMED
        ) {
            throw new ConflictException(
                "Solo se pueden reprogramar citas "
                    + "PENDING o CONFIRMED. "
                    + "Estado actual: "
                    + status
            );
        }
    }

    // =========================================================
    // BUSINESS RELATION VALIDATION
    // =========================================================

    private void validateBusinessRelations(
        Business business,
        Customer customer,
        Professional professional,
        Service service
    ) {

        if (
            business == null
                || business.getId() == null
        ) {
            throw new ConflictException(
                "La cita no tiene un negocio válido."
            );
        }

        if (
            customer == null
                || customer.getBusiness() == null
                || customer.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El cliente no tiene un negocio válido asociado."
            );
        }

        if (
            professional == null
                || professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El profesional no tiene un negocio válido asociado."
            );
        }

        if (
            service == null
                || service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {
            throw new ConflictException(
                "El servicio no tiene un negocio válido asociado."
            );
        }

        Long businessId =
            business.getId();

        if (
            !businessId.equals(
                customer
                    .getBusiness()
                    .getId()
            )
        ) {
            throw new ConflictException(
                "El cliente no pertenece al negocio "
                    + businessId
            );
        }

        if (
            !businessId.equals(
                professional
                    .getBusiness()
                    .getId()
            )
        ) {
            throw new ConflictException(
                "El profesional no pertenece al negocio "
                    + businessId
            );
        }

        if (
            !businessId.equals(
                service
                    .getBusiness()
                    .getId()
            )
        ) {
            throw new ConflictException(
                "El servicio no pertenece al negocio "
                    + businessId
            );
        }
    }

    // =========================================================
    // ACTOR VALIDATION
    // =========================================================

    private void validateActors(
        Business business,
        Customer customer,
        Professional professional,
        Service service
    ) {

        if (!business.isActive()) {
            throw new ConflictException(
                "El negocio no está activo."
            );
        }

        if (!customer.isActive()) {
            throw new ConflictException(
                "El cliente no está activo."
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
    }

    // =========================================================
    // APPOINTMENT CREATION VALIDATION
    // =========================================================

    private void validateCreationStatus(
        Appointment.AppointmentStatus status
    ) {

        if (status == null) {
            return;
        }

        if (
            status
                != Appointment.AppointmentStatus.PENDING
                && status
                != Appointment.AppointmentStatus.CONFIRMED
        ) {
            throw new IllegalArgumentException(
                "Una nueva cita solo puede crearse "
                    + "con estado PENDING o CONFIRMED."
            );
        }
    }

    private void validateProfessionalProvidesService(
        Long professionalId,
        Long serviceId
    ) {

        if (
            professionalId == null
                || serviceId == null
        ) {
            throw new IllegalArgumentException(
                "professionalId y serviceId son obligatorios."
            );
        }

        ProfessionalServiceAssignmentId assignmentId =
            new ProfessionalServiceAssignmentId(
                professionalId,
                serviceId
            );

        if (
            !assignmentRepository.existsById(
                assignmentId
            )
        ) {
            throw new ConflictException(
                "El servicio "
                    + serviceId
                    + " no está asignado al profesional "
                    + professionalId
            );
        }
    }

    private void validateFutureStart(
        OffsetDateTime startAt
    ) {

        if (startAt == null) {
            throw new IllegalArgumentException(
                "La fecha y hora de inicio de la cita es obligatoria."
            );
        }

        if (
            !startAt.isAfter(
                OffsetDateTime.now()
            )
        ) {
            throw new IllegalArgumentException(
                "La fecha y hora de la cita debe estar en el futuro."
            );
        }
    }

    // =========================================================
    // AVAILABILITY VALIDATION
    // =========================================================

    private void validateAvailability(
        Business business,
        Professional professional,
        OffsetDateTime startAt,
        OffsetDateTime endAt
    ) {

        ZoneId businessZone =
            resolveBusinessZone(
                business
            );

        LocalDateTime localStart =
            startAt
                .atZoneSameInstant(
                    businessZone
                )
                .toLocalDateTime();

        LocalDateTime localEnd =
            endAt
                .atZoneSameInstant(
                    businessZone
                )
                .toLocalDateTime();

        if (
            !localStart
                .toLocalDate()
                .equals(
                    localEnd.toLocalDate()
                )
        ) {
            throw new ConflictException(
                "La cita no puede extenderse a un día diferente."
            );
        }

        LocalDate date =
            localStart.toLocalDate();

        LocalTime startTime =
            localStart.toLocalTime();

        LocalTime endTime =
            localEnd.toLocalTime();

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findActiveByProfessionalIdAndExceptionDate(
                    professional.getId(),
                    date
                );

        if (!exceptions.isEmpty()) {

            validateAgainstScheduleExceptions(
                professional.getId(),
                date,
                startTime,
                endTime,
                exceptions
            );

            return;
        }

        validateAgainstWeeklyAvailability(
            professional.getId(),
            localStart,
            startTime,
            endTime
        );
    }

    private void validateAgainstScheduleExceptions(
        Long professionalId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        List<ScheduleException> exceptions
    ) {

        boolean unavailable =
            exceptions
                .stream()
                .anyMatch(exception ->
                    exception.getType()
                        == ScheduleException.ExceptionType.UNAVAILABLE
                );

        if (unavailable) {
            throw new ConflictException(
                "El profesional "
                    + professionalId
                    + " no está disponible el día "
                    + date
            );
        }

        boolean fitsCustomHours =
            exceptions
                .stream()
                .filter(exception ->
                    exception.getType()
                        == ScheduleException.ExceptionType.CUSTOM_HOURS
                )
                .anyMatch(exception ->
                    !startTime.isBefore(
                        exception.getStartTime()
                    )
                        && !endTime.isAfter(
                            exception.getEndTime()
                        )
                );

        if (!fitsCustomHours) {
            throw new ConflictException(
                "La cita solicitada no está dentro "
                    + "del horario excepcional del profesional "
                    + professionalId
                    + " para el día "
                    + date
            );
        }
    }

    private void validateAgainstWeeklyAvailability(
        Long professionalId,
        LocalDateTime localStart,
        LocalTime startTime,
        LocalTime endTime
    ) {

        List<ProfessionalAvailability> availability =
            availabilityRepository
                .findAllByProfessionalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                    professionalId
                );

        boolean available =
            availability
                .stream()
                .filter(slot ->
                    slot.getDayOfWeek()
                        == localStart.getDayOfWeek()
                )
                .anyMatch(slot ->
                    !startTime.isBefore(
                        slot.getStartTime()
                    )
                        && !endTime.isAfter(
                            slot.getEndTime()
                        )
                );

        if (!available) {
            throw new ConflictException(
                "La cita solicitada queda fuera "
                    + "del horario habitual del profesional "
                    + professionalId
                    + " para "
                    + localStart.getDayOfWeek()
            );
        }
    }

    // =========================================================
    // ID VALIDATION
    // =========================================================

    private void validateBusinessId(
        Long businessId
    ) {

        if (businessId == null) {
            throw new IllegalArgumentException(
                "businessId es obligatorio."
            );
        }

        if (businessId <= 0) {
            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }
    }

    private void validateAppointmentId(
        Long appointmentId
    ) {

        if (appointmentId == null) {
            throw new IllegalArgumentException(
                "appointmentId es obligatorio."
            );
        }

        if (appointmentId <= 0) {
            throw new IllegalArgumentException(
                "appointmentId debe ser mayor que 0."
            );
        }
    }

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

    private void validateCustomerId(
        Long customerId
    ) {

        if (customerId == null) {
            throw new IllegalArgumentException(
                "customerId es obligatorio."
            );
        }

        if (customerId <= 0) {
            throw new IllegalArgumentException(
                "customerId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // EXISTENCE VALIDATION
    // =========================================================

    private void validateBusinessExists(
        Long businessId
    ) {

        if (
            !businessRepository.existsById(
                businessId
            )
        ) {
            throw new ResourceNotFoundException(
                "No existe ningún negocio con id: "
                    + businessId
            );
        }
    }

    // =========================================================
    // INTERNAL TYPES
    // =========================================================

    private record DateTimeRange(
        OffsetDateTime fromInclusive,
        OffsetDateTime toExclusive
    ) {
    }

    // =========================================================
    // CUSTOMER EMAIL EVENT
    // =========================================================

    /**
     * Aviso interno al cliente tras crear, reprogramar
     * o cancelar una cita desde el panel.
     *
     * El listener lo procesa AFTER_COMMIT.
     *
     * managementToken es secreto:
     * no forma parte de toString, logs ni de la
     * idempotency key.
     */
    public enum AppointmentCustomerEmailKind {
        CONFIRMATION,
        RESCHEDULE,
        CANCELLATION
    }

    public record AppointmentCustomerEmailEvent(
        AppointmentCustomerEmailKind kind,
        Long appointmentId,
        String businessName,
        String customerName,
        String customerEmail,
        String serviceName,
        String professionalName,
        OffsetDateTime startAt,
        String timezone,
        String managementToken
    ) {

        @Override
        public String toString() {

            boolean tokenPresent =
                managementToken != null
                    && !managementToken.isBlank();

            return "AppointmentCustomerEmailEvent["
                + "kind="
                + kind
                + ", appointmentId="
                + appointmentId
                + ", managementTokenPresent="
                + tokenPresent
                + "]";
        }
    }
}