package com.wacds.appointments.service;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.service.dto.ServiceRequest;
import com.wacds.appointments.service.dto.ServiceResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ServiceRepository serviceRepository;

    private final BusinessRepository businessRepository;

    private final ServiceMapper serviceMapper;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea un servicio dentro del catálogo del negocio.
     *
     * Operación estructural.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * STAFF no puede modificar el catálogo.
     */
    @Transactional
    public ServiceResponse create(
        Long businessId,
        ServiceRequest request
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

        String normalizedSlug =
            normalizeRequiredSlug(
                request.slug()
            );

        validateSlugAvailable(
            businessId,
            normalizedSlug
        );

        Service service =
            serviceMapper.toEntity(
                request,
                business
            );

        if (
            service == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir el servicio."
            );
        }

        /*
         * Defensa explícita multi-tenant.
         */
        service.setBusiness(
            business
        );

        service.setSlug(
            normalizedSlug
        );

        Service savedService =
            serviceRepository.save(
                service
            );

        validateServiceBelongsToBusiness(
            savedService,
            businessId
        );

        return serviceMapper.toResponse(
            savedService
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Actualiza los datos editables de un servicio.
     *
     * El servicio nunca puede cambiar de tenant.
     *
     * Si cambia el slug:
     *
     * - se normaliza;
     * - se comprueba que no exista otro servicio
     *   con el mismo slug dentro del negocio.
     */
    @Transactional
    public ServiceResponse update(
        Long businessId,
        Long serviceId,
        ServiceRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateServiceId(
            serviceId
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

        Service service =
            findServiceInBusiness(
                businessId,
                serviceId
            );

        String normalizedSlug =
            normalizeRequiredSlug(
                request.slug()
            );

        String currentSlug =
            normalizeSlug(
                service.getSlug()
            );

        /*
         * Solo comprobamos unicidad si realmente
         * ha cambiado el slug.
         */
        if (
            !Objects.equals(
                currentSlug,
                normalizedSlug
            )
        ) {
            validateSlugAvailable(
                businessId,
                normalizedSlug
            );
        }

        /*
         * Nunca modificamos la pertenencia al tenant.
         */
        service.setBusiness(
            business
        );

        service.setName(
            request.name()
        );

        service.setSlug(
            normalizedSlug
        );

        service.setDescription(
            request.description()
        );

        service.setDurationMinutes(
            request.durationMinutes()
        );

        service.setPrice(
            request.price()
        );

        service.setActive(
            request.active()
        );

        service.setBookingEnabled(
            request.bookingEnabled()
        );

        Service savedService =
            serviceRepository.save(
                service
            );

        validateServiceBelongsToBusiness(
            savedService,
            businessId
        );

        return serviceMapper.toResponse(
            savedService
        );
    }

    // =========================================================
    // ACTIVE STATUS
    // =========================================================

    /**
     * Activa o desactiva un servicio.
     *
     * Desactivar NO elimina el servicio.
     *
     * Conservamos:
     *
     * - citas históricas;
     * - relaciones;
     * - precios;
     * - configuración;
     * - trazabilidad.
     */
    @Transactional
    public ServiceResponse setActive(
        Long businessId,
        Long serviceId,
        boolean active
    ) {

        validateBusinessId(
            businessId
        );

        validateServiceId(
            serviceId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Service service =
            findServiceInBusiness(
                businessId,
                serviceId
            );

        service.setActive(
            active
        );

        /*
         * Un servicio inactivo no debería poder seguir
         * aceptando nuevas reservas.
         */
        if (
            !active
        ) {
            service.setBookingEnabled(
                false
            );
        }

        Service savedService =
            serviceRepository.save(
                service
            );

        validateServiceBelongsToBusiness(
            savedService,
            businessId
        );

        return serviceMapper.toResponse(
            savedService
        );
    }

    // =========================================================
    // BOOKING ENABLED
    // =========================================================

    /**
     * Habilita o deshabilita la reserva online/manual
     * del servicio sin necesidad de archivarlo.
     *
     * Un servicio puede:
     *
     * active = true
     * bookingEnabled = false
     *
     * para permanecer visible en gestión pero no admitir
     * nuevas reservas.
     */
    @Transactional
    public ServiceResponse setBookingEnabled(
        Long businessId,
        Long serviceId,
        boolean bookingEnabled
    ) {

        validateBusinessId(
            businessId
        );

        validateServiceId(
            serviceId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Service service =
            findServiceInBusiness(
                businessId,
                serviceId
            );

        /*
         * No permitimos habilitar reservas en un
         * servicio archivado/inactivo.
         */
        if (
            bookingEnabled
                && !service.isActive()
        ) {
            throw new ConflictException(
                "No se pueden habilitar reservas "
                    + "para un servicio inactivo."
            );
        }

        service.setBookingEnabled(
            bookingEnabled
        );

        Service savedService =
            serviceRepository.save(
                service
            );

        validateServiceBelongsToBusiness(
            savedService,
            businessId
        );

        return serviceMapper.toResponse(
            savedService
        );
    }

    // =========================================================
    // LIST BY BUSINESS
    // =========================================================

    /**
     * Devuelve el catálogo completo del negocio.
     *
     * Incluye:
     *
     * - activos;
     * - inactivos;
     * - reservables;
     * - no reservables.
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> findAllByBusiness(
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

        List<Service> services =
            serviceRepository
                .findAllByBusinessIdOrderByNameAsc(
                    businessId
                );

        validateServicesBelongToBusiness(
            services,
            businessId
        );

        return services
            .stream()
            .map(
                serviceMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Busca un servicio por ID dentro del negocio
     * de la sesión autenticada.
     */
    @Transactional(readOnly = true)
    public ServiceResponse findById(
        Long serviceId
    ) {

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

        findActiveBusiness(
            businessId
        );

        Service service =
            findServiceInBusiness(
                businessId,
                serviceId
            );

        return serviceMapper.toResponse(
            service
        );
    }

    // =========================================================
    // FIND BY BUSINESS AND ID
    // =========================================================

    /**
     * Variante explícita businessId + serviceId.
     *
     * Útil para controllers legacy y operaciones internas
     * donde ya conocemos el tenant.
     */
    @Transactional(readOnly = true)
    public ServiceResponse findByBusinessAndId(
        Long businessId,
        Long serviceId
    ) {

        validateBusinessId(
            businessId
        );

        validateServiceId(
            serviceId
        );

        requireBusinessManagement(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Service service =
            findServiceInBusiness(
                businessId,
                serviceId
            );

        return serviceMapper.toResponse(
            service
        );
    }

    // =========================================================
    // FIND BY BUSINESS AND SLUG
    // =========================================================

    @Transactional(readOnly = true)
    public ServiceResponse findByBusinessAndSlug(
        Long businessId,
        String slug
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

        String normalizedSlug =
            normalizeRequiredSlug(
                slug
            );

        Service service =
            serviceRepository
                .findByBusinessIdAndSlug(
                    businessId,
                    normalizedSlug
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe ningún servicio con slug '"
                                + normalizedSlug
                                + "' para el negocio "
                                + businessId
                        )
                );

        validateServiceBelongsToBusiness(
            service,
            businessId
        );

        return serviceMapper.toResponse(
            service
        );
    }

    // =========================================================
    // SERVICE LOOKUP · TENANT SAFE
    // =========================================================

    /**
     * Centraliza la recuperación tenant-safe.
     *
     * Aunque actualmente ServiceRepository dispone
     * de findById global, ningún Service abandona esta
     * capa hasta haber validado explícitamente su Business.
     */
    private Service findServiceInBusiness(
        Long businessId,
        Long serviceId
    ) {

        Service service =
            serviceRepository
                .findById(
                    serviceId
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe el servicio "
                                + serviceId
                                + " dentro del negocio actual."
                        )
                );

        validateServiceBelongsToBusiness(
            service,
            businessId
        );

        return service;
    }

    // =========================================================
    // SECURITY · BUSINESS MANAGEMENT
    // =========================================================

    /**
     * Los servicios forman parte de la configuración
     * estructural del negocio.
     *
     * Roles permitidos:
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
     * Recupera el principal autenticado.
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
     * Busca el negocio y exige que continúe activo.
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
     * Comprueba que un Service pertenece al tenant esperado.
     */
    private void validateServiceBelongsToBusiness(
        Service service,
        Long expectedBusinessId
    ) {

        if (
            service == null
                || service.getId() == null
        ) {
            throw new IllegalStateException(
                "El servicio no es válido."
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
            /*
             * No revelamos el tenant propietario real.
             */
            throw new ResourceNotFoundException(
                "No existe el servicio "
                    + service.getId()
                    + " dentro del negocio actual."
            );
        }
    }

    /**
     * Defensa adicional para resultados de listas.
     */
    private void validateServicesBelongToBusiness(
        List<Service> services,
        Long expectedBusinessId
    ) {

        if (
            services == null
                || services.isEmpty()
        ) {
            return;
        }

        for (
            Service service
                : services
        ) {
            validateServiceBelongsToBusiness(
                service,
                expectedBusinessId
            );
        }
    }

    // =========================================================
    // SLUG UNIQUENESS
    // =========================================================

    private void validateSlugAvailable(
        Long businessId,
        String normalizedSlug
    ) {

        boolean exists =
            serviceRepository
                .existsByBusinessIdAndSlug(
                    businessId,
                    normalizedSlug
                );

        if (
            exists
        ) {
            throw new ConflictException(
                "Ya existe un servicio con el slug '"
                    + normalizedSlug
                    + "' para el negocio "
                    + businessId
            );
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        ServiceRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "Los datos del servicio son obligatorios."
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
    // SLUG NORMALIZATION
    // =========================================================

    private String normalizeRequiredSlug(
        String slug
    ) {

        String normalized =
            normalizeSlug(
                slug
            );

        if (
            normalized == null
        ) {
            throw new IllegalArgumentException(
                "El slug del servicio es obligatorio."
            );
        }

        return normalized;
    }

    private String normalizeSlug(
        String slug
    ) {

        if (
            slug == null
                || slug.isBlank()
        ) {
            return null;
        }

        return slug
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }
}