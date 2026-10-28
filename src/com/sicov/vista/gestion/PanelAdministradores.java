package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.servicio.Autorizacion;
import com.sicov.vista.UI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public class PanelAdministradores extends JPanel {
    private final Autorizacion autorizacion;
    private final JLabel estado = new JLabel(), resultado = new JLabel(" ");
    private final JPasswordField actual = new JPasswordField(), nueva = new JPasswordField(), repetir = new JPasswordField();
    private final JPasswordField recuperada = new JPasswordField(), repetirRecuperada = new JPasswordField();
    private final JPasswordField[] respuestas = camposRespuestas(), respuestasRecuperacion = camposRespuestas();
    private final JButton guardar, recuperar, cambiar;
    private final JPanel recuperacion;
    private JDialog ventanaCambio;
    private final List<JComponent> controles = new ArrayList<>();
    private boolean ocupado;

    public PanelAdministradores(ContextoAplicacion contexto) {
        super(new BorderLayout()); autorizacion = contexto.autorizacion;
        JPanel pagina = UI.pagina("Administradores · contraseña y recuperación");
        JPanel centro = new JPanel(new BorderLayout(12, 12));
        centro.add(estado, BorderLayout.NORTH);
        JTabbedPane pestanas = new JTabbedPane();
        JPanel claves = columna("Contraseña");
        claves.add(UI.campo("Contraseña actual (solo para cambiarla)", actual));
        claves.add(UI.campo("Nueva contraseña", nueva)); claves.add(UI.campo("Repetir contraseña", repetir));
        claves.add(Box.createVerticalGlue());
        JPanel configuracion = new JPanel(new BorderLayout(12, 12));
        JPanel columnas = new JPanel(new GridLayout(1, 2, 24, 0));
        columnas.add(claves); columnas.add(preguntas(respuestas));
        configuracion.add(columnas);
        guardar = UI.boton("Guardar contraseña y respuestas", this::guardar);
        cambiar = UI.boton("Cambiar contraseña", this::abrirCambio);
        JPanel acciones = new JPanel(new GridLayout(1, 2, 12, 0));
        acciones.add(guardar); acciones.add(cambiar);
        configuracion.add(acciones, BorderLayout.SOUTH);

        JPanel nuevaClave = columna("Elige una nueva contraseña");
        nuevaClave.add(UI.campo("Nueva contraseña", recuperada));
        nuevaClave.add(UI.campo("Repetir contraseña", repetirRecuperada));
        nuevaClave.add(Box.createVerticalGlue());
        recuperacion = new JPanel(new BorderLayout(12, 12));
        recuperacion.add(new JLabel("Responde las tres preguntas para cambiar tu contraseña."), BorderLayout.NORTH);
        JPanel columnasRecuperacion = new JPanel(new GridLayout(1, 2, 24, 0));
        columnasRecuperacion.add(preguntas(respuestasRecuperacion)); columnasRecuperacion.add(nuevaClave);
        recuperacion.add(columnasRecuperacion);
        recuperar = UI.boton("Guardar nueva contraseña", this::recuperar);
        recuperacion.add(recuperar, BorderLayout.SOUTH);
        configuracion.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        recuperacion.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        pestanas.addTab("Configurar / cambiar", configuracion);
        centro.add(pestanas);
        JPanel pie = new JPanel(new GridLayout(0, 1, 0, 6));
        pie.add(new JLabel("Las tres respuestas son obligatorias. Aceptan palabras, números y símbolos."));
        pie.add(new JLabel("Se ignoran las mayúsculas y los espacios al inicio y al final de las respuestas."));
        resultado.setForeground(UI.ACENTO); pie.add(resultado); centro.add(pie, BorderLayout.SOUTH);
        controles.addAll(List.of(actual, nueva, repetir, recuperada, repetirRecuperada));
        controles.addAll(Arrays.asList(respuestas)); controles.addAll(Arrays.asList(respuestasRecuperacion));
        pagina.add(centro); add(pagina); refrescar();
    }
    private void abrirCambio() {
        if (ocupado || !autorizacion.recuperacionConfigurada()) return;
        limpiarRecuperacion();
        ventanaCambio = new JDialog(SwingUtilities.getWindowAncestor(this), "Cambiar contraseña",
                Dialog.ModalityType.APPLICATION_MODAL);
        ventanaCambio.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        ventanaCambio.setContentPane(recuperacion);
        ventanaCambio.setSize(840, 470);
        ventanaCambio.setResizable(false);
        ventanaCambio.setLocationRelativeTo(this);
        ventanaCambio.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { limpiarRecuperacion(); }
        });
        ventanaCambio.setVisible(true);
    }
    private void limpiarRecuperacion() {
        recuperada.setText(""); repetirRecuperada.setText("");
        for (var campo : respuestasRecuperacion) campo.setText("");
        for (var control : controles) if (control instanceof JCheckBox mostrar && mostrar.isSelected()) mostrar.doClick();
    }
    private static JPasswordField[] camposRespuestas() {
        return new JPasswordField[]{new JPasswordField(), new JPasswordField(), new JPasswordField()};
    }
    private JPanel columna(String titulo) {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(titulo)); return panel;
    }
    private JPanel preguntas(JPasswordField[] campos) {
        JPanel panel = columna("Preguntas de recuperación · responde las tres");
        for (int i = 0; i < campos.length; i++) {
            String pregunta = Autorizacion.PREGUNTAS_RECUPERACION.get(i);
            campos[i].getAccessibleContext().setAccessibleName(pregunta);
            panel.add(UI.campo(pregunta, campos[i])); panel.add(Box.createVerticalStrut(8));
        }
        char eco = campos[0].getEchoChar();
        JCheckBox mostrar = new JCheckBox("Mostrar respuestas");
        mostrar.addActionListener(e -> { for (var campo : campos) campo.setEchoChar(mostrar.isSelected() ? '\0' : eco); });
        controles.add(mostrar); panel.add(mostrar); panel.add(Box.createVerticalGlue());
        for (Component c : panel.getComponents()) if (c instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
    private void refrescar() {
        boolean configurada = autorizacion.configurada(), recuperacionLista = autorizacion.recuperacionConfigurada();
        estado.setText(ocupado ? "Guardando los cambios…"
                : !configurada ? "Crea tu contraseña y registra las tres respuestas para habilitar la recuperación."
                : recuperacionLista ? "Contraseña y recuperación configuradas."
                : "Tu contraseña anterior está activa. Ingresa esa contraseña y registra las tres respuestas.");
        controles.forEach(c -> c.setEnabled(!ocupado));
        actual.setEnabled(!ocupado && configurada); guardar.setEnabled(!ocupado);
        recuperar.setEnabled(!ocupado && recuperacionLista);
        cambiar.setEnabled(!ocupado && recuperacionLista);
        cambiar.setToolTipText(recuperacionLista ? null : "Primero configura las tres respuestas en Configurar / cambiar.");
    }
    private char[][] leer(JPasswordField[] campos) {
        return Arrays.stream(campos).map(JPasswordField::getPassword).toArray(char[][]::new);
    }
    private void guardar() {
        char[] a = actual.getPassword(), n = nueva.getPassword(), r = repetir.getPassword();
        char[][] preguntas = leer(respuestas);
        ejecutar(() -> {
            if (!Arrays.equals(n, r)) throw new IllegalArgumentException("Las contraseñas nuevas no coinciden.");
            autorizacion.cambiar(a, n, preguntas);
        }, () -> {
            actual.setText(""); nueva.setText(""); repetir.setText("");
            for (var campo : respuestas) campo.setText("");
            resultado.setText("Contraseña y tres respuestas guardadas.");
        }, secretos(a, n, r, preguntas));
    }
    private void recuperar() {
        char[] n = recuperada.getPassword(), r = repetirRecuperada.getPassword();
        char[][] preguntas = leer(respuestasRecuperacion);
        ejecutar(() -> {
            if (!Arrays.equals(n, r)) throw new IllegalArgumentException("Las contraseñas nuevas no coinciden.");
            autorizacion.recuperar(preguntas, n);
        }, () -> {
            actual.setText(""); nueva.setText(""); repetir.setText("");
            recuperada.setText(""); repetirRecuperada.setText("");
            for (var campo : respuestasRecuperacion) campo.setText("");
            resultado.setText("Contraseña restablecida. Ya puedes usar la nueva contraseña.");
            if (ventanaCambio != null) ventanaCambio.dispose();
        }, secretos(new char[0], n, r, preguntas));
    }
    private char[][] secretos(char[] a, char[] n, char[] r, char[][] preguntas) {
        List<char[]> lista = new ArrayList<>(List.of(a, n, r)); lista.addAll(Arrays.asList(preguntas));
        return lista.toArray(char[][]::new);
    }
    @FunctionalInterface private interface Trabajo { void ejecutar() throws Exception; }
    private void ejecutar(Trabajo trabajo, Runnable exito, char[][] secretos) {
        ocupado = true; resultado.setText(" "); refrescar();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try { trabajo.ejecutar(); return null; }
                finally { for (char[] secreto : secretos) Arrays.fill(secreto, '\0'); }
            }
            @Override protected void done() {
                try { get(); exito.run(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); UI.error(destinoError(), e); }
                catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    UI.error(destinoError(), causa instanceof Exception ex ? ex : new Exception(causa));
                } finally { ocupado = false; refrescar(); }
            }
        }.execute();
    }
    private Component destinoError() {
        return ventanaCambio != null && ventanaCambio.isShowing() ? ventanaCambio : this;
    }
}
