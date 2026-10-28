package com.sicov.servicio;
import java.math.BigDecimal;

public final class ServicioCobro {
    private BigDecimal recibido = BigDecimal.ZERO;
    public BigDecimal recibido() { return recibido; }
    public void sumar(BigDecimal cantidad) {
        if (cantidad.signum() <= 0 || cantidad.scale() > 2)
            throw new IllegalArgumentException("Ingresa efectivo mayor que cero, con máximo dos decimales.");
        recibido = recibido.add(cantidad);
    }
    public boolean cubre(BigDecimal total) { return recibido.compareTo(total) >= 0; }
    public void limpiar() { recibido = BigDecimal.ZERO; }
}
