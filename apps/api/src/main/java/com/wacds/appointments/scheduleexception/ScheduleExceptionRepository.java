package com.wacds.appointments.scheduleexception;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleExceptionRepository
    extends JpaRepository<ScheduleException, Long> {

    List<ScheduleException>
        findAllByProfessionalIdOrderByExceptionDateAscStartTimeAsc(
            Long professionalId
        );

    List<ScheduleException>
        findAllByProfessionalIdAndActiveTrueOrderByExceptionDateAscStartTimeAsc(
            Long professionalId
        );

    Optional<ScheduleException>
        findByIdAndProfessionalId(
            Long id,
            Long professionalId
        );

    @Query("""
        SELECT scheduleException
        FROM ScheduleException scheduleException
        WHERE scheduleException.professional.id = :professionalId
          AND scheduleException.exceptionDate = :exceptionDate
          AND scheduleException.active = true
        ORDER BY scheduleException.startTime ASC
        """)
    List<ScheduleException>
        findActiveByProfessionalIdAndExceptionDate(
            @Param("professionalId")
            Long professionalId,

            @Param("exceptionDate")
            LocalDate exceptionDate
        );

    @Query("""
        SELECT CASE
            WHEN COUNT(scheduleException) > 0
            THEN true
            ELSE false
        END
        FROM ScheduleException scheduleException
        WHERE scheduleException.professional.id = :professionalId
          AND scheduleException.exceptionDate = :exceptionDate
          AND scheduleException.active = true
        """)
    boolean existsAnyActiveException(
        @Param("professionalId")
        Long professionalId,

        @Param("exceptionDate")
        LocalDate exceptionDate
    );

    @Query("""
        SELECT CASE
            WHEN COUNT(scheduleException) > 0
            THEN true
            ELSE false
        END
        FROM ScheduleException scheduleException
        WHERE scheduleException.professional.id = :professionalId
          AND scheduleException.exceptionDate = :exceptionDate
          AND scheduleException.type =
              com.wacds.appointments.scheduleexception.ScheduleException.ExceptionType.UNAVAILABLE
          AND scheduleException.active = true
        """)
    boolean existsActiveUnavailableException(
        @Param("professionalId")
        Long professionalId,

        @Param("exceptionDate")
        LocalDate exceptionDate
    );

    @Query("""
        SELECT CASE
            WHEN COUNT(scheduleException) > 0
            THEN true
            ELSE false
        END
        FROM ScheduleException scheduleException
        WHERE scheduleException.professional.id = :professionalId
          AND scheduleException.exceptionDate = :exceptionDate
          AND scheduleException.type =
              com.wacds.appointments.scheduleexception.ScheduleException.ExceptionType.CUSTOM_HOURS
          AND scheduleException.active = true
          AND scheduleException.startTime < :endTime
          AND scheduleException.endTime > :startTime
        """)
    boolean existsOverlappingCustomHours(
        @Param("professionalId")
        Long professionalId,

        @Param("exceptionDate")
        LocalDate exceptionDate,

        @Param("startTime")
        LocalTime startTime,

        @Param("endTime")
        LocalTime endTime
    );
}