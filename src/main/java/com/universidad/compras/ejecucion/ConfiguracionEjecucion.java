package com.universidad.compras.ejecucion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ConfiguracionEjecucion {
    @Bean
    PresupuestoService presupuestoService() {
        return new PresupuestoService();
    }

    @Bean
    OrdenCompraService ordenCompraService() {
        return new OrdenCompraService();
    }
}
