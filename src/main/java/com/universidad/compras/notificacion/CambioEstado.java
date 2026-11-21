package com.universidad.compras.notificacion;

public record CambioEstado(String solicitudId, String email, double monto,
                           String anterior, String actual, String detalle) {
}
