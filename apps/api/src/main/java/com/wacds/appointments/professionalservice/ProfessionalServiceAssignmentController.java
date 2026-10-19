package com.wacds.appointments.professionalservice;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.professionalservice.dto.ProfessionalServiceAssignmentResponse;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * API de gestión de asignaciones entre Professional y Service.
 *
 * Este controller pertenece exclusivamente al contexto
 * autenticado Business.
 *
 * =========================================================
 * RESPONSABILIDADES
 * =========================================================
 *
 * Permite:
 *
 * - asignar un servicio a un profesional;
 * - consultar servicios asignados a un profesional;
 * - eliminar una asignación.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Las operaciones de lectura requieren acceso operativo:
 *
 * - OWNER;
 * - ADMIN;
 * - STAFF.
 *
 * Las operaciones estructurales requieren:
 *
 * - OWNER;
 * - ADMIN.
 *
 * Todos los recursos deben pertenecer exactamente al
 * Business autenticado.
 *
 * =========================================================
 * API PÚBLICA
 * =========================================================
 *
 * Este controller NO expone endpoints públicos.
 *
 * La selección pública de servicios y profesionales debe
 * resolverse mediante una fachada pública específica que
 * reutilice la capa de dominio sin exponer las operaciones
 * administrativas de este controller.
 */
@RestController
@RequiredArgsConstructor
public class ProfessionalServiceAssignmentController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalServiceAssignmentService
        assignmentService;

    private final BusinessMembershipService
        businessMembershipService;

    private final ProfessionalRepository
        professionalRepository;

    private final ServiceRepository
        serviceRepository;

    // =========================================================
    // ASSIGN SERVICE
    // =========================================================

    /**
     * Asigna un servicio a un profesional.
     *
     * POST
     * /api/v1/professionals/{professionalId}/services/{serviceId}
     *
     * Roles:
     *
     * - OWNER;
     * - ADMIN.
     */
    @PostMapping(
        "/api/v1/professionals/{professionalId}/services/{serviceId}"
    )
    public ResponseEntity<ProfessionalServiceAssignmentResponse>
        assign(

            @PathVariable
            Long professionalId,

            @PathVariable
            Long serviceId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        validateProfessionalId(
            professionalId
        );

        validateServiceId(
            serviceId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        Professional professional =
            requireProfessionalInBusiness(
                professionalId,
                businessId
            );

        Service service =
            requireServiceInBusiness(
                serviceId,
                businessId
            );

        validateSameBusiness(
            professional,
            service,
            businessId
        );

        ProfessionalServiceAssignmentResponse response =
            assignmentService.assign(
                professionalId,
                serviceId
            );

        validateAssignmentResponse(
            response,
            professionalId,
            serviceId
        );

        return ResponseEntity
            .status(
                HttpStatus.CREATED
            )
            .body(
                response
            );
    }

    // =========================================================
    // FIND SERVICES BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve los servicios asignados a un profesional.
     *
     * GET
     * /api/v1/professionals/{professionalId}/services
     *
     * Roles:
     *
     * - OWNER;
     * - ADMIN;
     * - STAFF.
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}/services"
    )
    public ResponseEntity<
        List<ProfessionalServiceAssignmentResponse>
    > findServicesByProfessional(

        @PathVariable
        Long professionalId,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        validateProfessionalId(
            professionalId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireOperationalAccess(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        List<ProfessionalServiceAssignmentResponse> response =
            assignmentService
                .findServicesByProfessional(
                    professionalId
                );

        validateAssignmentList(
            response,
            professionalId
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // REMOVE SERVICE
    // =========================================================

    /**
     * Elimina una asignación Professional <-> Service.
     *
     * DELETE
     * /api/v1/professionals/{professionalId}/services/{serviceId}
     *
     * Roles:
     *
     * - OWNER;
     * - ADMIN.
     */
    @DeleteMapping(
        "/api/v1/professionals/{professionalId}/services/{serviceId}"
    )
    public ResponseEntity<Void> remove(

        @PathVariable
        Long professionalId,

        @PathVariable
        Long serviceId,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        validateProfessionalId(
            professionalId
        );

        validateServiceId(
            serviceId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        Professional professional =
            requireProfessionalInBusiness(
                professionalId,
                businessId
            );

        Service service =
            requireServiceInBusiness(
                serviceId,
                businessId
            );

        validateSameBusiness(
            professional,
            service,
            businessId
        );

        assignmentService.remove(
            professionalId,
            serviceId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    // =========================================================
    // SECURITY · CURRENT BUSINESS
    // =========================================================

    /**
     * Obtiene el Business activo del principal autenticado.
     *
     * El tenant nunca se deriva de professionalId
     * ni de serviceId.
     */
    private Long requireCurrentBusinessId(

        AuthenticatedUser authenticatedUser

    ) {

        if (
            authenticatedUser == null
                || authenticatedUser.getBusinessId() == null
        ) {

            throw new AccessDeniedException(
                "No existe un contexto de negocio autenticado válido."
            );
        }

        Long businessId =
            authenticatedUser.getBusinessId();

        if (
            businessId <= 0
        ) {

            throw new AccessDeniedException(
                "El businessId de la sesión no es válido."
            );
        }

        return businessId;
    }

    // =========================================================
    // SECURITY · OPERATIONAL ACCESS
    // =========================================================

    /**
     * Acceso operativo:
     *
     * - OWNER;
     * - ADMIN;
     * - STAFF.
     */
    private void requireOperationalAccess(

        AuthenticatedUser authenticatedUser,
        Long businessId

    ) {

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );
    }

    // =========================================================
    // SECURITY · BUSINESS MANAGEMENT
    // =========================================================

    /**
     * Gestión estructural:
     *
     * - OWNER;
     * - ADMIN.
     */
    private void requireBusinessManagement(

        AuthenticatedUser authenticatedUser,
        Long businessId

    ) {

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );
    }

    // =========================================================
    // TENANT-SAFE PROFESSIONAL LOOKUP
    // =========================================================

    /**
     * Busca el profesional y comprueba que pertenezca
     * exactamente al Business autenticado.
     *
     * Un professionalId perteneciente a otro tenant
     * se comporta como recurso inexistente.
     */
    private Professional requireProfessionalInBusiness(

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

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
                || !Objects.equals(
                    professional.getBusiness().getId(),
                    businessId
                )
        ) {

            throw new ResourceNotFoundException(
                "No existe el profesional "
                    + professionalId
                    + " dentro del negocio actual."
            );
        }

        return professional;
    }

    // =========================================================
    // TENANT-SAFE SERVICE LOOKUP
    // =========================================================

    /**
     * Busca el servicio y comprueba que pertenezca
     * exactamente al Business autenticado.
     *
     * Un serviceId perteneciente a otro tenant
     * se comporta como recurso inexistente.
     */
    private Service requireServiceInBusiness(

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

        if (
            service.getBusiness() == null
                || service.getBusiness().getId() == null
                || !Objects.equals(
                    service.getBusiness().getId(),
                    businessId
                )
        ) {

            throw new ResourceNotFoundException(
                "No existe el servicio "
                    + serviceId
                    + " dentro del negocio actual."
            );
        }

        return service;
    }

    // =========================================================
    // BUSINESS RELATION VALIDATION
    // =========================================================

    /**
     * Defensa adicional:
     *
     * Professional y Service deben pertenecer exactamente
     * al mismo Business autenticado.
     */
    private void validateSameBusiness(

        Professional professional,
        Service service,
        Long expectedBusinessId

    ) {

        if (
            professional == null
                || professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {

            throw new AccessDeniedException(
                "El profesional no tiene un negocio válido asociado."
            );
        }

        if (
            service == null
                || service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {

            throw new AccessDeniedException(
                "El servicio no tiene un negocio válido asociado."
            );
        }

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
                expectedBusinessId
            )
        ) {

            throw new AccessDeniedException(
                "El profesional no pertenece al negocio autenticado."
            );
        }

        if (
            !Objects.equals(
                serviceBusinessId,
                expectedBusinessId
            )
        ) {

            throw new AccessDeniedException(
                "El servicio no pertenece al negocio autenticado."
            );
        }

        if (
            !Objects.equals(
                professionalBusinessId,
                serviceBusinessId
            )
        ) {

            throw new AccessDeniedException(
                "El profesional y el servicio pertenecen "
                    + "a negocios diferentes."
            );
        }
    }

    // =========================================================
    // RESPONSE DEFENCE · SINGLE
    // =========================================================

    /**
     * Defensa frente a una respuesta inconsistente
     * devuelta accidentalmente por la capa de servicio.
     */
    private void validateAssignmentResponse(

        ProfessionalServiceAssignmentResponse response,
        Long expectedProfessionalId,
        Long expectedServiceId

    ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "La asignación creada no contiene una respuesta válida."
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
                "La asignación creada contiene "
                    + "un professionalId inesperado."
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
                "La asignación creada contiene "
                    + "un serviceId inesperado."
            );
        }
    }

    // =========================================================
    // RESPONSE DEFENCE · LIST
    // =========================================================

    /**
     * Todas las asignaciones devueltas deben
     * pertenecer al profesional solicitado.
     */
    private void validateAssignmentList(

        List<ProfessionalServiceAssignmentResponse> assignments,
        Long expectedProfessionalId

    ) {

        if (
            assignments == null
                || assignments.isEmpty()
        ) {

            return;
        }

        for (
            ProfessionalServiceAssignmentResponse assignment
                : assignments
        ) {

            if (
                assignment == null
            ) {

                throw new IllegalStateException(
                    "La consulta de servicios del profesional "
                        + "ha devuelto una asignación nula."
                );
            }

            if (
                assignment.professionalId() == null
                    || !Objects.equals(
                        assignment.professionalId(),
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
}