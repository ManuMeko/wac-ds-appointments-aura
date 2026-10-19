
package com.wacds.appointments.availability;

import com.wacds.appointments.professional.Professional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Representa una franja semanal de disponibilidad
 * de un profesional.
 *
 * Ejemplo:
 *
 * Profesional: 15
 * Dia: MONDAY
 * Inicio: 09:00
 * Fin: 14:00
 * Activo: true
 *
 * Un profesional puede tener varias franjas
 * para un mismo dia.
 *
 * Ejemplo:
 *
 * Lunes:
 * 09:00 - 14:00
 * 16:00 - 20:00
 *
 * REGLAS:
 *
 * - El profesional es obligatorio.
 * - El dia de la semana es obligatorio.
 * - La hora de inicio es obligatoria.
 * - La hora de finalizacion es obligatoria.
 * - La hora final debe ser posterior a la inicial.
 * - No se admiten franjas que crucen medianoche.
 *
 * IMPORTANTE:
 *
 * Esta entidad no controla por si sola
 * las reservas existentes ni los solapamientos
 * entre diferentes registros de disponibilidad.
 *
 * AvailabilitySlotService calcula los horarios
 * que pueden ofrecerse a los clientes.
 *
 * PublicBookingService protege las operaciones
 * de reserva mediante el bloqueo transaccional
 * de la agenda del profesional.
 *
 * COMPATIBILIDAD:
 *
 * Se mantienen la tabla, los indices,
 * las columnas y las relaciones existentes.
 */
@Entity
@Table(
    name = "professional_availability",
    indexes = {
        @Index(
            name = "idx_professional_availability_professional_id",
            columnList = "professional_id"
        ),
        @Index(
            name = "idx_professional_availability_professional_day",
            columnList = "professional_id, day_of_week"
        ),
        @Index(
            name = "idx_professional_availability_active",
            columnList = "professional_id, active"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalAvailability {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional propietario del horario.
     *
     * Cada disponibilidad corresponde
     * obligatoriamente a un profesional.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "professional_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_professional_availability_professional"
        )
    )
    private Professional professional;

    // =========================================================
    // DAY OF WEEK
    // =========================================================

    /**
     * Dia al que corresponde la franja.
     *
     * Se almacena como texto:
     *
     * MONDAY, TUESDAY, WEDNESDAY,
     * THURSDAY, FRIDAY, SATURDAY, SUNDAY.
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "day_of_week",
        nullable = false,
        length = 20
    )
    private DayOfWeek dayOfWeek;

    // =========================================================
    // WORKING HOURS
    // =========================================================

    /**
     * Hora de comienzo de la franja.
     *
     * Interpretada en la zona horaria
     * del negocio propietario.
     */
    @Column(
        name = "start_time",
        nullable = false
    )
    private LocalTime startTime;

    /**
     * Hora de finalizacion.
     *
     * Debe ser posterior a startTime.
     */
    @Column(
        name = "end_time",
        nullable = false
    )
    private LocalTime endTime;

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Indica si esta franja participa
     * en el calculo de disponibilidad.
     *
     * Se mantiene false como valor predeterminado,
     * igual que en la entidad original.
     */
    @Column(nullable = false)
    private boolean active;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    /**
     * Valida la entidad antes de insertarla.
     */
    @PrePersist
    void prePersist() {

        validate();

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    /**
     * Valida la entidad antes de actualizarla.
     */
    @PreUpdate
    void preUpdate() {

        validate();

        updatedAt = OffsetDateTime.now();
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    /**
     * Valida la consistencia de la franja.
     */
    private void validate() {

        validateProfessional();

        validateDayOfWeek();

        validateTimeRange(
            startTime,
            endTime
        );
    }

    /**
     * Verifica que exista un profesional asociado.
     *
     * No se exige ID en este punto para mantener
     * compatibilidad con el ciclo de vida de JPA.
     *
     * La clave foranea garantiza la asociacion
     * correcta al persistir.
     */
    private void validateProfessional() {

        if (professional == null) {
            throw new IllegalStateException(
                "La disponibilidad debe pertenecer "
                    + "a un profesional."
            );
        }
    }

    private void validateDayOfWeek() {

        if (dayOfWeek == null) {
            throw new IllegalStateException(
                "El día de la semana es obligatorio."
            );
        }
    }

    /**
     * Comprueba que ambas horas sean validas.
     *
     * El modelo actual solo permite franjas
     * dentro de un mismo dia.
     */
    private void validateTimeRange(
        LocalTime start,
        LocalTime end
    ) {

        if (start == null) {
            throw new IllegalStateException(
                "La hora de inicio es obligatoria."
            );
        }

        if (end == null) {
            throw new IllegalStateException(
                "La hora de finalización es obligatoria."
            );
        }

        if (!end.isAfter(start)) {
            throw new IllegalStateException(
                "La hora de finalización debe ser "
                    + "posterior a la hora de inicio."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS - STATUS
    // =========================================================

    /**
     * Habilita esta franja laboral.
     */
    public void activate() {

        this.active = true;
    }

    /**
     * Deshabilita esta franja laboral.
     *
     * No elimina el registro.
     */
    public void deactivate() {

        this.active = false;
    }

    /**
     * Actualiza el estado activo.
     */
    public void changeActiveStatus(
        boolean enabled
    ) {

        this.active = enabled;
    }

    // =========================================================
    // DOMAIN HELPERS - WORKING HOURS
    // =========================================================

    /**
     * Modifica el intervalo horario.
     *
     * Valida los valores antes de asignarlos,
     * evitando modificar la entidad cuando
     * el intervalo solicitado es invalido.
     */
    public void changeWorkingHours(
        LocalTime newStartTime,
        LocalTime newEndTime
    ) {

        validateTimeRange(
            newStartTime,
            newEndTime
        );

        this.startTime = newStartTime;
        this.endTime = newEndTime;
    }

    /**
     * Modifica el dia de la semana.
     */
    public void changeDayOfWeek(
        DayOfWeek newDayOfWeek
    ) {

        if (newDayOfWeek == null) {
            throw new IllegalArgumentException(
                "El día de la semana es obligatorio."
            );
        }

        this.dayOfWeek = newDayOfWeek;
    }

    // =========================================================
    // DOMAIN QUERIES
    // =========================================================

    /**
     * Determina si esta franja pertenece
     * al profesional indicado.
     */
    public boolean belongsToProfessional(
        Long professionalId
    ) {

        if (
            professionalId == null
                || professional == null
        ) {
            return false;
        }

        return Objects.equals(
            professional.getId(),
            professionalId
        );
    }

    /**
     * Determina si la franja esta habilitada
     * para participar en disponibilidad.
     */
    public boolean isAvailableForBooking() {

        return active
            && professional != null
            && dayOfWeek != null
            && startTime != null
            && endTime != null
            && endTime.isAfter(startTime);
    }

    /**
     * Comprueba si una hora determinada
     * se encuentra dentro de la franja.
     *
     * Se utiliza un intervalo semiabierto:
     *
     * [startTime, endTime)
     */
    public boolean containsTime(
        LocalTime time
    ) {

        if (
            time == null
                || startTime == null
                || endTime == null
                || !endTime.isAfter(startTime)
        ) {
            return false;
        }

        return !time.isBefore(startTime)
            && time.isBefore(endTime);
    }

    // =========================================================
    // DOMAIN FACTORY
    // =========================================================

    /**
     * Crea una franja activa y validada.
     *
     * Util para inicializar horarios
     * en servicios y pruebas de integracion.
     */
    public static ProfessionalAvailability createActive(
        Professional professional,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {

        ProfessionalAvailability availability =
            ProfessionalAvailability.builder()
                .professional(professional)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .active(true)
                .build();

        availability.validate();

        return availability;
    }
}
