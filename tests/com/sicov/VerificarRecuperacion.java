package com.sicov;
import com.sicov.controlador.ContextoAplicacion;
import com.sicov.vista.UI;
import com.sicov.vista.gestion.PanelAdministradores;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class VerificarRecuperacion {
    static <T> T campo(Object o, String nombre, Class<T> tipo) throws Exception {
        var f = o.getClass().getDeclaredField(nombre); f.setAccessible(true); return tipo.cast(f.get(o));
    }
    static void distribuir(Container c) {
        c.doLayout(); for (Component hijo : c.getComponents()) if (hijo instanceof Container cont) distribuir(cont);
    }
    static void imagen(Container panel, Path archivo) throws Exception {
        distribuir(panel);
        BufferedImage salida = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics(); panel.printAll(g); g.dispose(); ImageIO.write(salida, "png", archivo.toFile());
    }
    static void esperar(PanelAdministradores panel) throws Exception {
        long fin = System.nanoTime() + 30_000_000_000L;
        boolean[] ocupado = {true};
        while (ocupado[0] && System.nanoTime() < fin) {
            Thread.sleep(50);
            SwingUtilities.invokeAndWait(() -> {
                try { ocupado[0] = campo(panel, "ocupado", Boolean.class); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
        }
        if (ocupado[0]) throw new AssertionError("La operación no terminó");
    }
    public static void main(String[] args) throws Exception {
        Path salida = Path.of(args[0]); Files.createDirectories(salida);
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-ui-recuperacion-"));
        PanelAdministradores[] panel = new PanelAdministradores[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); UI.tema();
                panel[0] = new PanelAdministradores(contexto);
                panel[0].setSize(1266, 560);
                JTabbedPane pestanas = buscarPestanas(panel[0]);
                if (pestanas.getTabCount() != 1 || !pestanas.getTitleAt(0).equals("Configurar / cambiar"))
                    throw new AssertionError("Debe quedar solo la pestaña de configuración");
                imagen(panel[0], salida.resolve("configurar-recuperacion.png"));
                campo(panel[0], "nueva", JPasswordField.class).setText("clave-prueba");
                campo(panel[0], "repetir", JPasswordField.class).setText("clave-prueba");
                JPasswordField[] respuestas = campo(panel[0], "respuestas", JPasswordField[].class);
                for (int i = 0; i < respuestas.length; i++) respuestas[i].setText("texto " + i);
                campo(panel[0], "guardar", JButton.class).doClick();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        esperar(panel[0]);
        if (!contexto.autorizacion.recuperacionConfigurada()) throw new AssertionError("No guardó las preguntas desde el formulario");
        abrir(panel[0]);
        SwingUtilities.invokeAndWait(() -> {
            try {
                JDialog ventana = campo(panel[0], "ventanaCambio", JDialog.class);
                if (!ventana.isShowing() || ventana.getModalityType() != Dialog.ModalityType.APPLICATION_MODAL)
                    throw new AssertionError("El cambio debe abrir una ventana modal");
                campo(panel[0], "recuperada", JPasswordField.class).setText("descartar");
                ventana.dispatchEvent(new WindowEvent(ventana, WindowEvent.WINDOW_CLOSING));
                if (ventana.isDisplayable()) throw new AssertionError("La X debe cerrar la ventana");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        if (!contexto.autorizacion.verificar("clave-prueba".toCharArray())) throw new AssertionError("Cerrar cambió la contraseña");
        abrir(panel[0]);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (campo(panel[0], "recuperada", JPasswordField.class).getPassword().length != 0)
                    throw new AssertionError("Reabrir debe limpiar los datos anteriores");
                JDialog ventana = campo(panel[0], "ventanaCambio", JDialog.class);
                imagen(ventana.getContentPane(), salida.resolve("recuperar-contrasena.png"));
                campo(panel[0], "recuperada", JPasswordField.class).setText("nueva-prueba");
                campo(panel[0], "repetirRecuperada", JPasswordField.class).setText("nueva-prueba");
                JPasswordField[] respuestas = campo(panel[0], "respuestasRecuperacion", JPasswordField[].class);
                for (int i = 0; i < respuestas.length; i++) respuestas[i].setText("TEXTO " + i);
                campo(panel[0], "recuperar", JButton.class).doClick();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        esperar(panel[0]);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (campo(panel[0], "ventanaCambio", JDialog.class).isDisplayable())
                    throw new AssertionError("Guardar correctamente debe cerrar la ventana");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        if (!contexto.autorizacion.verificar("nueva-prueba".toCharArray())) throw new AssertionError("No recuperó desde el formulario");
        if (contexto.autorizacion.verificar("clave-prueba".toCharArray())) throw new AssertionError("Sigue activa la contraseña anterior");
        System.out.println("OK: pestaña única, ventana modal, cierre con X sin cambios y cierre tras recuperar; vistas renderizadas.");
    }
    static void abrir(PanelAdministradores panel) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try { campo(panel, "cambiar", JButton.class).doClick(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> {});
    }
    static JTabbedPane buscarPestanas(Container c) {
        for (Component hijo : c.getComponents()) {
            if (hijo instanceof JTabbedPane p) return p;
            if (hijo instanceof Container cont) { JTabbedPane p = buscarPestanas(cont); if (p != null) return p; }
        }
        return null;
    }
}
