package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.MovimientoInventario;
import com.sicov.vista.UI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/** Consulta y cancelación de ventas sin borrar su historial. */
public class PanelCancelaciones extends JPanel {
    private final ContextoAplicacion contexto;
    private final DefaultTableModel modelo = UI.tabla("Folio", "Fecha y hora", "Total", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final JTextField buscar = new JTextField(22);
    private final JComboBox<String> filtro = new JComboBox<>(new String[] { "Vigentes", "Canceladas", "Todas" });
    private final JTextArea detalle = new JTextArea();
    private final JLabel estado = new JLabel("Selecciona una venta para revisar su ticket antes de cancelar.");
    private final JButton cancelar, configurar;
    private List<MovimientoInventario> visibles = List.of();
    private boolean actualizando;

    public PanelCancelaciones(ContextoAplicacion contexto, Runnable configurarAdministrador) {
        super(new BorderLayout());
        this.contexto = contexto;
        JPanel pagina = UI.pagina("Cancelar ventas");
        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        JPanel cabecera = new JPanel(new BorderLayout(0, 10));
        cabecera.add(new JLabel("Revisa el ticket y autoriza la cancelación con la contraseña de administrador."),
                BorderLayout.NORTH);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.add(new JLabel("Buscar folio"));
        barra.add(buscar);
        buscar.setToolTipText("Escribe el folio completo o una parte.");
        buscar.getAccessibleContext().setAccessibleName("Buscar venta por folio");
        barra.add(new JLabel("Mostrar"));
        barra.add(filtro);
        configurar = UI.boton("Configurar contraseña", configurarAdministrador);
        barra.add(configurar);
        cabecera.add(barra, BorderLayout.CENTER);
        cabecera.add(
                UI.discreto("La venta cancelada se conserva en el historial y deja de contar en los totales de venta."),
                BorderLayout.SOUTH);
        contenido.add(cabecera, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(38);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(205);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(165);
        detalle.setEditable(false);
        detalle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        detalle.setMargin(new Insets(16, 16, 16, 16));
        detalle.getAccessibleContext().setAccessibleName("Detalle de la venta seleccionada");
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tabla),
                new JScrollPane(detalle));
        division.setResizeWeight(0.52);
        division.setDividerLocation(530);
        division.setBorder(BorderFactory.createLineBorder(UI.BORDE));
        contenido.add(division, BorderLayout.CENTER);

        cancelar = UI.boton("Cancelar venta seleccionada", this::cancelar);
        cancelar.setBackground(UI.ACENTO);
        cancelar.setForeground(Color.WHITE);
        JPanel pie = new JPanel(new BorderLayout(12, 10));
        estado.setForeground(UI.MUTED);
        pie.add(estado, BorderLayout.CENTER);
        pie.add(cancelar, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);
        pagina.add(contenido, BorderLayout.CENTER);
        add(pagina, BorderLayout.CENTER);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!actualizando && !e.getValueIsAdjusting())
                mostrar();
        });
        UI.escuchar(buscar, this::refrescar);
        filtro.addActionListener(e -> refrescar());
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing())
                refrescar();
        });
        contexto.escuchar(this::refrescar);
        refrescar();
    }

    private MovimientoInventario seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila >= 0 && fila < visibles.size() ? visibles.get(fila) : null;
    }

    private void refrescar() {
        MovimientoInventario seleccion = seleccionada();
        String folio = seleccion == null ? "" : seleccion.folio();
        String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
        String elegida = (String) filtro.getSelectedItem();
        actualizando = true;
        visibles = contexto.registros().stream().filter(r -> r.tipo() == MovimientoInventario.Tipo.VENTA)
                .filter(r -> r.folio().toLowerCase(Locale.ROOT).contains(texto))
                .filter(r -> "Todas".equals(elegida) || r.cancelado() == "Canceladas".equals(elegida))
                .sorted(Comparator.comparing(MovimientoInventario::fecha).reversed()).toList();
        modelo.setRowCount(0);
        int indice = 0;
        for (int i = 0; i < visibles.size(); i++) {
            var venta = visibles.get(i);
            modelo.addRow(
                    new Object[] { venta.folio(), venta.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                            UI.dinero(venta.total()), venta.cancelado() ? "Cancelada" : "Vigente" });
            if (venta.folio().equals(folio))
                indice = i;
        }
        if (!visibles.isEmpty())
            tabla.setRowSelectionInterval(indice, indice);
        actualizando = false;
        configurar.setVisible(!contexto.autorizacion.configurada());
        mostrar();
    }

    private void mostrar() {
        MovimientoInventario venta = seleccionada();
        cancelar.setEnabled(venta != null && !venta.cancelado() && contexto.autorizacion.configurada());
        cancelar.setToolTipText(
                !contexto.autorizacion.configurada() ? "Primero configura la contraseña de administrador." : null);
        if (venta == null) {
            detalle.setText(contexto.registros().stream().noneMatch(r -> r.tipo() == MovimientoInventario.Tipo.VENTA)
                    ? "Aún no hay ventas registradas.\nLas ventas que cobres aparecerán aquí."
                    : "No hay ventas para esta búsqueda y estado.\nPrueba con otro folio o selecciona Todas.");
            return;
        }
        StringBuilder texto = new StringBuilder("VENTA · ").append(venta.folio()).append('\n')
                .append(venta.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\nEstado: ")
                .append(venta.cancelado() ? "CANCELADA" : "Vigente").append("\n\n");
        for (var producto : venta.detalles()) {
            texto.append(producto.nombre()).append("\n  ").append(UI.medida(producto.cantidad(), producto.unidad()))
                    .append(" · ").append(UI.dinero(producto.precio())).append(" / ")
                    .append(producto.unidad().simbolo()).append(" · Importe ").append(UI.dinero(producto.importe()))
                    .append('\n');
            if (!producto.promocion().isBlank())
                texto.append("  Promoción: ").append(producto.promocion()).append('\n');
        }
        texto.append("\nTOTAL:    ").append(UI.dinero(venta.total())).append("\nRecibido: ")
                .append(UI.dinero(venta.recibido())).append("\nCambio:   ").append(UI.dinero(venta.cambio()));
        if (venta.cancelado())
            texto.append("\n\nMotivo de cancelación:\n")
                    .append(venta.motivo().isBlank() ? "Sin motivo registrado (venta anterior)." : venta.motivo())
                    .append("\n\nEsta venta está excluida de los totales de venta.");
        detalle.setText(texto.toString());
        detalle.setCaretPosition(0);
    }

    private void cancelar() {
        MovimientoInventario venta = seleccionada();
        if (venta == null || venta.cancelado() || !contexto.autorizacion.configurada())
            return;
        solicitar(this, contexto, venta);
        if (contexto.registros().stream().anyMatch(r -> r.folio().equals(venta.folio()) && r.cancelado())) {
            estado.setText("Venta " + venta.folio() + " cancelada. Se conserva en el historial.");
            estado.setForeground(UI.VERDE);
        }
    }

    /** Confirmación compartida con Reportes; Enter nunca confirma esta acción. */
    public static void solicitar(Component padre, ContextoAplicacion contexto, MovimientoInventario venta) {
        if (venta == null || venta.tipo() != MovimientoInventario.Tipo.VENTA || venta.cancelado())
            return;
        Confirmacion contenido = new Confirmacion(contexto, venta);
        JDialog dialogo = new JDialog(padre == null ? null : SwingUtilities.getWindowAncestor(padre), "Cancelar venta",
                Dialog.ModalityType.APPLICATION_MODAL);
        contenido.dialogo = dialogo;
        dialogo.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialogo.setContentPane(contenido);
        dialogo.getRootPane().setDefaultButton(null);
        dialogo.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "volver");
        dialogo.getRootPane().getActionMap().put("volver", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contenido.cerrar();
            }
        });
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                contenido.cerrar();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                contenido.clave.setText("");
            }
        });
        dialogo.pack();
        dialogo.setMinimumSize(new Dimension(520, dialogo.getHeight()));
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(padre);
        SwingUtilities.invokeLater(() -> contenido.motivo.requestFocusInWindow());
        dialogo.setVisible(true);
    }

    private static final class Confirmacion extends JPanel {
        private final ContextoAplicacion contexto;
        private final MovimientoInventario venta;
        private final JTextField motivo = new JTextField(34);
        private final JPasswordField clave = new JPasswordField(34);
        private final JLabel error = new JLabel(" ");
        private final JButton confirmar, volver;
        private JDialog dialogo;
        private boolean ocupado, completado;

        private Confirmacion(ContextoAplicacion contexto, MovimientoInventario venta) {
            super(new BorderLayout(12, 16));
            this.contexto = contexto;
            this.venta = venta;
            setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
            JPanel campos = new JPanel();
            campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
            JLabel titulo = UI.titulo("Cancelar " + venta.folio());
            titulo.setFont(titulo.getFont().deriveFont(21f));
            campos.add(titulo);
            campos.add(Box.createVerticalStrut(12));
            campos.add(new JLabel("Total del ticket: " + UI.dinero(venta.total())));
            campos.add(Box.createVerticalStrut(8));
            campos.add(new JLabel("Se conserva en el historial y deja de contar en ventas."));
            campos.add(Box.createVerticalStrut(18));
            campos.add(UI.campo("Motivo de cancelación (obligatorio)", motivo));
            campos.add(Box.createVerticalStrut(12));
            campos.add(UI.campo("Contraseña de administrador", clave));
            motivo.getAccessibleContext().setAccessibleName("Motivo de cancelación obligatorio");
            clave.getAccessibleContext().setAccessibleName("Contraseña de administrador");
            error.setForeground(UI.ACENTO);
            campos.add(Box.createVerticalStrut(12));
            campos.add(error);
            for (Component c : campos.getComponents())
                if (c instanceof JComponent jc)
                    jc.setAlignmentX(LEFT_ALIGNMENT);
            add(campos, BorderLayout.CENTER);
            confirmar = UI.boton("Cancelar venta", this::confirmar);
            confirmar.setBackground(UI.ACENTO);
            confirmar.setForeground(Color.WHITE);
            confirmar.setDefaultCapable(false);
            // Ni el botón enfocado debe aceptar Enter para una acción destructiva.
            confirmar.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                    "sin-confirmar");
            confirmar.getActionMap().put("sin-confirmar", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {}
            });
            volver = UI.boton("Volver", this::cerrar);
            volver.setDefaultCapable(false);
            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            acciones.add(volver);
            acciones.add(confirmar);
            add(acciones, BorderLayout.SOUTH);
            if (!contexto.autorizacion.configurada()) {
                error.setText("Primero configura la contraseña en Administradores.");
                confirmar.setEnabled(false);
            }
        }

        private void cerrar() {
            if (ocupado)
                return;
            clave.setText("");
            dialogo.dispose();
        }

        private void confirmar() {
            if (ocupado || completado || !contexto.autorizacion.configurada())
                return;
            char[] valor = clave.getPassword();
            clave.setText("");
            String razon = motivo.getText().strip();
            if (razon.isEmpty() || valor.length == 0) {
                error.setText(razon.isEmpty() ? "Escribe el motivo de la cancelación."
                        : "Escribe la contraseña de administrador.");
                Arrays.fill(valor, '\0');
                if (razon.isEmpty())
                    motivo.requestFocusInWindow();
                else
                    clave.requestFocusInWindow();
                return;
            }
            // Una sola solicitud reúne el motivo y la autorización; evita cancelaciones
            // repetidas.
            ocupado = true;
            confirmar.setEnabled(false);
            volver.setEnabled(false);
            motivo.setEnabled(false);
            clave.setEnabled(false);
            error.setText("Guardando cancelación…");
            // El contexto y sus oyentes se actualizan en el EDT, igual que el resto de
            // operaciones.
            SwingUtilities.invokeLater(() -> {
                try {
                    contexto.cancelarVenta(venta.folio(), valor, razon);
                    completado = true;
                    dialogo.dispose();
                } catch (Exception e) {
                    error.setText(e.getMessage() == null ? "No se pudo cancelar la venta. Inténtalo de nuevo."
                            : e.getMessage());
                } finally {
                    Arrays.fill(valor, '\0');
                    clave.setText("");
                    ocupado = false;
                    confirmar.setEnabled(!completado);
                    volver.setEnabled(true);
                    motivo.setEnabled(true);
                    clave.setEnabled(true);
                    if (!completado)
                        clave.requestFocusInWindow();
                }
            });
        }
    }
}
