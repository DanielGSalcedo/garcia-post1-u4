package com.universidad.compras.estado;

import com.universidad.compras.modelo.Solicitud;
import com.universidad.compras.notificacion.CambioEstado;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class FlujoSolicitud {
    private final Map<String, EstadoSolicitud> estados;
    private final ApplicationEventPublisher eventos;

    public FlujoSolicitud(List<EstadoSolicitud> estados, ApplicationEventPublisher eventos) {
        this.estados = estados.stream().collect(Collectors.toUnmodifiableMap(EstadoSolicitud::nombre, Function.identity()));
        this.eventos = eventos;
    }

    public void aprobar(Solicitud solicitud, String nivel) {
        cambiar(solicitud, EstadoSolicitud::aprobar, nivel, "Aprobación por " + nivel);
    }

    public void rechazar(Solicitud solicitud, String nivel) {
        cambiar(solicitud, EstadoSolicitud::rechazar, nivel, "Rechazo por " + nivel);
    }

    public void ejecutar(Solicitud solicitud) {
        cambiar(solicitud, EstadoSolicitud::ejecutar, null, "Presupuesto reservado y orden generada");
    }

    public void cancelar(Solicitud solicitud) {
        cambiar(solicitud, EstadoSolicitud::cancelar, null, "Solicitud cancelada");
    }

    public void revertirEjecucion(Solicitud solicitud, String detalle) {
        cambiar(solicitud, EstadoSolicitud::revertirEjecucion, null, detalle);
    }

    public void validarEjecucion(Solicitud solicitud) {
        synchronized (solicitud) {
            actual(solicitud.getEstado()).ejecutar();
        }
    }

    private EstadoSolicitud actual(String nombre) {
        EstadoSolicitud estado = nombre == null ? null : estados.get(nombre);
        if (estado == null) {
            throw new OperacionNoPermitida("Estado de solicitud desconocido: " + nombre);
        }
        return estado;
    }

    private void cambiar(Solicitud solicitud, Function<EstadoSolicitud, String> operacion,
                         String nivel, String detalle) {
        synchronized (solicitud) {
            String anterior = solicitud.getEstado();
            String siguiente = operacion.apply(actual(anterior));
            actual(siguiente);
            if (nivel != null) {
                solicitud.setNivelResolutor(nivel);
            }
            if (!anterior.equals(siguiente)) {
                solicitud.setEstado(siguiente);
                eventos.publishEvent(new CambioEstado(solicitud.getId(), solicitud.getSolicitanteEmail(),
                        solicitud.getMonto(), anterior, siguiente, detalle));
            }
        }
    }
}
