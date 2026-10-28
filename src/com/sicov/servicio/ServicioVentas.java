package com.sicov.servicio;

import com.sicov.modelo.*;
import java.math.BigDecimal;
import java.util.*;

public final class ServicioVentas {
    private final LinkedHashMap<String, DetalleVenta> carrito = new LinkedHashMap<>();
    public void agregar(Producto producto, BigDecimal cantidad) {
        producto.validarCantidad(cantidad);
        DetalleVenta anterior = carrito.get(producto.codigo());
        if (anterior == null) carrito.put(producto.codigo(), DetalleVenta.de(producto, cantidad));
        else cambiarCantidad(producto.codigo(), anterior.cantidad().add(cantidad));
    }
    public void cambiarCantidad(String codigo, BigDecimal cantidad) {
        DetalleVenta d = carrito.get(codigo);
        if (d == null) return;
        if (cantidad.signum() < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        if (cantidad.signum() == 0) { carrito.remove(codigo); return; }
        if (d.unidad() == UnidadVenta.PIEZA && cantidad.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("La cantidad de piezas debe ser entera.");
        carrito.put(codigo, new DetalleVenta(d.codigo(), d.nombre(), d.categoria(), d.unidad(),
                cantidad, d.precio(), d.precio().multiply(cantidad), ""));
    }
    public List<DetalleVenta> calcular(List<Promocion> promociones) {
        return new ServicioPromociones().calcular(new ArrayList<>(carrito.values()), promociones);
    }
    public boolean vacia() { return carrito.isEmpty(); }
    public void limpiar() { carrito.clear(); }
}
