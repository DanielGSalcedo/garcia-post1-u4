package com.universidad.compras.notificacion;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AvisoSolicitante {
    @EventListener
    public void reaccionar(CambioEstado evento) {
        ClientesNotificacion.enviarCorreo(evento.email(), "Estado de la solicitud " + evento.solicitudId(),
                "Cambio de " + evento.anterior() + " a " + evento.actual());
    }
}
