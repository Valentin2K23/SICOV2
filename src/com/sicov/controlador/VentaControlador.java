package com.sicov.controlador;
import com.sicov.modelo.*;
import com.sicov.servicio.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public final class VentaControlador {
    public final ServicioVentas venta = new ServicioVentas();
    public final ServicioCobro cobro = new ServicioCobro();
    private final ContextoAplicacion contexto;
    public VentaControlador(ContextoAplicacion contexto) { this.contexto = contexto; }
    public List<DetalleVenta> detalles() { return venta.calcular(contexto.promociones()); }
    public BigDecimal total() { return detalles().stream().map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public MovimientoInventario confirmar(boolean exacto) throws IOException {
        if (venta.vacia()) throw new IllegalArgumentException("Agrega productos antes de cobrar.");
        if (exacto && cobro.recibido().signum() != 0)
            throw new IllegalArgumentException("El doble Enter solo funciona sin efectivo registrado.");
        var registro = contexto.registrar(MovimientoInventario.Tipo.VENTA, detalles(),
                exacto ? total() : cobro.recibido(), "");
        venta.limpiar(); cobro.limpiar(); return registro;
    }
}
