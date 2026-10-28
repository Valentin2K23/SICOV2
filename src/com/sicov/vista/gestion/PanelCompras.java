package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class PanelCompras extends JPanel {
    private final ContextoAplicacion contexto;
    private final JTextField nombre = new JTextField(), precio = new JTextField(), codigo = new JTextField();
    private final JComboBox<String> categoria = new JComboBox<>();
    private final JComboBox<UnidadVenta> unidad = new JComboBox<>(UnidadVenta.values());
    private final JCheckBox sinCodigo = new JCheckBox("Sin código físico");
    private final JTextField buscar = new JTextField();
    private final Map<String, BigDecimal> cantidades = new LinkedHashMap<>();
    private final Set<String> confirmados = new LinkedHashSet<>();
    private List<Producto> productos = List.of();
    private final ModeloRecepcion modelo = new ModeloRecepcion();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloRecepcion> filtro = new TableRowSorter<>(modelo);
    private final JLabel estado = new JLabel("Selecciona las cantidades que realmente recibiste.");
    private boolean editando;

    public PanelCompras(ContextoAplicacion contexto) {
        super(new BorderLayout()); this.contexto = contexto;
        JPanel pagina = UI.pagina("Compras · recepción de productos");
        JPanel formulario = new JPanel(); formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setBorder(BorderFactory.createTitledBorder("Registrar producto"));
        categoria.setEditable(true);
        formulario.add(UI.campo("Nombre", nombre)); formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Precio de venta por unidad", precio)); formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Código · manual o escáner", codigo)); formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Categoría", categoria)); formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Unidad de venta", unidad)); formulario.add(sinCodigo);
        formulario.add(UI.boton("Guardar producto", this::guardar));
        formulario.add(Box.createVerticalStrut(6)); formulario.add(UI.boton("Limpiar / nuevo", this::limpiar));
        formulario.add(Box.createVerticalGlue());
        sinCodigo.setToolTipText("Disponible en el botón Buscar mercancía de Ventas.");
        for (Component componente : formulario.getComponents())
            if (componente instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel catalogo = new JPanel(new BorderLayout(8, 8));
        catalogo.add(UI.campo("Buscar por nombre o código · el escáner también funciona aquí", buscar), BorderLayout.NORTH);
        tabla.setRowSorter(filtro); tabla.setRowHeight(38);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.putClientProperty("terminateEditOnFocusLost", true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(330);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(3).setMaxWidth(65);
        JTextField editorRecepcion = new JTextField();
        tabla.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(editorRecepcion));
        editorRecepcion.addActionListener(e -> SwingUtilities.invokeLater(() -> { buscar.requestFocusInWindow(); buscar.selectAll(); }));
        catalogo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(UI.boton("Editar seleccionado", this::editar));
        acciones.add(UI.boton("Registrar recepción", this::recibir)); catalogo.add(acciones, BorderLayout.SOUTH);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formulario, catalogo);
        division.setResizeWeight(0.28); division.setDividerLocation(310);
        formulario.setMinimumSize(new Dimension(260, 300)); catalogo.setMinimumSize(new Dimension(400, 300));
        pagina.add(division, BorderLayout.CENTER); pagina.add(estado, BorderLayout.SOUTH); add(pagina);
        UI.enter(nombre, () -> precio.requestFocusInWindow());
        UI.enter(precio, () -> { try { UI.numero(precio.getText()); codigo.requestFocusInWindow(); } catch (Exception e) { UI.error(this, e); } });
        UI.enter(codigo, this::guardar);
        UI.escuchar(buscar, this::filtrar);
        UI.enter(buscar, this::buscarCodigo);
        contexto.escuchar(this::refrescar); refrescar();
    }
    private void refrescar() {
        productos = contexto.productos(); modelo.fireTableDataChanged();
        Object seleccion = categoria.getSelectedItem();
        categoria.removeAllItems(); contexto.categorias().forEach(categoria::addItem);
        if (seleccion != null) categoria.setSelectedItem(seleccion);
        filtrar();
    }
    private void filtrar() {
        String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
        filtro.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends ModeloRecepcion, ? extends Integer> e) {
                Producto p = productos.get(e.getIdentifier());
                return (p.nombre() + " " + p.codigo() + " " + p.categoria()).toLowerCase(Locale.ROOT).contains(texto);
            }
        });
    }
    private void buscarCodigo() {
        Producto p = contexto.producto(buscar.getText());
        if (p == null && tabla.getRowCount() == 1) p = productos.get(tabla.convertRowIndexToModel(0));
        if (p == null) return;
        int fila = tabla.convertRowIndexToView(productos.indexOf(p));
        if (fila >= 0) {
            tabla.setRowSelectionInterval(fila, fila);
            tabla.scrollRectToVisible(tabla.getCellRect(fila, 2, true));
            tabla.editCellAt(fila, 2);
            if (tabla.getEditorComponent() instanceof JTextField campo) { campo.requestFocusInWindow(); campo.selectAll(); }
        }
    }
    private void guardar() {
        try {
            Object cat = categoria.getEditor().getItem();
            Producto p = new Producto(codigo.getText(), nombre.getText(), UI.numero(precio.getText()),
                    cat == null ? "" : cat.toString(), (UnidadVenta) unidad.getSelectedItem(), sinCodigo.isSelected());
            contexto.guardarProducto(p, editando);
            estado.setText("Producto guardado: " + p.nombre() + ". Indica la cantidad recibida en el catálogo.");
            limpiar();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void limpiar() {
        nombre.setText(""); precio.setText(""); codigo.setText(""); codigo.setEditable(true);
        unidad.setSelectedItem(UnidadVenta.PIEZA); sinCodigo.setSelected(false); editando = false;
        nombre.requestFocusInWindow();
    }
    private void editar() {
        int fila = tabla.getSelectedRow(); if (fila < 0) return;
        Producto p = productos.get(tabla.convertRowIndexToModel(fila));
        nombre.setText(p.nombre()); precio.setText(p.precio().toPlainString()); codigo.setText(p.codigo());
        categoria.setSelectedItem(p.categoria()); unidad.setSelectedItem(p.unidad()); sinCodigo.setSelected(p.sinCodigoFisico());
        editando = true; codigo.setEditable(false); nombre.requestFocusInWindow();
    }
    private void recibir() {
        try {
            if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing()) return;
            List<DetalleVenta> detalles = new ArrayList<>();
            for (Producto p : productos) if (confirmados.contains(p.codigo()))
                detalles.add(DetalleVenta.de(p, cantidades.get(p.codigo())));
            var registro = contexto.registrar(MovimientoInventario.Tipo.ENTRADA, detalles, BigDecimal.ZERO, "Recepción de productos");
            for (String c : confirmados) cantidades.remove(c); confirmados.clear(); modelo.fireTableDataChanged();
            estado.setText("Recepción guardada · Folio " + registro.folio() + " · " + detalles.size() + " productos.");
        } catch (Exception e) { UI.error(this, e); }
    }
    private class ModeloRecepcion extends AbstractTableModel {
        private final String[] nombres = {"PRODUCTO — CÓDIGO", "Categoría", "Cantidad a recibir", "Recibir"};
        public int getRowCount() { return productos.size(); }
        public int getColumnCount() { return nombres.length; }
        public String getColumnName(int c) { return nombres[c]; }
        public Class<?> getColumnClass(int c) { return c == 3 ? Boolean.class : String.class; }
        public boolean isCellEditable(int r, int c) { return c >= 2; }
        public Object getValueAt(int r, int c) {
            Producto p = productos.get(r);
            return switch (c) {
                case 0 -> p.nombre() + " — " + p.codigo();
                case 1 -> p.categoria();
                case 2 -> cantidades.containsKey(p.codigo()) ? UI.cantidad(cantidades.get(p.codigo())) : "";
                default -> confirmados.contains(p.codigo());
            };
        }
        public void setValueAt(Object valor, int r, int c) {
            Producto p = productos.get(r);
            try {
                if (c == 2) {
                    if (valor.toString().isBlank()) { cantidades.remove(p.codigo()); confirmados.remove(p.codigo()); }
                    else {
                        BigDecimal q = UI.numero(valor.toString()); p.validarCantidad(q);
                        cantidades.put(p.codigo(), q); confirmados.add(p.codigo());
                    }
                } else if (Boolean.TRUE.equals(valor)) {
                    if (!cantidades.containsKey(p.codigo())) throw new IllegalArgumentException("Primero indica la cantidad a recibir.");
                    confirmados.add(p.codigo());
                } else confirmados.remove(p.codigo());
            } catch (Exception e) { SwingUtilities.invokeLater(() -> UI.error(PanelCompras.this, e)); }
            fireTableRowsUpdated(r, r);
        }
    }
}
