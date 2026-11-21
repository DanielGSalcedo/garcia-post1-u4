package com.universidad.compras.ejecucion;

interface ComandoCompra {
    TipoOperacion tipo();
    void ejecutar();
    void deshacer();
}
