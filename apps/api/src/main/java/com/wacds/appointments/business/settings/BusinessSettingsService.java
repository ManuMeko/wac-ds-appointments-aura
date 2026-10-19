package com.wacds.appointments.business.settings;

import com.wacds.appointments.business.dto.BusinessSettingsResponse;
import com.wacds.appointments.business.dto.BusinessSettingsUpdateRequest;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessSettingsService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_MINUTES_IN_ONE_YEAR =
        525600;

    private static final int MAX_BOOKING_HORIZON_DAYS =
        730;

    private static final int MIN_SLOT_INTERVAL_MINUTES =
        5;

    private static final int MAX_SLOT_INTERVAL_MINUTES =
        240;

    private static final int MAX_BUFFER_MINUTES =
        240;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessSettingsRepository
        businessSettingsRepository;

    private final BusinessSettingsMapper
        businessSettingsMapper;

    // =========================================================
    // FIND BY BUSINESS
    // =========================================================

    /**
     * Devuelve la configuración operativa correspondiente
     * a un Business.
     *
     * La autorización multi-tenant se aplica desde
     * BusinessSettingsController mediante
     * BusinessMembershipService.
     */
    @Transactional(readOnly = true)
    public BusinessSettingsResponse findByBusinessId(
        Long businessId
    ) {

        BusinessSettings settings =
            findEntityByBusinessId(
                businessId
            );

        return businessSettingsMapper.toResponse(
            settings
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Actualiza parcialmente la configuración operativa.
     *
     * Semántica PATCH:
     *
     * null:
     *     no modificar el campo.
     *
     * valor:
     *     aplicar el nuevo valor.
     *
     * La operación trabaja primero sobre un estado objetivo
     * completo.
     *
     * Después:
     *
     * 1. normaliza dependencias;
     * 2. valida el estado objetivo;
     * 3. aplica únicamente los cambios necesarios;
     * 4. persiste mediante optimistic locking.
     */
    @Transactional
    public BusinessSettingsResponse update(
        Long businessId,
        BusinessSettingsUpdateRequest request
    ) {

        // -----------------------------------------------------
        // REQUEST
        // -----------------------------------------------------

        validateBusinessId(
            businessId
        );

        validateUpdateRequest(
            request
        );

        // -----------------------------------------------------
        // CURRENT SETTINGS
        // -----------------------------------------------------

        BusinessSettings settings =
            findEntityByBusinessId(
                businessId
            );

        // =====================================================
        // EXPLICIT OPTIMISTIC LOCK CHECK
        // =====================================================

        validateExpectedVersion(
            settings,
            request.version()
        );

        // =====================================================
        // CALCULATE TARGET STATE
        // =====================================================

        int targetMinimumBookingNoticeMinutes =
            valueOrCurrent(
                request.minimumBookingNoticeMinutes(),
                settings.getMinimumBookingNoticeMinutes()
            );

        int targetBookingHorizonDays =
            valueOrCurrent(
                request.bookingHorizonDays(),
                settings.getBookingHorizonDays()
            );

        int targetSlotIntervalMinutes =
            valueOrCurrent(
                request.slotIntervalMinutes(),
                settings.getSlotIntervalMinutes()
            );

        int targetDefaultBufferBeforeMinutes =
            valueOrCurrent(
                request.defaultBufferBeforeMinutes(),
                settings.getDefaultBufferBeforeMinutes()
            );

        int targetDefaultBufferAfterMinutes =
            valueOrCurrent(
                request.defaultBufferAfterMinutes(),
                settings.getDefaultBufferAfterMinutes()
            );

        boolean targetAutoConfirmAppointments =
            valueOrCurrent(
                request.autoConfirmAppointments(),
                settings.isAutoConfirmAppointments()
            );

        boolean targetOnlineCancellationEnabled =
            valueOrCurrent(
                request.onlineCancellationEnabled(),
                settings.isOnlineCancellationEnabled()
            );

        int targetMinimumCancellationNoticeMinutes =
            valueOrCurrent(
                request.minimumCancellationNoticeMinutes(),
                settings.getMinimumCancellationNoticeMinutes()
            );

        boolean targetOnlineReschedulingEnabled =
            valueOrCurrent(
                request.onlineReschedulingEnabled(),
                settings.isOnlineReschedulingEnabled()
            );

        int targetMinimumRescheduleNoticeMinutes =
            valueOrCurrent(
                request.minimumRescheduleNoticeMinutes(),
                settings.getMinimumRescheduleNoticeMinutes()
            );

        boolean targetRemindersEnabled =
            valueOrCurrent(
                request.remindersEnabled(),
                settings.isRemindersEnabled()
            );

        int targetFirstReminderMinutesBefore =
            valueOrCurrent(
                request.firstReminderMinutesBefore(),
                settings.getFirstReminderMinutesBefore()
            );

        boolean targetSecondReminderEnabled =
            valueOrCurrent(
                request.secondReminderEnabled(),
                settings.isSecondReminderEnabled()
            );

        int targetSecondReminderMinutesBefore =
            valueOrCurrent(
                request.secondReminderMinutesBefore(),
                settings.getSecondReminderMinutesBefore()
            );

        // =====================================================
        // NORMALIZE TARGET STATE
        // =====================================================

        /*
         * Regla de dominio:
         *
         * Si los recordatorios generales están deshabilitados,
         * el segundo recordatorio no puede permanecer activo.
         *
         * Esta normalización debe ejecutarse ANTES de validar
         * el estado objetivo.
         *
         * De este modo un PATCH:
         *
         * {
         *   "remindersEnabled": false
         * }
         *
         * funciona correctamente aunque el estado anterior fuera:
         *
         * remindersEnabled = true
         * secondReminderEnabled = true
         */
        if (!targetRemindersEnabled) {

            targetSecondReminderEnabled =
                false;
        }

        // =====================================================
        // COMBINED DOMAIN VALIDATION
        // =====================================================

        validateTargetState(
            targetMinimumBookingNoticeMinutes,
            targetBookingHorizonDays,
            targetSlotIntervalMinutes,
            targetDefaultBufferBeforeMinutes,
            targetDefaultBufferAfterMinutes,
            targetMinimumCancellationNoticeMinutes,
            targetMinimumRescheduleNoticeMinutes,
            targetRemindersEnabled,
            targetFirstReminderMinutesBefore,
            targetSecondReminderEnabled,
            targetSecondReminderMinutesBefore
        );

        // =====================================================
        // APPLY PATCH
        // =====================================================

        if (
            request.minimumBookingNoticeMinutes()
                != null
        ) {

            settings.setMinimumBookingNoticeMinutes(
                targetMinimumBookingNoticeMinutes
            );
        }

        if (
            request.bookingHorizonDays()
                != null
        ) {

            settings.setBookingHorizonDays(
                targetBookingHorizonDays
            );
        }

        if (
            request.slotIntervalMinutes()
                != null
        ) {

            settings.setSlotIntervalMinutes(
                targetSlotIntervalMinutes
            );
        }

        if (
            request.defaultBufferBeforeMinutes()
                != null
        ) {

            settings.setDefaultBufferBeforeMinutes(
                targetDefaultBufferBeforeMinutes
            );
        }

        if (
            request.defaultBufferAfterMinutes()
                != null
        ) {

            settings.setDefaultBufferAfterMinutes(
                targetDefaultBufferAfterMinutes
            );
        }

        if (
            request.autoConfirmAppointments()
                != null
        ) {

            settings.setAutoConfirmAppointments(
                targetAutoConfirmAppointments
            );
        }

        // =====================================================
        // ONLINE CANCELLATION
        // =====================================================

        if (
            request.onlineCancellationEnabled()
                != null
        ) {

            settings.setOnlineCancellationEnabled(
                targetOnlineCancellationEnabled
            );
        }

        if (
            request.minimumCancellationNoticeMinutes()
                != null
        ) {

            settings.setMinimumCancellationNoticeMinutes(
                targetMinimumCancellationNoticeMinutes
            );
        }

        // =====================================================
        // ONLINE RESCHEDULING
        // =====================================================

        if (
            request.onlineReschedulingEnabled()
                != null
        ) {

            settings.setOnlineReschedulingEnabled(
                targetOnlineReschedulingEnabled
            );
        }

        if (
            request.minimumRescheduleNoticeMinutes()
                != null
        ) {

            settings.setMinimumRescheduleNoticeMinutes(
                targetMinimumRescheduleNoticeMinutes
            );
        }

        // =====================================================
        // REMINDERS
        // =====================================================

        if (
            request.remindersEnabled()
                != null
        ) {

            settings.setRemindersEnabled(
                targetRemindersEnabled
            );
        }

        if (
            request.firstReminderMinutesBefore()
                != null
        ) {

            settings.setFirstReminderMinutesBefore(
                targetFirstReminderMinutesBefore
            );
        }

        /*
         * IMPORTANTE:
         *
         * Aplicamos secondReminderEnabled cuando:
         *
         * - el cliente lo ha enviado explícitamente;
         * - o remindersEnabled ha sido deshabilitado.
         *
         * Esto garantiza que:
         *
         * remindersEnabled = false
         *
         * siempre implique:
         *
         * secondReminderEnabled = false.
         */
        if (
            request.secondReminderEnabled()
                != null
                || (
                    request.remindersEnabled()
                        != null
                        && !targetRemindersEnabled
                )
        ) {

            settings.setSecondReminderEnabled(
                targetSecondReminderEnabled
            );
        }

        if (
            request.secondReminderMinutesBefore()
                != null
        ) {

            settings.setSecondReminderMinutesBefore(
                targetSecondReminderMinutesBefore
            );
        }

        // =====================================================
        // SAVE + JPA OPTIMISTIC LOCK
        // =====================================================

        try {

            BusinessSettings savedSettings =
                businessSettingsRepository.saveAndFlush(
                    settings
                );

            return businessSettingsMapper.toResponse(
                savedSettings
            );

        } catch (
            OptimisticLockingFailureException exception
        ) {

            throw new ConflictException(
                "La configuración del negocio ha sido modificada "
                    + "por otra sesión. Recarga los datos antes "
                    + "de volver a guardar."
            );
        }
    }

    // =========================================================
    // FIND ENTITY
    // =========================================================

    /**
     * Recupera la entidad de configuración asociada
     * al Business.
     *
     * La migración V12 creó una configuración
     * para los negocios existentes.
     */
    @Transactional(readOnly = true)
    public BusinessSettings findEntityByBusinessId(
        Long businessId
    ) {

        validateBusinessId(
            businessId
        );

        return businessSettingsRepository
            .findByBusinessId(
                businessId
            )
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe configuración operativa "
                            + "para el negocio con id: "
                            + businessId
                    )
            );
    }

    // =========================================================
    // EXISTS
    // =========================================================

    @Transactional(readOnly = true)
    public boolean existsByBusinessId(
        Long businessId
    ) {

        if (
            businessId == null
                || businessId <= 0
        ) {
            return false;
        }

        return businessSettingsRepository
            .existsByBusinessId(
                businessId
            );
    }

    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    /**
     * Comprueba explícitamente la versión que el cliente
     * leyó previamente.
     *
     * Si el frontend envía una versión obsoleta,
     * rechazamos el PATCH antes de modificar la entidad.
     *
     * Si version es null, dejamos como última línea
     * de defensa el optimistic locking nativo de JPA.
     *
     * Esto mantiene compatibilidad con posibles clientes
     * antiguos mientras frontend y API terminan de adoptar
     * version de forma obligatoria.
     */
    private void validateExpectedVersion(
        BusinessSettings settings,
        Long expectedVersion
    ) {

        if (expectedVersion == null) {
            return;
        }

        Long currentVersion =
            settings.getVersion();

        if (currentVersion == null) {

            throw new IllegalStateException(
                "BusinessSettings no tiene una versión válida."
            );
        }

        if (
            !currentVersion.equals(
                expectedVersion
            )
        ) {

            throw new ConflictException(
                "La configuración ha cambiado desde que fue cargada. "
                    + "Versión esperada: "
                    + expectedVersion
                    + ", versión actual: "
                    + currentVersion
                    + "."
            );
        }
    }

    // =========================================================
    // TARGET STATE VALIDATION
    // =========================================================

    /**
     * Valida el estado completo que tendrá BusinessSettings
     * una vez aplicado el PATCH.
     *
     * Esto evita validar únicamente campos aislados y permite
     * proteger relaciones entre diferentes propiedades.
     */
    private void validateTargetState(
        int minimumBookingNoticeMinutes,
        int bookingHorizonDays,
        int slotIntervalMinutes,
        int defaultBufferBeforeMinutes,
        int defaultBufferAfterMinutes,
        int minimumCancellationNoticeMinutes,
        int minimumRescheduleNoticeMinutes,
        boolean remindersEnabled,
        int firstReminderMinutesBefore,
        boolean secondReminderEnabled,
        int secondReminderMinutesBefore
    ) {

        // -----------------------------------------------------
        // BOOKING WINDOW
        // -----------------------------------------------------

        validateRange(
            minimumBookingNoticeMinutes,
            0,
            MAX_MINUTES_IN_ONE_YEAR,
            "minimumBookingNoticeMinutes"
        );

        validateRange(
            bookingHorizonDays,
            1,
            MAX_BOOKING_HORIZON_DAYS,
            "bookingHorizonDays"
        );

        // -----------------------------------------------------
        // SLOT GENERATION
        // -----------------------------------------------------

        validateRange(
            slotIntervalMinutes,
            MIN_SLOT_INTERVAL_MINUTES,
            MAX_SLOT_INTERVAL_MINUTES,
            "slotIntervalMinutes"
        );

        // -----------------------------------------------------
        // BUFFERS
        // -----------------------------------------------------

        validateRange(
            defaultBufferBeforeMinutes,
            0,
            MAX_BUFFER_MINUTES,
            "defaultBufferBeforeMinutes"
        );

        validateRange(
            defaultBufferAfterMinutes,
            0,
            MAX_BUFFER_MINUTES,
            "defaultBufferAfterMinutes"
        );

        // -----------------------------------------------------
        // CANCELLATION
        // -----------------------------------------------------

        validateRange(
            minimumCancellationNoticeMinutes,
            0,
            MAX_MINUTES_IN_ONE_YEAR,
            "minimumCancellationNoticeMinutes"
        );

        // -----------------------------------------------------
        // RESCHEDULING
        // -----------------------------------------------------

        validateRange(
            minimumRescheduleNoticeMinutes,
            0,
            MAX_MINUTES_IN_ONE_YEAR,
            "minimumRescheduleNoticeMinutes"
        );

        // -----------------------------------------------------
        // REMINDERS
        // -----------------------------------------------------

        validateRange(
            firstReminderMinutesBefore,
            1,
            MAX_MINUTES_IN_ONE_YEAR,
            "firstReminderMinutesBefore"
        );

        validateRange(
            secondReminderMinutesBefore,
            1,
            MAX_MINUTES_IN_ONE_YEAR,
            "secondReminderMinutesBefore"
        );

        /*
         * Defensa adicional.
         *
         * En condiciones normales este caso ya habrá sido
         * normalizado antes de llegar aquí.
         */
        if (
            secondReminderEnabled
                && !remindersEnabled
        ) {

            throw new IllegalArgumentException(
                "No puede habilitarse el segundo recordatorio "
                    + "si los recordatorios generales están "
                    + "deshabilitados."
            );
        }

        /*
         * Cuando existe segundo recordatorio, este debe
         * estar más cerca de la cita que el primero.
         *
         * Ejemplo válido:
         *
         * first  = 1440 minutos
         * second = 120 minutos
         */
        if (
            secondReminderEnabled
                && secondReminderMinutesBefore
                    >= firstReminderMinutesBefore
        ) {

            throw new IllegalArgumentException(
                "El segundo recordatorio debe ejecutarse "
                    + "más cerca de la cita que el primero."
            );
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateUpdateRequest(
        BusinessSettingsUpdateRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                "La solicitud de actualización "
                    + "de configuración es obligatoria."
            );
        }
    }

    // =========================================================
    // BUSINESS ID VALIDATION
    // =========================================================

    private void validateBusinessId(
        Long businessId
    ) {

        if (businessId == null) {

            throw new IllegalArgumentException(
                "businessId es obligatorio."
            );
        }

        if (businessId <= 0) {

            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // RANGE VALIDATION
    // =========================================================

    private void validateRange(
        int value,
        int minimum,
        int maximum,
        String fieldName
    ) {

        if (
            value < minimum
                || value > maximum
        ) {

            throw new IllegalArgumentException(
                fieldName
                    + " debe estar entre "
                    + minimum
                    + " y "
                    + maximum
                    + "."
            );
        }
    }

    // =========================================================
    // PATCH HELPERS
    // =========================================================

    private int valueOrCurrent(
        Integer requestedValue,
        int currentValue
    ) {

        return requestedValue != null
            ? requestedValue
            : currentValue;
    }

    private boolean valueOrCurrent(
        Boolean requestedValue,
        boolean currentValue
    ) {

        return requestedValue != null
            ? requestedValue
            : currentValue;
    }
}