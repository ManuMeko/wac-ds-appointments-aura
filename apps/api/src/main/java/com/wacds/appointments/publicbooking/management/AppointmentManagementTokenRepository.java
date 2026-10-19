
package com.wacds.appointments.publicbooking.management;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WAC D.S. - Model 01 - Appointments
 *
 * Repositorio de credenciales privadas para la gestion
 * publica de reservas.
 *
 * Seguridad:
 *
 * - Los tokens originales nunca se almacenan.
 * - Las busquedas utilizan hashes SHA-256.
 * - Los tokens revocados no son utilizables.
 * - Los tokens caducados no son utilizables.
 * - Cada token pertenece a una Appointment.
 * - Las operaciones de escritura pueden adquirir
 *   un bloqueo pesimista sobre la credencial.
 *
 * IMPORTANTE:
 *
 * El bloqueo pesimista solamente protege las operaciones
 * que utilizan la consulta correspondiente y conservan
 * una transaccion activa durante toda la operacion.
 *
 * La seguridad concurrente completa tambien depende
 * de los servicios y de las restricciones de la BD.
 */
public interface AppointmentManagementTokenRepository
    extends JpaRepository<AppointmentManagementToken, Long> {

    // =========================================================
    // TOKEN HASH LOOKUP
    // =========================================================

    /**
     * Recupera una credencial por su hash exacto.
     *
     * No comprueba necesariamente que siga activa.
     *
     * Utilizar para operaciones administrativas
     * o revocaciones individuales.
     */
    Optional<AppointmentManagementToken> findByTokenHash(
        String tokenHash
    );

    /**
     * Comprueba la existencia de un hash.
     *
     * Utilizado durante la emision de nuevos tokens
     * para detectar colisiones inesperadas.
     */
    boolean existsByTokenHash(
        String tokenHash
    );

    // =========================================================
    // APPOINTMENT LOOKUPS
    // =========================================================

    /**
     * Recupera todas las credenciales de una reserva,
     * incluyendo las revocadas y las expiradas.
     *
     * Uso previsto:
     *
     * - auditoria interna;
     * - diagnostico;
     * - rotacion de credenciales;
     * - herramientas administrativas.
     *
     * No exponer directamente mediante un endpoint publico
     * que acepte unicamente appointmentId.
     */
    List<AppointmentManagementToken>
        findAllByAppointmentIdOrderByCreatedAtDesc(
            Long appointmentId
        );

    /**
     * Recupera credenciales que no han sido revocadas.
     *
     * ATENCION:
     *
     * Esta consulta no excluye necesariamente
     * las credenciales expiradas.
     */
    List<AppointmentManagementToken>
        findAllByAppointmentIdAndRevokedAtIsNullOrderByCreatedAtDesc(
            Long appointmentId
        );

    // =========================================================
    // ACTIVE TOKEN LOOKUP - READ ONLY
    // =========================================================

    /**
     * Busca una credencial tecnicamente utilizable.
     *
     * Condiciones:
     *
     * - Coincidencia exacta del hash.
     * - revokedAt IS NULL.
     * - expiresAt IS NULL o expiresAt > now.
     *
     * Este metodo NO adquiere bloqueo pesimista.
     *
     * Es adecuado para consultas de lectura
     * que no modifican el estado de la reserva.
     *
     * Las reglas funcionales relativas al estado,
     * plazos y configuracion del negocio se validan
     * en la capa de servicios.
     */
    @Query("""
        SELECT token
        FROM AppointmentManagementToken token
        WHERE token.tokenHash = :tokenHash
          AND token.revokedAt IS NULL
          AND (
                token.expiresAt IS NULL
                OR token.expiresAt > :now
              )
        """)
    Optional<AppointmentManagementToken> findUsableByTokenHash(
        @Param("tokenHash") String tokenHash,
        @Param("now") OffsetDateTime now
    );

    // =========================================================
    // ACTIVE TOKEN LOOKUP - PESSIMISTIC WRITE LOCK
    // =========================================================

    /**
     * Recupera una credencial utilizable adquiriendo
     * un bloqueo pesimista de escritura.
     *
     * Esta consulta debe utilizarse en las operaciones
     * que modifican el estado asociado a una credencial.
     *
     * Ejemplos:
     *
     * - cancelacion;
     * - reprogramacion;
     * - revocacion coordinada;
     * - actualizacion del ultimo uso.
     *
     * PESSIMISTIC_WRITE se traduce normalmente en
     * un bloqueo de fila en PostgreSQL.
     *
     * Si otra transaccion mantiene un bloqueo incompatible
     * sobre la misma fila, esta consulta debera esperar
     * hasta que el bloqueo sea liberado o se produzca
     * un error de concurrencia/timeout.
     *
     * IMPORTANTE:
     *
     * El metodo debe invocarse dentro de una transaccion
     * de escritura gestionada por Spring.
     *
     * El bloqueo se mantiene hasta finalizar
     * dicha transaccion.
     *
     * Esta consulta no sustituye el bloqueo de agenda
     * necesario para evitar dobles reservas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT token
        FROM AppointmentManagementToken token
        WHERE token.tokenHash = :tokenHash
          AND token.revokedAt IS NULL
          AND (
                token.expiresAt IS NULL
                OR token.expiresAt > :now
              )
        """)
    Optional<AppointmentManagementToken>
        findUsableForUpdateByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") OffsetDateTime now
        );

    // =========================================================
    // APPOINTMENT - USABLE TOKENS
    // =========================================================

    /**
     * Recupera las credenciales utilizables
     * pertenecientes a una Appointment.
     *
     * Solamente devuelve tokens:
     *
     * - no revocados;
     * - no expirados.
     *
     * Ordenados del mas reciente al mas antiguo.
     *
     * Esta consulta no garantiza por si misma
     * que exista una sola credencial activa.
     */
    @Query("""
        SELECT token
        FROM AppointmentManagementToken token
        WHERE token.appointment.id = :appointmentId
          AND token.revokedAt IS NULL
          AND (
                token.expiresAt IS NULL
                OR token.expiresAt > :now
              )
        ORDER BY token.createdAt DESC
        """)
    List<AppointmentManagementToken>
        findUsableByAppointmentIdOrderByCreatedAtDesc(
            @Param("appointmentId") Long appointmentId,
            @Param("now") OffsetDateTime now
        );

    // =========================================================
    // REVOCATION
    // =========================================================

    /**
     * Revoca todas las credenciales no revocadas
     * asociadas a una Appointment.
     *
     * Caracteristicas:
     *
     * - Operacion masiva en base de datos.
     * - Idempotente respecto a credenciales ya revocadas.
     * - Devuelve el numero de filas modificadas.
     * - Actualiza revokedAt y updatedAt.
     *
     * IMPORTANTE:
     *
     * El UPDATE puede adquirir bloqueos de fila propios
     * de PostgreSQL, pero no constituye por si solo
     * un protocolo completo de serializacion.
     *
     * Las emisiones y las operaciones de gestion deben
     * coordinar sus bloqueos en los servicios.
     *
     * clearAutomatically=true limpia el contexto
     * de persistencia despues del UPDATE.
     *
     * Cualquier entidad previamente gestionada puede
     * quedar desacoplada; los servicios deben evitar
     * depender de cambios pendientes sin persistir.
     */
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("""
        UPDATE AppointmentManagementToken token
        SET token.revokedAt = :revokedAt,
            token.updatedAt = :revokedAt
        WHERE token.appointment.id = :appointmentId
          AND token.revokedAt IS NULL
        """)
    int revokeAllActiveByAppointmentId(
        @Param("appointmentId") Long appointmentId,
        @Param("revokedAt") OffsetDateTime revokedAt
    );

    // =========================================================
    // EXPIRED TOKEN LOOKUP
    // =========================================================

    /**
     * Recupera credenciales expiradas
     * que todavia no han sido revocadas.
     *
     * Uso previsto:
     *
     * - mantenimiento;
     * - auditoria;
     * - limpieza controlada;
     * - tareas programadas.
     *
     * Este metodo solamente consulta.
     * No elimina ni modifica credenciales.
     */
    @Query("""
        SELECT token
        FROM AppointmentManagementToken token
        WHERE token.revokedAt IS NULL
          AND token.expiresAt IS NOT NULL
          AND token.expiresAt <= :now
        ORDER BY token.expiresAt ASC
        """)
    List<AppointmentManagementToken> findExpiredNotRevoked(
        @Param("now") OffsetDateTime now
    );
}
