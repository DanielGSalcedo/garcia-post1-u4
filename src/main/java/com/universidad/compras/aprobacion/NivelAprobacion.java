package com.universidad.compras.aprobacion;

import com.universidad.compras.modelo.Solicitud;
import java.util.function.Function;

@FunctionalInterface
public interface NivelAprobacion {
    ResultadoAprobacion evaluar(Solicitud solicitud, Function<Solicitud, ResultadoAprobacion> siguiente);
}
