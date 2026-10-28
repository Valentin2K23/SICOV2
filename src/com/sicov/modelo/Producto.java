package com.sicov.modelo;

import java.math.BigDecimal;

public record Producto(String codigo, String nombre, BigDecimal precio, String categoria,
                       UnidadVenta unidad, boolean sinCodigoFisico) {
    public Producto {
        codigo = codigo.strip(); nombre = nombre.strip(); categoria = categoria.strip();
        if (codigo.isEmpty() || nombre.isEmpty() || categoria.isEmpty())
            throw new IllegalArgumentException("Completa nombre, código y categoría.");
        if (codigo.contains("*") || codigo.contains("\n") || codigo.contains("\r"))
            throw new IllegalArgumentException("El código no puede contener * ni saltos de línea.");
        if (precio == null || precio.signum() < 0 || precio.scale() > 2)
            throw new IllegalArgumentException("El precio debe ser positivo o cero, con máximo dos decimales.");
        if (unidad == null) throw new IllegalArgumentException("Selecciona una unidad.");
    }
    public void validarCantidad(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0)
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        if (unidad == UnidadVenta.PIEZA && cantidad.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("Los productos por pieza requieren cantidades enteras.");
    }
    @Override public String toString() { return nombre + " — " + codigo; }
}
