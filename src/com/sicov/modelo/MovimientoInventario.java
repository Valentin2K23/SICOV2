package com.sicov.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Registro documental; no calcula ni limita existencias. */
public record MovimientoInventario(String folio, LocalDateTime fecha, Tipo tipo, boolean cancelado, BigDecimal total,
        BigDecimal recibido, BigDecimal cambio, String motivo, List<DetalleVenta> detalles) {
    public enum Tipo {
        ENTRADA, VENTA, MERMA
    }

    public MovimientoInventario {
        detalles = List.copyOf(detalles);
    }

    public MovimientoInventario cancelar(String motivoCancelacion) {
        // Marcar la operación mantiene sus importes y productos como evidencia del
        // movimiento original.
        String observacion = "Cancelación: " + motivoCancelacion;
        return new MovimientoInventario(folio, fecha, tipo, true, total, recibido, cambio,
                motivo.isBlank() ? observacion : motivo + "\n" + observacion, detalles);
    }

    public MovimientoInventario cancelar() {
        return new MovimientoInventario(folio, fecha, tipo, true, total, recibido, cambio, motivo, detalles);
    }
}
