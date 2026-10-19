package com.wacds.appointments.scheduleexception;

import com.wacds.appointments.appointment.AppointmentRepository;
import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionRequest;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleExceptionService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ScheduleExceptionRepository
        scheduleExceptionRepository;

    private final ProfessionalRepository
        professionalRepository;

    private final ScheduleExceptionMapper
        scheduleExceptionMapper;

    private final BusinessMembershipService
        businessMembershipService;

    /*
     * Reutilizamos exactamente el mismo lock de agenda
     * utilizado por:
     *
     * - PublicBookingService
     * - AppointmentService
     * - ProfessionalAvailabilityService
     *
     * De esta forma cualquier mutación que afecte
     * a la agenda del mismo profesional queda serializada
     * en PostgreSQL.
     */
    private final AppointmentRepository
        appointmentRepository;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea una excepción de horario.
     *
     * Gestión estructural:
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     *
     * Tipos soportados:
     *
     * - UNAVAILABLE
     * - CUSTOM_HOURS
     *
     * Como una excepción modifica directamente
     * la disponibilidad real del profesional,
     * participa en el mismo protocolo de locking
     * que las reservas y la disponibilidad semanal.
     */
    @Transactional
    public ScheduleExceptionResponse create(
        Long professionalId,
        ScheduleExceptionRequest request
    ) {

        validateProfessionalId(
            professionalId
        );

        validateRequest(
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
         * El lock debe adquirirse ANTES de comprobar
         * conflictos.
         *
         * Así evitamos:
         *
         * - dos excepciones incompatibles creadas
         *   simultáneamente;
         * - una excepción compitiendo con una reserva;
         * - una excepción compitiendo con un cambio
         *   de disponibilidad semanal.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        /*
         * Una excepción explícitamente inactiva
         * puede persistirse sin participar en las
         * restricciones de conflicto activas.
         */
        if (
            !Boolean.FALSE.equals(
                request.active()
            )
        ) {

            validateConflicts(
                professionalId,
                request
            );
        }

        ScheduleException scheduleException =
            scheduleExceptionMapper.toEntity(
                request,
                professional
            );

        if (
            scheduleException == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la excepción de horario."
            );
        }

        ScheduleException saved =
            scheduleExceptionRepository.save(
                scheduleException
            );

        validateScheduleExceptionBelongsToBusiness(
            saved,
            businessId
        );

        return mapToResponse(
            saved
        );
    }

    // =========================================================
    // FIND ALL BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve todas las excepciones del profesional,
     * tanto activas como inactivas.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse>
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

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findAllByProfessionalIdOrderByExceptionDateAscStartTimeAsc(
                    professionalId
                );

        validateScheduleExceptionsBelongToBusiness(
            exceptions,
            businessId
        );

        return exceptions
            .stream()
            .map(
                this::mapToResponse
            )
            .toList();
    }

    // =========================================================
    // FIND ACTIVE BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve únicamente las excepciones activas
     * configuradas para el profesional.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse>
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

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findAllByProfessionalIdAndActiveTrueOrderByExceptionDateAscStartTimeAsc(
                    professionalId
                );

        validateScheduleExceptionsBelongToBusiness(
            exceptions,
            businessId
        );

        return exceptions
            .stream()
            .map(
                this::mapToResponse
            )
            .toList();
    }

    // =========================================================
    // FIND ACTIVE BY PROFESSIONAL AND DATE
    // =========================================================

    /**
     * Devuelve las excepciones activas
     * del profesional para una fecha concreta.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse>
        findActiveByProfessionalAndDate(
            Long professionalId,
            LocalDate exceptionDate
        ) {

        validateProfessionalId(
            professionalId
        );

        validateExceptionDate(
            exceptionDate
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

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findActiveByProfessionalIdAndExceptionDate(
                    professionalId,
                    exceptionDate
                );

        validateScheduleExceptionsBelongToBusiness(
            exceptions,
            businessId
        );

        return exceptions
            .stream()
            .map(
                this::mapToResponse
            )
            .toList();
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Busca una excepción mediante su id global.
     *
     * Nunca devolvemos directamente el recurso:
     * antes comprobamos que pertenece al Business
     * autenticado.
     */
    @Transactional(readOnly = true)
    public ScheduleExceptionResponse findById(
        Long scheduleExceptionId
    ) {

        validateScheduleExceptionId(
            scheduleExceptionId
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

        ScheduleException scheduleException =
            scheduleExceptionRepository
                .findById(
                    scheduleExceptionId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ninguna excepción de horario con id: "
                            + scheduleExceptionId
                    )
                );

        validateScheduleExceptionBelongsToBusiness(
            scheduleException,
            businessId
        );

        return mapToResponse(
            scheduleException
        );
    }

    // =========================================================
    // FIND BY PROFESSIONAL AND ID
    // =========================================================

    /**
     * Busca una excepción concreta perteneciente
     * al profesional indicado.
     */
    @Transactional(readOnly = true)
    public ScheduleExceptionResponse
        findByProfessionalAndId(
            Long professionalId,
            Long scheduleExceptionId
        ) {

        validateProfessionalId(
            professionalId
        );

        validateScheduleExceptionId(
            scheduleExceptionId
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

        ScheduleException scheduleException =
            scheduleExceptionRepository
                .findByIdAndProfessionalId(
                    scheduleExceptionId,
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe la excepción de horario "
                            + scheduleExceptionId
                            + " para el profesional "
                            + professionalId
                            + "."
                    )
                );

        validateScheduleExceptionBelongsToBusiness(
            scheduleException,
            businessId
        );

        return mapToResponse(
            scheduleException
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Elimina una excepción de horario.
     *
     * Como modifica la disponibilidad efectiva
     * del profesional, utiliza el mismo lock
     * que el resto de operaciones de agenda.
     *
     * OWNER ✅
     * ADMIN ✅
     * STAFF ❌
     */
    @Transactional
    public void delete(
        Long professionalId,
        Long scheduleExceptionId
    ) {

        validateProfessionalId(
            professionalId
        );

        validateScheduleExceptionId(
            scheduleExceptionId
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
         * Serializamos antes de leer y eliminar
         * la excepción.
         */
        lockProfessionalSchedule(
            businessId,
            professionalId
        );

        ScheduleException scheduleException =
            scheduleExceptionRepository
                .findByIdAndProfessionalId(
                    scheduleExceptionId,
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe la excepción de horario "
                            + scheduleExceptionId
                            + " para el profesional "
                            + professionalId
                            + "."
                    )
                );

        validateScheduleExceptionBelongsToBusiness(
            scheduleException,
            businessId
        );

        scheduleExceptionRepository.delete(
            scheduleException
        );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        ScheduleExceptionRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "Los datos de la excepción de horario son obligatorios."
            );
        }

        if (
            request.type() == null
        ) {
            throw new IllegalArgumentException(
                "El tipo de excepción es obligatorio."
            );
        }

        if (
            request.exceptionDate() == null
        ) {
            throw new IllegalArgumentException(
                "La fecha de la excepción es obligatoria."
            );
        }

        /*
         * UNAVAILABLE representa indisponibilidad
         * durante el día completo.
         *
         * Por contrato no admite horas parciales.
         */
        if (
            request.type()
                == ScheduleException.ExceptionType.UNAVAILABLE
        ) {

            if (
                request.startTime() != null
                    || request.endTime() != null
            ) {
                throw new IllegalArgumentException(
                    "Una excepción UNAVAILABLE no debe incluir "
                        + "hora de inicio ni hora de finalización."
                );
            }

            return;
        }

        /*
         * CUSTOM_HOURS representa uno o varios
         * intervalos concretos disponibles ese día.
         */
        if (
            request.type()
                == ScheduleException.ExceptionType.CUSTOM_HOURS
        ) {

            if (
                request.startTime() == null
                    || request.endTime() == null
            ) {
                throw new IllegalArgumentException(
                    "Una excepción CUSTOM_HOURS debe incluir "
                        + "hora de inicio y hora de finalización."
                );
            }

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

            return;
        }

        throw new IllegalArgumentException(
            "El tipo de excepción indicado no es válido."
        );
    }

    // =========================================================
    // CONFLICT VALIDATION
    // =========================================================

    /**
     * Valida conflictos entre excepciones activas.
     *
     * Reglas:
     *
     * 1. UNAVAILABLE ocupa el día completo.
     * 2. UNAVAILABLE no puede coexistir con otra excepción activa.
     * 3. CUSTOM_HOURS no puede coexistir con UNAVAILABLE.
     * 4. CUSTOM_HOURS puede tener varias franjas en el mismo día.
     * 5. Las franjas CUSTOM_HOURS no pueden solaparse.
     */
    private void validateConflicts(
        Long professionalId,
        ScheduleExceptionRequest request
    ) {

        LocalDate date =
            request.exceptionDate();

        /*
         * UNAVAILABLE ocupa conceptualmente
         * el día completo.
         */
        if (
            request.type()
                == ScheduleException.ExceptionType.UNAVAILABLE
        ) {

            boolean hasExistingException =
                scheduleExceptionRepository
                    .existsAnyActiveException(
                        professionalId,
                        date
                    );

            if (
                hasExistingException
            ) {
                throw new ConflictException(
                    "El profesional "
                        + professionalId
                        + " ya tiene una excepción activa "
                        + "para la fecha "
                        + date
                        + "."
                );
            }

            return;
        }

        /*
         * Si llegamos aquí, validateRequest(...)
         * ha garantizado que el tipo es CUSTOM_HOURS.
         */
        boolean unavailable =
            scheduleExceptionRepository
                .existsActiveUnavailableException(
                    professionalId,
                    date
                );

        if (
            unavailable
        ) {
            throw new ConflictException(
                "El profesional "
                    + professionalId
                    + " está marcado como no disponible "
                    + "para la fecha "
                    + date
                    + "."
            );
        }

        /*
         * CUSTOM_HOURS puede coexistir con otras
         * franjas del mismo día siempre que
         * no se solapen.
         */
        boolean overlaps =
            scheduleExceptionRepository
                .existsOverlappingCustomHours(
                    professionalId,
                    date,
                    request.startTime(),
                    request.endTime()
                );

        if (
            overlaps
        ) {
            throw new ConflictException(
                "El horario excepcional "
                    + request.startTime()
                    + " - "
                    + request.endTime()
                    + " se solapa con otro horario excepcional "
                    + "del profesional "
                    + professionalId
                    + " para la fecha "
                    + date
                    + "."
            );
        }
    }

    // =========================================================
    // PROFESSIONAL LOOKUP · TENANT SAFE
    // =========================================================

    /**
     * Recupera un profesional y comprueba que pertenece
     * al Business actualmente seleccionado en la sesión.
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
             * realmente el recurso.
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
    // SCHEDULE EXCEPTION TENANT VALIDATION
    // =========================================================

    /**
     * Valida que una excepción pertenezca
     * a un profesional del tenant autenticado.
     */
    private void validateScheduleExceptionBelongsToBusiness(
        ScheduleException scheduleException,
        Long expectedBusinessId
    ) {

        if (
            scheduleException == null
                || scheduleException.getId() == null
        ) {
            throw new IllegalStateException(
                "La excepción de horario no es válida."
            );
        }

        Professional professional =
            scheduleException.getProfessional();

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalStateException(
                "La excepción de horario no tiene "
                    + "un profesional válido asociado."
            );
        }

        validateProfessionalBelongsToBusiness(
            professional,
            expectedBusinessId
        );
    }

    /**
     * Defensa multi-tenant adicional para
     * resultados de colección.
     */
    private void validateScheduleExceptionsBelongToBusiness(
        List<ScheduleException> exceptions,
        Long expectedBusinessId
    ) {

        if (
            exceptions == null
                || exceptions.isEmpty()
        ) {
            return;
        }

        for (
            ScheduleException scheduleException :
            exceptions
        ) {

            validateScheduleExceptionBelongsToBusiness(
                scheduleException,
                expectedBusinessId
            );
        }
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    /**
     * Centraliza el mapping y evita propagar
     * respuestas nulas hacia el controller.
     */
    private ScheduleExceptionResponse mapToResponse(
        ScheduleException scheduleException
    ) {

        if (
            scheduleException == null
        ) {
            throw new IllegalStateException(
                "No se puede mapear una excepción nula."
            );
        }

        ScheduleExceptionResponse response =
            scheduleExceptionMapper.toResponse(
                scheduleException
            );

        if (
            response == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir "
                    + "la respuesta de excepción de horario."
            );
        }

        return response;
    }

    // =========================================================
    // CONCURRENCY LOCK
    // =========================================================

    /**
     * Bloquea la fila del profesional.
     *
     * AppointmentRepository ejecuta:
     *
     * SELECT ... FOR UPDATE
     *
     * sobre professionals.
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
    // ID / DATE VALIDATION
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

    private void validateScheduleExceptionId(
        Long scheduleExceptionId
    ) {

        if (
            scheduleExceptionId == null
        ) {
            throw new IllegalArgumentException(
                "scheduleExceptionId es obligatorio."
            );
        }

        if (
            scheduleExceptionId <= 0
        ) {
            throw new IllegalArgumentException(
                "scheduleExceptionId debe ser mayor que 0."
            );
        }
    }

    private void validateExceptionDate(
        LocalDate exceptionDate
    ) {

        if (
            exceptionDate == null
        ) {
            throw new IllegalArgumentException(
                "exceptionDate es obligatoria."
            );
        }
    }
}