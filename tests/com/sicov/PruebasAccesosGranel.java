package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioReportes;
import com.sicov.vista.ventas.PanelVentas;
import java.awt.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Ranking y actualización del granel a mano, usando únicamente datos temporales. */
public final class PruebasAccesosGranel {
    private static int verificaciones;
    private static final ServicioReportes REPORTES = new ServicioReportes();
    private static void comprobar(boolean condicion, String mensaje) {
        verificaciones++; if (!condicion) throw new AssertionError(mensaje);
    }
    private static BigDecimal n(String valor) { return new BigDecimal(valor); }
    private static Producto producto(String codigo, String nombre, UnidadVenta unidad) {
        return new Producto(codigo, nombre, n("100"), "Prueba", unidad, true);
    }
    private static List<String> codigos(List<Producto> productos) {
        return productos.stream().map(Producto::codigo).toList();
    }
    private static MovimientoInventario movimiento(String folio, MovimientoInventario.Tipo tipo,
            DetalleVenta... detalles) {
        BigDecimal total = java.util.Arrays.stream(detalles).map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MovimientoInventario(folio, LocalDateTime.of(2026, 1, 1, 12, 0), tipo, false,
                total, total, BigDecimal.ZERO, "Prueba", List.of(detalles));
    }
    private static MovimientoInventario venta(String folio, Producto producto, String cantidad) {
        return movimiento(folio, MovimientoInventario.Tipo.VENTA, DetalleVenta.de(producto, n(cantidad)));
    }
    private static void probarFrecuencias() {
        Producto jamon = producto("J", "Jamón", UnidadVenta.KILOGRAMO);
        Producto queso = producto("Q", "Queso", UnidadVenta.KILOGRAMO);
        Producto crema = producto("C", "Crema", UnidadVenta.LITRO);
        Producto arroz = producto("A", "Arroz", UnidadVenta.KILOGRAMO);
        Producto frijol = producto("F", "Frijol", UnidadVenta.KILOGRAMO);
        Producto aceite = producto("L", "Aceite", UnidadVenta.LITRO);
        Producto huevo = producto("H", "Huevo por pieza", UnidadVenta.PIEZA);
        Producto desconocido = producto("X", "Fuera del catálogo", UnidadVenta.KILOGRAMO);
        List<Producto> productos = new ArrayList<>(List.of(jamon, huevo, aceite, frijol, arroz, queso, crema));
        List<MovimientoInventario> registros = new ArrayList<>();
        registros.add(venta("V-J", jamon, "10"));
        for (int i = 0; i < 3; i++) registros.add(venta("V-Q" + i, queso, "0.250"));
        for (int i = 0; i < 2; i++) registros.add(venta("V-C" + i, crema, "0.100"));
        DetalleVenta detalleArroz = DetalleVenta.de(arroz, n("0.250"));
        registros.add(movimiento("V-A", MovimientoInventario.Tipo.VENTA,
                detalleArroz, detalleArroz, detalleArroz, detalleArroz));
        registros.add(venta("V-F", frijol, "0.500"));
        for (int i = 0; i < 5; i++) {
            registros.add(venta("V-L" + i, aceite, "2").cancelar("Prueba"));
            registros.add(movimiento("E-L" + i, MovimientoInventario.Tipo.ENTRADA, DetalleVenta.de(aceite, n("20"))));
            registros.add(movimiento("M-L" + i, MovimientoInventario.Tipo.MERMA, DetalleVenta.de(aceite, n("10"))));
            registros.add(venta("V-H" + i, huevo, "100"));
            registros.add(venta("V-X" + i, desconocido, "10"));
        }
        List<Producto> originales = List.copyOf(productos);
        List<MovimientoInventario> registrosOriginales = List.copyOf(registros);
        List<String> orden = codigos(REPORTES.granelMasVendidos(productos, registros));
        comprobar(orden.equals(List.of("Q", "C", "A", "F", "J", "L")),
                "Ordena por ventas vigentes con el código, con desempate alfabético y cero ventas al final");
        comprobar(orden.indexOf("Q") < orden.indexOf("J"), "Tres ventas de 250 g superan una venta de 10 kg");
        comprobar(orden.indexOf("A") > orden.indexOf("C"), "Los detalles duplicados en una venta cuentan una sola vez");
        comprobar(orden.get(orden.size() - 1).equals("L"), "Cancelaciones, entradas y merma no aumentan la frecuencia");
        comprobar(orden.contains("C") && orden.contains("L"), "Incluye productos por litro además de kilogramo");
        comprobar(!orden.contains("H") && !orden.contains("X"), "Excluye piezas sin código físico y productos fuera del catálogo");
        comprobar(productos.equals(originales) && registros.equals(registrosOriginales), "El ranking conserva las colecciones originales");
        comprobar(REPORTES.granelMasVendidos(List.copyOf(productos), List.copyOf(registros)).equals(
                REPORTES.granelMasVendidos(productos, registros)), "Acepta también colecciones inmutables");
    }
    private static void probarLimites() {
        List<Producto> productos = new ArrayList<>();
        for (int i = 9; i >= 0; i--)
            productos.add(producto("G" + i, String.format("Granel %02d", i), UnidadVenta.KILOGRAMO));
        comprobar(codigos(REPORTES.granelMasVendidos(productos, List.of())).equals(
                List.of("G0", "G1", "G2", "G3", "G4", "G5", "G6", "G7")),
                "Sin ventas muestra como máximo ocho productos ordenados por nombre");
        Producto ultimo = productos.get(0);
        comprobar(codigos(REPORTES.granelMasVendidos(productos, List.of(venta("V", ultimo, "0.1")))).equals(
                List.of("G9", "G0", "G1", "G2", "G3", "G4", "G5", "G6")),
                "Un producto vendido entra en los ocho desplazando al de menor prioridad");
        List<Producto> empate = List.of(producto("2", "queso", UnidadVenta.KILOGRAMO),
                producto("1", "Queso", UnidadVenta.KILOGRAMO), producto("3", "Arroz", UnidadVenta.KILOGRAMO));
        comprobar(codigos(REPORTES.granelMasVendidos(empate, List.of())).equals(List.of("3", "1", "2")),
                "Los empates ignoran mayúsculas en el nombre y usan el código para estabilizar el orden");
        comprobar(REPORTES.granelMasVendidos(List.of(), List.of()).isEmpty(), "Catálogo vacío produce accesos vacíos");
        comprobar(REPORTES.granelMasVendidos(List.of(producto("H", "Huevo", UnidadVenta.PIEZA)), List.of()).isEmpty(),
                "Un catálogo de piezas no crea accesos de granel");
    }
    private static <T> T campo(PanelVentas panel, String nombre, Class<T> tipo) throws Exception {
        Field campo = PanelVentas.class.getDeclaredField(nombre); campo.setAccessible(true);
        return tipo.cast(campo.get(panel));
    }
    private static List<JButton> botones(JPanel panel) {
        List<JButton> botones = new ArrayList<>();
        for (Component componente : panel.getComponents()) if (componente instanceof JButton boton) botones.add(boton);
        return botones;
    }
    private static List<String> codigosBotones(JPanel panel) {
        return botones(panel).stream().map(b -> (String) b.getClientProperty("codigoProducto")).toList();
    }
    private static void distribuir(Container padre) {
        padre.doLayout();
        for (Component hijo : padre.getComponents()) if (hijo instanceof Container contenedor) distribuir(contenedor);
    }
    private static void probarVista() throws Exception {
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-accesos-granel-"));
        for (int i = 9; i >= 0; i--) contexto.guardarProducto(producto("G" + i, String.format("Granel %02d", i),
                i % 2 == 0 ? UnidadVenta.KILOGRAMO : UnidadVenta.LITRO), false);
        contexto.guardarProducto(producto("P", "Abarrote sin código", UnidadVenta.PIEZA), false);
        contexto.autorizacion.cambiar(new char[0], "clave-prueba".toCharArray(),
                new char[][]{"uno".toCharArray(), "dos".toCharArray(), "tres".toCharArray()});
        PanelVentas panel = new PanelVentas(contexto);
        JPanel accesos = campo(panel, "accesosRapidos", JPanel.class);
        JScrollPane scroll = campo(panel, "rapidosScroll", JScrollPane.class);
        comprobar(botones(accesos).size() == 8 && codigosBotones(accesos).equals(
                List.of("G0", "G1", "G2", "G3", "G4", "G5", "G6", "G7")),
                "La interfaz limita A mano a ocho productos exclusivamente a granel");
        comprobar(accesos.getLayout() instanceof GridLayout grilla && grilla.getColumns() == 2,
                "Los accesos conservan dos columnas");
        comprobar(scroll.getHorizontalScrollBarPolicy() == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                "A mano evita el desplazamiento horizontal");
        panel.setSize(1200, 800); distribuir(panel); distribuir(panel);
        comprobar(scroll.getViewport().getExtentSize().height < accesos.getPreferredSize().height,
                "Ocho accesos se desplazan dentro del área visible");
        comprobar(botones(accesos).stream().allMatch(b -> b.getHeight() >= 48),
                "Los ocho productos conservan botones de buen tamaño");
        Rectangle primeraVista = scroll.getViewport().getViewRect();
        comprobar(botones(accesos).stream().filter(b -> b.getBounds().intersects(primeraVista))
                .map(b -> (String) b.getClientProperty("codigoProducto")).toList().equals(List.of("G0", "G1", "G2", "G3")),
                "El área visible conserva cuatro accesos en dos filas");
        int altoBoton = botones(accesos).get(0).getHeight();
        scroll.getViewport().setViewPosition(new Point(0, primeraVista.height + ((GridLayout) accesos.getLayout()).getVgap()));
        Rectangle segundaVista = scroll.getViewport().getViewRect();
        comprobar(botones(accesos).stream().filter(b -> b.getBounds().intersects(segundaVista))
                .map(b -> (String) b.getClientProperty("codigoProducto")).toList().equals(List.of("G4", "G5", "G6", "G7")),
                "Desplazarse permite consultar los cuatro accesos siguientes");
        comprobar(botones(accesos).stream().allMatch(b -> b.getHeight() == altoBoton),
                "Desplazarse conserva el tamaño uniforme de los botones");
        var venta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(contexto.producto("G9"), n("0.250"))), n("25"), "Prueba");
        comprobar(codigosBotones(accesos).get(0).equals("G9") && botones(accesos).size() == 8,
                "Registrar una venta refresca la prioridad inmediatamente");
        contexto.cancelarVenta(venta.folio(), "clave-prueba".toCharArray(), "Prueba de actualización");
        comprobar(codigosBotones(accesos).equals(List.of("G0", "G1", "G2", "G3", "G4", "G5", "G6", "G7")),
                "Cancelar la venta restaura el ranking de los accesos");
        comprobar(contexto.productos().size() == 11 && contexto.registros().size() == 1,
                "Actualizar accesos conserva el catálogo y el historial de operaciones");
        ContextoAplicacion vacio = new ContextoAplicacion(Files.createTempDirectory("sicov-sin-granel-"));
        vacio.guardarProducto(producto("P", "Huevo por pieza", UnidadVenta.PIEZA), false);
        PanelVentas panelVacio = new PanelVentas(vacio);
        comprobar(botones(campo(panelVacio, "accesosRapidos", JPanel.class)).isEmpty(),
                "La pantalla no rellena accesos con piezas cuando no hay granel");
        vacio.guardarProducto(producto("G", "Queso nuevo", UnidadVenta.KILOGRAMO), false);
        comprobar(codigosBotones(campo(panelVacio, "accesosRapidos", JPanel.class)).equals(List.of("G")),
                "Dar de alta el primer granel actualiza la pantalla abierta");
    }
    public static void main(String[] args) throws Exception {
        probarFrecuencias(); probarLimites();
        SwingUtilities.invokeAndWait(() -> {
            try { probarVista(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("OK: " + verificaciones + " verificaciones de accesos a granel.");
    }
}
