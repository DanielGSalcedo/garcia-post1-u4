package com.universidad.compras.notificacion;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AuditoriaSolicitudes {
    @EventListener
    public void reaccionar(CambioEstado evento) {
        ClientesNotificacion.registrarAuditoria(evento.solicitudId(), evento.actual(), evento.detalle());
    }
}
