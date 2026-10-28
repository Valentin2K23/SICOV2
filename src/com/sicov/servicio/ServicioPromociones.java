package com.sicov.servicio;

import com.sicov.modelo.*;
import java.math.*;
import java.util.*;

public final class ServicioPromociones {
    public List<DetalleVenta> calcular(List<DetalleVenta> carrito, List<Promocion> promociones) {
        Map<String, DetalleVenta> productos = new LinkedHashMap<>();
        Map<String, BigDecimal> disponibles = new LinkedHashMap<>(), importes = new LinkedHashMap<>();
        Map<String, List<String>> etiquetas = new LinkedHashMap<>();
        for (DetalleVenta d : carrito) {
            productos.put(d.codigo(), d); disponibles.put(d.codigo(), d.cantidad());
            importes.put(d.codigo(), BigDecimal.ZERO); etiquetas.put(d.codigo(), new ArrayList<>());
        }
        // Una unidad solo participa en una promoción. Ante coincidencias, prioriza el mayor ahorro por paquete.
        List<Promocion> ordenadas = promociones.stream().filter(Promocion::activa)
                .filter(p -> productos.keySet().containsAll(p.componentes().keySet()))
                .sorted(Comparator.<Promocion, BigDecimal>comparing(p -> ahorro(p, productos)).reversed()).toList();
        for (Promocion promo : ordenadas) {
            if (ahorro(promo, productos).signum() <= 0) continue;
            BigDecimal veces = null;
            for (var c : promo.componentes().entrySet()) {
                BigDecimal n = disponibles.get(c.getKey()).divide(c.getValue(), 0, RoundingMode.DOWN);
                veces = veces == null ? n : veces.min(n);
            }
            if (veces == null || veces.signum() == 0) continue;
            BigDecimal precioNormal = BigDecimal.ZERO;
            for (var c : promo.componentes().entrySet())
                precioNormal = precioNormal.add(productos.get(c.getKey()).precio().multiply(c.getValue()));
            BigDecimal porRepartir = promo.precio().multiply(veces).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPaquetes = porRepartir;
            int restante = promo.componentes().size();
            for (var c : promo.componentes().entrySet()) {
                restante--;
                BigDecimal costo = productos.get(c.getKey()).precio().multiply(c.getValue());
                BigDecimal proporcion = restante == 0 ? porRepartir
                        : totalPaquetes.multiply(costo).divide(precioNormal, 2, RoundingMode.HALF_UP).min(porRepartir);
                porRepartir = porRepartir.subtract(proporcion);
                importes.put(c.getKey(), importes.get(c.getKey()).add(proporcion));
                disponibles.put(c.getKey(), disponibles.get(c.getKey()).subtract(c.getValue().multiply(veces)));
                etiquetas.get(c.getKey()).add(promo.nombre() + " · PROMOCIÓN - " + veces.toPlainString());
            }
        }
        List<DetalleVenta> resultado = new ArrayList<>();
        for (DetalleVenta d : carrito) {
            BigDecimal importe = importes.get(d.codigo()).add(d.precio().multiply(disponibles.get(d.codigo())))
                    .setScale(2, RoundingMode.HALF_UP);
            resultado.add(new DetalleVenta(d.codigo(), d.nombre(), d.categoria(), d.unidad(), d.cantidad(), d.precio(),
                    importe, String.join(" | ", etiquetas.get(d.codigo()))));
        }
        return resultado;
    }
    private BigDecimal ahorro(Promocion p, Map<String, DetalleVenta> productos) {
        BigDecimal normal = BigDecimal.ZERO;
        for (var c : p.componentes().entrySet()) normal = normal.add(productos.get(c.getKey()).precio().multiply(c.getValue()));
        return normal.subtract(p.precio());
    }
}
