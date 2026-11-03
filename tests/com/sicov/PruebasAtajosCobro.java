package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.vista.ventas.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import javax.swing.*;

/** Verifica Enter, importe insuficiente y separación entre pagar y cerrar el ticket. */
public final class PruebasAtajosCobro {
    private static int verificaciones;
    private static void comprobar(boolean condicion, String mensaje) {
        verificaciones++; if (!condicion) throw new AssertionError(mensaje);
    }
    private static JButton boton(Container padre, String texto) {
        for (Component hijo : padre.getComponents()) {
            if (hijo instanceof JButton b && b.getText().equals(texto)) return b;
            if (hijo instanceof Container c) { JButton b = boton(c, texto); if (b != null) return b; }
        }
        return null;
    }
    private static void distribuir(Container padre) {
        padre.doLayout();
        for (Component hijo : padre.getComponents()) if (hijo instanceof Container c) distribuir(c);
    }
    private static void enter(JComponent panel) {
        Object nombre = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        comprobar(nombre != null && panel.getActionMap().get(nombre) != null, "Existe acción Enter en ventana");
        panel.getActionMap().get(nombre).actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, nombre.toString()));
    }
    public static void main(String[] args) throws Exception {
        int[] pagos = {0}, cierres = {0}, pagosConGuardia = {0}, cierresConGuardia = {0};
        PanelVentas[] ventas = new PanelVentas[1];
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-atajos-"));
        Method procesar = PanelVentas.class.getDeclaredMethod("procesarEnter", KeyEvent.class, Runnable.class);
        procesar.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                PanelCobro cobro = new PanelCobro(v -> {}, () -> {}, () -> {}, () -> pagos[0]++);
                cobro.actualizar(new BigDecimal("20"), new BigDecimal("50")); enter(cobro);
                comprobar(pagos[0] == 0, "Enter no confirma efectivo insuficiente");
                cobro.actualizar(new BigDecimal("100"), new BigDecimal("50")); enter(cobro);
                comprobar(pagos[0] == 1, "Enter confirma con efectivo suficiente");
                cobro.efectivo.setText("100"); cobro.actualizar(BigDecimal.ZERO, new BigDecimal("50"));
                cobro.efectivo.postActionEvent(); comprobar(pagos[0] == 2, "Enter funciona dentro del campo de efectivo");
                cobro.efectivo.setText("importe inválido"); cobro.actualizar(BigDecimal.ZERO, new BigDecimal("50"));
                enter(cobro); comprobar(pagos[0] == 2, "Enter no acepta texto inválido");
                cobro.setSize(500, 450); distribuir(cobro);
                comprobar(cobro.efectivo.getHeight() == 40, "El campo de efectivo conserva altura compacta");
                comprobar(boton(cobro, "Importe exacto").getHeight() == 34 && boton(cobro, "Borrar efectivo").getHeight() == 34,
                        "Los botones de efectivo conservan altura compacta");
                var producto = new Producto("A", "Huevo", new BigDecimal("3"), "Huevo", UnidadVenta.PIEZA, true);
                var registro = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                        java.util.List.of(DetalleVenta.de(producto, new BigDecimal("6"))), new BigDecimal("20"), "");
                PanelTicket ticket = new PanelTicket(registro, () -> cierres[0]++); enter(ticket);
                comprobar(cierres[0] == 1, "Enter cierra el ticket");
                ventas[0] = new PanelVentas(contexto);
                KeyEvent presion = new KeyEvent(ventas[0], KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n');
                procesar.invoke(ventas[0], presion, (Runnable) () -> pagosConGuardia[0]++);
                comprobar(presion.isConsumed(), "La confirmación consume el evento antes de los botones");
                KeyEvent repeticion = new KeyEvent(ventas[0], KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n');
                procesar.invoke(ventas[0], repeticion, (Runnable) () -> cierresConGuardia[0]++);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                comprobar(pagosConGuardia[0] == 1 && cierresConGuardia[0] == 0,
                        "Mantener Enter para pagar no cierra el ticket recién abierto");
                Field campo = PanelVentas.class.getDeclaredField("atajo"); campo.setAccessible(true);
                KeyEventDispatcher dispatcher = (KeyEventDispatcher) campo.get(ventas[0]);
                dispatcher.dispatchKeyEvent(new KeyEvent(ventas[0], KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_ENTER, '\n'));
                procesar.invoke(ventas[0], new KeyEvent(ventas[0], KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n'),
                        (Runnable) () -> cierresConGuardia[0]++);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> comprobar(cierresConGuardia[0] == 1,
                "Una nueva pulsación de Enter después de soltarlo sí cierra el ticket"));
        System.out.println("OK: " + verificaciones + " verificaciones de Enter y cobro compacto.");
    }
}
