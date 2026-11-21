package com.universidad.compras.notificacion;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class TableroContabilidad {
    @EventListener
    public void reaccionar(CambioEstado evento) {
        ClientesNotificacion.actualizarDashboardContabilidad(evento.solicitudId(), evento.actual(), evento.monto());
    }
}
