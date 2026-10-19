package com.wacds.appointments.professional;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.dto.ProfessionalRequest;
import com.wacds.appointments.professional.dto.ProfessionalResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalRepository professionalRepository;

    private final BusinessRepository businessRepository;

    private final ProfessionalMapper professionalMapper;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea un profesional dentro del negocio.
     *
     * Operación estructural.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * STAFF queda excluido.
     */
    @Transactional
    public ProfessionalResponse create(
        Long businessId,
        ProfessionalRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateRequest(
            request
        );

        requireBusinessManagement(
            businessId
        );

        Business business =
            findActiveBusiness(
                businessId
            );

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        validateEmailAvailable(
            businessId,
            normalizedEmail
        );

        Professional professional =
            professionalMapper.toEntity(
                request,
                business
            );

        if (
            professional == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir el profesional."
            );
        }

        /*
         * Refuerzo multi-tenant.
         */
        professional.setBusiness(
            business
        );

        /*
         * El email queda normalizado independientemente
         * del mapper.
         */
        professional.setEmail(
            normalizedEmail
        );

        /*
         * Coherencia de dominio:
         *
         * un profesional inactivo nunca puede
         * aceptar nuevas reservas.
         */
        if (
            !professional.isActive()
        ) {
            professional.setBookingEnabled(
                false
            );
        }

        Professional savedProfessional =
            professionalRepository.save(
                professional
            );

        validateProfessionalBelongsToBusiness(
            savedProfessional,
            businessId
        );

        return professionalMapper.toResponse(
            savedProfessional
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Actualiza un profesional existente.
     *
     * El profesional nunca puede cambiar de Business.
     *
     * Se actualizan únicamente los campos permitidos
     * por ProfessionalRequest.
     */
    @Transactional
    public ProfessionalResponse update(
        Long businessId,
        Long professionalId,
        ProfessionalRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateProfessionalId(
            professionalId
        );

        validateRequest(
            request
        );

        requireBusinessManagement(
            businessId
        );

        Business business =
            findActiveBusiness(
                businessId
            );

        Professional professional =
            findProfessionalInBusiness(
                businessId,
                professionalId
            );

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        String currentNormalizedEmail =
            normalizeEmail(
                professional.getEmail()
            );

        /*
         * Si el email cambia, comprobamos que el nuevo
         * no esté utilizado por otro profesional.
         *
         * Como el repositorio actual expone la comprobación
         * general por businessId + email, únicamente la
         * ejecutamos cuando el valor realmente cambia.
         */
        if (
            !Objects.equals(
                currentNormalizedEmail,
                normalizedEmail
            )
        ) {
            validateEmailAvailable(
                businessId,
                normalizedEmail
            );
        }

        Professional mappedProfessional =
            professionalMapper.toEntity(
                request,
                business
            );

        if (
            mappedProfessional == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir la actualización "
                    + "del profesional."
            );
        }

        /*
         * Actualización explícita.
         *
         * No copiamos:
         *
         * - id
         * - timestamps
         *
         * y el Business se fija expresamente después.
         */
        professional.setFirstName(
            mappedProfessional.getFirstName()
        );

        professional.setLastName(
            mappedProfessional.getLastName()
        );

        professional.setDisplayName(
            mappedProfessional.getDisplayName()
        );

        professional.setEmail(
            normalizedEmail
        );

        professional.setPhone(
            mappedProfessional.getPhone()
        );

        professional.setBio(
            mappedProfessional.getBio()
        );

        professional.setActive(
            mappedProfessional.isActive()
        );

        professional.setBookingEnabled(
            mappedProfessional.isBookingEnabled()
        );

        /*
         * Defensa multi-tenant.
         */
        professional.setBusiness(
            business
        );

        /*
         * Estado coherente:
         *
         * active=false implica siempre
         * bookingEnabled=false.
         */
        if (
            !professional.isActive()
        ) {
            professional.setBookingEnabled(
                false
            );
        }

        Professional savedProfessional =
            professionalRepository.save(
                professional
            );

        validateProfessionalBelongsToBusiness(
            savedProfessional,
            businessId
        );

        return professionalMapper.toResponse(
            savedProfessional
        );
    }

    // =========================================================
    // ACTIVE STATUS
    // =========================================================

    /**
     * Activa o desactiva un profesional.
     *
     * Desactivar no elimina registros históricos.
     *
     * Al desactivar:
     *
     * bookingEnabled = false
     */
    @Transactional
    public ProfessionalResponse setActive(
        Long businessId,
        Long professionalId,
        boolean active
    ) {

        validateBusinessId(
            businessId
        );

        validateProfessionalId(
            professionalId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Professional professional =
            findProfessionalInBusiness(
                businessId,
                professionalId
            );

        professional.setActive(
            active
        );

        /*
         * Nunca dejamos un profesional inactivo
         * aceptando reservas.
         */
        if (
            !active
        ) {
            professional.setBookingEnabled(
                false
            );
        }

        Professional savedProfessional =
            professionalRepository.save(
                professional
            );

        validateProfessionalBelongsToBusiness(
            savedProfessional,
            businessId
        );

        return professionalMapper.toResponse(
            savedProfessional
        );
    }

    // =========================================================
    // BOOKING ENABLED
    // =========================================================

    /**
     * Activa o pausa la recepción de nuevas reservas.
     *
     * Estados válidos:
     *
     * active=true, bookingEnabled=true
     * active=true, bookingEnabled=false
     * active=false, bookingEnabled=false
     *
     * No permitimos:
     *
     * active=false, bookingEnabled=true
     */
    @Transactional
    public ProfessionalResponse setBookingEnabled(
        Long businessId,
        Long professionalId,
        boolean bookingEnabled
    ) {

        validateBusinessId(
            businessId
        );

        validateProfessionalId(
            professionalId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Professional professional =
            findProfessionalInBusiness(
                businessId,
                professionalId
            );

        if (
            bookingEnabled
                && !professional.isActive()
        ) {
            throw new ConflictException(
                "No se pueden habilitar reservas "
                    + "para un profesional inactivo."
            );
        }

        professional.setBookingEnabled(
            bookingEnabled
        );

        Professional savedProfessional =
            professionalRepository.save(
                professional
            );

        validateProfessionalBelongsToBusiness(
            savedProfessional,
            businessId
        );

        return professionalMapper.toResponse(
            savedProfessional
        );
    }

    // =========================================================
    // LIST ALL
    // =========================================================

    /**
     * Lista todos los profesionales del negocio.
     *
     * Incluye:
     *
     * - activos;
     * - inactivos;
     * - reservables;
     * - no reservables.
     */
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> findAllByBusiness(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        List<Professional> professionals =
            professionalRepository
                .findAllByBusinessIdOrderByDisplayNameAsc(
                    businessId
                );

        validateProfessionalsBelongToBusiness(
            professionals,
            businessId
        );

        return professionals
            .stream()
            .map(
                professionalMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // LIST BOOKABLE
    // =========================================================

    /**
     * Lista únicamente profesionales:
     *
     * active = true
     * bookingEnabled = true
     */
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> findBookableByBusiness(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        List<Professional> professionals =
            professionalRepository
                .findAllByBusinessIdAndActiveTrueAndBookingEnabledTrueOrderByDisplayNameAsc(
                    businessId
                );

        validateProfessionalsBelongToBusiness(
            professionals,
            businessId
        );

        return professionals
            .stream()
            .map(
                professionalMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // FIND BY ID · CURRENT BUSINESS
    // =========================================================

    /**
     * Busca un profesional por ID dentro
     * del Business autenticado.
     */
    @Transactional(readOnly = true)
    public ProfessionalResponse findById(
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

        findActiveBusiness(
            businessId
        );

        Professional professional =
            findProfessionalInBusiness(
                businessId,
                professionalId
            );

        return professionalMapper.toResponse(
            professional
        );
    }

    // =========================================================
    // FIND BY BUSINESS AND ID
    // =========================================================

    /**
     * Busca un profesional dentro de un
     * Business concreto.
     */
    @Transactional(readOnly = true)
    public ProfessionalResponse findByBusinessAndId(
        Long businessId,
        Long professionalId
    ) {

        validateBusinessId(
            businessId
        );

        validateProfessionalId(
            professionalId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Professional professional =
            findProfessionalInBusiness(
                businessId,
                professionalId
            );

        return professionalMapper.toResponse(
            professional
        );
    }

    // =========================================================
    // PROFESSIONAL LOOKUP · TENANT SAFE
    // =========================================================

    /**
     * Punto único de recuperación privada de
     * profesionales dentro de un tenant.
     */
    private Professional findProfessionalInBusiness(
        Long businessId,
        Long professionalId
    ) {

        Professional professional =
            professionalRepository
                .findByIdAndBusinessId(
                    professionalId,
                    businessId
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe el profesional "
                                + professionalId
                                + " dentro del negocio "
                                + businessId
                        )
                );

        validateProfessionalBelongsToBusiness(
            professional,
            businessId
        );

        return professional;
    }

    // =========================================================
    // SECURITY · BUSINESS MANAGEMENT
    // =========================================================

    /**
     * La administración de profesionales pertenece
     * a la configuración estructural del Business.
     *
     * Roles:
     *
     * OWNER
     * ADMIN
     *
     * STAFF queda excluido.
     */
    private void requireBusinessManagement(
        Long businessId
    ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );
    }

    /**
     * Obtiene y valida el principal JWT.
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

        if (
            authenticatedUser.getUserId() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
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
    // BUSINESS
    // =========================================================

    /**
     * Recupera un Business y exige que esté activo.
     */
    private Business findActiveBusiness(
        Long businessId
    ) {

        Business business =
            businessRepository
                .findById(
                    businessId
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe ningún negocio con id: "
                                + businessId
                        )
                );

        if (
            !business.isActive()
        ) {
            throw new ConflictException(
                "El negocio "
                    + businessId
                    + " no está activo."
            );
        }

        return business;
    }

    // =========================================================
    // TENANT VALIDATION
    // =========================================================

    /**
     * Comprueba que el Professional pertenece
     * al Business esperado.
     */
    private void validateProfessionalBelongsToBusiness(
        Professional professional,
        Long expectedBusinessId
    ) {

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
            /*
             * No revelamos información del tenant propietario.
             */
            throw new ResourceNotFoundException(
                "No existe el profesional "
                    + professional.getId()
                    + " dentro del negocio actual."
            );
        }
    }

    /**
     * Defensa adicional para listados.
     */
    private void validateProfessionalsBelongToBusiness(
        List<Professional> professionals,
        Long expectedBusinessId
    ) {

        if (
            professionals == null
                || professionals.isEmpty()
        ) {
            return;
        }

        for (
            Professional professional
                : professionals
        ) {
            validateProfessionalBelongsToBusiness(
                professional,
                expectedBusinessId
            );
        }
    }

    // =========================================================
    // EMAIL UNIQUENESS
    // =========================================================

    /**
     * Comprueba unicidad del email dentro
     * del mismo Business.
     */
    private void validateEmailAvailable(
        Long businessId,
        String normalizedEmail
    ) {

        if (
            normalizedEmail == null
        ) {
            return;
        }

        boolean exists =
            professionalRepository
                .existsByBusinessIdAndEmailIgnoreCase(
                    businessId,
                    normalizedEmail
                );

        if (
            exists
        ) {
            throw new ConflictException(
                "Ya existe un profesional con el email '"
                    + normalizedEmail
                    + "' en el negocio "
                    + businessId
            );
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        ProfessionalRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "Los datos del profesional son obligatorios."
            );
        }
    }

    // =========================================================
    // ID VALIDATION
    // =========================================================

    private void validateBusinessId(
        Long businessId
    ) {

        if (
            businessId == null
        ) {
            throw new IllegalArgumentException(
                "businessId es obligatorio."
            );
        }

        if (
            businessId <= 0
        ) {
            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }
    }

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
    // EMAIL NORMALIZATION
    // =========================================================

    private String normalizeEmail(
        String email
    ) {

        if (
            email == null
                || email.isBlank()
        ) {
            return null;
        }

        return email
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }
}