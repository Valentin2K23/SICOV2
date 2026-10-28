package com.sicov.controlador;

import com.sicov.modelo.*;
import com.sicov.persistencia.*;
import com.sicov.servicio.Autorizacion;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ContextoAplicacion {
    private final ArchivoProductos archivoProductos;
    private final ArchivoPromociones archivoPromociones;
    private final ArchivoVentas archivoVentas;
    private final ArchivoCompras archivoCompras;
    private final ArchivoMovimientos archivoMermas;
    private List<Producto> productos;
    private List<Categoria> categorias;
    private List<Promocion> promociones;
    private List<MovimientoInventario> ventas, entradas, mermas;
    private final List<Runnable> oyentes = new ArrayList<>();
    public final Autorizacion autorizacion;

    public ContextoAplicacion(Path directorio) throws IOException {
        archivoProductos = new ArchivoProductos(directorio);
        archivoPromociones = new ArchivoPromociones(directorio);
        archivoVentas = new ArchivoVentas(directorio);
        archivoCompras = new ArchivoCompras(directorio);
        archivoMermas = new ArchivoMovimientos(directorio);
        productos = archivoProductos.leer(); categorias = archivoProductos.categorias();
        promociones = archivoPromociones.leer(); ventas = archivoVentas.leer();
        entradas = archivoCompras.leer(); mermas = archivoMermas.leer();
        autorizacion = new Autorizacion(directorio.resolve("configuracion.properties"));
    }
    public List<Producto> productos() { return List.copyOf(productos); }
    public List<String> categorias() { return categorias.stream().map(Categoria::nombre).toList(); }
    public List<Categoria> categoriasConfiguradas() { return List.copyOf(categorias); }
    public List<Promocion> promociones() { return List.copyOf(promociones); }
    public Producto producto(String codigo) {
        return productos.stream().filter(p -> p.codigo().equals(codigo.strip())).findFirst().orElse(null);
    }
    public void escuchar(Runnable oyente) { oyentes.add(oyente); }
    private void notificar() { List.copyOf(oyentes).forEach(Runnable::run); }
    public void agregarCategoria(String nombre) throws IOException {
        nombre = nombre.strip();
        if (nombre.isEmpty()) throw new IllegalArgumentException("Escribe el nombre de la categoría.");
        String valor = nombre;
        if (categorias.stream().anyMatch(c -> c.nombre().equalsIgnoreCase(valor))) return;
        List<Categoria> nuevas = new ArrayList<>(categorias); nuevas.add(new Categoria(nombre, ""));
        archivoProductos.guardarCategorias(nuevas); categorias = nuevas; notificar();
    }
    public void guardarCategoria(String nombre, String seccion) throws IOException {
        if (seccion == null || seccion.isBlank()) throw new IllegalArgumentException("Selecciona una sección.");
        Categoria propuesta = new Categoria(nombre, seccion);
        List<Categoria> nuevas = new ArrayList<>(categorias);
        int indice = -1;
        for (int i = 0; i < nuevas.size(); i++)
            if (nuevas.get(i).nombre().equalsIgnoreCase(propuesta.nombre())) { indice = i; break; }
        if (indice < 0) nuevas.add(propuesta);
        else nuevas.set(indice, new Categoria(nuevas.get(indice).nombre(), propuesta.seccion()));
        archivoProductos.guardarCategorias(nuevas); categorias = nuevas; notificar();
    }
    public void eliminarCategoria(String nombre) throws IOException {
        if (productos.stream().anyMatch(p -> p.categoria().equals(nombre)))
            throw new IllegalArgumentException("Esta categoría tiene productos. Reasígnalos antes de eliminarla.");
        List<Categoria> nuevas = new ArrayList<>(categorias); nuevas.removeIf(c -> c.nombre().equals(nombre));
        archivoProductos.guardarCategorias(nuevas); categorias = nuevas; notificar();
    }
    public void guardarProducto(Producto producto, boolean actualizar) throws IOException {
        if (producto(producto.codigo()) != null && !actualizar)
            throw new IllegalArgumentException("Ese código ya existe. Selecciona el producto para editarlo.");
        agregarCategoria(producto.categoria());
        String categoria = categorias().stream().filter(c -> c.equalsIgnoreCase(producto.categoria())).findFirst().orElseThrow();
        Producto guardado = new Producto(producto.codigo(), producto.nombre(), producto.precio(), categoria,
                producto.unidad(), producto.sinCodigoFisico());
        List<Producto> nuevos = new ArrayList<>(productos);
        nuevos.removeIf(p -> p.codigo().equals(producto.codigo())); nuevos.add(guardado);
        nuevos.sort(Comparator.comparing(Producto::nombre, String.CASE_INSENSITIVE_ORDER));
        archivoProductos.guardar(nuevos); productos = nuevos; notificar();
    }
    public void guardarPromocion(Promocion promocion) throws IOException {
        for (var c : promocion.componentes().entrySet()) {
            Producto p = producto(c.getKey());
            if (p == null) throw new IllegalArgumentException("No existe el producto " + c.getKey());
            p.validarCantidad(c.getValue());
        }
        List<Promocion> nuevas = new ArrayList<>(promociones);
        nuevas.removeIf(p -> p.id().equals(promocion.id())); nuevas.add(promocion);
        archivoPromociones.guardar(nuevas); promociones = nuevas; notificar();
    }
    public List<MovimientoInventario> registros() {
        List<MovimientoInventario> todos = new ArrayList<>(entradas); todos.addAll(ventas); todos.addAll(mermas);
        return List.copyOf(todos);
    }
    public MovimientoInventario registrar(MovimientoInventario.Tipo tipo, List<DetalleVenta> detalles,
                                           BigDecimal recibido, String motivo) throws IOException {
        if (detalles.isEmpty()) throw new IllegalArgumentException("Agrega al menos un producto.");
        if (tipo == MovimientoInventario.Tipo.MERMA && motivo.isBlank())
            throw new IllegalArgumentException("Escribe el motivo de la merma.");
        BigDecimal total = detalles.stream().map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (tipo == MovimientoInventario.Tipo.VENTA && recibido.compareTo(total) < 0)
            throw new IllegalArgumentException("El efectivo no cubre el total.");
        var fecha = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        List<MovimientoInventario> actuales = tipo == MovimientoInventario.Tipo.ENTRADA ? entradas
                : tipo == MovimientoInventario.Tipo.VENTA ? ventas : mermas;
        String prefijo = switch (tipo) { case ENTRADA -> "E"; case VENTA -> "V"; case MERMA -> "M"; };
        String folio = prefijo + "-" + fecha.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format("%06d", actuales.size() + 1);
        var registro = new MovimientoInventario(folio, fecha, tipo, false, total, recibido,
                tipo == MovimientoInventario.Tipo.VENTA ? recibido.subtract(total) : BigDecimal.ZERO, motivo, detalles);
        List<MovimientoInventario> nuevos = new ArrayList<>(actuales); nuevos.add(registro);
        if (tipo == MovimientoInventario.Tipo.ENTRADA) { archivoCompras.guardar(nuevos); entradas = nuevos; }
        else if (tipo == MovimientoInventario.Tipo.VENTA) { archivoVentas.guardar(nuevos); ventas = nuevos; }
        else { archivoMermas.guardar(nuevos); mermas = nuevos; }
        notificar(); return registro;
    }
    public void cancelarVenta(String folio, char[] clave) throws IOException {
        if (!autorizacion.verificar(clave)) throw new IllegalArgumentException("Contraseña incorrecta.");
        if (ventas.stream().noneMatch(v -> v.folio().equals(folio) && !v.cancelado()))
            throw new IllegalArgumentException("Selecciona una venta vigente.");
        List<MovimientoInventario> nuevas = ventas.stream().map(v -> v.folio().equals(folio) ? v.cancelar() : v).toList();
        archivoVentas.guardar(nuevas); ventas = nuevas; notificar();
    }
}
