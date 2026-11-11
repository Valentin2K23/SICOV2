package com.sicov.vista;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.vista.gestion.*;
import com.sicov.vista.promociones.PanelPromociones;
import com.sicov.vista.ventas.PanelVentas;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import javax.swing.*;

/**
 * Contenedor de pantallas, menú lateral e historial de navegación de la aplicación.
 */
public class VentanaPrincipal extends JFrame {
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JPanel menuGestion = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 55));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private final ArrayDeque<Runnable> historial = new ArrayDeque<>();
    private final KeyEventDispatcher atajoRegreso = this::teclaRegreso;
    private boolean escapePresionado;
    private String actual = "ventas";
    private final PanelVentas ventas;
    private final PanelCategorias categorias;
    private final JButton botonVentas = UI.boton("Volver a ventas", () -> navegar("ventas"));
    private final JButton botonGestion = UI.boton("Gestión", () -> mostrarGestion(!menuGestion.isVisible()));
    private final JButton botonRegresar = UI.boton("← Regresar · Esc", this::regresar);

    public VentanaPrincipal(ContextoAplicacion contexto) {
        super("SICOV");
        setLayout(new BorderLayout());
        JPanel cabecera = new JPanel(new GridLayout(1, 3, 20, 0));
        cabecera.setBackground(UI.VERDE);
        JLabel marca = new JLabel("SICOV");
        marca.setForeground(Color.WHITE);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 28f));
        marca.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel identidad = new JPanel();
        identidad.setLayout(new BoxLayout(identidad, BoxLayout.Y_AXIS));
        identidad.setOpaque(false);
        identidad.add(marca);
        JLabel descripcion = new JLabel("ABARROTES · PUNTO DE VENTA");
        descripcion.setFont(descripcion.getFont().deriveFont(10f));
        descripcion.setForeground(new Color(215, 230, 218));
        descripcion.setAlignmentX(Component.CENTER_ALIGNMENT);
        identidad.add(descripcion);
        cabecera.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, UI.DORADO),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        JPanel accesos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accesos.setOpaque(false);
        JPanel navegacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navegacion.setOpaque(false);
        JLabel soporte = new JLabel("<html><small>Soporte técnico</small><br><b>5525972795</b></html>",
                new IconoCabecera(true), SwingConstants.LEFT);
        soporte.setForeground(new Color(222, 234, 225));
        soporte.setIconTextGap(8);
        soporte.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        accesos.add(soporte);
        botonGestion.setIcon(new IconoCabecera(false));
        botonGestion.setIconTextGap(8);
        botonGestion.setToolTipText("Abrir o cerrar el menú de Gestión");
        navegacion.add(botonGestion);
        navegacion.add(botonVentas);
        // Tres columnas iguales mantienen la marca centrada al mostrar Volver a ventas.
        cabecera.add(navegacion);
        cabecera.add(identidad);
        cabecera.add(accesos);
        add(cabecera, BorderLayout.NORTH);
        ventas = new PanelVentas(contexto);
        contenido.add(ventas, "ventas");
        categorias = new PanelCategorias(contexto, this::guardarPaso, this::regresar);
        contenido.add(new PanelCompras(contexto), "compras");
        contenido.add(categorias, "categorias");
        contenido.add(new PanelSalidas(contexto), "salidas");
        contenido.add(new PanelReportes(contexto), "reportes");
        contenido.add(new PanelCancelaciones(contexto, () -> navegar("administradores")), "cancelaciones");
        contenido.add(new PanelPromociones(contexto), "promociones");
        contenido.add(new PanelAdministradores(contexto), "administradores");
        menuGestion.setOpaque(false);
        menuGestion.setFocusCycleRoot(true);
        menuGestion.add(new PanelGestion(this::navegar, () -> mostrarGestion(false)), BorderLayout.WEST);
        JPanel fondoMenu = new JPanel();
        fondoMenu.setOpaque(false);
        fondoMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mostrarGestion(false);
            }
        });
        menuGestion.add(fondoMenu);
        menuGestion.setVisible(false);
        JLayeredPane cuerpo = new JLayeredPane() {
            @Override
            public void doLayout() {
                contenido.setBounds(0, 0, getWidth(), getHeight());
                menuGestion.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        cuerpo.add(contenido, JLayeredPane.DEFAULT_LAYER);
        cuerpo.add(menuGestion, JLayeredPane.PALETTE_LAYER);
        add(cuerpo);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(6, 20, 8, 20));
        pie.add(botonRegresar, BorderLayout.WEST);
        JLabel ayuda = new JLabel("B buscar · + / − cantidad · Supr eliminar · Esc regresar");
        ayuda.setForeground(new Color(105, 112, 104));
        ayuda.setFont(ayuda.getFont().deriveFont(11f));
        ayuda.setToolTipText(
                "Atajos en Ventas, fuera de campos de texto o con el campo de código vacío. +, − y Supr actúan sobre el producto seleccionado.");
        pie.add(ayuda, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);
        actualizarNavegacion();
        setMinimumSize(new Dimension(1050, 650));
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!ventas.hayVentaPendiente() || JOptionPane.showConfirmDialog(VentanaPrincipal.this,
                        "Hay una venta sin cobrar. ¿Salir y descartarla?", "Salir de SICOV",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    dispose();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                ventas.enfocarCodigo();
            }
        });
    }

    public void navegar(String destino) {
        if (destino.equals("gestion")) {
            mostrarGestion(true);
            return;
        }
        mostrarGestion(false);
        if (!actual.equals(destino)) {
            guardarPaso();
            if (destino.equals("categorias"))
                categorias.mostrarMenu();
            actual = destino;
            tarjetas.show(contenido, actual);
        }
        actualizarNavegacion();
        if (actual.equals("ventas"))
            ventas.enfocarCodigo();
    }

    /**
     * Conserva la pantalla y el estado de categorías para restaurarlos al regresar.
     */
    private void guardarPaso() {
        String anterior = actual;
        Runnable categoriaAnterior = categorias.restauracion();
        historial.push(() -> {
            actual = anterior;
            if (actual.equals("categorias"))
                categoriaAnterior.run();
            tarjetas.show(contenido, actual);
            actualizarNavegacion();
            if (actual.equals("ventas"))
                ventas.enfocarCodigo();
        });
    }

    private void actualizarNavegacion() {
        botonVentas.setVisible(!actual.equals("ventas"));
        botonRegresar.setEnabled(menuGestion.isVisible() || !historial.isEmpty());
    }

    private void mostrarGestion(boolean visible) {
        menuGestion.setVisible(visible);
        // El menú cubre la pantalla actual y suspende los atajos de venta mientras está abierto.
        ventas.activarAtajos(!visible);
        botonGestion.setBackground(visible ? UI.SUAVE : Color.WHITE);
        botonGestion.getAccessibleContext()
                .setAccessibleDescription(visible ? "Menú de Gestión abierto" : "Menú de Gestión cerrado");
        actualizarNavegacion();
        if (visible)
            menuGestion.transferFocusDownCycle();
        else if (actual.equals("ventas"))
            ventas.enfocarCodigo();
        else
            botonGestion.requestFocusInWindow();
    }

    public void regresar() {
        // Cerrar el menú tiene prioridad; no consume una entrada del historial.
        if (menuGestion.isVisible())
            mostrarGestion(false);
        else if (!historial.isEmpty())
            historial.pop().run();
    }

    private boolean teclaRegreso(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ESCAPE)
            return false;
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            escapePresionado = false;
            return false;
        }
        Window origen = e.getComponent() instanceof Window ventana ? ventana
                : SwingUtilities.getWindowAncestor(e.getComponent());
        // Los diálogos modales manejan Esc por su cuenta.
        if (e.getID() != KeyEvent.KEY_PRESSED || origen != this)
            return false;
        if (!escapePresionado) {
            escapePresionado = true;
            MenuSelectionManager.defaultManager().clearSelectedPath();
            regresar();
        }
        e.consume();
        return true;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(atajoRegreso);
    }

    @Override
    public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(atajoRegreso);
        escapePresionado = false;
        super.removeNotify();
    }

    private record IconoCabecera(boolean telefono) implements Icon {
        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D dibujo = (Graphics2D) g.create();
            dibujo.translate(x, y);
            dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            dibujo.setColor(c.getForeground());
            dibujo.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (telefono) {
                Path2D receptor = new Path2D.Double();
                receptor.moveTo(4, 1);
                receptor.lineTo(7, 5);
                receptor.lineTo(5, 7);
                receptor.curveTo(6, 10, 8, 12, 11, 13);
                receptor.lineTo(13, 11);
                receptor.lineTo(17, 14);
                receptor.lineTo(15, 17);
                receptor.curveTo(9, 18, 0, 9, 1, 3);
                receptor.closePath();
                dibujo.draw(receptor);
            } else {
                for (int linea = 4; linea <= 14; linea += 5)
                    dibujo.drawLine(2, linea, 16, linea);
            }
            dibujo.dispose();
        }
    }
}
