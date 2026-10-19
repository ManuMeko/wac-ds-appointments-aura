package com.wacds.appointments.business.settings;

import com.wacds.appointments.business.dto.BusinessSettingsResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BusinessSettingsMapper {

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    /**
     * Convierte una entidad BusinessSettings persistida
     * en la respuesta utilizada por la API.
     *
     * Este mapper no contiene lógica de negocio:
     *
     * - no modifica valores;
     * - no aplica defaults;
     * - no corrige configuraciones;
     * - no ejecuta validaciones funcionales.
     *
     * Su única responsabilidad es transformar el estado
     * persistido de BusinessSettings en BusinessSettingsResponse.
     */
    public BusinessSettingsResponse toResponse(
        BusinessSettings settings
    ) {

        if (settings == null) {
            return null;
        }

        validateForMapping(
            settings
        );

        return new BusinessSettingsResponse(

            // =================================================
            // IDENTITY
            // =================================================

            settings.getId(),

            settings
                .getBusiness()
                .getId(),

            // =================================================
            // BOOKING WINDOW
            // =================================================

            settings
                .getMinimumBookingNoticeMinutes(),

            settings
                .getBookingHorizonDays(),

            // =================================================
            // SLOT GENERATION
            // =================================================

            settings
                .getSlotIntervalMinutes(),

            // =================================================
            // DEFAULT BUFFERS
            // =================================================

            settings
                .getDefaultBufferBeforeMinutes(),

            settings
                .getDefaultBufferAfterMinutes(),

            // =================================================
            // CONFIRMATION
            // =================================================

            settings
                .isAutoConfirmAppointments(),

            // =================================================
            // ONLINE CANCELLATION
            // =================================================

            settings
                .isOnlineCancellationEnabled(),

            settings
                .getMinimumCancellationNoticeMinutes(),

            // =================================================
            // ONLINE RESCHEDULING
            // =================================================

            settings
                .isOnlineReschedulingEnabled(),

            settings
                .getMinimumRescheduleNoticeMinutes(),

            // =================================================
            // REMINDERS
            // =================================================

            settings
                .isRemindersEnabled(),

            settings
                .getFirstReminderMinutesBefore(),

            settings
                .isSecondReminderEnabled(),

            settings
                .getSecondReminderMinutesBefore(),

            // =================================================
            // OPTIMISTIC LOCKING
            // =================================================

            settings
                .getVersion(),

            // =================================================
            // AUDIT
            // =================================================

            settings
                .getCreatedAt(),

            settings
                .getUpdatedAt()
        );
    }

    // =========================================================
    // ENTITY LIST -> RESPONSE LIST
    // =========================================================

    /**
     * Convierte una colección de BusinessSettings
     * en una lista de respuestas API.
     *
     * Si la lista es:
     *
     * - null;
     * - vacía;
     *
     * devuelve una lista inmutable vacía.
     */
    public List<BusinessSettingsResponse> toResponseList(
        List<BusinessSettings> settingsList
    ) {

        if (
            settingsList == null
                || settingsList.isEmpty()
        ) {
            return List.of();
        }

        return settingsList
            .stream()
            .filter(
                Objects::nonNull
            )
            .map(
                this::toResponse
            )
            .toList();
    }

    // =========================================================
    // INTERNAL VALIDATION
    // =========================================================

    /**
     * Valida exclusivamente que la entidad contiene
     * los datos estructurales mínimos necesarios
     * para generar una respuesta coherente.
     *
     * No valida reglas funcionales de BusinessSettings.
     *
     * Las reglas de negocio deben permanecer en:
     *
     * - BusinessSettings;
     * - BusinessSettingsService;
     * - servicios operativos consumidores.
     */
    private void validateForMapping(
        BusinessSettings settings
    ) {

        if (settings.getId() == null) {
            throw new IllegalStateException(
                "BusinessSettings no está persistido."
            );
        }

        if (settings.getBusiness() == null) {
            throw new IllegalStateException(
                "BusinessSettings no tiene un Business asociado."
            );
        }

        if (
            settings
                .getBusiness()
                .getId() == null
        ) {
            throw new IllegalStateException(
                "El Business asociado a BusinessSettings "
                    + "no está persistido."
            );
        }

        if (settings.getVersion() == null) {
            throw new IllegalStateException(
                "BusinessSettings no tiene una versión válida."
            );
        }

        if (settings.getCreatedAt() == null) {
            throw new IllegalStateException(
                "BusinessSettings no tiene createdAt."
            );
        }

        if (settings.getUpdatedAt() == null) {
            throw new IllegalStateException(
                "BusinessSettings no tiene updatedAt."
            );
        }
    }
}