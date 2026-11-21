package com.universidad.compras.ejecucion;

import java.time.Instant;

public record RegistroOperacion(long id, TipoOperacion tipo, Instant ejecutadaEn, Instant deshechaEn) {
    public boolean activa() {
        return deshechaEn == null;
    }
}
