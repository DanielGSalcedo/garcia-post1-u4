package com.universidad.compras.aprobacion;

import com.universidad.compras.modelo.Solicitud;
import com.universidad.compras.estado.FlujoSolicitud;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class CadenaAprobacion implements ServicioAprobacion {
    private final Function<Solicitud, ResultadoAprobacion> inicio;
    private final FlujoSolicitud flujo;

    public CadenaAprobacion(List<NivelAprobacion> niveles, FlujoSolicitud flujo) {
        this.flujo = flujo;
        Function<Solicitud, ResultadoAprobacion> cadena = solicitud ->
                new ResultadoAprobacion(false, "Cadena de aprobación", "Ningún nivel resolvió la solicitud");
        for (int i = niveles.size() - 1; i >= 0; i--) {
            var nivel = niveles.get(i);
            var siguiente = cadena;
            cadena = solicitud -> nivel.evaluar(solicitud, siguiente);
        }
        inicio = cadena;
    }

    @Override
    public ResultadoAprobacion evaluar(Solicitud solicitud) {
        synchronized (solicitud) {
            double monto = solicitud.getMonto();
            var resultado = Double.isFinite(monto) && monto > 0
                    ? inicio.apply(solicitud)
                    : new ResultadoAprobacion(false, "Validación de solicitud", "El monto debe ser positivo y finito");
            if (resultado.isAprobada()) {
                flujo.aprobar(solicitud, resultado.getNivelResolutor());
            } else {
                flujo.rechazar(solicitud, resultado.getNivelResolutor());
            }
            return resultado;
        }
    }
}
