package com.sicov.servicio;

import com.sicov.modelo.*;

import java.math.BigDecimal;
import java.util.*;

/** Resume ventas vigentes usando los datos guardados en cada operación. */
public final class ServicioReportes {
    /**
     * Ordena el granel por el número de ventas vigentes en que aparece cada producto.
     */
    public List<Producto> granelMasVendidos(List<Producto> productos, List<MovimientoInventario> registros) {
        Map<String, Integer> frecuencia = new HashMap<>();
        for (var registro : registros) {
            if (registro.tipo() != MovimientoInventario.Tipo.VENTA || registro.cancelado())
                continue;
            Set<String> vendidos = new HashSet<>();
            for (var detalle : registro.detalles())
                vendidos.add(detalle.codigo());
            for (String codigo : vendidos)
                frecuencia.merge(codigo, 1, Integer::sum);
        }
        return productos.stream().filter(producto -> producto.unidad() != UnidadVenta.PIEZA)
                .sorted(Comparator.<Producto>comparingInt(producto -> frecuencia.getOrDefault(producto.codigo(), 0))
                        .reversed().thenComparing(Producto::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Producto::codigo))
                .limit(8).toList();
    }

    public Map<String, BigDecimal> ventasPorCategoria(List<MovimientoInventario> registros) {
        Map<String, BigDecimal> totales = new TreeMap<>();
        for (var registro : registros)
            if (registro.tipo() == MovimientoInventario.Tipo.VENTA && !registro.cancelado())
                for (var detalle : registro.detalles())
                    totales.merge(detalle.categoria(), detalle.importe(), BigDecimal::add);
        return totales;
    }
}
