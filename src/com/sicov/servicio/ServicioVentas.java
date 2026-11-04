package com.sicov.servicio;

import com.sicov.modelo.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Mantiene un renglón por producto y conserva el orden en que se agregó al carrito.
 */
public final class ServicioVentas {
    private final LinkedHashMap<String, DetalleVenta> carrito = new LinkedHashMap<>();

    public void agregar(Producto producto, BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        DetalleVenta anterior = carrito.get(producto.codigo());
        if (anterior == null)
            carrito.put(producto.codigo(), DetalleVenta.de(producto, cantidad));
        else
            cambiarCantidad(producto.codigo(), anterior.cantidad().add(cantidad));
    }

    public void cambiarCantidad(String codigo, BigDecimal cantidad) {
        DetalleVenta detalle = carrito.get(codigo);
        if (detalle == null)
            return;
        if (cantidad.signum() < 0)
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        if (cantidad.signum() == 0) {
            carrito.remove(codigo);
            return;
        }
        if (detalle.unidad() == UnidadVenta.PIEZA && cantidad.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("La cantidad de piezas debe ser entera.");
        carrito.put(codigo, new DetalleVenta(detalle.codigo(), detalle.nombre(), detalle.categoria(), detalle.unidad(),
                cantidad, detalle.precio(), detalle.precio().multiply(cantidad), ""));
    }

    public List<DetalleVenta> calcular(List<Promocion> promociones) {
        // Recalcular desde el carrito original evita acumular descuentos al cambiar
        // cantidades.
        return new ServicioPromociones().calcular(new ArrayList<>(carrito.values()), promociones);
    }

    public boolean vacia() {
        return carrito.isEmpty();
    }

    public void limpiar() {
        carrito.clear();
    }
}
