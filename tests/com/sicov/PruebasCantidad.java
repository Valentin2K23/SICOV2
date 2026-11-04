package com.sicov;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioVentas;
import com.sicov.vista.UI;
import com.sicov.vista.ventas.PanelCantidad;
import com.sicov.vista.ventas.PanelTicket;

import java.math.*;
import java.nio.file.*;
import java.util.*;

/**
 * Conversiones de báscula e importe sin abrir ventanas ni usar datos de la tienda.
 */
public final class PruebasCantidad {
    private static int verificaciones;
    private static final Producto JAMON = new Producto("J", "Jamón", new BigDecimal("143.70"), "Granel",
            UnidadVenta.KILOGRAMO, true);
    private static final Producto LECHE = new Producto("L", "Leche a granel", new BigDecimal("26.50"), "Granel",
            UnidadVenta.LITRO, true);
    private static final Producto HUEVO = new Producto("H", "Huevo por pieza", new BigDecimal("3.20"), "Granel",
            UnidadVenta.PIEZA, true);

    private static void comprobar(boolean valor, String mensaje) {
        verificaciones++;
        if (!valor)
            throw new AssertionError(mensaje);
    }

    private static BigDecimal cantidad(Producto p, String valor, String modo) {
        return PanelCantidad.convertir(p, new BigDecimal(valor), modo);
    }

    private static void igual(Producto p, String valor, String modo, String esperado) {
        comprobar(cantidad(p, valor, modo).compareTo(new BigDecimal(esperado)) == 0,
                valor + " " + modo + " = " + esperado);
    }

    private static void invalida(Producto p, String valor, String modo) {
        try {
            cantidad(p, valor, modo);
            throw new AssertionError("Se aceptó una cantidad inválida: " + valor + " " + modo);
        } catch (IllegalArgumentException e) {
            verificaciones++;
        }
    }

    private static void importeExacto(Producto producto, String pesos) {
        BigDecimal medida = cantidad(producto, pesos, "pesos");
        comprobar(
                medida.multiply(producto.precio()).setScale(2, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal(pesos)) == 0,
                producto.nombre() + ": el importe " + pesos + " se conserva al convertir la cantidad");
    }

    private static void probarVentaPorImporte() throws Exception {
        Path datos = Files.createTempDirectory("sicov-importe-");
        ContextoAplicacion contexto = new ContextoAplicacion(datos);
        Producto huevo = new Producto("0304", "HUEVO", new BigDecimal("38"), "Granel", UnidadVenta.KILOGRAMO, true);
        contexto.guardarProducto(huevo, false);
        VentaControlador controlador = new VentaControlador(contexto);
        BigDecimal medida = cantidad(huevo, "20", "pesos");
        controlador.venta.agregar(huevo, medida);
        comprobar(controlador.total().compareTo(new BigDecimal("20.00")) == 0,
                "Regresión: veinte pesos de huevo cuestan $20.00 en el carrito");
        comprobar(UI.medida(medida, huevo.unidad()).equals("≈ 526.316 g"),
                "La etiqueta muestra gramos aproximados sin alterar la cantidad de cálculo");
        controlador.cobro.sumar(new BigDecimal("20"));
        MovimientoInventario venta = controlador.confirmar(false);
        comprobar(venta.total().compareTo(new BigDecimal("20")) == 0 && venta.cambio().signum() == 0,
                "El cobro de veinte pesos no produce un centavo de cambio");
        comprobar(PanelTicket.texto(venta).contains("TOTAL:    $20.00"), "El ticket conserva el importe solicitado");
        DetalleVenta guardado = new ContextoAplicacion(datos).registros().get(0).detalles().get(0);
        comprobar(guardado.importe().compareTo(new BigDecimal("20")) == 0 && guardado.cantidad().compareTo(medida) == 0,
                "La persistencia conserva la precisión de la cantidad y el importe exacto");
        ServicioVentas acumulada = new ServicioVentas();
        for (int i = 0; i < 1000; i++)
            acumulada.agregar(huevo, medida);
        comprobar(acumulada.calcular(List.of()).get(0).importe().compareTo(new BigDecimal("20000")) == 0,
                "Mil capturas por importe no acumulan diferencias de centavos");
        Producto granel = new Producto("G", "Granel", new BigDecimal("3"), "Granel", UnidadVenta.KILOGRAMO, true);
        ServicioVentas promocion = new ServicioVentas();
        for (int i = 0; i < 3; i++)
            promocion.agregar(granel, cantidad(granel, "1", "pesos"));
        Promocion paquete = new Promocion("P", "Un kilo", "Granel", new BigDecimal("2.70"), true,
                Map.of("G", BigDecimal.ONE));
        DetalleVenta detalle = promocion.calcular(List.of(paquete)).get(0);
        comprobar(detalle.importe().compareTo(new BigDecimal("2.70")) == 0 && !detalle.promocion().isEmpty(),
                "Tres capturas de un tercio de kilo alcanzan la promoción de un kilo");
    }

    public static void main(String[] args) throws Exception {
        igual(JAMON, "250", "gramos", "0.25");
        igual(JAMON, "250.5", "gramos", "0.2505");
        igual(JAMON, "1.25", "kilos", "1.25");
        igual(JAMON, "1000", "gramos", "1");
        igual(LECHE, "750", "mililitros", "0.75");
        igual(LECHE, "1.5", "litros", "1.5");
        igual(HUEVO, "12", "piezas", "12");
        igual(HUEVO, "6.00", "piezas", "6");
        importeExacto(JAMON, "20");
        importeExacto(LECHE, "30");
        for (String precio : List.of("0.01", "0.03", "3", "38", "143.70", "999999999999.99")) {
            Producto producto = new Producto("P", "Granel a " + precio, new BigDecimal(precio), "Granel",
                    UnidadVenta.KILOGRAMO, true);
            for (String pesos : List.of("0.01", "0.10", "1", "20", "30", "99.99"))
                importeExacto(producto, pesos);
        }
        invalida(HUEVO, "0.5", "piezas");
        invalida(HUEVO, "2", "gramos");
        invalida(HUEVO, "20", "pesos");
        invalida(JAMON, "2", "litros");
        invalida(LECHE, "2", "kilos");
        invalida(JAMON, "1", "inexistente");
        invalida(JAMON, "0", "gramos");
        invalida(LECHE, "-1", "litros");
        invalida(HUEVO, "0", "piezas");
        importeExacto(JAMON, "0.01");
        invalida(JAMON, "0.001", "pesos");
        Producto gratis = new Producto("G", "Muestra", BigDecimal.ZERO, "Granel", UnidadVenta.KILOGRAMO, true);
        invalida(gratis, "1", "pesos");
        igual(gratis, "100", "gramos", "0.1");
        probarVentaPorImporte();
        System.out.println("OK: " + verificaciones + " verificaciones de cantidad y granel.");
    }
}
