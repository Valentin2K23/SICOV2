package com.sicov.vista;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.vista.gestion.*;
import com.sicov.vista.promociones.PanelPromociones;
import com.sicov.vista.ventas.PanelVentas;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import javax.swing.*;

public class VentanaPrincipal extends JFrame {
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final ArrayDeque<Runnable> historial = new ArrayDeque<>();
    private final KeyEventDispatcher atajoRegreso = this::teclaRegreso;
    private boolean escapePresionado;
    private String actual = "ventas";
    private final PanelVentas ventas;
    private final PanelCategorias categorias;
    private final JButton botonVentas = UI.boton("Volver a ventas", () -> navegar("ventas"));
    private final JButton botonGestion = UI.boton("Gestión", () -> navegar("gestion"));
    private final JButton botonRegresar = UI.boton("← Regresar · Esc", this::regresar);
    public VentanaPrincipal(ContextoAplicacion contexto) {
        super("SICOV"); setLayout(new BorderLayout());
        JPanel cabecera = new JPanel(new BorderLayout()); cabecera.setBackground(UI.AZUL);
        JLabel marca = new JLabel("SICOV"); marca.setForeground(Color.WHITE); marca.setFont(marca.getFont().deriveFont(Font.BOLD, 26f));
        JPanel identidad = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); identidad.setOpaque(false);
        identidad.add(marca);
        JLabel descripcion = new JLabel("ABARROTES · PUNTO DE VENTA");
        descripcion.setForeground(new Color(225, 235, 214)); identidad.add(descripcion);
        cabecera.add(identidad);
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, UI.ACENTO),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        JPanel accesos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); accesos.setOpaque(false);
        accesos.add(botonVentas); accesos.add(botonGestion);
        cabecera.add(accesos, BorderLayout.EAST); add(cabecera, BorderLayout.NORTH);
        ventas = new PanelVentas(contexto);
        contenido.add(ventas, "ventas"); contenido.add(new PanelGestion(this::navegar), "gestion");
        categorias = new PanelCategorias(contexto, this::guardarPaso, this::regresar);
        contenido.add(new PanelCompras(contexto), "compras"); contenido.add(categorias, "categorias");
        contenido.add(new PanelSalidas(contexto), "salidas"); contenido.add(new PanelReportes(contexto), "reportes");
        contenido.add(new PanelCancelaciones(contexto, () -> navegar("administradores")), "cancelaciones");
        contenido.add(new PanelPromociones(contexto), "promociones");
        contenido.add(new PanelAdministradores(contexto), "administradores"); add(contenido);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(6, 20, 8, 20));
        pie.add(botonRegresar, BorderLayout.WEST);
        JLabel ayuda = new JLabel("B buscar · + / − cantidad · Supr eliminar · Esc regresar");
        ayuda.setForeground(new Color(105, 112, 104)); ayuda.setFont(ayuda.getFont().deriveFont(11f));
        ayuda.setToolTipText("Atajos en Ventas, fuera de campos de texto o con el campo de código vacío. +, − y Supr actúan sobre el producto seleccionado."); pie.add(ayuda, BorderLayout.EAST); add(pie, BorderLayout.SOUTH);
        actualizarNavegacion();
        setMinimumSize(new Dimension(1050, 650)); setSize(1280, 720); setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!ventas.hayVentaPendiente() || JOptionPane.showConfirmDialog(VentanaPrincipal.this,
                        "Hay una venta sin cobrar. ¿Salir y descartarla?", "Salir de SICOV",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) dispose();
            }
            @Override public void windowOpened(WindowEvent e) { ventas.enfocarCodigo(); }
        });
    }
    public void navegar(String destino) {
        if (!actual.equals(destino)) {
            guardarPaso();
            if (destino.equals("categorias")) categorias.mostrarMenu();
            actual = destino; tarjetas.show(contenido, actual);
        }
        actualizarNavegacion();
        if (actual.equals("ventas")) ventas.enfocarCodigo();
    }
    private void guardarPaso() {
        String anterior = actual;
        Runnable categoriaAnterior = categorias.restauracion();
        historial.push(() -> {
            actual = anterior;
            if (actual.equals("categorias")) categoriaAnterior.run();
            tarjetas.show(contenido, actual);
            actualizarNavegacion();
            if (actual.equals("ventas")) ventas.enfocarCodigo();
        });
    }
    private void actualizarNavegacion() {
        botonVentas.setVisible(!actual.equals("ventas"));
        botonGestion.setVisible(!actual.equals("gestion"));
        botonRegresar.setEnabled(!historial.isEmpty());
    }
    public void regresar() { if (!historial.isEmpty()) historial.pop().run(); }
    private boolean teclaRegreso(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ESCAPE) return false;
        if (e.getID() == KeyEvent.KEY_RELEASED) { escapePresionado = false; return false; }
        Window origen = e.getComponent() instanceof Window ventana ? ventana : SwingUtilities.getWindowAncestor(e.getComponent());
        if (e.getID() != KeyEvent.KEY_PRESSED || origen != this) return false;
        if (!escapePresionado) {
            escapePresionado = true;
            MenuSelectionManager.defaultManager().clearSelectedPath();
            regresar();
        }
        e.consume(); return true;
    }
    @Override public void addNotify() {
        super.addNotify(); KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(atajoRegreso);
    }
    @Override public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(atajoRegreso);
        escapePresionado = false; super.removeNotify();
    }
}
