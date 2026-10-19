package com.wacds.appointments.reminder;

import com.wacds.appointments.reminder.AppointmentReminder.ReminderChannel;

/**
 * Contrato común para cualquier proveedor encargado de entregar
 * recordatorios de AppointmentReminder.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Esta interfaz desacopla completamente:
 *
 * - dominio;
 * - worker;
 * - scheduler;
 * - persistencia;
 * - lógica de negocio;
 *
 * del proveedor concreto utilizado para entregar:
 *
 * - EMAIL;
 * - SMS;
 * - WHATSAPP.
 *
 * =========================================================
 * ARQUITECTURA
 * =========================================================
 *
 * AppointmentReminderWorker
 *          ↓
 * ReminderDeliveryService
 *          ↓
 * implementación concreta
 *
 * Ejemplos:
 *
 * DevelopmentReminderDeliveryService
 * ResendEmailReminderDeliveryService
 * SmsReminderDeliveryService
 * WhatsAppReminderDeliveryService
 *
 * =========================================================
 * RESPONSABILIDADES
 * =========================================================
 *
 * Una implementación de esta interfaz:
 *
 * - determina qué canal soporta;
 * - intenta realizar la entrega;
 * - traduce la respuesta externa a DeliveryResult;
 * - no modifica directamente AppointmentReminder;
 * - no cambia status;
 * - no persiste sentAt;
 * - no incrementa attemptCount;
 * - no modifica lastError.
 *
 * La persistencia del resultado pertenece a
 * AppointmentReminderService / AppointmentReminderWorker.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Ningún DeliveryResult debe contener:
 *
 * - API keys;
 * - passwords;
 * - access tokens;
 * - management tokens;
 * - payloads sensibles;
 * - stack traces completos;
 * - credenciales externas.
 */
public interface ReminderDeliveryService {

    // =========================================================
    // SUPPORT
    // =========================================================

    /**
     * Indica si esta implementación puede procesar
     * el canal solicitado.
     *
     * Permite registrar múltiples proveedores simultáneamente.
     *
     * Ejemplo:
     *
     * ResendEmailReminderDeliveryService
     *     supports(EMAIL) = true
     *
     * SmsReminderDeliveryService
     *     supports(SMS) = true
     */
    boolean supports(
        ReminderChannel channel
    );

    // =========================================================
    // DELIVER
    // =========================================================

    /**
     * Intenta entregar un recordatorio.
     *
     * =====================================================
     * PRECONDICIÓN
     * =====================================================
     *
     * El worker debe haber realizado previamente:
     *
     * PENDING / FAILED
     *        ↓
     * PROCESSING
     *
     * =====================================================
     * POSTCONDICIÓN
     * =====================================================
     *
     * Este método únicamente devuelve:
     *
     * DeliveryResult.success = true
     *
     * o:
     *
     * DeliveryResult.success = false
     *
     * No debe persistir directamente ningún cambio de estado.
     */
    DeliveryResult deliver(
        AppointmentReminder reminder
    );

    // =========================================================
    // DELIVERY RESULT
    // =========================================================

    /**
     * Resultado independiente del proveedor externo.
     *
     * De esta forma detalles específicos de:
     *
     * - Resend;
     * - SES;
     * - Brevo;
     * - Twilio;
     * - Meta;
     *
     * no contaminan el dominio.
     */
    record DeliveryResult(

        /**
         * true cuando el proveedor ha aceptado correctamente
         * la entrega.
         */
        boolean success,

        /**
         * Identificador externo asignado por el proveedor.
         *
         * Ejemplo:
         *
         * Resend message id.
         *
         * Puede ser null cuando el proveedor no proporcione uno.
         */
        String providerMessageId,

        /**
         * Mensaje de error sanitizado.
         *
         * Solo debe contener información segura para:
         *
         * - persistencia;
         * - logs;
         * - observabilidad.
         */
        String errorMessage

    ) {

        // =====================================================
        // CONSTANTS
        // =====================================================

        private static final String DEFAULT_ERROR_MESSAGE =
            "El proveedor no pudo entregar el recordatorio.";

        private static final int MAX_PROVIDER_MESSAGE_ID_LENGTH =
            255;

        private static final int MAX_ERROR_MESSAGE_LENGTH =
            500;

        // =====================================================
        // COMPACT CONSTRUCTOR
        // =====================================================

        /**
         * Normaliza y valida cualquier DeliveryResult,
         * incluso cuando una implementación lo construye
         * directamente sin usar las factorías.
         *
         * IMPORTANTE:
         *
         * Un constructor compacto de un record NO puede contener
         * un return explícito.
         */
        public DeliveryResult {

            providerMessageId =
                normalizeNullable(
                    providerMessageId,
                    MAX_PROVIDER_MESSAGE_ID_LENGTH
                );

            errorMessage =
                normalizeNullable(
                    errorMessage,
                    MAX_ERROR_MESSAGE_LENGTH
                );

            // -------------------------------------------------
            // SUCCESS VALIDATION
            // -------------------------------------------------

            if (
                success
                    && errorMessage != null
            ) {

                throw new IllegalArgumentException(
                    "Un resultado exitoso no puede contener "
                        + "errorMessage."
                );
            }

            // -------------------------------------------------
            // FAILURE VALIDATION
            // -------------------------------------------------

            if (
                !success
                    && providerMessageId != null
            ) {

                throw new IllegalArgumentException(
                    "Un resultado fallido no puede contener "
                        + "providerMessageId."
                );
            }

            if (
                !success
                    && errorMessage == null
            ) {

                errorMessage =
                    DEFAULT_ERROR_MESSAGE;
            }
        }

        // =====================================================
        // SUCCESS FACTORY
        // =====================================================

        /**
         * Resultado de entrega aceptada correctamente.
         */
        public static DeliveryResult sent(
            String providerMessageId
        ) {

            return new DeliveryResult(
                true,
                providerMessageId,
                null
            );
        }

        /**
         * Resultado exitoso cuando el proveedor no devuelve
         * identificador externo.
         */
        public static DeliveryResult sent() {

            return new DeliveryResult(
                true,
                null,
                null
            );
        }

        // =====================================================
        // FAILURE FACTORY
        // =====================================================

        /**
         * Resultado de entrega fallida.
         */
        public static DeliveryResult failed(
            String errorMessage
        ) {

            return new DeliveryResult(
                false,
                null,
                errorMessage
            );
        }

        /**
         * Resultado fallido con mensaje genérico seguro.
         */
        public static DeliveryResult failed() {

            return new DeliveryResult(
                false,
                null,
                DEFAULT_ERROR_MESSAGE
            );
        }

        // =====================================================
        // SEMANTIC HELPERS
        // =====================================================

        /**
         * Alias semántico para código más legible.
         */
        public boolean sentSuccessfully() {

            return success;
        }

        /**
         * Alias semántico para comprobar fallo.
         */
        public boolean failedDelivery() {

            return !success;
        }

        /**
         * Indica si existe un identificador externo
         * proporcionado por el proveedor.
         */
        public boolean hasProviderMessageId() {

            return providerMessageId != null;
        }

        /**
         * Indica si existe un mensaje de error.
         */
        public boolean hasErrorMessage() {

            return errorMessage != null;
        }

        // =====================================================
        // NORMALIZATION
        // =====================================================

        /**
         * Normaliza valores textuales externos.
         *
         * - null permanece null;
         * - elimina espacios exteriores;
         * - convierte vacío en null;
         * - limita longitud.
         */
        private static String normalizeNullable(
            String value,
            int maxLength
        ) {

            if (
                value == null
            ) {

                return null;
            }

            String normalized =
                value.trim();

            if (
                normalized.isEmpty()
            ) {

                return null;
            }

            if (
                normalized.length()
                    > maxLength
            ) {

                return normalized.substring(
                    0,
                    maxLength
                );
            }

            return normalized;
        }
    }
}