package com.wacds.appointments.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El bootstrap de desarrollo crea un OWNER si el flag está activo.
 * Con el perfil prod el proceso se niega a arrancar en ese caso,
 * aunque una variable de entorno intente activarlo.
 */
@Component
@Profile("prod")
public class ProductionBootstrapGuard implements InitializingBean {

    private final boolean developmentOwnerBootstrapEnabled;

    public ProductionBootstrapGuard(
        @Value("${app.bootstrap.dev-owner.enabled:false}")
        boolean developmentOwnerBootstrapEnabled
    ) {
        this.developmentOwnerBootstrapEnabled =
            developmentOwnerBootstrapEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        if (developmentOwnerBootstrapEnabled) {
            throw new IllegalStateException(
                "APP_BOOTSTRAP_DEV_OWNER_ENABLED debe ser false "
                    + "con el perfil prod."
            );
        }
    }
}
