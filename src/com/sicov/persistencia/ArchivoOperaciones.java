package com.sicov.persistencia;

import com.sicov.modelo.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class ArchivoOperaciones {
    private final Path ruta;
    private static final String CAB = "folio,fecha,tipo,cancelado,total,recibido,cambio,motivo,codigo,producto,categoria,unidad,cantidad,precio,importe,promocion";
    public ArchivoOperaciones(Path ruta) { this.ruta = ruta; }
    public List<MovimientoInventario> leer() throws IOException {
        Map<String, List<List<String>>> grupos = new LinkedHashMap<>();
        for (List<String> f : ArchivoCsv.leer(ruta, CAB)) grupos.computeIfAbsent(f.get(0), k -> new ArrayList<>()).add(f);
        List<MovimientoInventario> resultado = new ArrayList<>();
        for (List<List<String>> grupo : grupos.values()) {
            List<String> f = grupo.get(0);
            List<DetalleVenta> detalles = new ArrayList<>();
            for (List<String> l : grupo) detalles.add(new DetalleVenta(l.get(8), l.get(9), l.get(10),
                    UnidadVenta.valueOf(l.get(11)), new BigDecimal(l.get(12)), new BigDecimal(l.get(13)),
                    new BigDecimal(l.get(14)), l.get(15)));
            resultado.add(new MovimientoInventario(f.get(0), LocalDateTime.parse(f.get(1)),
                    MovimientoInventario.Tipo.valueOf(f.get(2)), Boolean.parseBoolean(f.get(3)),
                    new BigDecimal(f.get(4)), new BigDecimal(f.get(5)), new BigDecimal(f.get(6)), f.get(7), detalles));
        }
        return resultado;
    }
    public void guardar(List<MovimientoInventario> registros) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (MovimientoInventario r : registros) for (DetalleVenta d : r.detalles())
            filas.add(List.of(r.folio(), r.fecha().toString(), r.tipo().name(), String.valueOf(r.cancelado()),
                    r.total().toPlainString(), r.recibido().toPlainString(), r.cambio().toPlainString(), r.motivo(),
                    d.codigo(), d.nombre(), d.categoria(), d.unidad().name(), d.cantidad().toPlainString(),
                    d.precio().toPlainString(), d.importe().toPlainString(), d.promocion()));
        ArchivoCsv.guardar(ruta, CAB, filas);
    }
}
