package com.wacds.appointments.professional;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository
    extends JpaRepository<Professional, Long> {

    List<Professional> findAllByBusinessIdOrderByDisplayNameAsc(
        Long businessId
    );

    List<Professional> findAllByBusinessIdAndActiveTrueOrderByDisplayNameAsc(
        Long businessId
    );

    List<Professional>
        findAllByBusinessIdAndActiveTrueAndBookingEnabledTrueOrderByDisplayNameAsc(
            Long businessId
        );

    Optional<Professional> findByIdAndBusinessId(
        Long id,
        Long businessId
    );

    boolean existsByBusinessIdAndEmailIgnoreCase(
        Long businessId,
        String email
    );
}