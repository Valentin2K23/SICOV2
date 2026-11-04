package com.sicov;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.vista.*;
import com.sicov.vista.gestion.*;
import com.sicov.vista.ventas.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class VerificarPantallas {
    static <T> T campo(Object obj, String nombre, Class<T> tipo) throws Exception {
        var f = obj.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return tipo.cast(f.get(obj));
    }

    static void dibujar(VentanaPrincipal f, Path dir, String nombre) throws Exception {
        f.validate();
        BufferedImage imagen = new BufferedImage(f.getContentPane().getWidth(), f.getContentPane().getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        f.getContentPane().printAll(g);
        g.dispose();
        ImageIO.write(imagen, "png", dir.resolve(nombre + ".png").toFile());
    }

    static JButton boton(Container c, String texto) {
        for (Component hijo : c.getComponents()) {
            if (hijo instanceof JButton b && b.getText().contains(texto))
                return b;
            if (hijo instanceof Container cont) {
                JButton b = boton(cont, texto);
                if (b != null)
                    return b;
            }
        }
        return null;
    }

    static void escape(VentanaPrincipal frame) throws Exception {
        KeyEventDispatcher atajo = campo(frame, "atajoRegreso", KeyEventDispatcher.class);
        atajo.dispatchKeyEvent(new KeyEvent(frame, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, (char) 27));
        // Mantener Esc pulsado no debe saltarse pantallas por la repetición del
        // teclado.
        String anterior = campo(frame, "actual", String.class);
        PanelCategorias categorias = campo(frame, "categorias", PanelCategorias.class);
        String vista = campo(categorias, "pantallaActual", String.class);
        atajo.dispatchKeyEvent(new KeyEvent(frame, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, (char) 27));
        if (!anterior.equals(campo(frame, "actual", String.class))
                || !vista.equals(campo(categorias, "pantallaActual", String.class)))
            throw new AssertionError("Esc mantenido retrocede más de una pantalla");
        atajo.dispatchKeyEvent(new KeyEvent(frame, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_ESCAPE, (char) 27));
    }

    static void vista(VentanaPrincipal frame, String pantalla, String interna) throws Exception {
        if (!campo(frame, "actual", String.class).equals(pantalla) || (interna != null
                && !campo(campo(frame, "categorias", PanelCategorias.class), "pantallaActual", String.class)
                        .equals(interna)))
            throw new AssertionError("Se esperaba " + pantalla + "/" + interna);
    }

    public static void main(String[] args) throws Exception {
        Path salida = Path.of(args[0]);
        Files.createDirectories(salida);
        var c = new ContextoAplicacion(Files.createTempDirectory("sicov-ui-"));
        for (var p : java.util.List.of(
                new Producto("2530", "Leche entera", new BigDecimal("15"), "Lácteos", UnidadVenta.PIEZA, false),
                new Producto("5380", "Papas adobadas", new BigDecimal("16"), "Botanas", UnidadVenta.PIEZA, false),
                new Producto("1040", "Queso fresco", new BigDecimal("100"), "Lácteos", UnidadVenta.KILOGRAMO, true),
                new Producto("1500", "Tequila blanco", new BigDecimal("300"), "Alcohol", UnidadVenta.PIEZA, false),
                new Producto("0012", "Refresco", new BigDecimal("60"), "Bebidas", UnidadVenta.PIEZA, false)))
            c.guardarProducto(p, false);
        c.guardarPromocion(new Promocion("leche", "2 leches por $25", "Lácteos", new BigDecimal("25"), true,
                Map.of("2530", new BigDecimal("2"))));
        c.registrar(MovimientoInventario.Tipo.ENTRADA,
                java.util.List.of(DetalleVenta.de(c.producto("2530"), new BigDecimal("12"))), BigDecimal.ZERO,
                "Recepción");
        c.registrar(MovimientoInventario.Tipo.VENTA,
                java.util.List.of(DetalleVenta.de(c.producto("5380"), new BigDecimal("2"))), new BigDecimal("50"), "");
        c.guardarCategoria("Lácteos", "Lácteos");
        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal frame = null;
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UI.tema();
                frame = new VentanaPrincipal(c);
                frame.setExtendedState(JFrame.NORMAL);
                frame.addNotify();
                frame.setSize(1280, 720);
                frame.validate();
                escape(frame);
                vista(frame, "ventas", null);
                PanelVentas ventas = campo(frame, "ventas", PanelVentas.class);
                JTextField codigo = campo(ventas, "codigo", JTextField.class);
                codigo.setText("2530*3");
                codigo.postActionEvent();
                codigo.setText("5380*2");
                codigo.postActionEvent();
                if (campo(ventas, "controlador", VentaControlador.class).total().compareTo(new BigDecimal("72")) != 0)
                    throw new AssertionError("Captura de códigos");
                dibujar(frame, salida, "ventas-720");
                frame.navegar("compras");
                JPanel tarjetas = campo(frame, "contenido", JPanel.class);
                PanelCompras compras = null;
                for (Component p : tarjetas.getComponents())
                    if (p instanceof PanelCompras pc)
                        compras = pc;
                JTable tabla = campo(compras, "tabla", JTable.class);
                tabla.getModel().setValueAt("5", 0, 2);
                if (!Boolean.TRUE.equals(tabla.getModel().getValueAt(0, 3)))
                    throw new AssertionError("Palomita al confirmar cantidad");
                dibujar(frame, salida, "compras-720");
                frame.navegar("reportes");
                dibujar(frame, salida, "reportes-720");
                frame.navegar("gestion");
                dibujar(frame, salida, "gestion-720");
                JPanel menuGestion = campo(frame, "menuGestion", JPanel.class);
                vista(frame, "reportes", null);
                if (!menuGestion.isVisible())
                    throw new AssertionError("Gestión debe desplegar un menú sobre la pantalla actual");
                escape(frame);
                vista(frame, "reportes", null);
                if (menuGestion.isVisible())
                    throw new AssertionError("Esc debe cerrar primero el menú de Gestión");
                boton(frame, "Gestión").doClick();
                boton(menuGestion, "Categorías").doClick();
                if (menuGestion.isVisible())
                    throw new AssertionError("Seleccionar una sección debe cerrar el menú");
                dibujar(frame, salida, "categorias-menu-720");
                PanelCategorias categorias = campo(frame, "categorias", PanelCategorias.class);
                JButton configurar = boton(categorias, "Configurar"), consultar = boton(categorias, "Ver categorías");
                if (configurar.getWidth() != configurar.getHeight() || consultar.getWidth() != consultar.getHeight())
                    throw new AssertionError("Los dos botones deben ser cuadrados");
                configurar.doClick();
                if (boton(categorias, "Nueva categoría") != null)
                    throw new AssertionError("Debe quitarse Nueva categoría");
                JTable tablaCategorias = campo(categorias, "tablaCategorias", JTable.class);
                int filaLacteos = c.categorias().indexOf("Lácteos");
                if (!tablaCategorias.getValueAt(filaLacteos, 2).equals(2L))
                    throw new AssertionError("Cuenta productos distintos de cada categoría");
                tablaCategorias.setRowSelectionInterval(filaLacteos, filaLacteos);
                campo(categorias, "seccion", JComboBox.class).setSelectedItem("Abarrotes");
                boton(categorias, "Guardar categoría").doClick();
                if (!c.categoriasConfiguradas().get(filaLacteos).seccion().equals("Abarrotes"))
                    throw new AssertionError("Permite cambiar la sección de categorías existentes");
                campo(categorias, "nombre", JTextField.class).setText("Galletas y pan");
                campo(categorias, "seccion", JComboBox.class).setSelectedItem("Repostería");
                dibujar(frame, salida, "categorias-configurar-720");
                boton(categorias, "Guardar categoría").doClick();
                if (c.categoriasConfiguradas().stream()
                        .noneMatch(cat -> cat.nombre().equals("Galletas y pan") && cat.seccion().equals("Repostería")))
                    throw new AssertionError("Guardar categoría desde formulario");
                c.guardarProducto(new Producto("901", "Galletas de prueba", BigDecimal.ONE, "Galletas y pan",
                        UnidadVenta.PIEZA, false), false);
                if (!tablaCategorias.getValueAt(c.categorias().indexOf("Galletas y pan"), 2).equals(1L))
                    throw new AssertionError("Actualiza número de productos al registrar uno");
                c.guardarProducto(
                        new Producto("901", "Galletas de prueba", BigDecimal.ONE, "Botanas", UnidadVenta.PIEZA, false),
                        true);
                if (!tablaCategorias.getValueAt(c.categorias().indexOf("Galletas y pan"), 2).equals(0L))
                    throw new AssertionError("Actualiza número de productos al cambiar su categoría");
                escape(frame);
                vista(frame, "categorias", "menu");
                consultar.doClick();
                dibujar(frame, salida, "categorias-lista-720");
                campo(categorias, "lista", JList.class).setSelectedIndex(c.categorias().indexOf("Lácteos"));
                var productosCategoria = campo(categorias, "modeloProductos",
                        javax.swing.table.DefaultTableModel.class);
                if (productosCategoria.getRowCount() != 2 || productosCategoria.getColumnCount() != 4
                        || !productosCategoria.getValueAt(0, 2).equals("12"))
                    throw new AssertionError("Productos y estimación de la categoría");
                c.registrar(MovimientoInventario.Tipo.ENTRADA,
                        java.util.List.of(DetalleVenta.de(c.producto("2530"), BigDecimal.ONE)), BigDecimal.ZERO,
                        "Recepción");
                if (!productosCategoria.getValueAt(0, 2).equals("13"))
                    throw new AssertionError("Actualiza estimación visible");
                dibujar(frame, salida, "categorias-productos-720");
                frame.navegar("gestion");
                escape(frame);
                vista(frame, "categorias", "productos");
                if (!campo(categorias, "categoriaAbierta", String.class).equals("Lácteos"))
                    throw new AssertionError("Regresa a la categoría exacta que se estaba consultando");
                JDialog modal = new JDialog(frame);
                try {
                    if (campo(frame, "atajoRegreso", KeyEventDispatcher.class).dispatchKeyEvent(
                            new KeyEvent(modal, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, (char) 27)))
                        throw new AssertionError("No debe navegar la ventana principal desde un diálogo");
                } finally {
                    modal.dispose();
                }
                boton(categorias, "Regresar a la lista").doClick();
                vista(frame, "categorias", "consultar");
                campo(categorias, "lista", JList.class).setSelectedIndex(c.categorias().indexOf("Galletas y pan"));
                if (productosCategoria.getRowCount() != 0)
                    throw new AssertionError("Categoría vacía muestra cero productos");
                escape(frame);
                vista(frame, "categorias", "consultar");
                escape(frame);
                vista(frame, "categorias", "menu");
                escape(frame);
                vista(frame, "reportes", null);
                frame.navegar("categorias");
                if (!campo(categorias, "contenido", JPanel.class).getComponent(0).isVisible())
                    throw new AssertionError("Al entrar se muestran los dos botones");
                frame.setSize(1920, 1080);
                dibujar(frame, salida, "categorias-menu-1080");
                if (configurar.getWidth() != configurar.getHeight())
                    throw new AssertionError("Botones cuadrados en 1080p");
                frame.setSize(1280, 720);
                frame.navegar("promociones");
                dibujar(frame, salida, "promociones-720");
                frame.navegar("ventas");
                frame.setSize(1920, 1080);
                dibujar(frame, salida, "ventas-1080");
                System.out.println(
                        "OK: captura, recepción, categorías, navegación, botones cuadrados y actualización de cantidades; pantallas renderizadas.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (frame != null)
                    frame.dispose();
            }
        });
    }
}
