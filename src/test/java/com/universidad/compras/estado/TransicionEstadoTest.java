package com.universidad.compras.estado;

import com.universidad.compras.modelo.Solicitud;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransicionEstadoTest {
    @Autowired FlujoSolicitud flujo;
    private final Solicitud solicitud = new Solicitud("S-030", "ana@example.com", 3_000_000, "SOFTWARE", "CC-100");

    @Test
    void unaAprobadaSePuedeEjecutarPeroSoloUnaVez() {
        flujo.aprobar(solicitud, "Gerente de Área");
        flujo.ejecutar(solicitud);
        assertEquals("EJECUTADA", solicitud.getEstado());
        assertThrows(OperacionNoPermitida.class, () -> flujo.ejecutar(solicitud));
        assertEquals("EJECUTADA", solicitud.getEstado());
    }

    @Test
    void pendienteRechazaEjecucionSinCambiarDeEstado() {
        assertThrows(OperacionNoPermitida.class, () -> flujo.ejecutar(solicitud));
        assertEquals("PENDIENTE", solicitud.getEstado());
    }

    @Test
    void rechazarEsValidoEnRevisionYAlcanzaUnEstadoTerminal() {
        solicitud.setEstado("EN_APROBACION");
        flujo.rechazar(solicitud, "Gerente de Área");
        assertEquals("RECHAZADA", solicitud.getEstado());
        assertThrows(OperacionNoPermitida.class, () -> flujo.aprobar(solicitud, "Director Financiero"));
        assertEquals("RECHAZADA", solicitud.getEstado());
        assertEquals("Gerente de Área", solicitud.getNivelResolutor());
    }

    @Test
    void cancelarImpideAprobarOActuarDeNuevo() {
        flujo.cancelar(solicitud);
        assertThrows(OperacionNoPermitida.class, () -> flujo.aprobar(solicitud, "Gerente de Área"));
        assertThrows(OperacionNoPermitida.class, () -> flujo.cancelar(solicitud));
        assertEquals("CANCELADA", solicitud.getEstado());
    }
}
