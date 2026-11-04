package com.sicov.controlador;

import com.sicov.modelo.*;
import com.sicov.servicio.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Coordina el carrito, el efectivo recibido y el ticket de la venta guardada.
 */
public final class VentaControlador {
    public final ServicioVentas venta = new ServicioVentas();
    public final ServicioCobro cobro = new ServicioCobro();
    private final ContextoAplicacion contexto;
    private MovimientoInventario ticketPendiente;

    public VentaControlador(ContextoAplicacion contexto) {
        this.contexto = contexto;
    }

    public List<DetalleVenta> detalles() {
        return ticketPendiente == null ? venta.calcular(contexto.promociones()) : ticketPendiente.detalles();
    }

    public boolean tieneTicketPendiente() {
        return ticketPendiente != null;
    }

    public MovimientoInventario ticketPendiente() {
        return ticketPendiente;
    }

    public void cerrarTicket() {
        if (ticketPendiente == null)
            return;
        // La venta se conserva mientras el ticket está abierto; al cerrarlo inicia el
        // siguiente cobro.
        venta.limpiar();
        cobro.limpiar();
        ticketPendiente = null;
    }

    public BigDecimal total() {
        return detalles().stream().map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public MovimientoInventario confirmar(boolean exacto) throws IOException {
        // Reutilizar el ticket evita registrar dos ventas ante una confirmación
        // repetida.
        if (ticketPendiente != null)
            return ticketPendiente;
        if (venta.vacia())
            throw new IllegalArgumentException("Agrega productos antes de cobrar.");
        if (exacto && cobro.recibido().signum() != 0)
            throw new IllegalArgumentException("El pago exacto requiere no haber registrado efectivo.");
        var registro = contexto.registrar(MovimientoInventario.Tipo.VENTA, detalles(),
                exacto ? total() : cobro.recibido(), "");
        ticketPendiente = registro;
        return registro;
    }
}
