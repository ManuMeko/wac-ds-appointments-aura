
package com.wacds.appointments.publicbooking.management;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.availabilityslot.AvailabilitySlotService;
import com.wacds.appointments.availabilityslot.dto.AvailabilitySlotResponse;
import com.wacds.appointments.availabilityslot.dto.DailyAvailabilityResponse;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.business.settings.BusinessSettingsRepository;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.publicbooking.dto.PublicBookingCancelResponse;
import com.wacds.appointments.publicbooking.dto.PublicBookingManagementResponse;
import com.wacds.appointments.publicbooking.dto.PublicBookingRescheduleRequest;
import com.wacds.appointments.publicbooking.dto.PublicBookingRescheduleResponse;
import com.wacds.appointments.reminder.AppointmentReminderService;
import com.wacds.appointments.service.Service;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * WAC D.S. - Model 01 - Appointments
 *
 * Gestion publica de reservas mediante credenciales privadas.
 *
 * Responsabilidades:
 * - Consulta autorizada de una reserva.
 * - Aplicacion de las politicas del negocio.
 * - Cancelacion transaccional.
 * - Reprogramacion transaccional.
 * - Validacion del motor central de disponibilidad.
 * - Coordinacion de bloqueos de agenda.
 * - Revocacion de credenciales tras cancelacion.
 * - Sincronizacion de recordatorios.
 *
 * Seguridad:
 * La identidad de la reserva procede exclusivamente del
 * managementToken validado por el backend.
 *
 * El identificador de reserva no constituye una credencial.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PublicBookingManagementService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String BOOKING_CANCELLED_MESSAGE =
        "Reserva cancelada correctamente.";

    private static final String BOOKING_RESCHEDULED_MESSAGE =
        "Reserva reprogramada correctamente.";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentManagementTokenService
        appointmentManagementTokenService;

    private final BusinessSettingsRepository
        businessSettingsRepository;

    private final AppointmentRepository
        appointmentRepository;

    private final AvailabilitySlotService
        availabilitySlotService;

    private final AppointmentReminderService
        appointmentReminderService;

    // =========================================================
    // GET BOOKING
    // =========================================================

    /**
     * Consulta los datos de la reserva autorizada.
     *
     * La consulta del token no utiliza bloqueo de escritura.
     * La autorizacion se resuelve exclusivamente en servidor.
     */
    @Transactional(readOnly = true)
    public PublicBookingManagementResponse getBooking(
        String managementToken
    ) {
        Appointment appointment =
            appointmentManagementTokenService.requireAppointment(
                managementToken
            );

        validateAppointment(appointment);
        validateAppointmentRelations(appointment);

        Business business = appointment.getBusiness();
        Long businessId = business.getId();

        BusinessSettings settings =
            requireBusinessSettings(businessId);

        validateBusinessSettings(settings, businessId);

        ZoneId zone = resolveBusinessZone(business);

        OffsetDateTime startAt =
            toBusinessOffset(appointment.getStartAt(), zone);

        OffsetDateTime endAt =
            toBusinessOffset(appointment.getEndAt(), zone);

        OffsetDateTime now = OffsetDateTime.now(zone);

        OffsetDateTime cancellationDeadline =
            calculateCancellationDeadline(startAt, settings);

        OffsetDateTime reschedulingDeadline =
            calculateReschedulingDeadline(startAt, settings);

        boolean cancellationAllowed =
            isCancellationAllowed(
                appointment,
                settings,
                now,
                cancellationDeadline
            );

        boolean reschedulingAllowed =
            isReschedulingAllowed(
                appointment,
                settings,
                now,
                reschedulingDeadline
            );

        return buildManagementResponse(
            appointment,
            settings,
            zone,
            startAt,
            endAt,
            cancellationAllowed,
            reschedulingAllowed,
            cancellationDeadline,
            reschedulingDeadline
        );
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    /**
     * Cancela la reserva del managementToken recibido.
     *
     * La operacion comparte una transaccion con:
     * - bloqueo del token;
     * - bloqueo de la agenda;
     * - cambio de estado;
     * - cancelacion de recordatorios activos;
     * - revocacion de credenciales.
     *
     * Una excepcion provoca rollback de la transaccion.
     */
    @Transactional
    public PublicBookingCancelResponse cancelBooking(
        String managementToken
    ) {
        AppointmentManagementToken token =
            appointmentManagementTokenService
                .requireUsableTokenAndMarkUsed(managementToken);

        Appointment appointment = token.getAppointment();

        validateAppointment(appointment);
        validateAppointmentRelations(appointment);

        Business business = appointment.getBusiness();
        Professional professional = appointment.getProfessional();

        Long businessId = business.getId();
        Long professionalId = professional.getId();

        ZoneId zone = resolveBusinessZone(business);

        /*
         * El servicio de token adquiere PESSIMISTIC_WRITE.
         * Conservamos el bloqueo de agenda existente.
         */
        lockProfessionalSchedule(businessId, professionalId);

        /*
         * Las reglas se comprueban despues de adquirir
         * los bloqueos relevantes de esta operacion.
         */
        BusinessSettings settings =
            requireBusinessSettings(businessId);

        validateBusinessSettings(settings, businessId);
        validateOnlineCancellationEnabled(settings);
        validateCancellableStatus(appointment.getStatus());

        OffsetDateTime now = OffsetDateTime.now(zone);

        OffsetDateTime startAt =
            toBusinessOffset(appointment.getStartAt(), zone);

        OffsetDateTime endAt =
            toBusinessOffset(appointment.getEndAt(), zone);

        /*
         * Defensa explicita: no se puede cancelar
         * una cita que ya ha comenzado.
         *
         * Se aplica tambien cuando el aviso minimo
         * configurado es de cero minutos.
         */
        validateAppointmentNotStarted(
            startAt,
            now,
            "cancelarse"
        );

        OffsetDateTime deadline =
            calculateCancellationDeadline(startAt, settings);

        validateCancellationDeadline(
            now,
            deadline,
            settings
        );

        appointment.setStatus(
            Appointment.AppointmentStatus.CANCELLED
        );

        Appointment saved =
            appointmentRepository.saveAndFlush(appointment);

        validateAppointment(saved);

        if (
            saved.getStatus()
                != Appointment.AppointmentStatus.CANCELLED
        ) {
            throw new IllegalStateException(
                "La reserva no se ha cancelado correctamente."
            );
        }

        /*
         * Materializar la respuesta antes de ejecutar
         * el UPDATE masivo de revocacion.
         *
         * El repositorio utiliza clearAutomatically=true.
         */
        PublicBookingCancelResponse response =
            buildCancelResponse(
                saved,
                zone,
                startAt,
                endAt,
                now
            );

        /*
         * Conservar reminders historicos enviados.
         * Cancelar exclusivamente los activos.
         */
        appointmentReminderService.cancelActiveReminders(
            saved.getId()
        );

        /*
         * Invalidar todas las credenciales de esta cita.
         * La revocacion pertenece a la misma transaccion.
         */
        appointmentManagementTokenService
            .revokeAllForAppointment(saved.getId());

        return response;
    }

    // =========================================================
    // RESCHEDULE BOOKING
    // =========================================================

    /**
     * Reprograma una reserva existente.
     *
     * El cliente solamente puede solicitar startAt.
     *
     * No permite cambiar:
     * - businessId;
     * - customerId;
     * - professionalId;
     * - serviceId;
     * - status;
     * - duracion;
     * - endAt.
     *
     * El nuevo endAt siempre procede del motor
     * central de disponibilidad.
     */
    @Transactional
    public PublicBookingRescheduleResponse rescheduleBooking(
        String managementToken,
        PublicBookingRescheduleRequest request
    ) {
        validateRescheduleRequest(request);

        AppointmentManagementToken token =
            appointmentManagementTokenService
                .requireUsableTokenAndMarkUsed(managementToken);

        Appointment appointment = token.getAppointment();

        validateAppointment(appointment);
        validateAppointmentRelations(appointment);

        Business business = appointment.getBusiness();
        Professional professional = appointment.getProfessional();
        Service service = appointment.getService();

        Long businessId = business.getId();
        Long professionalId = professional.getId();
        Long serviceId = service.getId();

        ZoneId zone = resolveBusinessZone(business);

        /*
         * El token ya esta bloqueado por la consulta
         * PESSIMISTIC_WRITE del servicio de credenciales.
         *
         * Bloqueamos tambien la agenda del profesional.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        BusinessSettings settings =
            requireBusinessSettings(businessId);

        validateBusinessSettings(settings, businessId);

        validateOnlineReschedulingEnabled(settings);
        validateReschedulableStatus(appointment.getStatus());

        OffsetDateTime now = OffsetDateTime.now(zone);

        OffsetDateTime previousStartAt =
            toBusinessOffset(appointment.getStartAt(), zone);

        OffsetDateTime previousEndAt =
            toBusinessOffset(appointment.getEndAt(), zone);

        /*
         * La cita original debe seguir siendo futura.
         */
        validateAppointmentNotStarted(
            previousStartAt,
            now,
            "reprogramarse"
        );

        OffsetDateTime deadline =
            calculateReschedulingDeadline(
                previousStartAt,
                settings
            );

        validateReschedulingDeadline(
            now,
            deadline,
            settings
        );

        OffsetDateTime requestedStartAt = request.startAt();

        validateFutureStart(requestedStartAt);

        if (
            requestedStartAt.isEqual(
                appointment.getStartAt()
            )
        ) {
            throw new ConflictException(
                "El nuevo horario coincide con el horario "
                    + "actual de la reserva."
            );
        }

        LocalDate requestedBusinessDate =
            requestedStartAt
                .atZoneSameInstant(zone)
                .toLocalDate();

        /*
         * La disponibilidad se calcula dentro
         * de la transaccion y con la agenda bloqueada.
         */
        DailyAvailabilityResponse availability =
            availabilitySlotService.getDailyAvailability(
                professionalId,
                serviceId,
                requestedBusinessDate
            );

        validateAvailabilityIdentity(
            availability,
            businessId,
            professionalId,
            serviceId,
            requestedBusinessDate
        );

        AvailabilitySlotResponse selectedSlot =
            findRequestedSlot(
                availability,
                requestedStartAt
            );

        OffsetDateTime newStartAt = selectedSlot.startAt();
        OffsetDateTime newEndAt = selectedSlot.endAt();

        validateSelectedSlot(
            service,
            newStartAt,
            newEndAt
        );

        /*
         * Doble comprobacion de futuro.
         * No se acepta un slot que haya quedado
         * en el pasado durante la operacion.
         */
        validateFutureStart(newStartAt);

        LocalDate selectedBusinessDate =
            newStartAt
                .atZoneSameInstant(zone)
                .toLocalDate();

        if (
            !requestedBusinessDate.equals(
                selectedBusinessDate
            )
        ) {
            throw new ConflictException(
                "El horario seleccionado no corresponde "
                    + "a la fecha solicitada."
            );
        }

        /*
         * Comprobacion final de solapamientos,
         * excluyendo la propia reserva.
         *
         * La disponibilidad central aplica sus
         * reglas de agenda, excepciones y buffers.
         */
        boolean overlaps =
            appointmentRepository
                .existsOverlappingAppointmentInBusinessExcludingId(
                    businessId,
                    professionalId,
                    appointment.getId(),
                    newStartAt,
                    newEndAt,
                    Appointment.AppointmentStatus.CANCELLED
                );

        if (overlaps) {
            throw new ConflictException(
                "El horario seleccionado ya no está disponible. "
                    + "Selecciona otro horario."
            );
        }

        appointment.setStartAt(newStartAt);
        appointment.setEndAt(newEndAt);

        Appointment saved =
            appointmentRepository.saveAndFlush(appointment);

        validateAppointment(saved);

        if (!saved.getStartAt().isEqual(newStartAt)) {
            throw new IllegalStateException(
                "La reserva no se ha reprogramado correctamente."
            );
        }

        if (!saved.getEndAt().isEqual(newEndAt)) {
            throw new IllegalStateException(
                "La hora final de la reserva reprogramada "
                    + "no es correcta."
            );
        }

        if (
            !Objects.equals(
                saved.getId(),
                appointment.getId()
            )
        ) {
            throw new IllegalStateException(
                "La operación ha modificado una reserva inesperada."
            );
        }

        /*
         * Sincronizacion de reminders dentro
         * de la misma transaccion.
         */
        appointmentReminderService
            .synchronizeForAppointment(saved);

        /*
         * El token se conserva tras reprogramar.
         * Sigue siendo la misma cita.
         */
        OffsetDateTime rescheduledAt =
            OffsetDateTime.now(zone);

        return buildRescheduleResponse(
            saved,
            zone,
            previousStartAt,
            previousEndAt,
            newStartAt,
            newEndAt,
            rescheduledAt
        );
    }

    // =========================================================
    // PROFESSIONAL SCHEDULE LOCK
    // =========================================================

    private void lockProfessionalSchedule(
        Long businessId,
        Long professionalId
    ) {
        validatePositiveId(businessId, "businessId");
        validatePositiveId(professionalId, "professionalId");

        Long lockedProfessionalId =
            appointmentRepository
                .lockProfessionalSchedule(
                    businessId,
                    professionalId
                )
                .orElseThrow(
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
        validatePositiveId(businessId, "businessId");

        return businessSettingsRepository
            .findByBusinessId(businessId)
            .orElseThrow(
                () -> new ConflictException(
                    "El negocio no tiene configuración operativa."
                )
            );
    }

    private void validateBusinessSettings(
        BusinessSettings settings,
        Long expectedBusinessId
    ) {
        if (
            settings == null ||
            settings.getId() == null
        ) {
            throw new IllegalStateException(
                "BusinessSettings no es válido."
            );
        }

        if (
            settings.getBusiness() == null ||
            settings.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "BusinessSettings no tiene un Business válido."
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
    }

    // =========================================================
    // CANCELLATION VALIDATION
    // =========================================================

    private void validateOnlineCancellationEnabled(
        BusinessSettings settings
    ) {
        if (settings == null) {
            throw new IllegalArgumentException(
                "BusinessSettings es obligatorio."
            );
        }

        if (!settings.isOnlineCancellationEnabled()) {
            throw new ConflictException(
                "La cancelación online está deshabilitada "
                    + "para este negocio."
            );
        }
    }

    private void validateCancellableStatus(
        Appointment.AppointmentStatus status
    ) {
        if (!isCancellableStatus(status)) {
            throw new ConflictException(
                "La reserva no puede cancelarse desde "
                    + "el estado " + status + "."
            );
        }
    }

    private void validateCancellationDeadline(
        OffsetDateTime now,
        OffsetDateTime deadline,
        BusinessSettings settings
    ) {
        if (
            now == null ||
            deadline == null ||
            settings == null
        ) {
            throw new IllegalStateException(
                "No se puede validar el plazo de cancelación."
            );
        }

        if (now.isAfter(deadline)) {
            throw new ConflictException(
                "La reserva ya no puede cancelarse online. "
                    + "La cancelación requiere una antelación "
                    + "mínima de "
                    + settings.getMinimumCancellationNoticeMinutes()
                    + " minutos."
            );
        }
    }

    // =========================================================
    // RESCHEDULING VALIDATION
    // =========================================================

    private void validateOnlineReschedulingEnabled(
        BusinessSettings settings
    ) {
        if (settings == null) {
            throw new IllegalArgumentException(
                "BusinessSettings es obligatorio."
            );
        }

        if (!settings.isOnlineReschedulingEnabled()) {
            throw new ConflictException(
                "La reprogramación online está deshabilitada "
                    + "para este negocio."
            );
        }
    }

    private void validateReschedulableStatus(
        Appointment.AppointmentStatus status
    ) {
        if (!isReschedulableStatus(status)) {
            throw new ConflictException(
                "La reserva no puede reprogramarse desde "
                    + "el estado " + status + "."
            );
        }
    }

    private void validateReschedulingDeadline(
        OffsetDateTime now,
        OffsetDateTime deadline,
        BusinessSettings settings
    ) {
        if (
            now == null ||
            deadline == null ||
            settings == null
        ) {
            throw new IllegalStateException(
                "No se puede validar el plazo de reprogramación."
            );
        }

        if (now.isAfter(deadline)) {
            throw new ConflictException(
                "La reserva ya no puede reprogramarse online. "
                    + "La reprogramación requiere una antelación "
                    + "mínima de "
                    + settings.getMinimumRescheduleNoticeMinutes()
                    + " minutos."
            );
        }
    }

    private void validateRescheduleRequest(
        PublicBookingRescheduleRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de reprogramación es obligatoria."
            );
        }

        if (request.startAt() == null) {
            throw new IllegalArgumentException(
                "startAt es obligatorio."
            );
        }
    }

    private void validateFutureStart(
        OffsetDateTime startAt
    ) {
        if (startAt == null) {
            throw new IllegalArgumentException(
                "La nueva fecha y hora es obligatoria."
            );
        }

        if (!startAt.isAfter(OffsetDateTime.now())) {
            throw new ConflictException(
                "La nueva fecha y hora debe estar en el futuro."
            );
        }
    }

    /**
     * Defensa explicita frente a citas ya iniciadas.
     *
     * No depende del valor de antelacion minima
     * configurado para el negocio.
     */
    private void validateAppointmentNotStarted(
        OffsetDateTime startAt,
        OffsetDateTime now,
        String action
    ) {
        if (startAt == null || now == null) {
            throw new IllegalStateException(
                "No se puede comprobar la fecha de la reserva."
            );
        }

        if (!startAt.isAfter(now)) {
            throw new ConflictException(
                "La reserva ya no puede "
                    + action
                    + " online porque su horario "
                    + "de inicio ha llegado o ha transcurrido."
            );
        }
    }

    // =========================================================
    // EFFECTIVE POLICIES
    // =========================================================

    private boolean isCancellationAllowed(
        Appointment appointment,
        BusinessSettings settings,
        OffsetDateTime now,
        OffsetDateTime deadline
    ) {
        if (
            appointment == null ||
            settings == null ||
            now == null ||
            deadline == null
        ) {
            return false;
        }

        if (!settings.isOnlineCancellationEnabled()) {
            return false;
        }

        if (!isCancellableStatus(appointment.getStatus())) {
            return false;
        }

        if (
            appointment.getStartAt() == null ||
            !appointment.getStartAt().isAfter(now)
        ) {
            return false;
        }

        return !now.isAfter(deadline);
    }

    private boolean isReschedulingAllowed(
        Appointment appointment,
        BusinessSettings settings,
        OffsetDateTime now,
        OffsetDateTime deadline
    ) {
        if (
            appointment == null ||
            settings == null ||
            now == null ||
            deadline == null
        ) {
            return false;
        }

        if (!settings.isOnlineReschedulingEnabled()) {
            return false;
        }

        if (!isReschedulableStatus(appointment.getStatus())) {
            return false;
        }

        if (
            appointment.getStartAt() == null ||
            !appointment.getStartAt().isAfter(now)
        ) {
            return false;
        }

        return !now.isAfter(deadline);
    }

    // =========================================================
    // DEADLINES
    // =========================================================

    private OffsetDateTime calculateCancellationDeadline(
        OffsetDateTime startAt,
        BusinessSettings settings
    ) {
        if (startAt == null || settings == null) {
            throw new IllegalArgumentException(
                "No se puede calcular el plazo de cancelación."
            );
        }

        if (!settings.isOnlineCancellationEnabled()) {
            return null;
        }

        return startAt.minusMinutes(
            settings.getMinimumCancellationNoticeMinutes()
        );
    }

    private OffsetDateTime calculateReschedulingDeadline(
        OffsetDateTime startAt,
        BusinessSettings settings
    ) {
        if (startAt == null || settings == null) {
            throw new IllegalArgumentException(
                "No se puede calcular el plazo de reprogramación."
            );
        }

        if (!settings.isOnlineReschedulingEnabled()) {
            return null;
        }

        return startAt.minusMinutes(
            settings.getMinimumRescheduleNoticeMinutes()
        );
    }

    // =========================================================
    // AVAILABILITY
    // =========================================================

    /**
     * Valida que el resultado del motor central
     * corresponde exactamente a la consulta solicitada.
     */
    private void validateAvailabilityIdentity(
        DailyAvailabilityResponse availability,
        Long expectedBusinessId,
        Long expectedProfessionalId,
        Long expectedServiceId,
        LocalDate expectedDate
    ) {
        if (availability == null) {
            throw new ConflictException(
                "No se ha podido calcular la disponibilidad."
            );
        }

        if (
            !Objects.equals(
                availability.businessId(),
                expectedBusinessId
            )
        ) {
            throw new IllegalStateException(
                "La disponibilidad pertenece a otro negocio."
            );
        }

        if (
            !Objects.equals(
                availability.professionalId(),
                expectedProfessionalId
            )
        ) {
            throw new IllegalStateException(
                "La disponibilidad pertenece a otro profesional."
            );
        }

        if (
            !Objects.equals(
                availability.serviceId(),
                expectedServiceId
            )
        ) {
            throw new IllegalStateException(
                "La disponibilidad pertenece a otro servicio."
            );
        }

        if (
            !Objects.equals(
                availability.date(),
                expectedDate
            )
        ) {
            throw new IllegalStateException(
                "La disponibilidad corresponde a otra fecha."
            );
        }
    }

    /**
     * Localiza el slot exacto por instante absoluto.
     *
     * isEqual permite comparar instantes con
     * offsets diferentes.
     */
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
                "El nuevo startAt es obligatorio."
            );
        }

        List<AvailabilitySlotResponse> slots =
            availability.slots();

        if (slots == null || slots.isEmpty()) {
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
                    "El horario seleccionado no está disponible."
                )
            );
    }

    private void validateSelectedSlot(
        Service service,
        OffsetDateTime startAt,
        OffsetDateTime endAt
    ) {
        if (service == null) {
            throw new IllegalStateException(
                "El servicio de la reserva no es válido."
            );
        }

        if (
            service.getDurationMinutes() == null ||
            service.getDurationMinutes() <= 0
        ) {
            throw new IllegalStateException(
                "El servicio no tiene una duración válida."
            );
        }

        if (
            startAt == null ||
            endAt == null ||
            !endAt.isAfter(startAt)
        ) {
            throw new IllegalStateException(
                "El slot seleccionado no contiene "
                    + "un intervalo válido."
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
    // STATUS RULES
    // =========================================================

    private boolean isCancellableStatus(
        Appointment.AppointmentStatus status
    ) {
        return status == Appointment.AppointmentStatus.PENDING
            || status == Appointment.AppointmentStatus.CONFIRMED;
    }

    private boolean isReschedulableStatus(
        Appointment.AppointmentStatus status
    ) {
        return status == Appointment.AppointmentStatus.PENDING
            || status == Appointment.AppointmentStatus.CONFIRMED;
    }

    // =========================================================
    // RELATION VALIDATION
    // =========================================================

    /**
     * Verifica que todas las entidades de la reserva
     * pertenecen al mismo negocio.
     */
    private void validateAppointmentRelations(
        Appointment appointment
    ) {
        validateAppointment(appointment);

        validateManagementRelations(
            appointment.getBusiness(),
            appointment.getCustomer(),
            appointment.getProfessional(),
            appointment.getService()
        );
    }

    private void validateManagementRelations(
        Business business,
        Customer customer,
        Professional professional,
        Service service
    ) {
        if (
            business == null ||
            business.getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Business válido."
            );
        }

        Long businessId = business.getId();

        if (
            customer == null ||
            customer.getId() == null ||
            customer.getBusinessId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Customer válido."
            );
        }

        if (
            !Objects.equals(
                customer.getBusinessId(),
                businessId
            )
        ) {
            throw new IllegalStateException(
                "El Customer de la reserva pertenece a otro negocio."
            );
        }

        if (
            professional == null ||
            professional.getId() == null ||
            professional.getBusiness() == null ||
            professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Professional válido."
            );
        }

        if (
            !Objects.equals(
                professional.getBusiness().getId(),
                businessId
            )
        ) {
            throw new IllegalStateException(
                "El Professional de la reserva pertenece "
                    + "a otro negocio."
            );
        }

        if (
            service == null ||
            service.getId() == null ||
            service.getBusiness() == null ||
            service.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Service válido."
            );
        }

        if (
            !Objects.equals(
                service.getBusiness().getId(),
                businessId
            )
        ) {
            throw new IllegalStateException(
                "El Service de la reserva pertenece a otro negocio."
            );
        }
    }

    // =========================================================
    // APPOINTMENT VALIDATION
    // =========================================================

    private void validateAppointment(
        Appointment appointment
    ) {
        if (
            appointment == null ||
            appointment.getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva asociada al token no es válida."
            );
        }

        if (
            appointment.getBusiness() == null ||
            appointment.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Business válido."
            );
        }

        if (
            appointment.getCustomer() == null ||
            appointment.getCustomer().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Customer válido."
            );
        }

        if (
            appointment.getProfessional() == null ||
            appointment.getProfessional().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Professional válido."
            );
        }

        if (
            appointment.getService() == null ||
            appointment.getService().getId() == null
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un Service válido."
            );
        }

        if (appointment.getStatus() == null) {
            throw new IllegalStateException(
                "La reserva no tiene estado."
            );
        }

        if (
            appointment.getStartAt() == null ||
            appointment.getEndAt() == null ||
            !appointment.getEndAt().isAfter(
                appointment.getStartAt()
            )
        ) {
            throw new IllegalStateException(
                "La reserva no tiene un intervalo temporal válido."
            );
        }
    }

    private void validatePositiveId(
        Long value,
        String fieldName
    ) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                fieldName + " debe ser mayor que cero."
            );
        }
    }

    // =========================================================
    // MANAGEMENT RESPONSE
    // =========================================================

    private PublicBookingManagementResponse buildManagementResponse(
        Appointment appointment,
        BusinessSettings settings,
        ZoneId businessZone,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean cancellationAllowed,
        boolean reschedulingAllowed,
        OffsetDateTime cancellationDeadline,
        OffsetDateTime reschedulingDeadline
    ) {
        validateAppointmentRelations(appointment);

        if (settings == null || businessZone == null) {
            throw new IllegalStateException(
                "No se puede construir la respuesta de gestión."
            );
        }

        Business business = appointment.getBusiness();
        Customer customer = appointment.getCustomer();
        Professional professional = appointment.getProfessional();
        Service service = appointment.getService();

        return new PublicBookingManagementResponse(
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
            startAt,
            endAt,
            businessZone.getId(),
            settings.isOnlineCancellationEnabled(),
            settings.getMinimumCancellationNoticeMinutes(),
            settings.isOnlineReschedulingEnabled(),
            settings.getMinimumRescheduleNoticeMinutes(),
            cancellationAllowed,
            reschedulingAllowed,
            cancellationDeadline,
            reschedulingDeadline
        );
    }

    // =========================================================
    // CANCEL RESPONSE
    // =========================================================

    private PublicBookingCancelResponse buildCancelResponse(
        Appointment appointment,
        ZoneId businessZone,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime cancelledAt
    ) {
        validateAppointmentRelations(appointment);

        if (
            businessZone == null ||
            startAt == null ||
            endAt == null ||
            cancelledAt == null
        ) {
            throw new IllegalStateException(
                "No se puede construir la respuesta de cancelación."
            );
        }

        Business business = appointment.getBusiness();
        Customer customer = appointment.getCustomer();
        Professional professional = appointment.getProfessional();
        Service service = appointment.getService();

        return new PublicBookingCancelResponse(
            appointment.getId(),
            appointment.getStatus(),
            business.getId(),
            business.getName(),
            service.getId(),
            service.getName(),
            professional.getId(),
            professional.getDisplayName(),
            customer.getFullName(),
            startAt,
            endAt,
            businessZone.getId(),
            cancelledAt,
            BOOKING_CANCELLED_MESSAGE
        );
    }

    // =========================================================
    // RESCHEDULE RESPONSE
    // =========================================================

    private PublicBookingRescheduleResponse buildRescheduleResponse(
        Appointment appointment,
        ZoneId businessZone,
        OffsetDateTime previousStartAt,
        OffsetDateTime previousEndAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime rescheduledAt
    ) {
        validateAppointmentRelations(appointment);

        if (
            businessZone == null ||
            previousStartAt == null ||
            previousEndAt == null ||
            startAt == null ||
            endAt == null ||
            rescheduledAt == null
        ) {
            throw new IllegalStateException(
                "No se puede construir la respuesta de reprogramación."
            );
        }

        Business business = appointment.getBusiness();
        Customer customer = appointment.getCustomer();
        Professional professional = appointment.getProfessional();
        Service service = appointment.getService();

        return new PublicBookingRescheduleResponse(
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
            previousStartAt,
            previousEndAt,
            startAt,
            endAt,
            businessZone.getId(),
            rescheduledAt,
            true,
            BOOKING_RESCHEDULED_MESSAGE
        );
    }

    // =========================================================
    // BUSINESS TIMEZONE
    // =========================================================

    private ZoneId resolveBusinessZone(Business business) {
        if (business == null) {
            throw new IllegalArgumentException(
                "Business es obligatorio."
            );
        }

        String timezone = business.getTimezone();

        if (
            timezone == null ||
            timezone.isBlank()
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

    private OffsetDateTime toBusinessOffset(
        OffsetDateTime dateTime,
        ZoneId businessZone
    ) {
        if (dateTime == null || businessZone == null) {
            throw new IllegalStateException(
                "No se puede convertir la fecha de la reserva."
            );
        }

        return dateTime
            .atZoneSameInstant(businessZone)
            .toOffsetDateTime();
    }
}
