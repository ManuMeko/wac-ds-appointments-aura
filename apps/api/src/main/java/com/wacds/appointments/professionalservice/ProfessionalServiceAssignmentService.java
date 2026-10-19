package com.wacds.appointments.professionalservice;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.professionalservice.dto.ProfessionalServiceAssignmentResponse;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de dominio encargado de gestionar las relaciones
 * entre Professional y Service.
 *
 * =========================================================
 * RESPONSABILIDADES
 * =========================================================
 *
 * Gestiona:
 *
 * - asignación de servicios a profesionales;
 * - consulta de servicios asignados;
 * - eliminación de asignaciones.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Este servicio pertenece al contexto autenticado Business.
 *
 * Las operaciones estructurales requieren:
 *
 * - OWNER;
 * - ADMIN.
 *
 * Las operaciones de lectura operativa permiten:
 *
 * - OWNER;
 * - ADMIN;
 * - STAFF.
 *
 * =========================================================
 * API PÚBLICA
 * =========================================================
 *
 * Este servicio NO debe utilizarse como fachada anónima
 * para la web pública.
 *
 * El catálogo público utilizará una capa específica que
 * consultará las relaciones Professional <-> Service
 * mediante repositorios tenant-safe.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ProfessionalServiceAssignmentService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalServiceAssignmentRepository
        assignmentRepository;

    private final ProfessionalRepository
        professionalRepository;

    private final ServiceRepository
        serviceRepository;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // ASSIGN
    // =========================================================

    /**
     * Asigna un servicio a un profesional.
     *
     * Operación estructural:
     *
     * - OWNER;
     * - ADMIN.
     *
     * El tenant se obtiene exclusivamente desde
     * la sesión autenticada.
     */
    @Transactional
    public ProfessionalServiceAssignmentResponse assign(
        Long professionalId,
        Long serviceId
    ) {

        validateProfessionalId(
            professionalId
        );

        validateServiceId(
            serviceId
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
            findProfessionalInBusiness(
                professionalId,
                businessId
            );

        Service service =
            findServiceInBusiness(
                serviceId,
                businessId
            );

        validateSameBusiness(
            professional,
            service,
            businessId
        );

        ProfessionalServiceAssignmentId assignmentId =
            new ProfessionalServiceAssignmentId(
                professionalId,
                serviceId
            );

        if (
            assignmentRepository.existsById(
                assignmentId
            )
        ) {

            throw new ConflictException(
                "El servicio "
                    + serviceId
                    + " ya está asignado al profesional "
                    + professionalId
                    + "."
            );
        }

        ProfessionalServiceAssignment assignment =
            ProfessionalServiceAssignment
                .builder()
                .id(
                    assignmentId
                )
                .professional(
                    professional
                )
                .service(
                    service
                )
                .build();

        ProfessionalServiceAssignment saved =
            assignmentRepository.save(
                assignment
            );

        ProfessionalServiceAssignmentResponse response =
            toResponse(
                saved
            );

        validateResponse(
            response,
            professionalId,
            serviceId
        );

        return response;
    }

    // =========================================================
    // FIND SERVICES BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve los servicios asignados a un profesional.
     *
     * Lectura operativa:
     *
     * - OWNER;
     * - ADMIN;
     * - STAFF.
     */
    @Transactional(readOnly = true)
    public List<ProfessionalServiceAssignmentResponse>
        findServicesByProfessional(
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
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );

        Professional professional =
            findProfessionalInBusiness(
                professionalId,
                businessId
            );

        validateProfessionalBelongsToBusiness(
            professional,
            businessId
        );

        List<ProfessionalServiceAssignmentResponse> responses =
            assignmentRepository
                .findAllWithServiceByProfessionalId(
                    professionalId
                )
                .stream()
                .map(
                    this::toResponse
                )
                .toList();

        validateResponseList(
            responses,
            professionalId
        );

        return responses;
    }

    // =========================================================
    // REMOVE
    // =========================================================

    /**
     * Elimina la asignación de un servicio
     * a un profesional.
     *
     * Operación estructural:
     *
     * - OWNER;
     * - ADMIN.
     */
    @Transactional
    public void remove(
        Long professionalId,
        Long serviceId
    ) {

        validateProfessionalId(
            professionalId
        );

        validateServiceId(
            serviceId
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
            findProfessionalInBusiness(
                professionalId,
                businessId
            );

        Service service =
            findServiceInBusiness(
                serviceId,
                businessId
            );

        validateSameBusiness(
            professional,
            service,
            businessId
        );

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

            throw new ResourceNotFoundException(
                "El servicio "
                    + serviceId
                    + " no está asignado al profesional "
                    + professionalId
                    + "."
            );
        }

        assignmentRepository.deleteById(
            assignmentId
        );
    }

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    /**
     * Recupera el principal autenticado desde
     * SecurityContext.
     */
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
            !(
                principal
                    instanceof AuthenticatedUser authenticatedUser
            )
        ) {

            throw new AccessDeniedException(
                "El principal autenticado no es válido."
            );
        }

        if (
            authenticatedUser.getUserId() == null
        ) {

            throw new AccessDeniedException(
                "La sesión no contiene un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
        ) {

            throw new AccessDeniedException(
                "La sesión no contiene un businessId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() <= 0
        ) {

            throw new AccessDeniedException(
                "El businessId de la sesión no es válido."
            );
        }

        if (
            authenticatedUser.getRole() == null
        ) {

            throw new AccessDeniedException(
                "La sesión no contiene un rol válido."
            );
        }

        return authenticatedUser;
    }

    // =========================================================
    // PROFESSIONAL LOOKUP
    // =========================================================

    /**
     * Busca un profesional y valida que pertenezca
     * al Business autenticado.
     */
    private Professional findProfessionalInBusiness(
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
    // SERVICE LOOKUP
    // =========================================================

    /**
     * Busca un servicio y valida que pertenezca
     * al Business autenticado.
     */
    private Service findServiceInBusiness(
        Long serviceId,
        Long businessId
    ) {

        Service service =
            serviceRepository
                .findById(
                    serviceId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún servicio con id: "
                            + serviceId
                    )
                );

        validateServiceBelongsToBusiness(
            service,
            businessId
        );

        return service;
    }

    // =========================================================
    // TENANT VALIDATION · PROFESSIONAL
    // =========================================================

    private void validateProfessionalBelongsToBusiness(
        Professional professional,
        Long expectedBusinessId
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

            throw new ResourceNotFoundException(
                "No existe el profesional "
                    + professional.getId()
                    + " dentro del negocio actual."
            );
        }
    }

    // =========================================================
    // TENANT VALIDATION · SERVICE
    // =========================================================

    private void validateServiceBelongsToBusiness(
        Service service,
        Long expectedBusinessId
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

        Long serviceBusinessId =
            service
                .getBusiness()
                .getId();

        if (
            !Objects.equals(
                serviceBusinessId,
                expectedBusinessId
            )
        ) {

            throw new ResourceNotFoundException(
                "No existe el servicio "
                    + service.getId()
                    + " dentro del negocio actual."
            );
        }
    }

    // =========================================================
    // TENANT VALIDATION · RELATION
    // =========================================================

    private void validateSameBusiness(
        Professional professional,
        Service service,
        Long expectedBusinessId
    ) {

        validateProfessionalBelongsToBusiness(
            professional,
            expectedBusinessId
        );

        validateServiceBelongsToBusiness(
            service,
            expectedBusinessId
        );

        Long professionalBusinessId =
            professional
                .getBusiness()
                .getId();

        Long serviceBusinessId =
            service
                .getBusiness()
                .getId();

        if (
            !Objects.equals(
                professionalBusinessId,
                serviceBusinessId
            )
        ) {

            throw new ConflictException(
                "El profesional y el servicio "
                    + "deben pertenecer al mismo negocio."
            );
        }
    }

    // =========================================================
    // RESPONSE VALIDATION · SINGLE
    // =========================================================

    private void validateResponse(
        ProfessionalServiceAssignmentResponse response,
        Long expectedProfessionalId,
        Long expectedServiceId
    ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "La asignación no contiene una respuesta válida."
            );
        }

        if (
            response.professionalId() == null
                || !Objects.equals(
                    response.professionalId(),
                    expectedProfessionalId
                )
        ) {

            throw new IllegalStateException(
                "La asignación contiene un professionalId inesperado."
            );
        }

        if (
            response.serviceId() == null
                || !Objects.equals(
                    response.serviceId(),
                    expectedServiceId
                )
        ) {

            throw new IllegalStateException(
                "La asignación contiene un serviceId inesperado."
            );
        }
    }

    // =========================================================
    // RESPONSE VALIDATION · LIST
    // =========================================================

    private void validateResponseList(
        List<ProfessionalServiceAssignmentResponse> responses,
        Long expectedProfessionalId
    ) {

        if (
            responses == null
                || responses.isEmpty()
        ) {

            return;
        }

        for (
            ProfessionalServiceAssignmentResponse response
                : responses
        ) {

            if (
                response == null
            ) {

                throw new IllegalStateException(
                    "La consulta de asignaciones ha devuelto "
                        + "un elemento nulo."
                );
            }

            if (
                response.professionalId() == null
                    || !Objects.equals(
                        response.professionalId(),
                        expectedProfessionalId
                    )
            ) {

                throw new AccessDeniedException(
                    "La consulta contiene una asignación "
                        + "perteneciente a otro profesional."
                );
            }
        }
    }

    // =========================================================
    // ID VALIDATION · PROFESSIONAL
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

    // =========================================================
    // ID VALIDATION · SERVICE
    // =========================================================

    private void validateServiceId(
        Long serviceId
    ) {

        if (
            serviceId == null
        ) {

            throw new IllegalArgumentException(
                "serviceId es obligatorio."
            );
        }

        if (
            serviceId <= 0
        ) {

            throw new IllegalArgumentException(
                "serviceId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // MAPPING
    // =========================================================

    private ProfessionalServiceAssignmentResponse toResponse(
        ProfessionalServiceAssignment assignment
    ) {

        if (
            assignment == null
        ) {

            throw new IllegalStateException(
                "La asignación no puede ser nula."
            );
        }

        Professional professional =
            assignment.getProfessional();

        Service service =
            assignment.getService();

        if (
            professional == null
                || professional.getId() == null
        ) {

            throw new IllegalStateException(
                "La asignación no contiene "
                    + "un profesional válido."
            );
        }

        if (
            service == null
                || service.getId() == null
        ) {

            throw new IllegalStateException(
                "La asignación no contiene "
                    + "un servicio válido."
            );
        }

        return new ProfessionalServiceAssignmentResponse(
            professional.getId(),
            service.getId(),
            service.getName(),
            service.getSlug(),
            service.getDurationMinutes(),
            service.getPrice(),
            service.isActive(),
            service.isBookingEnabled(),
            assignment.getCreatedAt()
        );
    }
}