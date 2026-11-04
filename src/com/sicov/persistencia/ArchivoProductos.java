package com.sicov.persistencia;

import com.sicov.modelo.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

/**
 * Persistencia del catálogo y de las categorías que organizan sus productos.
 */
public final class ArchivoProductos {
    private static final String CABECERA = "codigo,nombre,precio,categoria,unidad,sin_codigo_fisico";
    private final Path directorio;

    public ArchivoProductos(Path directorio) {
        this.directorio = directorio;
    }

    public List<Producto> leer() throws IOException {
        List<Producto> productos = new ArrayList<>();
        for (List<String> fila : ArchivoCsv.leer(directorio.resolve("productos.csv"), CABECERA))
            productos.add(new Producto(fila.get(0), fila.get(1), new BigDecimal(fila.get(2)), fila.get(3),
                    UnidadVenta.valueOf(fila.get(4)), Boolean.parseBoolean(fila.get(5))));
        return productos;
    }

    public void guardar(List<Producto> productos) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (Producto producto : productos)
            filas.add(List.of(producto.codigo(), producto.nombre(), producto.precio().toPlainString(),
                    producto.categoria(), producto.unidad().name(), String.valueOf(producto.sinCodigoFisico())));
        ArchivoCsv.guardar(directorio.resolve("productos.csv"), CABECERA, filas);
    }

    public List<Categoria> categorias() throws IOException {
        List<Categoria> resultado = new ArrayList<>();
        // Los catálogos anteriores a las secciones solo guardaban el nombre de la
        // categoría.
        for (List<String> fila : ArchivoCsv.leer(directorio.resolve("categorias.csv"), "nombre,seccion", "nombre"))
            resultado.add(new Categoria(fila.get(0), fila.size() > 1 ? fila.get(1) : ""));
        return resultado;
    }

    public void guardarCategorias(List<Categoria> categorias) throws IOException {
        ArchivoCsv.guardar(directorio.resolve("categorias.csv"), "nombre,seccion",
                categorias.stream().map(c -> List.of(c.nombre(), c.seccion())).toList());
    }
}
