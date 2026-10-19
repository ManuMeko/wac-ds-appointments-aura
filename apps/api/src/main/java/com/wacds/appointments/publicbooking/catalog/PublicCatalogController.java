package com.wacds.appointments.publicbooking.catalog;

import com.wacds.appointments.publicbooking.catalog.dto.PublicCatalogProfessionalResponse;
import com.wacds.appointments.publicbooking.catalog.dto.PublicCatalogServiceResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API pública de catálogo para el flujo de reservas.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Expone únicamente la información necesaria para que un
 * cliente pueda construir una reserva desde la web pública:
 *
 * Business
 *      ↓
 * Services reservables
 *      ↓
 * Professionals compatibles
 *      ↓
 * disponibilidad
 *      ↓
 * creación de la reserva
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Este controller es público y anónimo.
 *
 * NO requiere:
 *
 * - JWT;
 * - cuenta;
 * - sesión Business;
 * - membresía OWNER / ADMIN / STAFF.
 *
 * Todas las reglas de tenant, estado del Business,
 * publicBookingEnabled, Service y Professional se delegan
 * en PublicCatalogService.
 *
 * =========================================================
 * ENDPOINTS
 * =========================================================
 *
 * GET
 * /api/v1/public/catalog/{businessSlug}/services
 *
 * GET
 * /api/v1/public/catalog/{businessSlug}/services/{serviceId}/professionals
 *
 * =========================================================
 * CACHE
 * =========================================================
 *
 * El catálogo público puede cambiar cuando:
 *
 * - se activa/desactiva un Service;
 * - se habilitan/deshabilitan reservas de un Service;
 * - se activa/desactiva un Professional;
 * - se habilitan/deshabilitan reservas de un Professional;
 * - cambia una asignación Professional <-> Service;
 * - se activa/desactiva el Business;
 * - cambia publicBookingEnabled.
 *
 * Por ello devolvemos Cache-Control: no-store.
 */
@RestController
@RequestMapping(
    value = "/api/v1/public/catalog",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
public class PublicCatalogController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final PublicCatalogService
        publicCatalogService;

    // =========================================================
    // SERVICES
    // =========================================================

    /**
     * Devuelve los servicios reservables de un Business
     * identificado por su slug público.
     *
     * Ejemplo:
     *
     * GET
     * /api/v1/public/catalog/aura-studio/services
     */
    @GetMapping(
        "/{businessSlug}/services"
    )
    public ResponseEntity<List<PublicCatalogServiceResponse>>
        findBookableServices(

            @PathVariable
            @NotBlank(
                message = "businessSlug es obligatorio."
            )
            @Size(
                max = 180,
                message = "businessSlug no puede superar los 180 caracteres."
            )
            String businessSlug

        ) {

        List<PublicCatalogServiceResponse> response =
            publicCatalogService
                .findBookableServicesByBusinessSlug(
                    businessSlug
                );

        return buildNoStoreResponse(
            response
        );
    }

    // =========================================================
    // PROFESSIONALS BY SERVICE
    // =========================================================

    /**
     * Devuelve los profesionales reservables compatibles
     * con un Service concreto.
     *
     * Ejemplo:
     *
     * GET
     * /api/v1/public/catalog/aura-studio/services/1/professionals
     */
    @GetMapping(
        "/{businessSlug}/services/{serviceId}/professionals"
    )
    public ResponseEntity<
        List<PublicCatalogProfessionalResponse>
    > findBookableProfessionalsByService(

        @PathVariable
        @NotBlank(
            message = "businessSlug es obligatorio."
        )
        @Size(
            max = 180,
            message = "businessSlug no puede superar los 180 caracteres."
        )
        String businessSlug,

        @PathVariable
        @Positive(
            message = "serviceId debe ser mayor que 0."
        )
        Long serviceId

    ) {

        List<PublicCatalogProfessionalResponse> response =
            publicCatalogService
                .findBookableProfessionalsByService(
                    businessSlug,
                    serviceId
                );

        return buildNoStoreResponse(
            response
        );
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    /**
     * Evita cachear un catálogo que puede cambiar
     * inmediatamente por configuración del negocio.
     */
    private <T> ResponseEntity<List<T>>
        buildNoStoreResponse(
            List<T> response
        ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "El catálogo público ha devuelto "
                    + "una respuesta nula."
            );
        }

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }
}