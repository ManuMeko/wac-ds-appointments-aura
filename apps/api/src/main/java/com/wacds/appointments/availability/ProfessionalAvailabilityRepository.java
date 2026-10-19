package com.wacds.appointments.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ProfessionalAvailabilityRepository
    extends JpaRepository<ProfessionalAvailability, Long> {

    List<ProfessionalAvailability>
        findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(
            Long professionalId
        );

    List<ProfessionalAvailability>
        findAllByProfessionalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
            Long professionalId
        );

    Optional<ProfessionalAvailability>
        findByIdAndProfessionalId(
            Long id,
            Long professionalId
        );

    @Query("""
        SELECT CASE
            WHEN COUNT(availability) > 0
            THEN true
            ELSE false
        END
        FROM ProfessionalAvailability availability
        WHERE availability.professional.id = :professionalId
          AND availability.dayOfWeek = :dayOfWeek
          AND availability.active = true
          AND availability.startTime < :endTime
          AND availability.endTime > :startTime
        """)
    boolean existsOverlappingAvailability(
        @Param("professionalId")
        Long professionalId,

        @Param("dayOfWeek")
        DayOfWeek dayOfWeek,

        @Param("startTime")
        LocalTime startTime,

        @Param("endTime")
        LocalTime endTime
    );
}