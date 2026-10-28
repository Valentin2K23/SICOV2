package com.sicov;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.servicio.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

public final class PruebasSicov {
    private static int verificaciones;
    private static BigDecimal n(String s) { return new BigDecimal(s); }
    private static void igual(BigDecimal actual, String esperado) {
        comprobar(actual.compareTo(n(esperado)) == 0, "Esperado " + esperado + ", obtenido " + actual);
    }
    private static void comprobar(boolean cumple, String mensaje) {
        verificaciones++; if (!cumple) throw new AssertionError(mensaje);
    }
    private static void falla(Runnable accion) {
        boolean fallo = false;
        try { accion.run(); } catch (IllegalArgumentException e) { fallo = true; }
        comprobar(fallo, "Se esperaba rechazar el dato inválido");
    }
    public static void main(String[] args) throws Exception {
        Path datos = Files.createTempDirectory("sicov-pruebas-");
        ContextoAplicacion c = new ContextoAplicacion(datos);
        Producto leche = new Producto("001", "Leche, \"entera\"", n("15.00"), "Lácteos", UnidadVenta.PIEZA, false);
        Producto tequila = new Producto("002", "Tequila", n("300.00"), "Alcohol", UnidadVenta.PIEZA, false);
        Producto refresco = new Producto("003", "Refresco", n("60.00"), "Bebidas", UnidadVenta.PIEZA, false);
        Producto queso = new Producto("004", "Queso", n("100.00"), "Lácteos", UnidadVenta.KILOGRAMO, true);
        Producto total580 = new Producto("005", "Paquete", n("580.00"), "Otros", UnidadVenta.PIEZA, false);
        for (Producto p : List.of(leche, tequila, refresco, queso, total580)) c.guardarProducto(p, false);
        c.guardarProducto(new Producto("006", "Crema", n("40"), "lácteos", UnidadVenta.LITRO, true), false);
        comprobar(c.producto("006").categoria().equals("Lácteos"), "Unifica mayúsculas de categorías");
        c.guardarPromocion(new Promocion("leche", "2 leches por $25", "Lácteos", n("25"), true, Map.of("001", n("2"))));
        c.guardarPromocion(new Promocion("combo", "Tequila + 2 refrescos", "Alcohol", n("400"), true, Map.of("002", n("1"), "003", n("2"))));
        VentaControlador v = new VentaControlador(c);
        v.venta.agregar(leche, n("1")); v.venta.agregar(leche, n("2"));
        comprobar(v.detalles().size() == 1, "Agrupa productos repetidos"); igual(v.total(), "40");
        v.venta.agregar(leche, n("1")); igual(v.total(), "50");
        comprobar(v.detalles().get(0).promocion().contains("PROMOCIÓN - 2"), "Cuenta las promociones");
        v.venta.cambiarCantidad("001", BigDecimal.ZERO); comprobar(v.venta.vacia(), "Cero elimina la fila");
        v.venta.agregar(tequila, n("1")); v.venta.agregar(refresco, n("3")); igual(v.total(), "460");
        igual(v.detalles().stream().map(DetalleVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add), "460");
        v.venta.limpiar(); v.venta.agregar(queso, n("0.125")); igual(v.total(), "12.50");
        falla(() -> v.venta.cambiarCantidad("004", n("-1")));
        falla(() -> v.venta.agregar(leche, n("0.5")));
        falla(() -> v.venta.agregar(leche, n("-1")));
        v.venta.limpiar(); v.venta.agregar(total580, BigDecimal.ONE);
        v.cobro.sumar(n("300")); comprobar(!v.cobro.cubre(v.total()), "Pago parcial");
        v.cobro.sumar(n("500")); MovimientoInventario venta = v.confirmar(false);
        igual(venta.total(), "580"); igual(venta.recibido(), "800"); igual(venta.cambio(), "220");
        comprobar(v.venta.vacia(), "Vacía carrito al guardar"); igual(v.cobro.recibido(), "0");
        v.venta.agregar(leche, n("3")); var exacta = v.confirmar(true);
        igual(exacta.total(), "40"); igual(exacta.recibido(), "40"); igual(exacta.cambio(), "0");
        v.venta.agregar(leche, n("1")); v.cobro.sumar(n("1"));
        boolean rechazo = false; try { v.confirmar(true); } catch (IllegalArgumentException e) { rechazo = true; }
        comprobar(rechazo, "Pago exacto solo sin efectivo"); v.cobro.limpiar(); v.venta.limpiar();
        var entrada = c.registrar(MovimientoInventario.Tipo.ENTRADA,
                List.of(DetalleVenta.de(leche, n("20")), DetalleVenta.de(queso, n("1.5"))), BigDecimal.ZERO, "Recepción");
        comprobar(entrada.detalles().size() == 2 && entrada.folio().startsWith("E-"), "Recepción con un folio");
        var merma = c.registrar(MovimientoInventario.Tipo.MERMA, List.of(DetalleVenta.de(queso, n("0.5"))),
                BigDecimal.ZERO, "Producto retirado");
        comprobar(merma.folio().startsWith("M-"), "Merma con folio");
        c.guardarProducto(new Producto("001", "Leche nueva", n("99"), "Otra categoría", UnidadVenta.PIEZA, false), true);
        ContextoAplicacion recarga = new ContextoAplicacion(datos);
        comprobar(recarga.producto("001").nombre().equals("Leche nueva"), "Persiste edición");
        var historica = recarga.registros().stream().filter(r -> r.folio().equals(exacta.folio())).findFirst().orElseThrow();
        comprobar(historica.detalles().get(0).nombre().equals("Leche, \"entera\""), "Conserva nombre histórico con comillas");
        igual(historica.detalles().get(0).precio(), "15");
        comprobar(historica.detalles().get(0).categoria().equals("Lácteos"), "Conserva categoría histórica");
        comprobar(recarga.promociones().size() == 2, "Persisten promociones");
        c.autorizacion.cambiar(new char[0], "clave-segura".toCharArray(),
                new char[][]{"Luna".toCharArray(), "casa azul".toCharArray(), "primavera".toCharArray()});
        boolean claveRechazada = false;
        try { c.cancelarVenta(venta.folio(), "incorrecta".toCharArray()); } catch (IllegalArgumentException e) { claveRechazada = true; }
        comprobar(claveRechazada, "Cancelación requiere contraseña");
        c.cancelarVenta(venta.folio(), "clave-segura".toCharArray());
        Map<String, BigDecimal> resumen = new ServicioReportes().ventasPorCategoria(c.registros());
        comprobar(!resumen.containsKey("Otros"), "Cancela sin sumar a reportes"); igual(resumen.get("Lácteos"), "40");
        ContextoAplicacion finalRecarga = new ContextoAplicacion(datos);
        comprobar(finalRecarga.registros().stream().anyMatch(r -> r.folio().equals(venta.folio()) && r.cancelado()), "Persiste cancelación");
        comprobar(finalRecarga.autorizacion.verificar("clave-segura".toCharArray()), "Persiste contraseña");
        comprobar(!Files.readString(datos.resolve("configuracion.properties")).contains("clave-segura"), "No almacena contraseña literal");
        Path fallos = Files.createTempDirectory("sicov-fallo-");
        ContextoAplicacion cFallo = new ContextoAplicacion(fallos);
        VentaControlador vFallo = new VentaControlador(cFallo); vFallo.venta.agregar(leche, BigDecimal.ONE);
        Files.createDirectory(fallos.resolve("ventas.csv")); Files.writeString(fallos.resolve("ventas.csv/bloqueo"), "Prueba");
        boolean errorEscritura = false;
        try { vFallo.confirmar(true); } catch (IOException e) { errorEscritura = true; }
        comprobar(errorEscritura && !vFallo.venta.vacia(), "Conserva carrito si falla el guardado");
        comprobar(cFallo.registros().isEmpty(), "No registra venta fallida");
        System.out.println("OK: " + verificaciones + " verificaciones. Datos temporales: " + datos);
    }
}
