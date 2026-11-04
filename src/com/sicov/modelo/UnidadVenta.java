package com.sicov.modelo;

/**
 * Unidad usada tanto por el precio del catálogo como por las cantidades del historial.
 */
public enum UnidadVenta {
    PIEZA("Pieza", "pza"), KILOGRAMO("Kilogramo", "kg"), LITRO("Litro", "L");

    private final String nombre;
    private final String simbolo;

    UnidadVenta(String nombre, String simbolo) {
        this.nombre = nombre;
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
