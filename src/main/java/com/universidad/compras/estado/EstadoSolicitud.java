package com.universidad.compras.estado;

public interface EstadoSolicitud {
    String nombre();

    default String aprobar() { return noPermitida("aprobar"); }
    default String rechazar() { return noPermitida("rechazar"); }
    default String ejecutar() { return noPermitida("ejecutar"); }
    default String cancelar() { return noPermitida("cancelar"); }
    default String revertirEjecucion() { return noPermitida("revertir la ejecución"); }

    private String noPermitida(String operacion) {
        throw new OperacionNoPermitida("No se puede " + operacion + " una solicitud en estado " + nombre());
    }
}
