package com.universidad.compras.aprobacion;

import com.universidad.compras.modelo.Solicitud;
import java.util.function.Function;

public record NivelPorMonto(String nombre, double montoMaximo) implements NivelAprobacion {
    @Override
    public ResultadoAprobacion evaluar(Solicitud solicitud, Function<Solicitud, ResultadoAprobacion> siguiente) {
        if (solicitud.getMonto() > montoMaximo) {
            return siguiente.apply(solicitud);
        }
        return new ResultadoAprobacion(true, nombre, "Monto dentro de la autoridad del nivel");
    }
}
