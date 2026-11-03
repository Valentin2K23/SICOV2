package com.sicov;

import com.sicov.modelo.*;
import com.sicov.vista.ventas.PanelCantidad;
import java.math.*;

/** Conversiones de báscula e importe sin abrir ventanas ni usar datos de la tienda. */
public final class PruebasCantidad {
    private static int verificaciones;
    private static final Producto JAMON = new Producto("J", "Jamón", new BigDecimal("143.70"), "Granel", UnidadVenta.KILOGRAMO, true);
    private static final Producto LECHE = new Producto("L", "Leche a granel", new BigDecimal("26.50"), "Granel", UnidadVenta.LITRO, true);
    private static final Producto HUEVO = new Producto("H", "Huevo por pieza", new BigDecimal("3.20"), "Granel", UnidadVenta.PIEZA, true);
    private static void comprobar(boolean valor, String mensaje) {
        verificaciones++; if (!valor) throw new AssertionError(mensaje);
    }
    private static BigDecimal cantidad(Producto p, String valor, String modo) {
        return PanelCantidad.convertir(p, new BigDecimal(valor), modo);
    }
    private static void igual(Producto p, String valor, String modo, String esperado) {
        comprobar(cantidad(p, valor, modo).compareTo(new BigDecimal(esperado)) == 0, valor + " " + modo + " = " + esperado);
    }
    private static void invalida(Producto p, String valor, String modo) {
        try { cantidad(p, valor, modo); throw new AssertionError("Se aceptó una cantidad inválida: " + valor + " " + modo); }
        catch (IllegalArgumentException e) { verificaciones++; }
    }
    public static void main(String[] args) {
        igual(JAMON, "250", "gramos", "0.25"); igual(JAMON, "250.5", "gramos", "0.2505");
        igual(JAMON, "1.25", "kilos", "1.25"); igual(JAMON, "1000", "gramos", "1");
        igual(LECHE, "750", "mililitros", "0.75"); igual(LECHE, "1.5", "litros", "1.5");
        igual(HUEVO, "12", "piezas", "12"); igual(HUEVO, "6.00", "piezas", "6");
        igual(JAMON, "20", "pesos", "0.139"); igual(LECHE, "30", "pesos", "1.132");
        BigDecimal real = cantidad(JAMON, "20", "pesos").multiply(JAMON.precio()).setScale(2, RoundingMode.HALF_UP);
        comprobar(real.compareTo(new BigDecimal("20")) <= 0 && real.compareTo(new BigDecimal("19.97")) == 0,
                "La cantidad por importe no cobra más de lo solicitado y muestra el importe real");
        invalida(HUEVO, "0.5", "piezas"); invalida(HUEVO, "2", "gramos"); invalida(HUEVO, "20", "pesos");
        invalida(JAMON, "2", "litros"); invalida(LECHE, "2", "kilos"); invalida(JAMON, "1", "inexistente");
        invalida(JAMON, "0", "gramos"); invalida(LECHE, "-1", "litros"); invalida(HUEVO, "0", "piezas");
        invalida(JAMON, "0.01", "pesos"); invalida(JAMON, "0.001", "pesos");
        Producto gratis = new Producto("G", "Muestra", BigDecimal.ZERO, "Granel", UnidadVenta.KILOGRAMO, true);
        invalida(gratis, "1", "pesos"); igual(gratis, "100", "gramos", "0.1");
        System.out.println("OK: " + verificaciones + " verificaciones de cantidad y granel.");
    }
}
