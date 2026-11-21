package com.universidad.compras.ejecucion;

record ReservarPresupuesto(PresupuestoService receptor, String centroCosto, double monto) implements ComandoCompra {
    @Override
    public TipoOperacion tipo() {
        return TipoOperacion.RESERVA_PRESUPUESTAL;
    }

    @Override
    public void ejecutar() {
        if (!receptor.reservar(centroCosto, monto)) {
            throw new IllegalStateException("No se pudo reservar el presupuesto");
        }
    }

    @Override
    public void deshacer() {
        receptor.liberar(centroCosto, monto);
    }
}
