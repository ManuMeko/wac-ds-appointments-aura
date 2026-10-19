package com.wacds.appointments.publicbooking.catalog;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignment;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignmentRepository;
import com.wacds.appointments.publicbooking.catalog.dto.PublicCatalogProfessionalResponse;
import com.wacds.appointments.publicbooking.catalog.dto.PublicCatalogServiceResponse;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.service.ServiceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio de dominio del catálogo público de reservas.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Alimenta los primeros pasos del flujo público:
 *
 * Business
 *      ↓
 * Services reservables
 *      ↓
 * Professionals compatibles
 *      ↓
 * AvailabilitySlotService
 *      ↓
 * PublicBookingService
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Esta clase pertenece al contexto público y anónimo.
 *
 * NO depende de:
 *
 * - JWT;
 * - SecurityContext;
 * - AuthenticatedUser;
 * - BusinessMembershipService.
 *
 * El tenant público se resuelve exclusivamente mediante
 * el slug del Business.
 *
 * =========================================================
 * REGLAS
 * =========================================================
 *
 * Un catálogo público solo puede exponerse si:
 *
 * - el Business existe;
 * - Business.active = true;
 * - Business.publicBookingEnabled = true.
 *
 * Un Service solo puede exponerse si:
 *
 * - pertenece al Business;
 * - active = true;
 * - bookingEnabled = true.
 *
 * Un Professional solo puede exponerse para un Service si:
 *
 * - pertenece al mismo Business;
 * - active = true;
 * - bookingEnabled = true;
 * - existe asignación Professional <-> Service.
 *
 * =========================================================
 * PRIVACIDAD
 * =========================================================
 *
 * Nunca devuelve directamente:
 *
 * - entidades JPA;
 * - email de profesionales;
 * - teléfono de profesionales;
 * - timestamps internos;
 * - configuración administrativa;
 * - información de otros tenants.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PublicCatalogService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessRepository
        businessRepository;

    private final ServiceRepository
        serviceRepository;

    private final ProfessionalServiceAssignmentRepository
        assignmentRepository;

    // =========================================================
    // PUBLIC SERVICES
    // =========================================================

    /**
     * Devuelve los servicios que pueden participar
     * actualmente en el flujo público de reservas.
     *
     * El Business se identifica mediante su slug público.
     *
     * Ejemplo:
     *
     * aura-studio
     */
    @Transactional(readOnly = true)
    public List<PublicCatalogServiceResponse>
        findBookableServicesByBusinessSlug(
            String businessSlug
        ) {

        Business business =
            requirePublicBookableBusiness(
                businessSlug
            );

        Long businessId =
            requireBusinessId(
                business
            );

        List<Service> services =
            serviceRepository
                .findAllByBusinessIdAndActiveTrueAndBookingEnabledTrueOrderByNameAsc(
                    businessId
                );

        if (
            services == null
                || services.isEmpty()
        ) {

            return List.of();
        }

        return services
            .stream()
            .filter(Objects::nonNull)
            .filter(
                service ->
                    isServicePubliclyBookable(
                        service,
                        business
                    )
            )
            .map(
                service ->
                    toPublicServiceResponse(
                        service,
                        business
                    )
            )
            .toList();
    }

    // =========================================================
    // PUBLIC PROFESSIONALS BY SERVICE
    // =========================================================

    /**
     * Devuelve los profesionales públicos compatibles
     * con un Service concreto.
     *
     * El Business continúa resolviéndose por slug para evitar
     * confiar únicamente en IDs enviados por el navegador.
     */
    @Transactional(readOnly = true)
    public List<PublicCatalogProfessionalResponse>
        findBookableProfessionalsByService(
            String businessSlug,
            Long serviceId
        ) {

        validateServiceId(
            serviceId
        );

        Business business =
            requirePublicBookableBusiness(
                businessSlug
            );

        Long businessId =
            requireBusinessId(
                business
            );

        Service service =
            requirePublicBookableService(
                business,
                serviceId
            );

        List<ProfessionalServiceAssignment> assignments =
            assignmentRepository
                .findAllBookableWithProfessionalByBusinessIdAndServiceId(
                    businessId,
                    service.getId()
                );

        if (
            assignments == null
                || assignments.isEmpty()
        ) {

            return List.of();
        }

        /*
         * LinkedHashMap:
         *
         * - elimina defensivamente duplicados por professionalId;
         * - mantiene el orden recibido desde el repository.
         */
        Map<Long, PublicCatalogProfessionalResponse>
            professionalsById =
                new LinkedHashMap<>();

        for (
            ProfessionalServiceAssignment assignment
                : assignments
        ) {

            if (
                assignment == null
            ) {

                continue;
            }

            Professional professional =
                assignment.getProfessional();

            if (
                !isProfessionalPubliclyBookable(
                    professional,
                    business
                )
            ) {

                continue;
            }

            if (
                assignment.getService() == null
                    || assignment.getService().getId() == null
                    || !Objects.equals(
                        assignment.getService().getId(),
                        service.getId()
                    )
            ) {

                continue;
            }

            PublicCatalogProfessionalResponse response =
                toPublicProfessionalResponse(
                    professional
                );

            professionalsById.putIfAbsent(
                response.id(),
                response
            );
        }

        return List.copyOf(
            professionalsById.values()
        );
    }

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Resuelve un Business mediante su slug público
     * y comprueba que actualmente pueda exponer reservas.
     */
    private Business requirePublicBookableBusiness(
        String businessSlug
    ) {

        String normalizedSlug =
            normalizeBusinessSlug(
                businessSlug
            );

        Business business =
            businessRepository
                .findBySlugIgnoreCase(
                    normalizedSlug
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No se ha encontrado el negocio solicitado."
                        )
                );

        requireBusinessId(
            business
        );

        if (
            !business.canAcceptPublicBookings()
        ) {

            throw new ConflictException(
                "Las reservas públicas no están disponibles "
                    + "actualmente para este negocio."
            );
        }

        return business;
    }

    /**
     * Obtiene defensivamente el identificador del Business.
     */
    private Long requireBusinessId(
        Business business
    ) {

        if (
            business == null
                || business.getId() == null
                || business.getId() <= 0
        ) {

            throw new IllegalStateException(
                "El negocio no contiene un identificador válido."
            );
        }

        return business.getId();
    }

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Recupera un Service asegurando que:
     *
     * - pertenece al Business público;
     * - está activo;
     * - permite nuevas reservas.
     */
    private Service requirePublicBookableService(
        Business business,
        Long serviceId
    ) {

        Long businessId =
            requireBusinessId(
                business
            );

        Service service =
            serviceRepository
                .findByIdAndBusinessId(
                    serviceId,
                    businessId
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No se ha encontrado el servicio solicitado."
                        )
                );

        if (
            !isServicePubliclyBookable(
                service,
                business
            )
        ) {

            throw new ResourceNotFoundException(
                "El servicio solicitado no está disponible "
                    + "para reservas públicas."
            );
        }

        return service;
    }

    /**
     * Defensa en profundidad para un Service público.
     */
    private boolean isServicePubliclyBookable(
        Service service,
        Business expectedBusiness
    ) {

        if (
            service == null
                || expectedBusiness == null
                || expectedBusiness.getId() == null
        ) {

            return false;
        }

        if (
            service.getId() == null
                || service.getId() <= 0
        ) {

            return false;
        }

        if (
            service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {

            return false;
        }

        if (
            !Objects.equals(
                service.getBusiness().getId(),
                expectedBusiness.getId()
            )
        ) {

            return false;
        }

        return service.canAcceptBookings();
    }

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Defensa en profundidad para un Professional público.
     */
    private boolean isProfessionalPubliclyBookable(
        Professional professional,
        Business expectedBusiness
    ) {

        if (
            professional == null
                || expectedBusiness == null
                || expectedBusiness.getId() == null
        ) {

            return false;
        }

        if (
            professional.getId() == null
                || professional.getId() <= 0
        ) {

            return false;
        }

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {

            return false;
        }

        if (
            !Objects.equals(
                professional.getBusiness().getId(),
                expectedBusiness.getId()
            )
        ) {

            return false;
        }

        return professional.canAcceptBookings();
    }

    // =========================================================
    // MAPPING · SERVICE
    // =========================================================

    private PublicCatalogServiceResponse
        toPublicServiceResponse(
            Service service,
            Business business
        ) {

        if (
            !isServicePubliclyBookable(
                service,
                business
            )
        ) {

            throw new IllegalStateException(
                "No se puede construir una respuesta pública "
                    + "para un servicio no reservable."
            );
        }

        return new PublicCatalogServiceResponse(
            service.getId(),
            service.getSlug(),
            service.getName(),
            service.getDescription(),
            service.getDurationMinutes(),
            service.getPrice(),
            business.getCurrency()
        );
    }

    // =========================================================
    // MAPPING · PROFESSIONAL
    // =========================================================

    private PublicCatalogProfessionalResponse
        toPublicProfessionalResponse(
            Professional professional
        ) {

        if (
            professional == null
                || professional.getId() == null
                || professional.getId() <= 0
        ) {

            throw new IllegalStateException(
                "No se puede construir una respuesta pública "
                    + "para un profesional inválido."
            );
        }

        if (
            professional.getDisplayName() == null
                || professional.getDisplayName().isBlank()
        ) {

            throw new IllegalStateException(
                "El profesional no contiene un nombre público válido."
            );
        }

        return new PublicCatalogProfessionalResponse(
            professional.getId(),
            professional.getDisplayName(),
            professional.getBio()
        );
    }

    // =========================================================
    // VALIDATION · BUSINESS SLUG
    // =========================================================

    private String normalizeBusinessSlug(
        String businessSlug
    ) {

        if (
            businessSlug == null
                || businessSlug.isBlank()
        ) {

            throw new IllegalArgumentException(
                "businessSlug es obligatorio."
            );
        }

        String normalized =
            businessSlug
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            normalized.length() > 180
        ) {

            throw new IllegalArgumentException(
                "businessSlug no puede superar los 180 caracteres."
            );
        }

        if (
            !normalized.matches(
                "^[a-z0-9]+(?:-[a-z0-9]+)*$"
            )
        ) {

            throw new IllegalArgumentException(
                "businessSlug no tiene un formato válido."
            );
        }

        return normalized;
    }

    // =========================================================
    // VALIDATION · SERVICE ID
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
}