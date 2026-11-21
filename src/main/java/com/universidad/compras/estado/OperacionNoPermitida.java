package com.universidad.compras.estado;

public final class OperacionNoPermitida extends IllegalStateException {
    public OperacionNoPermitida(String mensaje) {
        super(mensaje);
    }
}
