package com.sicov.persistencia;

import com.sicov.modelo.Promocion;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

/**
 * Guarda una fila por componente y reconstruye cada paquete agrupando por su identificador.
 */
public final class ArchivoPromociones {
    private static final String CABECERA = "id,nombre,categoria,precio,activa,codigo,cantidad";
    private final Path ruta;

    public ArchivoPromociones(Path directorio) {
        ruta = directorio.resolve("promociones.csv");
    }

    public List<Promocion> leer() throws IOException {
        Map<String, List<List<String>>> filasPorPromocion = new LinkedHashMap<>();
        for (List<String> fila : ArchivoCsv.leer(ruta, CABECERA))
            filasPorPromocion.computeIfAbsent(fila.get(0), id -> new ArrayList<>()).add(fila);
        List<Promocion> resultado = new ArrayList<>();
        for (List<List<String>> grupo : filasPorPromocion.values()) {
            List<String> promocion = grupo.get(0);
            Map<String, BigDecimal> componentes = new LinkedHashMap<>();
            for (List<String> linea : grupo)
                componentes.put(linea.get(5), new BigDecimal(linea.get(6)));
            resultado.add(new Promocion(promocion.get(0), promocion.get(1), promocion.get(2),
                    new BigDecimal(promocion.get(3)), Boolean.parseBoolean(promocion.get(4)), componentes));
        }
        return resultado;
    }

    public void guardar(List<Promocion> promociones) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (Promocion promocion : promociones)
            for (var componente : promocion.componentes().entrySet())
                filas.add(List.of(promocion.id(), promocion.nombre(), promocion.categoria(),
                        promocion.precio().toPlainString(), String.valueOf(promocion.activa()), componente.getKey(),
                        componente.getValue().toPlainString()));
        ArchivoCsv.guardar(ruta, CABECERA, filas);
    }
}
