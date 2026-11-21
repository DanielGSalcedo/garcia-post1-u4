package com.universidad.compras.notificacion;

import com.universidad.compras.aprobacion.ServicioAprobacion;
import com.universidad.compras.ejecucion.EjecucionSolicitudes;
import com.universidad.compras.estado.FlujoSolicitud;
import com.universidad.compras.estado.OperacionNoPermitida;
import com.universidad.compras.modelo.Solicitud;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class NotificacionEstadoTest {
    @Autowired FlujoSolicitud flujo;
    @Autowired ServicioAprobacion aprobacion;
    @Autowired EjecucionSolicitudes ejecucion;
    @Autowired CuartoSuscriptor cuarto;

    @BeforeEach
    void limpiar() {
        cuarto.eventos.clear();
    }

    @Test
    void activaLosTresClientesProvistos(CapturedOutput salida) {
        flujo.aprobar(solicitud("S-020"), "Gerente de Área");

        assertTrue(salida.getOut().contains("[EMAIL -> ana@example.com]"));
        assertTrue(salida.getOut().contains("[DASHBOARD] Solicitud S-020 -> APROBADA"));
        assertTrue(salida.getOut().contains("[AUDITORIA] S-020 -> APROBADA"));
    }

    @Test
    void unCuartoSuscriptorReaccionaSinCambiosEnElPublicador() {
        var solicitud = solicitud("S-021");
        aprobacion.evaluar(solicitud);
        ejecucion.reservarPresupuesto(solicitud);
        var orden = ejecucion.generarOrden(solicitud, "Proveedor A");
        ejecucion.deshacer(solicitud, orden.id());
        flujo.cancelar(solicitud);
        assertEquals(List.of("APROBADA", "EJECUTADA", "APROBADA", "CANCELADA"),
                cuarto.eventos.stream().map(CambioEstado::actual).toList());
        assertTrue(cuarto.eventos.stream().allMatch(e -> e.solicitudId().equals("S-021")));
    }

    @Test
    void unaOperacionInvalidaNoPublicaEventos() {
        assertThrows(OperacionNoPermitida.class, () -> flujo.ejecutar(solicitud("S-022")));
        assertTrue(cuarto.eventos.isEmpty());
    }

    private Solicitud solicitud(String id) {
        return new Solicitud(id, "ana@example.com", 2_500_000, "SOFTWARE", "CC-100");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracionPrueba {
        @Bean
        CuartoSuscriptor colector() {
            return new CuartoSuscriptor();
        }
    }

    static class CuartoSuscriptor {
        private final List<CambioEstado> eventos = new ArrayList<>();

        @EventListener
        public void recibir(CambioEstado evento) {
            eventos.add(evento);
        }
    }
}
