package com.wacds.appointments.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionBootstrapGuardTests {

    @Test
    void allowsTheProductionDefault() {
        ProductionBootstrapGuard guard =
            new ProductionBootstrapGuard(false);

        assertDoesNotThrow(guard::afterPropertiesSet);
    }

    @Test
    void refusesToStartWhenDevelopmentBootstrapIsEnabled() {
        ProductionBootstrapGuard guard =
            new ProductionBootstrapGuard(true);

        assertThrows(
            IllegalStateException.class,
            guard::afterPropertiesSet
        );
    }
}
