package com.wacds.appointments.professionalservice;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProfessionalServiceAssignmentId implements Serializable {

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "service_id")
    private Long serviceId;
}