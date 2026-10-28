package com.sicov.servicio;
import com.sicov.modelo.*;
import java.math.BigDecimal;
import java.util.*;

public final class ServicioReportes {
    public Map<String, BigDecimal> ventasPorCategoria(List<MovimientoInventario> registros) {
        Map<String, BigDecimal> totales = new TreeMap<>();
        for (var r : registros) if (r.tipo() == MovimientoInventario.Tipo.VENTA && !r.cancelado())
            for (var d : r.detalles()) totales.merge(d.categoria(), d.importe(), BigDecimal::add);
        return totales;
    }
}
