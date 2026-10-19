
package com.wacds.appointments.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WAC D.S. - Modelo 01 - Appointments.
 *
 * Repositorio principal de reservas.
 *
 * Responsabilidades:
 *
 * - Consultas aisladas por negocio.
 * - Consultas de agenda por profesional.
 * - Deteccion de solapamientos.
 * - Bloqueo transaccional de agenda.
 * - Bloqueo de citas individuales.
 *
 * SEGURIDAD Y CONCURRENCIA
 *
 * El bloqueo de agenda se realiza sobre la fila
 * del profesional en PostgreSQL mediante FOR UPDATE.
 *
 * La operacion debe ejecutarse dentro de una
 * transaccion de escritura.
 *
 * Todas las operaciones que modifiquen la agenda
 * de un profesional deben coordinarse usando
 * el mismo mecanismo de bloqueo.
 *
 * El repositorio no autoriza por si solo el acceso
 * de usuarios o clientes a una reserva.
 *
 * La autorizacion corresponde a las capas
 * de seguridad y servicios.
 */
public interface AppointmentRepository
    extends JpaRepository<Appointment, Long> {

    // =========================================================
    // PROFESSIONAL SCHEDULE LOCK
    // =========================================================

    /**
     * Bloquea la fila del profesional en PostgreSQL.
     *
     * SELECT ... FOR UPDATE.
     *
     * La condicion business_id evita bloquear
     * profesionales que no pertenezcan al negocio.
     *
     * Este bloqueo permite serializar operaciones
     * de agenda que utilicen el mismo protocolo.
     *
     * Ejemplo:
     *
     * Transaccion A:
     * - bloquea profesional;
     * - valida disponibilidad;
     * - crea o modifica cita;
     * - confirma la transaccion.
     *
     * Transaccion B:
     * - espera el bloqueo;
     * - continua despues de A;
     * - vuelve a consultar disponibilidad.
     *
     * IMPORTANTE:
     *
     * La consulta debe ejecutarse dentro de una
     * transaccion activa.
     *
     * El bloqueo permanece hasta COMMIT o ROLLBACK.
     *
     * No garantiza proteccion frente a otros flujos
     * que escriban citas sin adquirir el mismo bloqueo.
     */
    @Query(
        value = """
            SELECT professional.id
            FROM professionals professional
            WHERE professional.id = :professionalId
              AND professional.business_id = :businessId
            FOR UPDATE
            """,
        nativeQuery = true
    )
    Optional<Long> lockProfessionalSchedule(
        @Param("businessId") Long businessId,
        @Param("professionalId") Long professionalId
    );

    // =========================================================
    // APPOINTMENT ROW LOCK
    // =========================================================

    /**
     * Bloquea una cita individual dentro de su negocio.
     *
     * Utiliza PESSIMISTIC_WRITE de JPA.
     *
     * Preparado para operaciones que necesiten
     * actualizar una cita cuya version persistida
     * deba comprobarse bajo bloqueo.
     *
     * La adquisicion de este bloqueo debe seguir
     * un orden coherente con el bloqueo de agenda
     * y los tokens de gestion.
     *
     * IMPORTANTE:
     *
     * Este metodo se incorpora como soporte.
     * Los servicios actuales no lo utilizan
     * automaticamente.
     *
     * No sustituye al bloqueo de profesional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.id = :appointmentId
          AND appointment.business.id = :businessId
        """)
    Optional<Appointment> lockAppointmentInBusiness(
        @Param("appointmentId") Long appointmentId,
        @Param("businessId") Long businessId
    );

    // =========================================================
    // BUSINESS - BASIC QUERIES
    // =========================================================

    /**
     * Recupera todas las citas de un negocio
     * ordenadas por fecha de inicio descendente.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
        ORDER BY appointment.startAt DESC
        """)
    List<Appointment> findAllByBusinessIdOrderByStartAtDesc(
        @Param("businessId") Long businessId
    );

    /**
     * Recupera una cita solamente si pertenece
     * al negocio indicado.
     *
     * No constituye por si mismo una comprobacion
     * de permisos del usuario autenticado.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.id = :appointmentId
          AND appointment.business.id = :businessId
        """)
    Optional<Appointment> findByIdAndBusinessId(
        @Param("appointmentId") Long appointmentId,
        @Param("businessId") Long businessId
    );

    // =========================================================
    // PROFESSIONAL - TENANT SAFE
    // =========================================================

    /**
     * Recupera las citas de un profesional
     * perteneciente a un negocio concreto.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.professional.id = :professionalId
        ORDER BY appointment.startAt DESC
        """)
    List<Appointment>
        findAllByBusinessIdAndProfessionalIdOrderByStartAtDesc(
            @Param("businessId") Long businessId,
            @Param("professionalId") Long professionalId
        );

    // =========================================================
    // CUSTOMER - TENANT SAFE
    // =========================================================

    /**
     * Recupera las citas de un cliente
     * dentro del negocio correspondiente.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.customer.id = :customerId
        ORDER BY appointment.startAt DESC
        """)
    List<Appointment>
        findAllByBusinessIdAndCustomerIdOrderByStartAtDesc(
            @Param("businessId") Long businessId,
            @Param("customerId") Long customerId
        );

    // =========================================================
    // BUSINESS AGENDA
    // =========================================================

    /**
     * Recupera las citas cuyo inicio pertenece
     * al intervalo [from, to).
     *
     * No representa una consulta de todos los
     * intervalos solapados: filtra por startAt.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.startAt >= :from
          AND appointment.startAt < :to
        ORDER BY appointment.startAt ASC
        """)
    List<Appointment> findBusinessAppointmentsInRange(
        @Param("businessId") Long businessId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    /**
     * Recupera las citas cuyo inicio pertenece
     * al intervalo [from, to) y al estado indicado.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.status = :status
          AND appointment.startAt >= :from
          AND appointment.startAt < :to
        ORDER BY appointment.startAt ASC
        """)
    List<Appointment> findBusinessAppointmentsInRangeByStatus(
        @Param("businessId") Long businessId,
        @Param("status") Appointment.AppointmentStatus status,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    // =========================================================
    // PROFESSIONAL AGENDA - TENANT SAFE
    // =========================================================

    /**
     * Recupera citas de un profesional dentro
     * del negocio indicado.
     *
     * Se filtran las citas por su hora de inicio.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.professional.id = :professionalId
          AND appointment.startAt >= :from
          AND appointment.startAt < :to
        ORDER BY appointment.startAt ASC
        """)
    List<Appointment> findProfessionalAppointmentsInRangeByBusiness(
        @Param("businessId") Long businessId,
        @Param("professionalId") Long professionalId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    /**
     * Recupera citas de un profesional
     * en un estado determinado.
     *
     * Respeta el aislamiento por negocio.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.professional.id = :professionalId
          AND appointment.status = :status
          AND appointment.startAt >= :from
          AND appointment.startAt < :to
        ORDER BY appointment.startAt ASC
        """)
    List<Appointment>
        findProfessionalAppointmentsInRangeByBusinessAndStatus(
            @Param("businessId") Long businessId,
            @Param("professionalId") Long professionalId,
            @Param("status") Appointment.AppointmentStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
        );

    // =========================================================
    // UPCOMING BUSINESS APPOINTMENTS
    // =========================================================

    /**
     * Recupera las citas futuras que no tengan
     * el estado CANCELLED.
     *
     * El parametro from determina desde
     * que instante se buscan reservas.
     */
    @Query("""
        SELECT appointment
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.startAt >= :from
          AND appointment.status <> :cancelledStatus
        ORDER BY appointment.startAt ASC
        """)
    List<Appointment> findUpcomingBusinessAppointments(
        @Param("businessId") Long businessId,
        @Param("from") OffsetDateTime from,
        @Param("cancelledStatus")
        Appointment.AppointmentStatus cancelledStatus
    );

    // =========================================================
    // OVERLAP - TENANT SAFE
    // =========================================================

    /**
     * Comprueba si existe alguna cita no cancelada
     * que se solape con el intervalo solicitado.
     *
     * Regla de solapamiento:
     *
     * existing.startAt < requested.endAt
     * &&
     * existing.endAt > requested.startAt
     *
     * Los intervalos se interpretan como [inicio, fin).
     *
     * Dos citas contiguas no se consideran
     * solapadas por esta consulta.
     *
     * Los buffers adicionales deben comprobarse
     * mediante el motor central de disponibilidad.
     *
     * CANCELLED no bloquea esta comprobacion.
     *
     * Esta consulta no bloquea por si sola
     * nuevas inserciones concurrentes.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(appointment) > 0
            THEN true
            ELSE false
        END
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.professional.id = :professionalId
          AND appointment.status <> :cancelledStatus
          AND appointment.startAt < :endAt
          AND appointment.endAt > :startAt
        """)
    boolean existsOverlappingAppointmentInBusiness(
        @Param("businessId") Long businessId,
        @Param("professionalId") Long professionalId,
        @Param("startAt") OffsetDateTime startAt,
        @Param("endAt") OffsetDateTime endAt,
        @Param("cancelledStatus")
        Appointment.AppointmentStatus cancelledStatus
    );

    // =========================================================
    // OVERLAP EXCLUDING CURRENT APPOINTMENT
    // =========================================================

    /**
     * Comprueba si una reprogramacion provocaria
     * solapamiento con otra reserva.
     *
     * Excluye expresamente appointmentId.
     *
     * Esta exclusion es necesaria porque se
     * modifica la misma Appointment existente.
     *
     * Mantiene:
     *
     * - filtro por negocio;
     * - filtro por profesional;
     * - exclusion de CANCELLED;
     * - comparacion por instantes temporales.
     *
     * IMPORTANTE:
     *
     * Se debe ejecutar bajo el bloqueo de agenda
     * utilizado por el resto de las operaciones.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(appointment) > 0
            THEN true
            ELSE false
        END
        FROM Appointment appointment
        WHERE appointment.business.id = :businessId
          AND appointment.professional.id = :professionalId
          AND appointment.id <> :appointmentId
          AND appointment.status <> :cancelledStatus
          AND appointment.startAt < :endAt
          AND appointment.endAt > :startAt
        """)
    boolean existsOverlappingAppointmentInBusinessExcludingId(
        @Param("businessId") Long businessId,
        @Param("professionalId") Long professionalId,
        @Param("appointmentId") Long appointmentId,
        @Param("startAt") OffsetDateTime startAt,
        @Param("endAt") OffsetDateTime endAt,
        @Param("cancelledStatus")
        Appointment.AppointmentStatus cancelledStatus
    );
}
