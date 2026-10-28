package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioInventario;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

public final class PruebasCategorias {
    private static int verificaciones;
    private static void comprobar(boolean cumple, String mensaje) {
        verificaciones++; if (!cumple) throw new AssertionError(mensaje);
    }
    private static BigDecimal n(String valor) { return new BigDecimal(valor); }
    private static BigDecimal cantidad(ContextoAplicacion c, String codigo) {
        return new ServicioInventario().cantidadesEstimadas(c.productos(), c.registros()).get(codigo);
    }
    private static MovimientoInventario registrar(ContextoAplicacion c, MovimientoInventario.Tipo tipo,
            Producto producto, String cantidad) throws Exception {
        return c.registrar(tipo, List.of(DetalleVenta.de(producto, n(cantidad))), n("1000"), "Prueba");
    }
    public static void main(String[] args) throws Exception {
        Path datos = Files.createTempDirectory("sicov-categorias-");
        Files.writeString(datos.resolve("categorias.csv"), "nombre\n\"Galletas, pan\"\n\"Quesos\"\n");
        ContextoAplicacion c = new ContextoAplicacion(datos);
        comprobar(c.categorias().equals(List.of("Galletas, pan", "Quesos")), "Conserva categorías antiguas y comas");
        comprobar(c.categoriasConfiguradas().get(0).seccion().isEmpty(), "No inventa secciones para datos antiguos");
        comprobar(Categoria.SECCIONES.size() == 18, "Las 18 secciones solicitadas");
        String original = Files.readString(datos.resolve("categorias.csv"));
        try { c.guardarCategoria("Nueva", ""); throw new AssertionError("Aceptó sección vacía"); }
        catch (IllegalArgumentException esperada) { comprobar(Files.readString(datos.resolve("categorias.csv")).equals(original), "Error no modifica archivo"); }
        try { c.guardarCategoria("Nueva", "Otra"); throw new AssertionError("Aceptó sección no disponible"); }
        catch (IllegalArgumentException esperada) { comprobar(c.categorias().size() == 2, "Rechaza sección no disponible"); }
        c.guardarCategoria("galletas, pan", "Repostería");
        comprobar(c.categorias().size() == 2 && c.categorias().get(0).equals("Galletas, pan"), "Actualiza sección sin duplicar ni cambiar el nombre");
        c.guardarCategoria("Quesos", "Lácteos");
        c.guardarCategoria("Dulces de leche", "Dulces");
        Producto galletas = new Producto("001", "Galletas", n("10"), "Galletas, pan", UnidadVenta.PIEZA, false);
        Producto queso = new Producto("002", "Queso", n("50"), "Quesos", UnidadVenta.KILOGRAMO, true);
        Producto vacio = new Producto("003", "Sin movimientos", n("10"), "Quesos", UnidadVenta.PIEZA, false);
        for (Producto p : List.of(galletas, queso, vacio)) c.guardarProducto(p, false);
        comprobar(c.categoriasConfiguradas().get(0).seccion().equals("Repostería"), "Guardar producto conserva sección");
        registrar(c, MovimientoInventario.Tipo.ENTRADA, galletas, "10");
        registrar(c, MovimientoInventario.Tipo.ENTRADA, galletas, "5");
        registrar(c, MovimientoInventario.Tipo.VENTA, galletas, "3");
        registrar(c, MovimientoInventario.Tipo.MERMA, galletas, "2");
        comprobar(cantidad(c, "001").compareTo(n("10")) == 0, "Entradas menos ventas y merma");
        registrar(c, MovimientoInventario.Tipo.ENTRADA, queso, "2.5");
        registrar(c, MovimientoInventario.Tipo.VENTA, queso, "0.75");
        registrar(c, MovimientoInventario.Tipo.MERMA, queso, "0.125");
        comprobar(cantidad(c, "002").compareTo(n("1.625")) == 0, "Conserva cantidades fraccionarias exactas");
        comprobar(cantidad(c, "003").signum() == 0, "Producto sin movimientos inicia en cero");
        var venta = registrar(c, MovimientoInventario.Tipo.VENTA, galletas, "12");
        comprobar(cantidad(c, "001").compareTo(n("-2")) == 0, "Permite ventas con estimación negativa");
        c.autorizacion.cambiar(new char[0], "clave-prueba".toCharArray(),
                new char[][]{"uno".toCharArray(), "dos".toCharArray(), "tres".toCharArray()});
        c.cancelarVenta(venta.folio(), "clave-prueba".toCharArray());
        comprobar(cantidad(c, "001").compareTo(n("10")) == 0, "Cancelación deja de descontar cantidad");
        c.guardarProducto(new Producto("001", "Galletas nuevas", n("20"), "Dulces de leche", UnidadVenta.PIEZA, false), true);
        comprobar(cantidad(c, "001").compareTo(n("10")) == 0, "Cambio de nombre o categoría conserva saldo por código");
        ContextoAplicacion recarga = new ContextoAplicacion(datos);
        comprobar(recarga.categoriasConfiguradas().equals(c.categoriasConfiguradas()), "Persisten todas las secciones");
        comprobar(cantidad(recarga, "001").compareTo(n("10")) == 0, "Saldo y cancelación persisten al reiniciar");
        c.guardarProducto(new Producto("002", "Queso", n("50"), "Quesos", UnidadVenta.PIEZA, true), true);
        comprobar(cantidad(c, "002") == null, "No mezcla kilos con piezas al cambiar unidad");
        try { c.eliminarCategoria("Quesos"); throw new AssertionError("Eliminó categoría con productos"); }
        catch (IllegalArgumentException esperada) { comprobar(c.categorias().contains("Quesos"), "Protege categorías con productos"); }
        c.eliminarCategoria("Galletas, pan");
        comprobar(!new ContextoAplicacion(datos).categorias().contains("Galletas, pan"), "Elimina categorías vacías y conserva formato");
        System.out.println("OK: " + verificaciones + " verificaciones de categorías y estimaciones.");
    }
}
