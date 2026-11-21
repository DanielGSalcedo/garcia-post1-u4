package com.universidad.compras.ejecucion;

import com.universidad.compras.estado.FlujoSolicitud;
import com.universidad.compras.modelo.Solicitud;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class EjecucionSolicitudes {
    private final Map<String, Expediente> expedientes = new ConcurrentHashMap<>();
    private final PresupuestoService presupuesto;
    private final OrdenCompraService orden;
    private final FlujoSolicitud flujo;

    public EjecucionSolicitudes(PresupuestoService presupuesto, OrdenCompraService orden, FlujoSolicitud flujo) {
        this.presupuesto = presupuesto;
        this.orden = orden;
        this.flujo = flujo;
    }

    public RegistroOperacion reservarPresupuesto(Solicitud solicitud) {
        String centroCosto = obligatorio(solicitud.getCentroCosto(), "centro de costo");
        if (!Double.isFinite(solicitud.getMonto()) || solicitud.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo y finito");
        }
        return ejecutar(solicitud, new ReservarPresupuesto(presupuesto, centroCosto, solicitud.getMonto()));
    }

    public RegistroOperacion generarOrden(Solicitud solicitud, String proveedor) {
        String id = obligatorio(solicitud.getId(), "identificador de solicitud");
        return ejecutar(solicitud, new GenerarOrdenCompra(orden, id, obligatorio(proveedor, "proveedor")));
    }

    public RegistroOperacion deshacer(Solicitud solicitud, long operacionId) {
        synchronized (solicitud) {
            var expediente = expedientes.get(obligatorio(solicitud.getId(), "identificador de solicitud"));
            verificarTitular(expediente, solicitud);
            var operacion = expediente.operaciones.stream().filter(o -> o.id == operacionId).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Operación no encontrada: " + operacionId));
            if (!operacion.activa()) {
                throw new IllegalStateException("La operación ya fue deshecha");
            }
            boolean completaAntes = expediente.completo();
            operacion.comando.deshacer();
            operacion.deshechaEn = Instant.now();
            if (completaAntes) {
                flujo.revertirEjecucion(solicitud, "Operación deshecha: " + operacion.comando.tipo());
            }
            return operacion.snapshot();
        }
    }

    public List<RegistroOperacion> historial(String solicitudId) {
        var expediente = expedientes.get(obligatorio(solicitudId, "identificador de solicitud"));
        if (expediente == null) {
            return List.of();
        }
        synchronized (expediente.solicitud) {
            return expediente.operaciones.stream().map(Operacion::snapshot).toList();
        }
    }

    private RegistroOperacion ejecutar(Solicitud solicitud, ComandoCompra comando) {
        synchronized (solicitud) {
            flujo.validarEjecucion(solicitud);
            String id = obligatorio(solicitud.getId(), "identificador de solicitud");
            var expediente = expedientes.computeIfAbsent(id, clave -> new Expediente(solicitud));
            verificarTitular(expediente, solicitud);
            if (expediente.tieneActiva(comando.tipo())) {
                throw new IllegalStateException("Ya existe una operación activa de tipo " + comando.tipo());
            }
            comando.ejecutar();
            var operacion = new Operacion(expediente.operaciones.size() + 1L, comando);
            expediente.operaciones.add(operacion);
            if (expediente.completo()) {
                flujo.ejecutar(solicitud);
            }
            return operacion.snapshot();
        }
    }

    private static void verificarTitular(Expediente expediente, Solicitud solicitud) {
        if (expediente == null || expediente.solicitud != solicitud) {
            throw new IllegalArgumentException("La solicitud no corresponde al expediente de ejecución");
        }
    }

    private static String obligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Se requiere " + campo);
        }
        return valor;
    }

    private static final class Expediente {
        private final Solicitud solicitud;
        private final List<Operacion> operaciones = new ArrayList<>();

        Expediente(Solicitud solicitud) { this.solicitud = solicitud; }

        boolean tieneActiva(TipoOperacion tipo) {
            return operaciones.stream().anyMatch(o -> o.activa() && o.comando.tipo() == tipo);
        }

        boolean completo() {
            return tieneActiva(TipoOperacion.RESERVA_PRESUPUESTAL) && tieneActiva(TipoOperacion.ORDEN_DE_COMPRA);
        }
    }

    private static final class Operacion {
        private final long id;
        private final ComandoCompra comando;
        private final Instant ejecutadaEn = Instant.now();
        private Instant deshechaEn;

        Operacion(long id, ComandoCompra comando) {
            this.id = id;
            this.comando = comando;
        }

        boolean activa() { return deshechaEn == null; }

        RegistroOperacion snapshot() {
            return new RegistroOperacion(id, comando.tipo(), ejecutadaEn, deshechaEn);
        }
    }
}
