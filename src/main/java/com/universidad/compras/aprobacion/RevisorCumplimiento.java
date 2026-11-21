package com.universidad.compras.aprobacion;

import com.universidad.compras.modelo.Solicitud;
import java.util.function.Function;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
final class RevisorCumplimiento implements NivelAprobacion {
    private static final String NOMBRE = "Revisor de Cumplimiento Normativo";

    @Override
    public ResultadoAprobacion evaluar(Solicitud solicitud, Function<Solicitud, ResultadoAprobacion> siguiente) {
        if (!"INTERNACIONAL".equals(solicitud.getCategoria())) {
            return siguiente.apply(solicitud);
        }
        if (solicitud.getCentroCosto() == null || solicitud.getCentroCosto().isBlank()) {
            return new ResultadoAprobacion(false, NOMBRE, "La revisión internacional requiere centro de costo");
        }
        var porMonto = siguiente.apply(solicitud);
        if (!porMonto.isAprobada()) {
            return porMonto;
        }
        return new ResultadoAprobacion(true, NOMBRE, "Revisión internacional conforme. Nivel por monto: "
                + porMonto.getNivelResolutor());
    }
}
