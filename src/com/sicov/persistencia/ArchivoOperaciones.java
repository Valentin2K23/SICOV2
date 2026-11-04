package com.sicov.persistencia;

import com.sicov.modelo.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Guarda una fila por detalle y agrupa por folio al leer. Los datos históricos del producto se conservan en el CSV
 * aunque después cambien su nombre, precio o categoría en el catálogo.
 */
public class ArchivoOperaciones {
    private static final String CABECERA = "folio,fecha,tipo,cancelado,total,recibido,cambio,motivo,codigo,producto,categoria,unidad,cantidad,precio,importe,promocion";
    private final Path ruta;

    public ArchivoOperaciones(Path ruta) {
        this.ruta = ruta;
    }

    public List<MovimientoInventario> leer() throws IOException {
        Map<String, List<List<String>>> filasPorFolio = new LinkedHashMap<>();
        for (List<String> fila : ArchivoCsv.leer(ruta, CABECERA))
            filasPorFolio.computeIfAbsent(fila.get(0), folio -> new ArrayList<>()).add(fila);
        List<MovimientoInventario> resultado = new ArrayList<>();
        for (List<List<String>> grupo : filasPorFolio.values()) {
            List<String> operacion = grupo.get(0);
            List<DetalleVenta> detalles = new ArrayList<>();
            for (List<String> fila : grupo)
                detalles.add(new DetalleVenta(fila.get(8), fila.get(9), fila.get(10), UnidadVenta.valueOf(fila.get(11)),
                        new BigDecimal(fila.get(12)), new BigDecimal(fila.get(13)), new BigDecimal(fila.get(14)),
                        fila.get(15)));
            resultado.add(new MovimientoInventario(operacion.get(0), LocalDateTime.parse(operacion.get(1)),
                    MovimientoInventario.Tipo.valueOf(operacion.get(2)), Boolean.parseBoolean(operacion.get(3)),
                    new BigDecimal(operacion.get(4)), new BigDecimal(operacion.get(5)),
                    new BigDecimal(operacion.get(6)), operacion.get(7), detalles));
        }
        return resultado;
    }

    public void guardar(List<MovimientoInventario> registros) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (MovimientoInventario registro : registros)
            for (DetalleVenta detalle : registro.detalles())
                filas.add(List.of(registro.folio(), registro.fecha().toString(), registro.tipo().name(),
                        String.valueOf(registro.cancelado()), registro.total().toPlainString(),
                        registro.recibido().toPlainString(), registro.cambio().toPlainString(), registro.motivo(),
                        detalle.codigo(), detalle.nombre(), detalle.categoria(), detalle.unidad().name(),
                        detalle.cantidad().toPlainString(), detalle.precio().toPlainString(),
                        detalle.importe().toPlainString(), detalle.promocion()));
        ArchivoCsv.guardar(ruta, CABECERA, filas);
    }
}
