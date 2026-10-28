package com.sicov.vista.ventas;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.vista.UI;
import com.sicov.vista.promociones.PanelPromociones;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;

public class PanelVentas extends JPanel {
    private final ContextoAplicacion contexto;
    private final VentaControlador controlador;
    private final JTextField codigo = new JTextField(), cantidad = new JTextField(7);
    private final DefaultListModel<DetalleVenta> modelo = new DefaultListModel<>();
    private final JList<DetalleVenta> lista = new JList<>(modelo);
    private final PanelTotales totales = new PanelTotales();
    private final PanelCobro cobro;
    private final JPanel editorCantidad = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JLabel ultimaVenta = new JLabel("Escanea o escribe un código. Doble Enter sin efectivo: pago exacto.");
    private final JPanel aviso = new JPanel(new BorderLayout());
    private final JLabel mensaje = new JLabel();
    private final Timer temporizador;
    private boolean refrescando, enterPresionado;
    private long primerEnter;
    private final KeyEventDispatcher atajo = this::tecla;

    public PanelVentas(ContextoAplicacion contexto, Runnable gestion) {
        super(new BorderLayout(12, 10)); this.contexto = contexto; controlador = new VentaControlador(contexto);
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JPanel superior = new JPanel(new BorderLayout(12, 7));
        JPanel entrada = new JPanel(new BorderLayout(8, 0)); JLabel etiqueta = new JLabel("C:");
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 28f)); codigo.setFont(codigo.getFont().deriveFont(26f));
        entrada.add(etiqueta, BorderLayout.WEST); entrada.add(codigo);
        superior.add(entrada);
        aviso.setBackground(new Color(255, 238, 198)); aviso.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        aviso.add(mensaje); aviso.add(UI.boton("X", () -> aviso.setVisible(false)), BorderLayout.EAST);
        aviso.setVisible(false); superior.add(aviso, BorderLayout.SOUTH); add(superior, BorderLayout.NORTH);
        temporizador = new Timer(3000, e -> aviso.setVisible(false)); temporizador.setRepeats(false);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); lista.setFixedCellHeight(84);
        lista.setCellRenderer(new FilaProducto()); add(new JScrollPane(lista), BorderLayout.CENTER);
        JPanel inferior = new JPanel(); inferior.setLayout(new BoxLayout(inferior, BoxLayout.Y_AXIS));
        editorCantidad.add(new JLabel("Cantidad seleccionada"));
        editorCantidad.add(UI.boton("−", () -> ajustar(-1))); editorCantidad.add(cantidad); editorCantidad.add(UI.boton("+", () -> ajustar(1)));
        editorCantidad.add(new JLabel("Enter aplica la cantidad. Cero retira el producto."));
        editorCantidad.setVisible(false);
        JPanel resumenTotal = new JPanel(new BorderLayout());
        resumenTotal.add(editorCantidad, BorderLayout.CENTER); resumenTotal.add(totales, BorderLayout.EAST);
        inferior.add(resumenTotal);
        cobro = new PanelCobro(this::recibir, () -> { controlador.cobro.limpiar(); refrescar(); codigo.requestFocusInWindow(); });
        inferior.add(cobro);
        JPanel accesos = new JPanel(new BorderLayout());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(UI.boton("Buscar mercancía", () -> PanelAccesosRapidos.abrir(this, contexto, this::agregarProducto)));
        botones.add(UI.boton("% Promociones", () -> PanelPromociones.consultar(this, contexto)));
        botones.add(UI.boton("Gestión", gestion)); accesos.add(botones, BorderLayout.EAST);
        ultimaVenta.setForeground(UI.ACENTO); accesos.add(ultimaVenta); inferior.add(accesos); add(inferior, BorderLayout.SOUTH);
        UI.enter(codigo, this::agregarCodigo); UI.enter(cantidad, this::cambiarCantidad);
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !refrescando) seleccionar(); });
        contexto.escuchar(this::refrescar); refrescar();
    }
    public boolean hayVentaPendiente() { return !controlador.venta.vacia(); }
    public void enfocarCodigo() { SwingUtilities.invokeLater(codigo::requestFocusInWindow); }
    @Override public void addNotify() { super.addNotify(); KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(atajo); }
    @Override public void removeNotify() { KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(atajo); super.removeNotify(); }
    private boolean tecla(KeyEvent e) {
        if (!isShowing() || KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != SwingUtilities.getWindowAncestor(this)) {
            primerEnter = 0; enterPresionado = false; return false;
        }
        if (e.getKeyCode() != KeyEvent.VK_ENTER) {
            if (e.getID() == KeyEvent.KEY_PRESSED) primerEnter = 0;
            return false;
        }
        if (e.getID() == KeyEvent.KEY_RELEASED) { enterPresionado = false; return false; }
        if (e.getID() != KeyEvent.KEY_PRESSED) return false;
        if (enterPresionado) return true;
        enterPresionado = true;
        // Nunca interpreta el Enter del escáner ni la edición de un campo como pago exacto.
        if (!codigo.getText().isBlank() || !cobro.efectivo.getText().isBlank() || cantidad.isFocusOwner()
                || controlador.venta.vacia() || controlador.cobro.recibido().signum() != 0) { primerEnter = 0; return false; }
        long ahora = System.nanoTime();
        if (primerEnter != 0 && ahora - primerEnter <= 700_000_000L) { primerEnter = 0; confirmar(true); }
        else primerEnter = ahora;
        return true;
    }
    private void agregarCodigo() {
        String texto = codigo.getText().strip(); if (texto.isEmpty()) return;
        primerEnter = 0;
        try {
            String[] partes = texto.split("\\*", -1);
            if (partes.length > 2) throw new IllegalArgumentException("Usa CÓDIGO o CÓDIGO*CANTIDAD.");
            Producto p = contexto.producto(partes[0]);
            if (p == null) { mostrarAviso("Producto no encontrado"); codigo.selectAll(); return; }
            BigDecimal q = partes.length == 2 ? UI.numero(partes[1]) : pedirCantidad(p);
            if (q != null) agregar(p, q);
        } catch (Exception e) { UI.error(this, e); }
    }
    private BigDecimal pedirCantidad(Producto p) {
        if (p.unidad() == UnidadVenta.PIEZA) return BigDecimal.ONE;
        String valor = JOptionPane.showInputDialog(this, "Cantidad de " + p.nombre() + " (" + p.unidad().simbolo() + ")", "1");
        return valor == null ? null : UI.numero(valor);
    }
    private void agregarProducto(Producto p) {
        try { BigDecimal q = pedirCantidad(p); if (q != null) agregar(p, q); } catch (Exception e) { UI.error(this, e); }
    }
    private void agregar(Producto p, BigDecimal q) {
        controlador.venta.agregar(p, q); codigo.setText(""); lista.clearSelection(); refrescar();
        lista.ensureIndexIsVisible(modelo.size() - 1); codigo.requestFocusInWindow();
    }
    private void seleccionar() {
        DetalleVenta d = lista.getSelectedValue(); editorCantidad.setVisible(d != null);
        if (d != null) { codigo.setText(d.codigo()); codigo.selectAll(); cantidad.setText(UI.cantidad(d.cantidad())); }
    }
    private void ajustar(int paso) {
        DetalleVenta d = lista.getSelectedValue(); if (d == null) return;
        cantidad.setText(UI.cantidad(d.cantidad().add(BigDecimal.valueOf(paso)).max(BigDecimal.ZERO))); cambiarCantidad();
    }
    private void cambiarCantidad() {
        DetalleVenta d = lista.getSelectedValue(); if (d == null) return;
        try {
            BigDecimal q = UI.numero(cantidad.getText());
            if (q.signum() < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa.");
            controlador.venta.cambiarCantidad(d.codigo(), q); codigo.setText(""); refrescar(); comprobarCobro();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void recibir(BigDecimal valor) {
        try {
            if (controlador.venta.vacia()) throw new IllegalArgumentException("Agrega productos antes de registrar efectivo.");
            controlador.cobro.sumar(valor); refrescar(); comprobarCobro();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void comprobarCobro() {
        if (!controlador.venta.vacia() && controlador.cobro.recibido().signum() > 0
                && controlador.cobro.cubre(controlador.total())) confirmar(false);
    }
    private void confirmar(boolean exacto) {
        try {
            var registro = controlador.confirmar(exacto); codigo.setText(""); cobro.efectivo.setText("");
            ultimaVenta.setText("Venta " + registro.folio() + " · Cambio: " + UI.dinero(registro.cambio()));
            refrescar(); codigo.requestFocusInWindow();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void refrescar() {
        DetalleVenta seleccion = lista.getSelectedValue(); refrescando = true;
        List<DetalleVenta> detalles = controlador.detalles(); modelo.clear(); detalles.forEach(modelo::addElement);
        if (seleccion != null) for (int i = 0; i < modelo.size(); i++) if (modelo.get(i).codigo().equals(seleccion.codigo())) lista.setSelectedIndex(i);
        refrescando = false;
        DetalleVenta actual = lista.getSelectedValue(); editorCantidad.setVisible(actual != null);
        if (actual != null) cantidad.setText(UI.cantidad(actual.cantidad()));
        totales.actualizar(controlador.total()); cobro.actualizar(controlador.cobro.recibido(), controlador.total());
    }
    private void mostrarAviso(String texto) {
        mensaje.setText(texto); aviso.setVisible(true); temporizador.restart(); revalidate();
    }
    private static class FilaProducto extends JPanel implements ListCellRenderer<DetalleVenta> {
        private final JLabel texto = new JLabel(), importe = new JLabel();
        FilaProducto() {
            super(new BorderLayout(12, 0)); setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            importe.setFont(importe.getFont().deriveFont(Font.BOLD, 24f)); add(texto); add(importe, BorderLayout.EAST);
        }
        @Override public Component getListCellRendererComponent(JList<? extends DetalleVenta> lista, DetalleVenta d, int i, boolean seleccionado, boolean foco) {
            String cantidad = d.cantidad().compareTo(BigDecimal.ONE) == 0 ? "" : " × " + UI.cantidad(d.cantidad());
            texto.setText("<html><b>" + UI.html(d.nombre()) + ": " + UI.dinero(d.precio()) + cantidad + "</b><br>"
                    + "<span style='color:#536578'>C: " + UI.html(d.codigo()) + " | "
                    + UI.html(d.promocion().isBlank() ? "Sin promoción" : d.promocion()) + "</span></html>");
            importe.setText(UI.dinero(d.importe())); setBackground(seleccionado ? new Color(215, 239, 240) : Color.WHITE);
            return this;
        }
    }
}
