package com.wacds.appointments.scheduleexception;

import com.wacds.appointments.professional.Professional;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "schedule_exceptions",
    indexes = {
        @Index(
            name = "idx_schedule_exceptions_professional_id",
            columnList = "professional_id"
        ),
        @Index(
            name = "idx_schedule_exceptions_professional_date",
            columnList = "professional_id, exception_date"
        ),
        @Index(
            name = "idx_schedule_exceptions_professional_active",
            columnList = "professional_id, active"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleException {

    public enum ExceptionType {
        UNAVAILABLE,
        CUSTOM_HOURS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "professional_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_schedule_exceptions_professional"
        )
    )
    private Professional professional;

    @Column(
        name = "exception_date",
        nullable = false
    )
    private LocalDate exceptionDate;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private ExceptionType type;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active;

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

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}