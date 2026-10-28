package com.sicov.modelo;

import java.math.BigDecimal;

/** Conserva el nombre, categoría y precio vigentes al registrar la operación. */
public record DetalleVenta(String codigo, String nombre, String categoria, UnidadVenta unidad,
                           BigDecimal cantidad, BigDecimal precio, BigDecimal importe, String promocion) {
    public static DetalleVenta de(Producto p, BigDecimal cantidad) {
        p.validarCantidad(cantidad);
        return new DetalleVenta(p.codigo(), p.nombre(), p.categoria(), p.unidad(), cantidad,
                p.precio(), p.precio().multiply(cantidad).setScale(2, java.math.RoundingMode.HALF_UP), "");
    }
}
