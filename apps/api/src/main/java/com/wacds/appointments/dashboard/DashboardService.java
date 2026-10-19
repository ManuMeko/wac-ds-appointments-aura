package com.wacds.appointments.dashboard;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.appointment.AppointmentMapper;
import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.appointment.dto.AppointmentResponse;
import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.dashboard.dto.AppointmentStatusSummaryResponse;
import com.wacds.appointments.dashboard.dto.DashboardSummaryResponse;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class DashboardService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_UPCOMING_APPOINTMENTS =
        10;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessRepository businessRepository;

    private final AppointmentRepository appointmentRepository;

    private final AppointmentMapper appointmentMapper;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // BUSINESS DASHBOARD
    // =========================================================

    /**
     * Devuelve el resumen operativo principal del negocio.
     *
     * El Dashboard pertenece al área operativa diaria.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     * - STAFF
     *
     * La autorización se realiza antes de consultar
     * cualquier información del business.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getBusinessDashboard(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireOperationalAccess(
            businessId
        );

        Business business =
            findActiveBusinessEntity(
                businessId
            );

        ZoneId businessZone =
            resolveBusinessZone(
                business
            );

        LocalDate today =
            LocalDate.now(
                businessZone
            );

        OffsetDateTime startOfToday =
            today
                .atStartOfDay(
                    businessZone
                )
                .toOffsetDateTime();

        OffsetDateTime startOfTomorrow =
            today
                .plusDays(1)
                .atStartOfDay(
                    businessZone
                )
                .toOffsetDateTime();

        OffsetDateTime now =
            OffsetDateTime.now(
                businessZone
            );

        // -----------------------------------------------------
        // TODAY
        // -----------------------------------------------------

        /*
         * Citas del día actual.
         *
         * Incluimos todos los estados para que el dashboard
         * pueda representar:
         *
         * - PENDING
         * - CONFIRMED
         * - COMPLETED
         * - CANCELLED
         * - NO_SHOW
         */
        List<Appointment> todayAppointments =
            appointmentRepository
                .findBusinessAppointmentsInRange(
                    businessId,
                    startOfToday,
                    startOfTomorrow
                );

        validateAppointmentsBelongToBusiness(
            todayAppointments,
            businessId
        );

        // -----------------------------------------------------
        // UPCOMING
        // -----------------------------------------------------

        /*
         * Próximas citas.
         *
         * El repository excluye CANCELLED.
         */
        List<Appointment> upcomingAppointments =
            appointmentRepository
                .findUpcomingBusinessAppointments(
                    businessId,
                    now,
                    Appointment.AppointmentStatus.CANCELLED
                );

        validateAppointmentsBelongToBusiness(
            upcomingAppointments,
            businessId
        );

        // -----------------------------------------------------
        // STATUS SUMMARY
        // -----------------------------------------------------

        /*
         * Actualmente calculamos el resumen cargando las citas
         * del negocio.
         *
         * Es correcto funcionalmente para esta fase Business.
         *
         * Cuando aumente el volumen podremos sustituirlo por
         * queries COUNT específicas en PostgreSQL sin cambiar
         * el contrato del DashboardService.
         */
        List<Appointment> allAppointments =
            appointmentRepository
                .findAllByBusinessIdOrderByStartAtDesc(
                    businessId
                );

        validateAppointmentsBelongToBusiness(
            allAppointments,
            businessId
        );

        AppointmentStatusSummaryResponse statusSummary =
            buildStatusSummary(
                allAppointments
            );

        // -----------------------------------------------------
        // RESPONSE MAPPING
        // -----------------------------------------------------

        List<AppointmentResponse> todayResponses =
            todayAppointments
                .stream()
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        List<AppointmentResponse> upcomingResponses =
            upcomingAppointments
                .stream()
                .limit(
                    MAX_UPCOMING_APPOINTMENTS
                )
                .map(
                    appointmentMapper::toResponse
                )
                .toList();

        validateAppointmentResponsesBelongToBusiness(
            todayResponses,
            businessId
        );

        validateAppointmentResponsesBelongToBusiness(
            upcomingResponses,
            businessId
        );

        return new DashboardSummaryResponse(
            business.getId(),
            today,
            business.getTimezone(),
            todayAppointments.size(),
            upcomingAppointments.size(),
            statusSummary,
            todayResponses,
            upcomingResponses
        );
    }

    // =========================================================
    // STATUS SUMMARY
    // =========================================================

    private AppointmentStatusSummaryResponse buildStatusSummary(
        List<Appointment> appointments
    ) {

        if (appointments == null) {
            throw new IllegalArgumentException(
                "La colección de citas no puede ser null."
            );
        }

        long pending =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .filter(appointment ->
                    appointment.getStatus()
                        == Appointment.AppointmentStatus.PENDING
                )
                .count();

        long confirmed =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .filter(appointment ->
                    appointment.getStatus()
                        == Appointment.AppointmentStatus.CONFIRMED
                )
                .count();

        long completed =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .filter(appointment ->
                    appointment.getStatus()
                        == Appointment.AppointmentStatus.COMPLETED
                )
                .count();

        long cancelled =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .filter(appointment ->
                    appointment.getStatus()
                        == Appointment.AppointmentStatus.CANCELLED
                )
                .count();

        long noShow =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .filter(appointment ->
                    appointment.getStatus()
                        == Appointment.AppointmentStatus.NO_SHOW
                )
                .count();

        long total =
            appointments
                .stream()
                .filter(Objects::nonNull)
                .count();

        return new AppointmentStatusSummaryResponse(
            total,
            pending,
            confirmed,
            completed,
            cancelled,
            noShow
        );
    }

    // =========================================================
    // SECURITY · OPERATIONAL ACCESS
    // =========================================================

    /**
     * El Dashboard es una función operativa.
     *
     * Roles permitidos:
     *
     * OWNER
     * ADMIN
     * STAFF
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

    /**
     * Recupera el principal cargado por
     * JwtAuthenticationFilter.
     */
    private AuthenticatedUser requireAuthenticatedUser() {

        Authentication authentication =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (
            authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                    instanceof AuthenticatedUser authenticatedUser)
        ) {
            throw new BadCredentialsException(
                "No existe una sesión autenticada válida."
            );
        }

        if (authenticatedUser.getUserId() == null) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (authenticatedUser.getBusinessId() == null) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        if (authenticatedUser.getRole() == null) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un rol válido."
            );
        }

        return authenticatedUser;
    }

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Busca el business y exige que siga activo.
     */
    private Business findActiveBusinessEntity(
        Long businessId
    ) {

        Business business =
            businessRepository
                .findById(
                    businessId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún negocio con id: "
                            + businessId
                    )
                );

        if (!business.isActive()) {
            throw new ConflictException(
                "El negocio "
                    + businessId
                    + " no está activo."
            );
        }

        return business;
    }

    // =========================================================
    // TENANT VALIDATION · APPOINTMENTS
    // =========================================================

    /**
     * Defensa adicional frente a errores accidentales
     * en queries de repositorio.
     *
     * Todas las citas recuperadas para el Dashboard deben
     * pertenecer al business solicitado.
     */
    private void validateAppointmentsBelongToBusiness(
        List<Appointment> appointments,
        Long expectedBusinessId
    ) {

        if (
            appointments == null
                || appointments.isEmpty()
        ) {
            return;
        }

        for (Appointment appointment : appointments) {

            if (appointment == null) {
                throw new IllegalStateException(
                    "La consulta del dashboard ha devuelto "
                        + "una cita nula."
                );
            }

            if (
                appointment.getBusiness() == null
                    || appointment.getBusiness().getId() == null
            ) {
                throw new IllegalStateException(
                    "La cita "
                        + appointment.getId()
                        + " no tiene un negocio válido asociado."
                );
            }

            Long appointmentBusinessId =
                appointment
                    .getBusiness()
                    .getId();

            if (
                !Objects.equals(
                    appointmentBusinessId,
                    expectedBusinessId
                )
            ) {
                throw new IllegalStateException(
                    "Se ha detectado una cita perteneciente "
                        + "a otro negocio durante la generación "
                        + "del dashboard."
                );
            }
        }
    }

    /**
     * Segunda defensa después del mapeo DTO.
     */
    private void validateAppointmentResponsesBelongToBusiness(
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
                    "El dashboard ha generado "
                        + "una respuesta de cita nula."
                );
            }

            if (
                appointment.businessId() == null
                    || !Objects.equals(
                        appointment.businessId(),
                        expectedBusinessId
                    )
            ) {
                throw new IllegalStateException(
                    "Se ha generado una respuesta de cita "
                        + "perteneciente a otro negocio."
                );
            }
        }
    }

    // =========================================================
    // TIMEZONE
    // =========================================================

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
}