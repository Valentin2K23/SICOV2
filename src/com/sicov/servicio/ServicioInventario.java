package com.sicov.servicio;

import com.sicov.modelo.*;
import java.math.BigDecimal;
import java.util.*;

/** Estimación informativa; no impide vender cuando el saldo es cero o negativo. */
public final class ServicioInventario {
    public Map<String, BigDecimal> cantidadesEstimadas(List<Producto> productos, List<MovimientoInventario> registros) {
        Map<String, Producto> catalogo = new HashMap<>();
        Map<String, BigDecimal> cantidades = new HashMap<>();
        Set<String> unidadesDistintas = new HashSet<>();
        for (Producto p : productos) { catalogo.put(p.codigo(), p); cantidades.put(p.codigo(), BigDecimal.ZERO); }
        for (MovimientoInventario registro : registros) {
            if (registro.cancelado()) continue;
            for (DetalleVenta detalle : registro.detalles()) {
                Producto producto = catalogo.get(detalle.codigo());
                if (producto == null) continue;
                if (producto.unidad() != detalle.unidad()) { unidadesDistintas.add(producto.codigo()); continue; }
                BigDecimal cantidad = registro.tipo() == MovimientoInventario.Tipo.ENTRADA
                        ? detalle.cantidad() : detalle.cantidad().negate();
                cantidades.merge(producto.codigo(), cantidad, BigDecimal::add);
            }
        }
        // No sumar piezas, kg o litros entre sí si se cambió la unidad del producto.
        unidadesDistintas.forEach(cantidades::remove);
        return Map.copyOf(cantidades);
    }
}
