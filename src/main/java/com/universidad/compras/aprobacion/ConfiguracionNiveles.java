package com.universidad.compras.aprobacion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
class ConfiguracionNiveles {
    @Bean
    @Order(10)
    NivelAprobacion supervisor() {
        return new NivelPorMonto("Supervisor de Área", 2_000_000);
    }

    @Bean
    @Order(20)
    NivelAprobacion gerente() {
        return new NivelPorMonto("Gerente de Área", 10_000_000);
    }

    @Bean
    @Order(30)
    NivelAprobacion director() {
        return new NivelPorMonto("Director Financiero", Double.POSITIVE_INFINITY);
    }
}
