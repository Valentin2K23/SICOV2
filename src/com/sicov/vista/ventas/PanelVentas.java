package com.sicov.vista.ventas;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioReportes;
import com.sicov.vista.ImagenProducto;
import com.sicov.vista.UI;
import com.sicov.vista.promociones.PanelPromociones;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.*;

/**
 * Coordina el carrito y los diálogos de cobro y ticket en el hilo de eventos de Swing.
 */
public class PanelVentas extends JPanel {
    private final ContextoAplicacion contexto;
    private final VentaControlador controlador;
    private final JTextField codigo = new JTextField(), cantidad = new JTextField(5);
    private final DefaultListModel<DetalleVenta> modelo = new DefaultListModel<>();
    private final JList<DetalleVenta> lista = new JList<>(modelo) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int i = locationToIndex(e.getPoint());
            if (i < 0 || !getCellBounds(i, i).contains(e.getPoint()))
                return null;
            DetalleVenta d = modelo.get(i);
            return d.nombre() + " · " + UI.medida(d.cantidad(), d.unidad()) + " · " + UI.dinero(d.precio()) + "/"
                    + d.unidad().simbolo();
        }
    };
    private final PanelTotales totales = new PanelTotales();
    private final PanelCobro cobro;
    private final JPanel editorCantidad = new JPanel(new BorderLayout(0, 4));
    private final JLabel unidadCantidad = new JLabel(), pasoCantidad = UI.discreto("");
    private final JPanel accesosRapidos = new PanelRapidos();
    private final JScrollPane rapidosScroll = new JScrollPane(accesosRapidos);
    private final JLabel resumenProductos = UI.discreto("Sin productos todavía");
    private final JButton deshacer = UI.boton("Deshacer", this::deshacerEliminacion);
    private DetalleVenta retirado;
    private final JLabel ultimaVenta = new JLabel("Selecciona un producto para cambiar su cantidad.");
    private final JLabel contador = new JLabel("Sin productos");
    private final CardLayout tarjetasVenta = new CardLayout();
    private final JPanel contenidoVenta = new JPanel(tarjetasVenta);
    private final JPanel aviso = new JPanel(new BorderLayout());
    private final JLabel mensaje = new JLabel();
    private final Timer temporizador;
    // Evita reaccionar a selecciones de refresco y a pulsaciones repetidas durante
    // el cobro.
    private boolean refrescando, consumirCaracter, confirmando, enterPresionado;
    private boolean mostrandoCantidad, cantidadEditada;
    private boolean atajosActivos = true;
    private JDialog dialogoCobro, dialogoTicket;
    private final JButton pagar = UI.primario("Pagar", this::abrirCobro);
    private final KeyEventDispatcher atajo = this::tecla;

    public PanelVentas(ContextoAplicacion contexto) {
        super(new BorderLayout(16, 10));
        this.contexto = contexto;
        controlador = new VentaControlador(contexto);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 8, 18));
        // Captura por código y mensajes temporales al cajero.
        JPanel principal = new JPanel(new BorderLayout(0, 12));
        JPanel captura = UI.tarjeta();
        JPanel entrada = new JPanel(new BorderLayout(10, 7));
        entrada.setOpaque(false);
        entrada.add(UI.discreto("AGREGAR PRODUCTO"), BorderLayout.NORTH);
        codigo.setFont(codigo.getFont().deriveFont(25f));
        codigo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.VERDE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        codigo.getAccessibleContext().setAccessibleName("Código de producto");
        codigo.setToolTipText("Escanea o escribe el código. Para varias piezas, CÓDIGO*3.");
        entrada.add(codigo);
        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.setOpaque(false);
        botones.add(UI.boton("Agregar", this::agregarCodigo));
        botones.add(UI.primario("Buscar productos", this::buscarMercancia));
        entrada.add(botones, BorderLayout.EAST);
        captura.add(entrada);
        captura.add(UI.discreto("Código + Enter para agregar · Los productos a granel te pedirán la cantidad"),
                BorderLayout.SOUTH);
        JPanel superior = new JPanel(new BorderLayout(0, 6));
        superior.add(captura);
        aviso.setBackground(new Color(255, 241, 209));
        aviso.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));
        aviso.add(mensaje);
        aviso.add(UI.boton("Cerrar", () -> aviso.setVisible(false)), BorderLayout.EAST);
        aviso.setVisible(false);
        superior.add(aviso, BorderLayout.SOUTH);
        principal.add(superior, BorderLayout.NORTH);
        temporizador = new Timer(4500, e -> aviso.setVisible(false));
        temporizador.setRepeats(false);

        // La misma tarjeta alterna entre bienvenida y productos de la venta.
        JPanel carrito = UI.tarjeta();
        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);
        encabezado.add(UI.titulo("Venta actual"));
        contador.setForeground(UI.MUTED);
        encabezado.add(contador, BorderLayout.EAST);
        carrito.add(encabezado, BorderLayout.NORTH);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(82);
        lista.setBackground(Color.WHITE);
        lista.setCellRenderer(new FilaProducto(contexto));
        lista.setToolTipText("");
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e))
                    return;
                int indice = lista.locationToIndex(e.getPoint());
                if (indice < 0)
                    return;
                Rectangle fila = lista.getCellBounds(indice, indice);
                if (fila == null || !fila.contains(e.getPoint()))
                    return;
                if (e.getX() >= fila.x + fila.width - 60) {
                    if (e.getClickCount() == 1) {
                        lista.setSelectedIndex(indice);
                        eliminarSeleccionado();
                    }
                } else if (e.getClickCount() == 2)
                    editarMedida();
            }
        });
        lista.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = lista.locationToIndex(e.getPoint());
                Rectangle r = i < 0 ? null : lista.getCellBounds(i, i);
                lista.setCursor(Cursor.getPredefinedCursor(
                        r != null && r.contains(e.getPoint()) && e.getX() >= r.width - 60 ? Cursor.HAND_CURSOR
                                : Cursor.DEFAULT_CURSOR));
            }
        });
        JScrollPane desplazamiento = new JScrollPane(lista);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contenidoVenta.add(desplazamiento, "productos");
        JPanel vacio = new FondoCarrito();
        vacio.setBackground(Color.WHITE);
        JPanel bienvenida = new JPanel(new GridLayout(0, 1, 0, 12));
        bienvenida.setOpaque(false);
        JLabel tituloVacio = UI.titulo("Todo listo para vender");
        tituloVacio.setHorizontalAlignment(SwingConstants.CENTER);
        bienvenida.add(tituloVacio);
        JLabel detalle = new JLabel("Escanea un código, busca un producto o elige uno de la derecha.",
                SwingConstants.CENTER);
        detalle.setForeground(UI.MUTED);
        bienvenida.add(detalle);
        JLabel granel = UI.discreto("Por pieza, por peso o por importe. Tú eliges.");
        granel.setHorizontalAlignment(SwingConstants.CENTER);
        bienvenida.add(granel);
        vacio.add(bienvenida);
        contenidoVenta.add(vacio, "vacio");
        carrito.add(contenidoVenta);
        editorCantidad.setOpaque(false);
        editorCantidad.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UI.BORDE));
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 8));
        controles.setOpaque(false);
        controles.add(UI.discreto("Cantidad"));
        controles.add(UI.boton("−", () -> ajustar(-1)));
        UI.soloNumeros(cantidad);
        controles.add(cantidad);
        controles.add(unidadCantidad);
        controles.add(UI.boton("+", () -> ajustar(1)));
        controles.add(UI.boton("Editar cantidad", this::editarMedida));
        controles.add(pasoCantidad);
        cantidad.setToolTipText("Cantidad en la unidad indicada. Enter aplica; cero retira el producto.");
        editorCantidad.add(controles);
        editorCantidad.setVisible(false);
        carrito.add(editorCantidad, BorderLayout.SOUTH);
        principal.add(carrito);
        add(principal);

        // Totales y productos frecuentes; el cobro se abre en su propio diálogo.
        JPanel lateral = new JPanel(new BorderLayout(0, 12));
        lateral.setPreferredSize(new Dimension(336, 0));
        JPanel resumen = UI.tarjeta();
        JPanel importe = new JPanel(new BorderLayout(0, 10));
        importe.setOpaque(false);
        importe.add(totales, BorderLayout.NORTH);
        importe.add(resumenProductos);
        pagar.setFont(pagar.getFont().deriveFont(Font.BOLD, 18f));
        pagar.setPreferredSize(new Dimension(0, 42));
        importe.add(pagar, BorderLayout.SOUTH);
        resumen.add(importe);
        lateral.add(resumen, BorderLayout.NORTH);
        cobro = new PanelCobro(this::recibir, this::borrarEfectivo, this::importeExacto, this::confirmar);
        UI.escuchar(cobro.efectivo, this::actualizarCobro);
        JPanel aMano = UI.tarjeta();
        aMano.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.BORDE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JPanel tituloRapidos = new JPanel(new BorderLayout(0, 3));
        tituloRapidos.setOpaque(false);
        JLabel titulo = UI.titulo("A mano");
        titulo.setFont(titulo.getFont().deriveFont(19f));
        tituloRapidos.add(titulo);
        titulo.setToolTipText("Solo a granel, ordenados por cuántas ventas vigentes incluyen cada producto.");
        aMano.add(tituloRapidos, BorderLayout.NORTH);
        accesosRapidos.setOpaque(false);
        rapidosScroll.setBorder(BorderFactory.createEmptyBorder());
        rapidosScroll.getViewport().setBackground(Color.WHITE);
        rapidosScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rapidosScroll.setPreferredSize(new Dimension(0, 188));
        rapidosScroll.setMinimumSize(new Dimension(0, 116));
        rapidosScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 188));
        rapidosScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarTarjetasRapidas();
            }
        });
        rapidosScroll.getVerticalScrollBar().setUnitIncrement(62);
        rapidosScroll.getVerticalScrollBar().setBlockIncrement(124);
        JPanel areaRapidos = new JPanel();
        areaRapidos.setLayout(new BoxLayout(areaRapidos, BoxLayout.Y_AXIS));
        areaRapidos.setOpaque(false);
        rapidosScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        areaRapidos.add(rapidosScroll);
        areaRapidos.add(Box.createVerticalGlue());
        aMano.add(areaRapidos);
        lateral.add(aMano);
        lateral.add(UI.boton("Ver promociones", () -> PanelPromociones.consultar(this, contexto)), BorderLayout.SOUTH);
        add(lateral, BorderLayout.EAST);
        JPanel estado = new JPanel(new BorderLayout(8, 0));
        ultimaVenta.setForeground(UI.MUTED);
        ultimaVenta.setFont(ultimaVenta.getFont().deriveFont(12f));
        estado.add(ultimaVenta);
        deshacer.setVisible(false);
        estado.add(deshacer, BorderLayout.EAST);
        add(estado, BorderLayout.SOUTH);
        UI.enter(codigo, this::agregarCodigo);
        UI.enter(cantidad, this::cambiarCantidad);
        UI.escuchar(cantidad, () -> {
            if (!mostrandoCantidad)
                cantidadEditada = true;
        });
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refrescando)
                seleccionar();
        });
        contexto.escuchar(() -> {
            refrescarRapidos();
            refrescar();
        });
        refrescarRapidos();
        refrescar();
    }

    public boolean hayVentaPendiente() {
        return !controlador.venta.vacia() && !controlador.tieneTicketPendiente();
    }

    public void activarAtajos(boolean activos) {
        atajosActivos = activos;
    }

    public void enfocarCodigo() {
        SwingUtilities.invokeLater(codigo::requestFocusInWindow);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(atajo);
    }

    @Override
    public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(atajo);
        super.removeNotify();
    }

    private void refrescarRapidos() {
        accesosRapidos.removeAll();
        List<Producto> productos = new ServicioReportes().granelMasVendidos(contexto.productos(), contexto.registros());
        if (productos.isEmpty()) {
            JLabel vacio = UI.discreto(
                    "<html><div width='200'>Agrega productos por kilogramo o litro en Compras para verlos aquí.</div></html>");
            vacio.setHorizontalAlignment(SwingConstants.CENTER);
            rapidosScroll.setViewportView(vacio);
        } else if (rapidosScroll.getViewport().getView() != accesosRapidos)
            rapidosScroll.setViewportView(accesosRapidos);
        for (Producto p : productos) {
            String nombre = p.nombre().length() > 17 ? p.nombre().substring(0, 15) + "…" : p.nombre();
            JButton boton = UI.boton("<html><center><b>" + UI.html(nombre) + "</b><br>" + UI.dinero(p.precio()) + " / "
                    + p.unidad().simbolo() + "</center></html>", () -> agregarProducto(p));
            boton.setHorizontalAlignment(SwingConstants.CENTER);
            boton.setHorizontalTextPosition(SwingConstants.CENTER);
            boton.setVerticalTextPosition(SwingConstants.BOTTOM);
            boton.setIconTextGap(2);
            boton.setFont(boton.getFont().deriveFont(12f));
            boton.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            boton.putClientProperty("producto", p);
            boton.setToolTipText(p.nombre() + " · " + UI.dinero(p.precio()) + "/" + p.unidad().simbolo());
            boton.setBackground(UI.SUAVE);
            boton.putClientProperty("codigoProducto", p.codigo());
            accesosRapidos.add(boton);
        }
        // Mantiene los cuatro lugares de la primera vista sin inventar productos.
        if (!productos.isEmpty())
            while (accesosRapidos.getComponentCount() < 4) {
                JPanel espacio = new JPanel();
                espacio.setOpaque(false);
                accesosRapidos.add(espacio);
            }
        ajustarTarjetasRapidas();
        accesosRapidos.revalidate();
        accesosRapidos.repaint();
        rapidosScroll.getViewport().setViewPosition(new Point(0, 0));
    }

    private void ajustarTarjetasRapidas() {
        int visible = rapidosScroll.getViewport().getHeight();
        int alto = visible > 0 ? Math.max(54, Math.min(90, (visible - 8) / 2)) : 90;
        ((PanelRapidos) accesosRapidos).altoTarjeta = alto;
        int lado = Math.max(18, alto - 36);
        for (Component componente : accesosRapidos.getComponents())
            if (componente instanceof JButton boton) {
                Producto p = (Producto) boton.getClientProperty("producto");
                boton.setIcon(ImagenProducto.icono(p, contexto.imagenProducto(p.codigo()),
                        contexto.seccionCategoria(p.categoria()), lado));
            }
        rapidosScroll.getVerticalScrollBar().setUnitIncrement(alto + 8);
        rapidosScroll.getVerticalScrollBar().setBlockIncrement(2 * (alto + 8));
        accesosRapidos.revalidate();
    }

    private void buscarMercancia() {
        PanelAccesosRapidos.abrir(this, contexto, this::agregarProducto);
        enfocarCodigo();
    }

    /**
     * Resuelve atajos solo en la ventana activa y consume los eventos de la misma pulsación.
     */
    private boolean tecla(KeyEvent e) {
        Window activa = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        boolean enCobro = dialogoCobro != null && activa == dialogoCobro;
        boolean enTicket = dialogoTicket != null && activa == dialogoTicket;
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_RELEASED) {
            enterPresionado = false;
            if (enCobro || enTicket) {
                e.consume();
                return true;
            }
        }
        if ((enCobro || enTicket) && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyChar() == '\n')) {
            return procesarEnter(e, enCobro ? cobro::confirmarConEnter : dialogoTicket::dispose);
        }
        // También consume el carácter que originó la apertura de un diálogo.
        if (e.getID() == KeyEvent.KEY_TYPED && consumirCaracter) {
            consumirCaracter = false;
            e.consume();
            return true;
        }
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            consumirCaracter = false;
            return false;
        }
        if (!isShowing() || KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != SwingUtilities
                .getWindowAncestor(this))
            return false;
        if (e.getID() != KeyEvent.KEY_PRESSED)
            return false;
        consumirCaracter = false;
        Runnable accion = accionAtajo(e);
        if (accion == null)
            return false;
        consumirCaracter = true;
        e.consume();
        // Abre los diálogos después de terminar de procesar la pulsación.
        SwingUtilities.invokeLater(accion);
        return true;
    }

    private boolean procesarEnter(KeyEvent e, Runnable accion) {
        if (e.getID() == KeyEvent.KEY_PRESSED && !enterPresionado) {
            enterPresionado = true;
            SwingUtilities.invokeLater(accion);
        }
        // La misma pulsación que cobró no puede cerrar el ticket por repetición.
        e.consume();
        return true;
    }

    private Runnable accionAtajo(KeyEvent e) {
        if (!atajosActivos || controlador.tieneTicketPendiente() || (e.getModifiersEx()
                & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0)
            return null;
        Component foco = e.getComponent();
        // Mientras se escribe, B, + y − conservan su significado como texto.
        if (foco instanceof javax.swing.text.JTextComponent texto && (foco != codigo || !texto.getText().isBlank()))
            return null;
        if (e.getKeyCode() == KeyEvent.VK_B)
            return this::buscarMercancia;
        if ((e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE))
            return this::eliminarSeleccionado;
        if (e.getKeyCode() == KeyEvent.VK_ADD || e.getKeyChar() == '+')
            return () -> ajustar(1);
        if (e.getKeyCode() == KeyEvent.VK_SUBTRACT || e.getKeyChar() == '-')
            return () -> ajustar(-1);
        return null;
    }

    private void agregarCodigo() {
        String texto = codigo.getText().strip();
        if (texto.isEmpty())
            return;
        try {
            String[] partes = texto.split("\\*", -1);
            if (partes.length > 2)
                throw new IllegalArgumentException("Usa CÓDIGO o CÓDIGO*CANTIDAD.");
            Producto p = contexto.producto(partes[0]);
            if (p == null) {
                mostrarAviso("Producto no encontrado");
                codigo.selectAll();
                return;
            }
            BigDecimal q = partes.length == 2 ? UI.numero(partes[1]) : pedirCantidad(p);
            if (q != null)
                agregar(p, q);
        } catch (Exception e) {
            UI.error(this, e);
        }
    }

    private BigDecimal pedirCantidad(Producto p) {
        if (p.unidad() == UnidadVenta.PIEZA)
            return BigDecimal.ONE;
        return PanelCantidad.pedir(this, p, new BigDecimal("0.250"));
    }

    private void agregarProducto(Producto p) {
        try {
            BigDecimal q = pedirCantidad(p);
            if (q != null)
                agregar(p, q);
        } catch (Exception e) {
            UI.error(this, e);
        }
    }

    private void agregar(Producto p, BigDecimal q) {
        controlador.venta.agregar(p, q);
        codigo.setText("");
        refrescar();
        for (int i = 0; i < modelo.size(); i++)
            if (modelo.get(i).codigo().equals(p.codigo()))
                lista.setSelectedIndex(i);
        lista.ensureIndexIsVisible(lista.getSelectedIndex());
        codigo.requestFocusInWindow();
    }

    private void seleccionar() {
        DetalleVenta d = lista.getSelectedValue();
        editorCantidad.setVisible(d != null);
        if (d != null) {
            BigDecimal visible = d.unidad() == UnidadVenta.PIEZA ? d.cantidad() : d.cantidad().movePointRight(3);
            mostrandoCantidad = true;
            try {
                cantidad.setText(UI.cantidad(visible.setScale(2, RoundingMode.HALF_UP)));
                cantidadEditada = false;
            } finally {
                mostrandoCantidad = false;
            }
            unidadCantidad.setText(
                    d.unidad() == UnidadVenta.PIEZA ? "pzas" : d.unidad() == UnidadVenta.KILOGRAMO ? "g" : "ml");
            pasoCantidad.setText(d.unidad() == UnidadVenta.PIEZA ? "± 1 pieza"
                    : d.unidad() == UnidadVenta.KILOGRAMO ? "± 100 g" : "± 100 ml");
        }
    }

    private void ajustar(int paso) {
        DetalleVenta d = lista.getSelectedValue();
        if (d == null)
            return;
        BigDecimal incremento = d.unidad() == UnidadVenta.PIEZA ? BigDecimal.ONE : new BigDecimal("0.100");
        BigDecimal nueva = d.cantidad().add(incremento.multiply(BigDecimal.valueOf(paso))).max(BigDecimal.ZERO);
        cantidad.setText(UI.cantidad(d.unidad() == UnidadVenta.PIEZA ? nueva : nueva.movePointRight(3)));
        cambiarCantidad();
    }

    private BigDecimal cantidadCapturada(DetalleVenta detalle) {
        // Enter y Pagar conservan la precisión original si solo se mostró el redondeo.
        if (!cantidadEditada)
            return detalle.cantidad();
        BigDecimal valor = UI.numero(cantidad.getText());
        return detalle.unidad() == UnidadVenta.PIEZA ? valor : valor.movePointLeft(3);
    }

    private void cambiarCantidad() {
        DetalleVenta d = lista.getSelectedValue();
        if (d == null)
            return;
        try {
            BigDecimal q = cantidadCapturada(d);
            if (q.signum() < 0)
                throw new IllegalArgumentException("La cantidad no puede ser negativa.");
            if (q.signum() == 0) {
                eliminarSeleccionado();
                return;
            }
            controlador.venta.cambiarCantidad(d.codigo(), q);
            refrescar();
            enfocarCodigo();
        } catch (Exception e) {
            UI.error(this, e);
        }
    }

    private void eliminarSeleccionado() {
        DetalleVenta d = lista.getSelectedValue();
        if (d == null || controlador.tieneTicketPendiente())
            return;
        int indice = lista.getSelectedIndex();
        retirado = d;
        deshacer.setVisible(true);
        controlador.venta.cambiarCantidad(d.codigo(), BigDecimal.ZERO);
        refrescar();
        if (!modelo.isEmpty())
            lista.setSelectedIndex(Math.min(indice, modelo.size() - 1));
        ultimaVenta.setText("Quitaste " + d.nombre());
        enfocarCodigo();
    }

    private void deshacerEliminacion() {
        if (retirado == null || controlador.tieneTicketPendiente())
            return;
        DetalleVenta d = retirado;
        controlador.venta.agregar(new Producto(d.codigo(), d.nombre(), d.precio(), d.categoria(), d.unidad(), false),
                d.cantidad());
        retirado = null;
        deshacer.setVisible(false);
        refrescar();
        for (int i = 0; i < modelo.size(); i++)
            if (modelo.get(i).codigo().equals(d.codigo()))
                lista.setSelectedIndex(i);
        ultimaVenta.setText("Producto recuperado: " + d.nombre());
        enfocarCodigo();
    }

    private void editarMedida() {
        DetalleVenta d = lista.getSelectedValue();
        if (d == null || controlador.tieneTicketPendiente())
            return;
        Producto p = new Producto(d.codigo(), d.nombre(), d.precio(), d.categoria(), d.unidad(), false);
        BigDecimal nueva = PanelCantidad.pedir(this, p, d.cantidad(), "Aplicar cantidad");
        if (nueva != null) {
            controlador.venta.cambiarCantidad(d.codigo(), nueva);
            refrescar();
        }
        enfocarCodigo();
    }

    // Capturar efectivo prepara el cobro; confirmar es el único paso que registra
    // la venta.
    private void borrarEfectivo() {
        controlador.cobro.limpiar();
        cobro.efectivo.setText("");
        actualizarCobro();
    }

    private void importeExacto() {
        controlador.cobro.limpiar();
        if (controlador.total().signum() > 0)
            controlador.cobro.sumar(controlador.total());
        cobro.efectivo.setText("");
        actualizarCobro();
    }

    private void recibir(BigDecimal valor) {
        controlador.cobro.limpiar();
        controlador.cobro.sumar(valor);
        cobro.efectivo.setText("");
        actualizarCobro();
    }

    private void actualizarCobro() {
        cobro.actualizar(controlador.cobro.recibido(), controlador.total());
    }

    private void abrirCobro() {
        if (controlador.venta.vacia() || dialogoCobro != null || controlador.tieneTicketPendiente())
            return;
        if (!codigo.getText().isBlank()) {
            mostrarAviso("Hay un código pendiente. Agrégalo con Enter o borra el campo antes de pagar.");
            enfocarCodigo();
            return;
        }
        // Aplica una cantidad escrita aunque el cajero pulse Pagar sin presionar Enter.
        DetalleVenta seleccionado = lista.getSelectedValue();
        if (seleccionado != null) {
            try {
                controlador.venta.cambiarCantidad(seleccionado.codigo(), cantidadCapturada(seleccionado));
                refrescar();
            } catch (Exception e) {
                UI.error(this, e);
                return;
            }
        }
        if (controlador.venta.vacia())
            return;
        controlador.cobro.limpiar();
        cobro.efectivo.setText("");
        actualizarCobro();
        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(this), "Pagar venta",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogoCobro = dialogo;
        JPanel pagina = UI.pagina("Pagar venta");
        JPanel centro = new JPanel(new BorderLayout(0, 16));
        PanelTotales total = new PanelTotales();
        total.actualizar(controlador.total());
        centro.add(total, BorderLayout.NORTH);
        centro.add(cobro);
        pagina.add(centro);
        JPanel pieCobro = new JPanel(new BorderLayout(10, 0));
        pieCobro.add(UI.boton("Volver a la venta", dialogo::dispose), BorderLayout.WEST);
        pieCobro.add(UI.discreto("Enter confirmar · Esc volver"), BorderLayout.EAST);
        pagina.add(pieCobro, BorderLayout.SOUTH);
        dialogo.setContentPane(pagina);
        configurarDialogo(dialogo, 550, 650);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                cobro.efectivo.requestFocusInWindow();
            }
        });
        try {
            dialogo.setVisible(true);
        } finally {
            dialogo.dispose();
            dialogoCobro = null;
            if (!controlador.tieneTicketPendiente())
                controlador.cobro.limpiar();
            refrescar();
            enfocarCodigo();
        }
        // Termina el diálogo de pago antes de abrir otro diálogo modal.
        if (controlador.tieneTicketPendiente()) {
            try {
                mostrarTicket(controlador.ticketPendiente());
            } catch (Exception e) {
                UI.error(this, e);
            }
        }
    }

    /**
     * Guarda una sola venta y cierra el cobro; abrirCobro muestra después el ticket.
     */
    private void confirmar() {
        if (confirmando || controlador.tieneTicketPendiente())
            return;
        confirmando = true;
        try {
            if (!cobro.efectivo.getText().isBlank()) {
                BigDecimal efectivo = UI.numero(cobro.efectivo.getText()).stripTrailingZeros();
                if (efectivo.signum() < 0 || efectivo.scale() > 2)
                    throw new IllegalArgumentException("Revisa el efectivo recibido.");
                controlador.cobro.limpiar();
                if (efectivo.signum() > 0)
                    controlador.cobro.sumar(efectivo);
                cobro.efectivo.setText("");
            }
            controlador.confirmar(false);
            retirado = null;
            deshacer.setVisible(false);
            if (dialogoCobro != null)
                dialogoCobro.dispose();
        } catch (Exception e) {
            UI.error(this, e);
        } finally {
            confirmando = false;
        }
    }

    /**
     * Usa el registro guardado para conservar precios, descuentos y cambio del momento del pago.
     */
    private void mostrarTicket(MovimientoInventario registro) {
        JDialog ticket = new JDialog(SwingUtilities.getWindowAncestor(this), "Ticket · " + registro.folio(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogoTicket = ticket;
        ticket.setContentPane(new PanelTicket(registro, ticket::dispose));
        configurarDialogo(ticket, 570, 650);
        ticket.getRootPane().registerKeyboardAction(e -> ticket.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
        try {
            ticket.setVisible(true);
        } finally {
            ticket.dispose();
            dialogoTicket = null;
            controlador.cerrarTicket();
            codigo.setText("");
            cobro.efectivo.setText("");
            ultimaVenta.setText("Última venta: " + registro.folio() + " · Cambio: " + UI.dinero(registro.cambio()));
            refrescar();
            enfocarCodigo();
        }
    }

    private void configurarDialogo(JDialog dialogo, int ancho, int alto) {
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Rectangle pantalla = getGraphicsConfiguration().getBounds();
        dialogo.setSize(Math.min(ancho, pantalla.width - 60), Math.min(alto, pantalla.height - 80));
        dialogo.setLocationRelativeTo(this);
    }

    private void refrescar() {
        DetalleVenta seleccion = lista.getSelectedValue();
        refrescando = true;
        List<DetalleVenta> detalles = controlador.detalles();
        modelo.clear();
        detalles.forEach(modelo::addElement);
        if (seleccion != null)
            for (int i = 0; i < modelo.size(); i++)
                if (modelo.get(i).codigo().equals(seleccion.codigo()))
                    lista.setSelectedIndex(i);
        refrescando = false;
        DetalleVenta actual = lista.getSelectedValue();
        editorCantidad.setVisible(actual != null);
        if (actual != null)
            seleccionar();
        contador.setText(detalles.size() + (detalles.size() == 1 ? " producto distinto" : " productos distintos"));
        tarjetasVenta.show(contenidoVenta, detalles.isEmpty() ? "vacio" : "productos");
        BigDecimal piezas = detalles.stream().filter(d -> d.unidad() == UnidadVenta.PIEZA).map(DetalleVenta::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long granel = detalles.stream().filter(d -> d.unidad() != UnidadVenta.PIEZA).count();
        resumenProductos.setText(UI.cantidad(piezas) + " piezas" + (granel > 0 ? " · " + granel + " a granel" : ""));
        totales.actualizar(controlador.total());
        actualizarCobro();
        pagar.setEnabled(!detalles.isEmpty() && !controlador.tieneTicketPendiente());
        pagar.setText(detalles.isEmpty() ? "Pagar" : "Pagar  " + UI.dinero(controlador.total()));
    }

    private void mostrarAviso(String texto) {
        mensaje.setText(texto);
        aviso.setVisible(true);
        temporizador.restart();
        revalidate();
    }

    /**
     * Marca de agua del estado vacío; conserva los textos por encima del dibujo.
     */
    private static class FondoCarrito extends JPanel {
        FondoCarrito() {
            super(new GridBagLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D dibujo = (Graphics2D) g.create();
            dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double escala = Math.max(0, Math.min(320, Math.min(getWidth(), getHeight()) - 32)) / 320.0;
            dibujo.translate((getWidth() - 320 * escala) / 2, (getHeight() - 280 * escala) / 2);
            dibujo.scale(escala, escala);
            dibujo.setColor(new Color(235, 244, 238));
            dibujo.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            dibujo.drawPolyline(new int[] { 24, 58, 94, 260 }, new int[] { 30, 30, 214, 214 }, 4);
            dibujo.drawPolyline(new int[] { 66, 292, 264, 86 }, new int[] { 70, 70, 174, 174 }, 4);
            dibujo.drawLine(123, 78, 135, 166);
            dibujo.drawLine(193, 78, 193, 166);
            dibujo.drawLine(78, 122, 278, 122);
            dibujo.drawOval(105, 242, 24, 24);
            dibujo.drawOval(234, 242, 24, 24);
            dibujo.dispose();
        }
    }

    /**
     * Organiza hasta ocho productos en dos columnas con desplazamiento vertical.
     */
    private static class PanelRapidos extends JPanel implements Scrollable {
        private int altoTarjeta = 90;

        PanelRapidos() {
            super(new GridLayout(0, 2, 8, 8));
        }

        @Override
        public Dimension getPreferredSize() {
            int filas = Math.max(2, (getComponentCount() + 1) / 2);
            return new Dimension(0, filas * altoTarjeta + (filas - 1) * 8);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(292, 188);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return altoTarjeta + 8;
        }

        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return 2 * (altoTarjeta + 8);
        }
    }

    private static class IconoBasura implements Icon {
        public int getIconWidth() {
            return 22;
        }

        public int getIconHeight() {
            return 24;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D dibujo = (Graphics2D) g.create();
            dibujo.translate(x, y);
            dibujo.setColor(UI.ACENTO);
            dibujo.setStroke(new BasicStroke(1.8f));
            dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            dibujo.drawRoundRect(5, 7, 13, 15, 3, 3);
            dibujo.drawLine(3, 5, 20, 5);
            dibujo.drawRoundRect(8, 1, 7, 4, 2, 2);
            dibujo.drawLine(9, 10, 9, 18);
            dibujo.drawLine(14, 10, 14, 18);
            dibujo.dispose();
        }
    }

    private static class FilaProducto extends JPanel implements ListCellRenderer<DetalleVenta> {
        private final JLabel nombre = new JLabel(), detalle = UI.discreto(""), promocion = UI.discreto("");
        private final JLabel importe = new JLabel(), medida = new JLabel("", SwingConstants.CENTER),
                foto = new JLabel();
        private final ContextoAplicacion contexto;

        FilaProducto(ContextoAplicacion contexto) {
            super(new BorderLayout(12, 0));
            this.contexto = contexto;
            setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 8));
            medida.setPreferredSize(new Dimension(104, 44));
            medida.setOpaque(true);
            medida.setFont(medida.getFont().deriveFont(Font.BOLD, 14f));
            JPanel identificacion = new JPanel(new BorderLayout(6, 0));
            identificacion.setOpaque(false);
            identificacion.add(foto, BorderLayout.WEST);
            identificacion.add(medida);
            add(identificacion, BorderLayout.WEST);
            JPanel texto = new JPanel(new GridLayout(0, 1, 0, 2));
            texto.setOpaque(false);
            nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 16f));
            texto.add(nombre);
            texto.add(detalle);
            texto.add(promocion);
            add(texto);
            importe.setFont(importe.getFont().deriveFont(Font.BOLD, 23f));
            importe.setForeground(UI.VERDE);
            JPanel acciones = new JPanel(new BorderLayout(12, 0));
            acciones.setOpaque(false);
            acciones.add(importe);
            JLabel basura = new JLabel(new IconoBasura());
            basura.setPreferredSize(new Dimension(42, 40));
            acciones.add(basura, BorderLayout.EAST);
            add(acciones, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends DetalleVenta> lista, DetalleVenta d, int i,
                boolean seleccionado, boolean foco) {
            Producto p = contexto.producto(d.codigo());
            if (p == null)
                p = new Producto(d.codigo(), d.nombre(), d.precio(), d.categoria(), d.unidad(), false);
            foto.setIcon(ImagenProducto.icono(p, contexto.imagenProducto(d.codigo()),
                    contexto.seccionCategoria(d.categoria()), 44));
            nombre.setText(d.nombre());
            medida.setText(UI.medida(d.cantidad(), d.unidad()));
            medida.setBackground(seleccionado ? Color.WHITE : UI.SUAVE);
            medida.setForeground(UI.VERDE);
            detalle.setText(UI.dinero(d.precio()) + " / " + d.unidad().simbolo() + "  ·  " + d.codigo());
            promocion.setText(d.promocion().isBlank() ? " " : d.promocion());
            promocion.setForeground(UI.ACENTO);
            importe.setText(UI.dinero(d.importe()));
            setBackground(seleccionado ? UI.SUAVE : Color.WHITE);
            return this;
        }
    }
}
