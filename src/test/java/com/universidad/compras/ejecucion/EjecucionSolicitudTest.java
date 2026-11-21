package com.universidad.compras.ejecucion;

import com.universidad.compras.estado.FlujoSolicitud;
import com.universidad.compras.estado.OperacionNoPermitida;
import com.universidad.compras.modelo.Solicitud;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.universidad.compras.ejecucion.TipoOperacion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class EjecucionSolicitudTest {
    @Autowired FlujoSolicitud flujo;
    private final PresupuestoService presupuesto = mock(PresupuestoService.class);
    private final OrdenCompraService orden = mock(OrdenCompraService.class);
    private final Solicitud s = new Solicitud("S-010", "ana@example.com", 3_000_000, "SOFTWARE", "CC-100");
    private EjecucionSolicitudes ejecutor;

    @BeforeEach
    void preparar() {
        ejecutor = new EjecucionSolicitudes(presupuesto, orden, flujo);
        flujo.aprobar(s, "Gerente de Área");
        when(presupuesto.reservar("CC-100", 3_000_000)).thenReturn(true);
        when(orden.generar("S-010", "Proveedor A")).thenReturn("OC-10");
    }

    @Test
    void pasaAEjecutadaSoloConAmbasOperacionesActivas() {
        ejecutor.reservarPresupuesto(s);
        assertEquals("APROBADA", s.getEstado());
        ejecutor.generarOrden(s, "Proveedor A");
        assertEquals("EJECUTADA", s.getEstado());
        assertEquals(List.of(RESERVA_PRESUPUESTAL, ORDEN_DE_COMPRA),
                ejecutor.historial(s.getId()).stream().map(RegistroOperacion::tipo).toList());
    }

    @Test
    void deshaceLaOrdenSinLiberarLaReserva() {
        ejecutor.reservarPresupuesto(s);
        var ordenRegistrada = ejecutor.generarOrden(s, "Proveedor A");
        ejecutor.deshacer(s, ordenRegistrada.id());
        verify(orden).cancelar("OC-10");
        verify(presupuesto, never()).liberar(anyString(), anyDouble());
        assertEquals("APROBADA", s.getEstado());
        assertTrue(ejecutor.historial(s.getId()).get(0).activa());
        assertFalse(ejecutor.historial(s.getId()).get(1).activa());
    }

    @Test
    void deshaceUnaReservaAnteriorSinCancelarLaOrdenYConservaElHistorial() {
        var reserva = ejecutor.reservarPresupuesto(s);
        ejecutor.generarOrden(s, "Proveedor A");
        ejecutor.deshacer(s, reserva.id());
        verify(presupuesto).liberar("CC-100", 3_000_000);
        verify(orden, never()).cancelar(anyString());
        assertEquals("APROBADA", s.getEstado());
        ejecutor.reservarPresupuesto(s);
        var historial = ejecutor.historial(s.getId());
        assertEquals(List.of(false, true, true), historial.stream().map(RegistroOperacion::activa).toList());
        assertEquals("EJECUTADA", s.getEstado());
    }

    @Test
    void unReceptorFallidoNoCreaUnaEntradaNiCambiaElEstado() {
        when(presupuesto.reservar(anyString(), anyDouble())).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> ejecutor.reservarPresupuesto(s));
        assertTrue(ejecutor.historial(s.getId()).isEmpty());
        assertEquals("APROBADA", s.getEstado());
    }

    @Test
    void unaSolicitudPendienteNoActivaReceptoresNiComparteHistorial() {
        var otra = new Solicitud("S-011", "ana@example.com", 3_000_000, "SOFTWARE", "CC-100");
        assertThrows(OperacionNoPermitida.class, () -> ejecutor.generarOrden(otra, "Proveedor A"));
        verifyNoInteractions(presupuesto, orden);
        assertTrue(ejecutor.historial(otra.getId()).isEmpty());
    }
}
