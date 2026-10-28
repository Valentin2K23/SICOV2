package com.sicov.persistencia;

import com.sicov.modelo.Promocion;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

public final class ArchivoPromociones {
    private final Path ruta;
    private static final String CAB = "id,nombre,categoria,precio,activa,codigo,cantidad";
    public ArchivoPromociones(Path directorio) { ruta = directorio.resolve("promociones.csv"); }
    public List<Promocion> leer() throws IOException {
        Map<String, List<List<String>>> grupos = new LinkedHashMap<>();
        for (List<String> f : ArchivoCsv.leer(ruta, CAB)) grupos.computeIfAbsent(f.get(0), k -> new ArrayList<>()).add(f);
        List<Promocion> resultado = new ArrayList<>();
        for (List<List<String>> grupo : grupos.values()) {
            List<String> f = grupo.get(0);
            Map<String, BigDecimal> componentes = new LinkedHashMap<>();
            for (List<String> linea : grupo) componentes.put(linea.get(5), new BigDecimal(linea.get(6)));
            resultado.add(new Promocion(f.get(0), f.get(1), f.get(2), new BigDecimal(f.get(3)),
                    Boolean.parseBoolean(f.get(4)), componentes));
        }
        return resultado;
    }
    public void guardar(List<Promocion> promociones) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (Promocion p : promociones) for (var c : p.componentes().entrySet())
            filas.add(List.of(p.id(), p.nombre(), p.categoria(), p.precio().toPlainString(),
                    String.valueOf(p.activa()), c.getKey(), c.getValue().toPlainString()));
        ArchivoCsv.guardar(ruta, CAB, filas);
    }
}
