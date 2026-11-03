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
    private MovimientoInventario ticketPendiente;
    public VentaControlador(ContextoAplicacion contexto) { this.contexto = contexto; }
    public List<DetalleVenta> detalles() {
        return ticketPendiente == null ? venta.calcular(contexto.promociones()) : ticketPendiente.detalles();
    }
    public boolean tieneTicketPendiente() { return ticketPendiente != null; }
    public void cerrarTicket() {
        if (ticketPendiente == null) return;
        venta.limpiar(); cobro.limpiar(); ticketPendiente = null;
    }
    public BigDecimal total() { return detalles().stream().map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public MovimientoInventario confirmar(boolean exacto) throws IOException {
        if (ticketPendiente != null) return ticketPendiente;
        if (venta.vacia()) throw new IllegalArgumentException("Agrega productos antes de cobrar.");
        if (exacto && cobro.recibido().signum() != 0)
            throw new IllegalArgumentException("El pago exacto requiere no haber registrado efectivo.");
        var registro = contexto.registrar(MovimientoInventario.Tipo.VENTA, detalles(),
                exacto ? total() : cobro.recibido(), "");
        ticketPendiente = registro; return registro;
    }
}
