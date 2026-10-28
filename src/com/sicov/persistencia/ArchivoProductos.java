package com.sicov.persistencia;

import com.sicov.modelo.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

public final class ArchivoProductos {
    private final Path directorio;
    private static final String CAB = "codigo,nombre,precio,categoria,unidad,sin_codigo_fisico";
    public ArchivoProductos(Path directorio) { this.directorio = directorio; }
    public List<Producto> leer() throws IOException {
        List<Producto> productos = new ArrayList<>();
        for (List<String> f : ArchivoCsv.leer(directorio.resolve("productos.csv"), CAB))
            productos.add(new Producto(f.get(0), f.get(1), new BigDecimal(f.get(2)), f.get(3),
                    UnidadVenta.valueOf(f.get(4)), Boolean.parseBoolean(f.get(5))));
        return productos;
    }
    public void guardar(List<Producto> productos) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (Producto p : productos) filas.add(List.of(p.codigo(), p.nombre(), p.precio().toPlainString(),
                p.categoria(), p.unidad().name(), String.valueOf(p.sinCodigoFisico())));
        ArchivoCsv.guardar(directorio.resolve("productos.csv"), CAB, filas);
    }
    public List<Categoria> categorias() throws IOException {
        List<Categoria> resultado = new ArrayList<>();
        for (List<String> f : ArchivoCsv.leer(directorio.resolve("categorias.csv"), "nombre,seccion", "nombre"))
            resultado.add(new Categoria(f.get(0), f.size() > 1 ? f.get(1) : ""));
        return resultado;
    }
    public void guardarCategorias(List<Categoria> categorias) throws IOException {
        ArchivoCsv.guardar(directorio.resolve("categorias.csv"), "nombre,seccion",
                categorias.stream().map(c -> List.of(c.nombre(), c.seccion())).toList());
    }
}
