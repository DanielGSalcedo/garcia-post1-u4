package com.universidad.compras.estado;

import org.springframework.stereotype.Component;

final class Estados {
    private Estados() {
    }

    @Component
    static final class Pendiente implements EstadoSolicitud {
        @Override public String nombre() { return "PENDIENTE"; }
        @Override public String aprobar() { return "APROBADA"; }
        @Override public String rechazar() { return "RECHAZADA"; }
        @Override public String cancelar() { return "CANCELADA"; }
    }

    @Component
    static final class EnAprobacion implements EstadoSolicitud {
        @Override public String nombre() { return "EN_APROBACION"; }
        @Override public String aprobar() { return "APROBADA"; }
        @Override public String rechazar() { return "RECHAZADA"; }
        @Override public String cancelar() { return "CANCELADA"; }
    }

    @Component
    static final class Aprobada implements EstadoSolicitud {
        @Override public String nombre() { return "APROBADA"; }
        @Override public String ejecutar() { return "EJECUTADA"; }
        @Override public String cancelar() { return "CANCELADA"; }
    }

    @Component
    static final class Ejecutada implements EstadoSolicitud {
        @Override public String nombre() { return "EJECUTADA"; }
        @Override public String revertirEjecucion() { return "APROBADA"; }
    }

    @Component
    static final class Rechazada implements EstadoSolicitud {
        @Override public String nombre() { return "RECHAZADA"; }
    }

    @Component
    static final class Cancelada implements EstadoSolicitud {
        @Override public String nombre() { return "CANCELADA"; }
    }
}
