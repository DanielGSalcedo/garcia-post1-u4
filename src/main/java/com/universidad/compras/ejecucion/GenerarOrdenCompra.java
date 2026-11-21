package com.universidad.compras.ejecucion;

final class GenerarOrdenCompra implements ComandoCompra {
    private final OrdenCompraService receptor;
    private final String solicitudId;
    private final String proveedor;
    private String numeroOrden;

    GenerarOrdenCompra(OrdenCompraService receptor, String solicitudId, String proveedor) {
        this.receptor = receptor;
        this.solicitudId = solicitudId;
        this.proveedor = proveedor;
    }

    @Override
    public TipoOperacion tipo() {
        return TipoOperacion.ORDEN_DE_COMPRA;
    }

    @Override
    public void ejecutar() {
        numeroOrden = receptor.generar(solicitudId, proveedor);
        if (numeroOrden == null || numeroOrden.isBlank()) {
            throw new IllegalStateException("El proveedor no devolvió un número de orden");
        }
    }

    @Override
    public void deshacer() {
        receptor.cancelar(numeroOrden);
    }
}
