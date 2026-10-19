package com.wacds.appointments.customer;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.customer.dto.CustomerRequest;
import com.wacds.appointments.customer.dto.CustomerResponse;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final CustomerRepository customerRepository;

    private final BusinessRepository businessRepository;

    private final CustomerMapper customerMapper;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // PRIVATE BUSINESS API · CREATE
    // =========================================================

    /**
     * Crea un cliente desde el panel privado Business.
     *
     * Garantías:
     *
     * - usuario autenticado;
     * - acceso al tenant;
     * - negocio activo;
     * - email único dentro del negocio;
     * - teléfono único dentro del negocio.
     */
    @Transactional
    public CustomerResponse create(
        Long businessId,
        CustomerRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateRequest(
            request
        );

        requireBusinessAccess(
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

        String normalizedPhone =
            normalizePhone(
                request.phone()
            );

        validateEmailAvailable(
            businessId,
            normalizedEmail,
            null
        );

        validatePhoneAvailable(
            businessId,
            normalizedPhone,
            null
        );

        Customer customer =
            customerMapper.toEntity(
                request,
                business
            );

        if (
            customer == null
        ) {
            throw new IllegalStateException(
                "No se ha podido construir el cliente."
            );
        }

        /*
         * Defensa explícita multi-tenant.
         *
         * Aunque el mapper ya debería establecer el Business,
         * fijamos la relación aquí para impedir cualquier cambio
         * accidental de tenant.
         */
        customer.setBusiness(
            business
        );

        customer.setEmail(
            normalizedEmail
        );

        customer.setPhone(
            normalizedPhone
        );

        Customer saved =
            customerRepository.save(
                customer
            );

        return customerMapper.toResponse(
            saved
        );
    }

    // =========================================================
    // PRIVATE BUSINESS API · UPDATE
    // =========================================================

    /**
     * Actualiza los datos editables de un cliente.
     *
     * El customerId debe pertenecer al businessId recibido.
     *
     * Durante la edición:
     *
     * - email continúa siendo único dentro del tenant;
     * - teléfono continúa siendo único dentro del tenant;
     * - se excluye el propio customerId en las comprobaciones;
     * - nunca se cambia el tenant del cliente.
     */
    @Transactional
    public CustomerResponse update(
        Long businessId,
        Long customerId,
        CustomerRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateCustomerId(
            customerId
        );

        validateRequest(
            request
        );

        requireBusinessAccess(
            businessId
        );

        Business business =
            findActiveBusiness(
                businessId
            );

        Customer customer =
            findCustomerInBusiness(
                businessId,
                customerId
            );

        String normalizedFirstName =
            normalizeRequiredText(
                request.firstName(),
                "El nombre del cliente es obligatorio."
            );

        String normalizedLastName =
            normalizeNullableText(
                request.lastName()
            );

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        String normalizedPhone =
            normalizePhone(
                request.phone()
            );

        String normalizedNotes =
            normalizeNullableText(
                request.notes()
            );

        validateEmailAvailable(
            businessId,
            normalizedEmail,
            customerId
        );

        validatePhoneAvailable(
            businessId,
            normalizedPhone,
            customerId
        );

        /*
         * Defensa de tenant.
         *
         * El cliente se recupera mediante:
         *
         * businessId + customerId
         *
         * y nunca se permite moverlo a otro negocio.
         */
        customer.setBusiness(
            business
        );

        customer.setFirstName(
            normalizedFirstName
        );

        customer.setLastName(
            normalizedLastName
        );

        customer.setEmail(
            normalizedEmail
        );

        customer.setPhone(
            normalizedPhone
        );

        customer.setNotes(
            normalizedNotes
        );

        /*
         * active es opcional.
         *
         * Si el frontend no lo envía, mantenemos el estado
         * actual del cliente.
         */
        if (
            request.active() != null
        ) {
            customer.setActive(
                request.active()
            );
        }

        Customer saved =
            customerRepository.save(
                customer
            );

        return customerMapper.toResponse(
            saved
        );
    }

    // =========================================================
    // PRIVATE BUSINESS API · ACTIVE STATUS
    // =========================================================

    /**
     * Activa o desactiva un cliente.
     *
     * IMPORTANTE:
     *
     * Desactivar no elimina el Customer.
     *
     * Se conserva:
     *
     * - histórico;
     * - citas;
     * - información;
     * - relaciones existentes.
     */
    @Transactional
    public CustomerResponse setActive(
        Long businessId,
        Long customerId,
        boolean active
    ) {

        validateBusinessId(
            businessId
        );

        validateCustomerId(
            customerId
        );

        requireBusinessAccess(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Customer customer =
            findCustomerInBusiness(
                businessId,
                customerId
            );

        customer.setActive(
            active
        );

        Customer saved =
            customerRepository.save(
                customer
            );

        return customerMapper.toResponse(
            saved
        );
    }

    // =========================================================
    // PRIVATE BUSINESS API · LIST
    // =========================================================

    /**
     * Devuelve todos los clientes del negocio,
     * incluidos activos e inactivos.
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAllByBusiness(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireBusinessAccess(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        return customerRepository
            .findAllByBusinessIdOrderByFirstNameAscLastNameAsc(
                businessId
            )
            .stream()
            .map(
                customerMapper::toResponse
            )
            .toList();
    }

    /**
     * Devuelve únicamente los clientes activos.
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findActiveByBusiness(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        requireBusinessAccess(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        return customerRepository
            .findAllByBusinessIdAndActiveTrueOrderByFirstNameAscLastNameAsc(
                businessId
            )
            .stream()
            .map(
                customerMapper::toResponse
            )
            .toList();
    }

    // =========================================================
    // PRIVATE BUSINESS API · FIND BY ID
    // =========================================================

    /**
     * Obtiene un cliente por ID utilizando el tenant
     * presente en la sesión autenticada.
     */
    @Transactional(readOnly = true)
    public CustomerResponse findById(
        Long customerId
    ) {

        validateCustomerId(
            customerId
        );

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            authenticatedUser.getBusinessId();

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        findActiveBusiness(
            businessId
        );

        Customer customer =
            findCustomerInBusiness(
                businessId,
                customerId
            );

        return customerMapper.toResponse(
            customer
        );
    }

    /**
     * Obtiene un cliente indicando explícitamente
     * businessId + customerId.
     */
    @Transactional(readOnly = true)
    public CustomerResponse findByBusinessAndId(
        Long businessId,
        Long customerId
    ) {

        validateBusinessId(
            businessId
        );

        validateCustomerId(
            customerId
        );

        requireBusinessAccess(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        Customer customer =
            findCustomerInBusiness(
                businessId,
                customerId
            );

        return customerMapper.toResponse(
            customer
        );
    }

    // =========================================================
    // PRIVATE BUSINESS API · FIND BY EMAIL
    // =========================================================

    @Transactional(readOnly = true)
    public CustomerResponse findByBusinessAndEmail(
        Long businessId,
        String email
    ) {

        validateBusinessId(
            businessId
        );

        requireBusinessAccess(
            businessId
        );

        findActiveBusiness(
            businessId
        );

        String normalizedEmail =
            normalizeEmail(
                email
            );

        if (
            normalizedEmail == null
        ) {
            throw new IllegalArgumentException(
                "El email es obligatorio para realizar la búsqueda."
            );
        }

        Customer customer =
            customerRepository
                .findByBusinessIdAndEmailIgnoreCase(
                    businessId,
                    normalizedEmail
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe ningún cliente con el email '"
                                + normalizedEmail
                                + "' en el negocio "
                                + businessId
                        )
                );

        return customerMapper.toResponse(
            customer
        );
    }

    // =========================================================
    // PUBLIC BOOKING · FIND OR CREATE CUSTOMER
    // =========================================================

    /**
     * Resuelve el Customer utilizado durante una reserva pública.
     *
     * Este método NO exige JWT.
     *
     * El aislamiento se realiza estrictamente por:
     *
     * businessId + email
     * businessId + phone
     *
     * Reglas:
     *
     * 1. Si no existe ninguna coincidencia:
     *    crea un Customer.
     *
     * 2. Si existe el email:
     *    reutiliza el Customer.
     *
     * 3. Si existe el teléfono:
     *    reutiliza el Customer.
     *
     * 4. Si email y teléfono pertenecen a Customers distintos:
     *    genera conflicto.
     *
     * 5. Si el Customer estaba inactivo y vuelve a reservar:
     *    se reactiva.
     */
    @Transactional
    public Customer findOrCreateForPublicBooking(
        Long businessId,
        String firstName,
        String lastName,
        String email,
        String phone
    ) {

        validateBusinessId(
            businessId
        );

        Business business =
            findActiveBusiness(
                businessId
            );

        String normalizedFirstName =
            normalizeRequiredText(
                firstName,
                "El nombre del cliente es obligatorio."
            );

        String normalizedLastName =
            normalizeNullableText(
                lastName
            );

        String normalizedEmail =
            normalizeEmail(
                email
            );

        String normalizedPhone =
            normalizePhone(
                phone
            );

        if (
            normalizedEmail == null
                && normalizedPhone == null
        ) {
            throw new IllegalArgumentException(
                "La reserva debe incluir al menos "
                    + "un email o un teléfono de contacto."
            );
        }

        Optional<Customer> customerByEmail =
            normalizedEmail == null
                ? Optional.empty()
                : customerRepository
                    .findByBusinessIdAndEmailIgnoreCase(
                        businessId,
                        normalizedEmail
                    );

        Optional<Customer> customerByPhone =
            normalizedPhone == null
                ? Optional.empty()
                : customerRepository
                    .findByBusinessIdAndPhone(
                        businessId,
                        normalizedPhone
                    );

        /*
         * Nunca fusionamos automáticamente dos identidades
         * diferentes.
         */
        if (
            customerByEmail.isPresent()
                && customerByPhone.isPresent()
                && !customerByEmail
                    .get()
                    .getId()
                    .equals(
                        customerByPhone
                            .get()
                            .getId()
                    )
        ) {
            throw new ConflictException(
                "El email y el teléfono introducidos "
                    + "pertenecen a clientes distintos."
            );
        }

        Customer existingCustomer =
            customerByEmail
                .orElseGet(
                    () ->
                        customerByPhone
                            .orElse(
                                null
                            )
                );

        if (
            existingCustomer != null
        ) {

            updateExistingCustomerForBooking(
                existingCustomer,
                businessId,
                normalizedFirstName,
                normalizedLastName,
                normalizedEmail,
                normalizedPhone
            );

            return customerRepository.save(
                existingCustomer
            );
        }

        Customer newCustomer =
            Customer
                .builder()
                .business(
                    business
                )
                .firstName(
                    normalizedFirstName
                )
                .lastName(
                    normalizedLastName
                )
                .email(
                    normalizedEmail
                )
                .phone(
                    normalizedPhone
                )
                .notes(
                    null
                )
                .active(
                    true
                )
                .build();

        return customerRepository.save(
            newCustomer
        );
    }

    // =========================================================
    // PUBLIC BOOKING · EXISTING CUSTOMER UPDATE
    // =========================================================

    /**
     * Actualiza conservadoramente un cliente que vuelve
     * a realizar una reserva pública.
     */
    private void updateExistingCustomerForBooking(
        Customer customer,
        Long businessId,
        String firstName,
        String lastName,
        String email,
        String phone
    ) {

        if (
            customer == null
                || customer.getId() == null
        ) {
            throw new IllegalStateException(
                "El cliente existente no es válido."
            );
        }

        Long customerBusinessId =
            customer.getBusinessId();

        if (
            customerBusinessId == null
                || !customerBusinessId.equals(
                    businessId
                )
        ) {
            throw new IllegalStateException(
                "El cliente no pertenece al negocio solicitado."
            );
        }

        /*
         * Un cliente inactivo que vuelve voluntariamente
         * a reservar se reactiva automáticamente.
         */
        if (
            !customer.isActive()
        ) {
            customer.setActive(
                true
            );
        }

        customer.setFirstName(
            firstName
        );

        customer.setLastName(
            lastName
        );

        // -----------------------------------------------------
        // EMAIL
        // -----------------------------------------------------

        if (
            email != null
        ) {

            String currentEmail =
                normalizeEmail(
                    customer.getEmail()
                );

            if (
                currentEmail == null
                    || !currentEmail.equalsIgnoreCase(
                        email
                    )
            ) {

                validateEmailAvailable(
                    businessId,
                    email,
                    customer.getId()
                );

                customer.setEmail(
                    email
                );
            }
        }

        // -----------------------------------------------------
        // PHONE
        // -----------------------------------------------------

        if (
            phone != null
        ) {

            String currentPhone =
                normalizePhone(
                    customer.getPhone()
                );

            if (
                currentPhone == null
                    || !currentPhone.equals(
                        phone
                    )
            ) {

                validatePhoneAvailable(
                    businessId,
                    phone,
                    customer.getId()
                );

                customer.setPhone(
                    phone
                );
            }
        }
    }

    // =========================================================
    // CUSTOMER LOOKUP · TENANT SAFE
    // =========================================================

    /**
     * Recupera un Customer exclusivamente dentro
     * del tenant indicado.
     */
    private Customer findCustomerInBusiness(
        Long businessId,
        Long customerId
    ) {

        return customerRepository
            .findByIdAndBusinessId(
                customerId,
                businessId
            )
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe el cliente "
                            + customerId
                            + " dentro del negocio "
                            + businessId
                    )
            );
    }

    // =========================================================
    // BUSINESS
    // =========================================================

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
    // SECURITY · PRIVATE BUSINESS API
    // =========================================================

    /**
     * Valida que el usuario autenticado pertenezca
     * al Business solicitado.
     */
    private void requireBusinessAccess(
        Long businessId
    ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );
    }

    /**
     * Obtiene el principal autenticado del contexto Spring.
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
            throw new org.springframework.security.authentication
                .BadCredentialsException(
                    "No existe una sesión autenticada válida."
                );
        }

        if (
            authenticatedUser.getUserId() == null
                || authenticatedUser.getBusinessId() == null
        ) {
            throw new org.springframework.security.authentication
                .BadCredentialsException(
                    "La sesión autenticada no contiene "
                        + "un contexto de negocio válido."
                );
        }

        return authenticatedUser;
    }

    // =========================================================
    // UNIQUENESS VALIDATION
    // =========================================================

    /**
     * Comprueba que un email no pertenezca a otro
     * Customer del mismo tenant.
     */
    private void validateEmailAvailable(
        Long businessId,
        String email,
        Long excludedCustomerId
    ) {

        if (
            email == null
        ) {
            return;
        }

        boolean exists =
            excludedCustomerId == null
                ? customerRepository
                    .existsByBusinessIdAndEmailIgnoreCase(
                        businessId,
                        email
                    )
                : customerRepository
                    .existsByBusinessIdAndEmailIgnoreCaseAndIdNot(
                        businessId,
                        email,
                        excludedCustomerId
                    );

        if (
            exists
        ) {
            throw new ConflictException(
                "Ya existe un cliente con el email '"
                    + email
                    + "' en el negocio "
                    + businessId
            );
        }
    }

    /**
     * Comprueba que un teléfono no pertenezca a otro
     * Customer del mismo tenant.
     */
    private void validatePhoneAvailable(
        Long businessId,
        String phone,
        Long excludedCustomerId
    ) {

        if (
            phone == null
        ) {
            return;
        }

        boolean exists =
            excludedCustomerId == null
                ? customerRepository
                    .existsByBusinessIdAndPhone(
                        businessId,
                        phone
                    )
                : customerRepository
                    .existsByBusinessIdAndPhoneAndIdNot(
                        businessId,
                        phone,
                        excludedCustomerId
                    );

        if (
            exists
        ) {
            throw new ConflictException(
                "Ya existe un cliente con el teléfono '"
                    + phone
                    + "' en el negocio "
                    + businessId
            );
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        CustomerRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "Los datos del cliente son obligatorios."
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

    private void validateCustomerId(
        Long customerId
    ) {

        if (
            customerId == null
        ) {
            throw new IllegalArgumentException(
                "customerId es obligatorio."
            );
        }

        if (
            customerId <= 0
        ) {
            throw new IllegalArgumentException(
                "customerId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // NORMALIZATION
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

    private String normalizePhone(
        String phone
    ) {

        return normalizeNullableText(
            phone
        );
    }

    private String normalizeRequiredText(
        String value,
        String errorMessage
    ) {

        String normalized =
            normalizeNullableText(
                value
            );

        if (
            normalized == null
        ) {
            throw new IllegalArgumentException(
                errorMessage
            );
        }

        return normalized;
    }

    private String normalizeNullableText(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}