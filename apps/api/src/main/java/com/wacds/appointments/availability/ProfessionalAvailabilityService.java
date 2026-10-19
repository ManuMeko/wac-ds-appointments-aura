package com.wacds.appointments.availability;

import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.availability.dto.ProfessionalAvailabilityRequest;
import com.wacds.appointments.availability.dto.ProfessionalAvailabilityResponse;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfessionalAvailabilityService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalAvailabilityRepository
        availabilityRepository;

    private final ProfessionalRepository
        professionalRepository;

    private final ProfessionalAvailabilityMapper
        availabilityMapper;

    private final BusinessMembershipService
        businessMembershipService;

    /*
     * Reutilizamos exactamente el mismo lock de agenda
     * utilizado por AppointmentService y PublicBookingService.
     *
     * De esta forma, para un mismo profesional:
     *
     * - reservar una cita;
     * - reprogramar una cita;
     * - crear disponibilidad;
     * - eliminar disponibilidad;
     *
     * comparten el mismo protocolo de concurrencia.
     */
    private final AppointmentRepository
        appointmentRepository;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea una nueva franja de disponibilidad semanal.
     *
     * Gestión estructural:
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     *
     * Reglas:
     *
     * - El profesional debe pertenecer al Business actual.
     * - El Business debe estar activo.
     * - startTime debe ser anterior a endTime.
     * - No puede solaparse con otra disponibilidad activa.
     * - La modificación se serializa mediante lock de agenda.
     */
    @Transactional
    public ProfessionalAvailabilityResponse create(
        Long professionalId,
        ProfessionalAvailabilityRequest request
    ) {

        validateProfessionalId(
            professionalId
        );

        validateRequest(
            request
        );

        validateTimeRange(
            request
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        Professional professional =
            findProfessionalInCurrentBusiness(
                professionalId,
                businessId
            );

        /*
         * IMPORTANTE:
         *
         * El lock debe adquirirse antes de comprobar
         * solapamientos y antes de persistir.
         *
         * Esto evita carreras entre:
         *
         * - creación de citas;
         * - reprogramaciones;
         * - booking público;
         * - cambios de disponibilidad.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        boolean overlaps =
            availabilityRepository
                .existsOverlappingAvailability(
                    professionalId,
                    request.dayOfWeek(),
                    request.startTime(),
                    request.endTime()
                );

        if (overlaps) {
            throw new ConflictException(
                "El horario "
                    + request.startTime()
                    + " - "
                    + request.endTime()
                    + " se solapa con otro horario activo "
                    + "del profesional "
                    + professionalId
                    + " para "
                    + request.dayOfWeek()
                    + "."
            );
        }

        ProfessionalAvailability availability =
            availabilityMapper.toEntity(
                request,
                professional
            );

        if (availability == null) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la disponibilidad del profesional."
            );
        }

        ProfessionalAvailability saved =
            availabilityRepository.save(
                availability
            );

        validateAvailabilityBelongsToBusiness(
            saved,
            businessId
        );

        ProfessionalAvailabilityResponse response =
            availabilityMapper.toResponse(
                saved
            );

        if (response == null) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la respuesta de disponibilidad."
            );
        }

        return response;
    }

    // =========================================================
    // FIND ALL BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve todas las franjas de disponibilidad
     * configuradas para un profesional.
     *
     * Incluye activas e inactivas.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional(readOnly = true)
    public List<ProfessionalAvailabilityResponse>
        findAllByProfessional(
            Long professionalId
        ) {

        validateProfessionalId(
            professionalId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        findProfessionalInCurrentBusiness(
            professionalId,
            businessId
        );

        List<ProfessionalAvailability> availabilities =
            availabilityRepository
                .findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(
                    professionalId
                );

        validateAvailabilitiesBelongToBusiness(
            availabilities,
            businessId
        );

        return availabilities
            .stream()
            .map(
                availabilityMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // FIND ACTIVE BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve únicamente las franjas activas.
     *
     * Esta API sigue siendo PRIVADA y pertenece
     * a la configuración del profesional.
     *
     * La disponibilidad pública de slots continúa
     * siendo responsabilidad de AvailabilitySlotService.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional(readOnly = true)
    public List<ProfessionalAvailabilityResponse>
        findActiveByProfessional(
            Long professionalId
        ) {

        validateProfessionalId(
            professionalId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        findProfessionalInCurrentBusiness(
            professionalId,
            businessId
        );

        List<ProfessionalAvailability> availabilities =
            availabilityRepository
                .findAllByProfessionalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                    professionalId
                );

        validateAvailabilitiesBelongToBusiness(
            availabilities,
            businessId
        );

        return availabilities
            .stream()
            .map(
                availabilityMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Busca una disponibilidad por id.
     *
     * Aunque availabilityId sea un identificador global,
     * nunca devolvemos el resultado sin comprobar antes
     * que pertenece al Business de la sesión.
     */
    @Transactional(readOnly = true)
    public ProfessionalAvailabilityResponse findById(
        Long availabilityId
    ) {

        validateAvailabilityId(
            availabilityId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        ProfessionalAvailability availability =
            availabilityRepository
                .findById(
                    availabilityId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún horario con id: "
                            + availabilityId
                    )
                );

        validateAvailabilityBelongsToBusiness(
            availability,
            businessId
        );

        ProfessionalAvailabilityResponse response =
            availabilityMapper.toResponse(
                availability
            );

        if (response == null) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la respuesta de disponibilidad."
            );
        }

        return response;
    }

    // =========================================================
    // FIND BY PROFESSIONAL AND ID
    // =========================================================

    /**
     * Busca una disponibilidad concreta
     * perteneciente al profesional indicado.
     */
    @Transactional(readOnly = true)
    public ProfessionalAvailabilityResponse
        findByProfessionalAndId(
            Long professionalId,
            Long availabilityId
        ) {

        validateProfessionalId(
            professionalId
        );

        validateAvailabilityId(
            availabilityId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        findProfessionalInCurrentBusiness(
            professionalId,
            businessId
        );

        ProfessionalAvailability availability =
            availabilityRepository
                .findByIdAndProfessionalId(
                    availabilityId,
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe el horario "
                            + availabilityId
                            + " para el profesional "
                            + professionalId
                            + "."
                    )
                );

        validateAvailabilityBelongsToBusiness(
            availability,
            businessId
        );

        ProfessionalAvailabilityResponse response =
            availabilityMapper.toResponse(
                availability
            );

        if (response == null) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la respuesta de disponibilidad."
            );
        }

        return response;
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Elimina una franja semanal.
     *
     * Como eliminar disponibilidad cambia las reglas
     * de agenda del profesional, utiliza el mismo lock
     * que las reservas.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional
    public void delete(
        Long professionalId,
        Long availabilityId
    ) {

        validateProfessionalId(
            professionalId
        );

        validateAvailabilityId(
            availabilityId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        findProfessionalInCurrentBusiness(
            professionalId,
            businessId
        );

        /*
         * Serializamos la mutación de agenda antes
         * de recuperar y eliminar la disponibilidad.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        ProfessionalAvailability availability =
            availabilityRepository
                .findByIdAndProfessionalId(
                    availabilityId,
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe el horario "
                            + availabilityId
                            + " para el profesional "
                            + professionalId
                            + "."
                    )
                );

        validateAvailabilityBelongsToBusiness(
            availability,
            businessId
        );

        availabilityRepository.delete(
            availability
        );
    }

    // =========================================================
    // PROFESSIONAL LOOKUP · TENANT SAFE
    // =========================================================

    /**
     * Recupera un profesional y comprueba
     * que pertenece exactamente al Business
     * seleccionado en la sesión.
     */
    private Professional findProfessionalInCurrentBusiness(
        Long professionalId,
        Long businessId
    ) {

        Professional professional =
            professionalRepository
                .findById(
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún profesional con id: "
                            + professionalId
                    )
                );

        validateProfessionalBelongsToBusiness(
            professional,
            businessId
        );

        return professional;
    }

    // =========================================================
    // PROFESSIONAL TENANT VALIDATION
    // =========================================================

    private void validateProfessionalBelongsToBusiness(
        Professional professional,
        Long expectedBusinessId
    ) {

        if (
            expectedBusinessId == null
                || expectedBusinessId <= 0
        ) {
            throw new IllegalStateException(
                "El Business esperado no es válido."
            );
        }

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no es válido."
            );
        }

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no tiene "
                    + "un negocio válido asociado."
            );
        }

        Long professionalBusinessId =
            professional
                .getBusiness()
                .getId();

        if (
            !Objects.equals(
                professionalBusinessId,
                expectedBusinessId
            )
        ) {

            /*
             * No revelamos a qué Business pertenece
             * realmente el recurso solicitado.
             */
            throw new ResourceNotFoundException(
                "No existe el profesional "
                    + professional.getId()
                    + " dentro del negocio actual."
            );
        }

        if (
            !professional
                .getBusiness()
                .isActive()
        ) {
            throw new ConflictException(
                "El negocio del profesional no está activo."
            );
        }
    }

    // =========================================================
    // AVAILABILITY TENANT VALIDATION
    // =========================================================

    /**
     * Valida que una disponibilidad esté asociada
     * a un profesional perteneciente al tenant actual.
     */
    private void validateAvailabilityBelongsToBusiness(
        ProfessionalAvailability availability,
        Long expectedBusinessId
    ) {

        if (
            availability == null
                || availability.getId() == null
        ) {
            throw new IllegalStateException(
                "La disponibilidad no es válida."
            );
        }

        Professional professional =
            availability.getProfessional();

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalStateException(
                "La disponibilidad no tiene "
                    + "un profesional válido asociado."
            );
        }

        validateProfessionalBelongsToBusiness(
            professional,
            expectedBusinessId
        );
    }

    /**
     * Defensa adicional para resultados
     * devueltos como colección.
     */
    private void validateAvailabilitiesBelongToBusiness(
        List<ProfessionalAvailability> availabilities,
        Long expectedBusinessId
    ) {

        if (
            availabilities == null
                || availabilities.isEmpty()
        ) {
            return;
        }

        for (
            ProfessionalAvailability availability :
            availabilities
        ) {

            validateAvailabilityBelongsToBusiness(
                availability,
                expectedBusinessId
            );
        }
    }

    // =========================================================
    // CONCURRENCY LOCK
    // =========================================================

    /**
     * Bloquea la fila del profesional mediante
     * SELECT ... FOR UPDATE.
     *
     * Este protocolo debe ser común a toda operación
     * que cambie las reglas de agenda de un profesional.
     */
    private void lockProfessionalSchedule(
        Long businessId,
        Long professionalId
    ) {

        if (
            businessId == null
                || businessId <= 0
        ) {
            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }

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
    // SECURITY
    // =========================================================

    /**
     * Recupera el principal generado por
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
                || !(
                    authentication.getPrincipal()
                        instanceof AuthenticatedUser authenticatedUser
                )
        ) {
            throw new BadCredentialsException(
                "No existe una sesión autenticada válida."
            );
        }

        if (
            authenticatedUser.getUserId() == null
                || authenticatedUser.getUserId() <= 0
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
                || authenticatedUser.getBusinessId() <= 0
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        if (
            authenticatedUser.getRole() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un rol válido."
            );
        }

        return authenticatedUser;
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        ProfessionalAvailabilityRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "Los datos de disponibilidad son obligatorios."
            );
        }

        if (
            request.dayOfWeek() == null
        ) {
            throw new IllegalArgumentException(
                "El día de la semana es obligatorio."
            );
        }

        if (
            request.startTime() == null
        ) {
            throw new IllegalArgumentException(
                "La hora de inicio es obligatoria."
            );
        }

        if (
            request.endTime() == null
        ) {
            throw new IllegalArgumentException(
                "La hora de finalización es obligatoria."
            );
        }
    }

    // =========================================================
    // TIME RANGE VALIDATION
    // =========================================================

    private void validateTimeRange(
        ProfessionalAvailabilityRequest request
    ) {

        /*
         * validateRequest(...) garantiza previamente
         * que startTime y endTime existen.
         */
        if (
            !request
                .startTime()
                .isBefore(
                    request.endTime()
                )
        ) {
            throw new IllegalArgumentException(
                "La hora de inicio debe ser anterior "
                    + "a la hora de finalización."
            );
        }
    }

    // =========================================================
    // ID VALIDATION
    // =========================================================

    private void validateProfessionalId(
        Long professionalId
    ) {

        if (
            professionalId == null
        ) {
            throw new IllegalArgumentException(
                "professionalId es obligatorio."
            );
        }

        if (
            professionalId <= 0
        ) {
            throw new IllegalArgumentException(
                "professionalId debe ser mayor que 0."
            );
        }
    }

    private void validateAvailabilityId(
        Long availabilityId
    ) {

        if (
            availabilityId == null
        ) {
            throw new IllegalArgumentException(
                "availabilityId es obligatorio."
            );
        }

        if (
            availabilityId <= 0
        ) {
            throw new IllegalArgumentException(
                "availabilityId debe ser mayor que 0."
            );
        }
    }
}