package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.DetalleVenta;
import com.sicov.modelo.MovimientoInventario;
import com.sicov.modelo.Producto;
import com.sicov.modelo.UnidadVenta;
import com.sicov.vista.ImagenProducto;
import com.sicov.vista.UI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Mantiene el catálogo de productos y registra su recepción como una operación aparte.
 */
public class PanelCompras extends JPanel {
    private final ContextoAplicacion contexto;

    // El formulario edita datos del catálogo; guardarlo no registra mercancía
    // recibida.
    private final JTextField nombre = new JTextField(), precio = new JTextField(), codigo = new JTextField();
    private final JComboBox<String> categoria = new JComboBox<>();
    private final JComboBox<UnidadVenta> unidad = new JComboBox<>(UnidadVenta.values());
    private final JCheckBox sinCodigo = new JCheckBox("Sin código físico");
    private boolean editando;
    private Path imagenElegida;
    private boolean quitarImagen;
    private final JLabel vistaImagen = new JLabel(), estadoImagen = UI.discreto("Icono de categoría");
    private final JLabel precioEtiqueta = new JLabel("Precio de venta por pieza");
    private final JLabel ayudaUnidad = UI.discreto("Pieza: huevos sueltos, latas y paquetes.");

    // El borrador de recepción se conserva por código aunque cambie el filtro de la
    // tabla.
    private final JTextField buscar = new JTextField();
    private final Map<String, BigDecimal> cantidades = new LinkedHashMap<>();
    private final Set<String> confirmados = new LinkedHashSet<>();
    private List<Producto> productos = List.of();
    private final ModeloRecepcion modelo = new ModeloRecepcion();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloRecepcion> filtro = new TableRowSorter<>(modelo);
    private final JLabel estado = new JLabel("Selecciona las cantidades que realmente recibiste.");

    public PanelCompras(ContextoAplicacion contexto) {
        super(new BorderLayout());
        this.contexto = contexto;
        JPanel pagina = UI.pagina("Compras · recepción de productos");
        JPanel formulario = new JPanel();
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setBorder(BorderFactory.createTitledBorder("Registrar producto"));
        categoria.setEditable(true);
        formulario.add(UI.campo("Nombre", nombre));
        formulario.add(Box.createVerticalStrut(8));
        JPanel campoPrecio = new JPanel(new BorderLayout(5, 5));
        campoPrecio.setOpaque(false);
        campoPrecio.add(precioEtiqueta, BorderLayout.NORTH);
        campoPrecio.add(precio);
        campoPrecio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        formulario.add(campoPrecio);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Código · manual o escáner", codigo));
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Categoría", categoria));
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.campo("Se vende por", unidad));
        formulario.add(ayudaUnidad);
        formulario.add(Box.createVerticalStrut(6));
        formulario.add(sinCodigo);
        unidad.addActionListener(e -> {
            UnidadVenta seleccion = (UnidadVenta) unidad.getSelectedItem();
            precioEtiqueta.setText("Precio de venta por " + (seleccion == UnidadVenta.KILOGRAMO ? "kilogramo"
                    : seleccion == UnidadVenta.LITRO ? "litro" : "pieza"));
            ayudaUnidad.setText(seleccion == UnidadVenta.KILOGRAMO
                    ? "<html>Kilogramo: jamón, queso o huevo por peso.<br>En Ventas podrás capturar gramos.</html>"
                    : seleccion == UnidadVenta.LITRO ? "Litro: productos servidos a granel."
                            : "Pieza: huevos sueltos, latas y paquetes.");
            if (!editando && seleccion != UnidadVenta.PIEZA)
                sinCodigo.setSelected(true);
        });
        JPanel imagen = new JPanel(new BorderLayout(10, 5));
        imagen.setOpaque(false);
        imagen.add(new JLabel("Imagen del producto (opcional)"), BorderLayout.NORTH);
        vistaImagen.setPreferredSize(new Dimension(72, 72));
        imagen.add(vistaImagen, BorderLayout.WEST);
        JPanel accionesImagen = new JPanel(new GridLayout(2, 1, 0, 5));
        accionesImagen.setOpaque(false);
        accionesImagen.add(UI.boton("Elegir imagen…", this::elegirImagen));
        accionesImagen.add(UI.boton("Usar icono de categoría", () -> {
            imagenElegida = null;
            quitarImagen = true;
            actualizarImagen();
        }));
        imagen.add(accionesImagen);
        imagen.add(estadoImagen, BorderLayout.SOUTH);
        imagen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        formulario.add(Box.createVerticalStrut(10));
        formulario.add(imagen);
        formulario.add(Box.createVerticalStrut(10));
        formulario.add(UI.boton("Guardar producto", this::guardar));
        formulario.add(Box.createVerticalStrut(6));
        formulario.add(UI.boton("Limpiar / nuevo", this::limpiar));
        formulario.add(Box.createVerticalGlue());
        sinCodigo.setToolTipText("Disponible en el botón Buscar mercancía de Ventas.");
        for (Component componente : formulario.getComponents())
            if (componente instanceof JComponent jc)
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel catalogo = new JPanel(new BorderLayout(8, 8));
        catalogo.add(UI.campo("Buscar por nombre o código · el escáner también funciona aquí", buscar),
                BorderLayout.NORTH);
        tabla.setRowSorter(filtro);
        tabla.setRowHeight(48);
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion, boolean foco,
                    int fila, int columna) {
                JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila,
                        columna);
                Producto p = productos.get(tabla.convertRowIndexToModel(fila));
                celda.setIcon(ImagenProducto.icono(p, contexto.imagenProducto(p.codigo()),
                        contexto.seccionCategoria(p.categoria()), 36));
                celda.setIconTextGap(8);
                return celda;
            }
        });
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.putClientProperty("terminateEditOnFocusLost", true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(330);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(3).setMaxWidth(65);
        JTextField editorRecepcion = new JTextField();
        tabla.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(editorRecepcion));
        editorRecepcion.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            buscar.requestFocusInWindow();
            buscar.selectAll();
        }));
        catalogo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(UI.boton("Editar seleccionado", this::editar));
        acciones.add(UI.boton("Registrar recepción", this::recibir));
        catalogo.add(acciones, BorderLayout.SOUTH);
        JScrollPane desplazarFormulario = new JScrollPane(formulario);
        desplazarFormulario.setBorder(BorderFactory.createEmptyBorder());
        desplazarFormulario.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazarFormulario.getVerticalScrollBar().setUnitIncrement(20);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, desplazarFormulario, catalogo);
        division.setResizeWeight(0.28);
        division.setDividerLocation(310);
        desplazarFormulario.setMinimumSize(new Dimension(285, 300));
        catalogo.setMinimumSize(new Dimension(400, 300));
        pagina.add(division, BorderLayout.CENTER);
        pagina.add(estado, BorderLayout.SOUTH);
        add(pagina);
        UI.enter(nombre, () -> precio.requestFocusInWindow());
        UI.enter(precio, () -> {
            try {
                UI.numero(precio.getText());
                codigo.requestFocusInWindow();
            } catch (Exception e) {
                UI.error(this, e);
            }
        });
        UI.enter(codigo, this::guardar);
        UI.escuchar(buscar, this::filtrar);
        UI.enter(buscar, this::buscarCodigo);
        categoria.addActionListener(e -> actualizarImagen());
        UI.escuchar(codigo, this::actualizarImagen);
        contexto.escuchar(this::refrescar);
        refrescar();
        actualizarImagen();
    }

    private void elegirImagen() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elegir imagen del producto");
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes PNG o JPG", "png", "jpg", "jpeg"));
        selector.setAcceptAllFileFilterUsed(false);
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        Path seleccion = selector.getSelectedFile().toPath();
        if (!ImagenProducto.esImagenValida(seleccion)) {
            UI.error(this, new IllegalArgumentException(
                    "Selecciona una imagen PNG o JPG válida, de hasta 20 MB y 25 megapíxeles."));
            return;
        }
        imagenElegida = seleccion;
        quitarImagen = false;
        actualizarImagen();
    }

    private void actualizarImagen() {
        Object cat = categoria.isEditable() ? categoria.getEditor().getItem() : categoria.getSelectedItem();
        String nombreCategoria = cat == null || cat.toString().isBlank() ? "Sin categoría" : cat.toString();
        Producto vista = new Producto("vista", "Vista previa", BigDecimal.ZERO, nombreCategoria, UnidadVenta.PIEZA,
                false);
        Path foto = imagenElegida != null ? imagenElegida
                : quitarImagen ? null : contexto.imagenProducto(codigo.getText());
        vistaImagen.setIcon(ImagenProducto.icono(vista, foto, contexto.seccionCategoria(nombreCategoria), 72));
        estadoImagen.setText(imagenElegida != null ? "Imagen elegida · se aplica al guardar"
                : foto != null ? "Imagen del producto" : "Icono de categoría");
    }

    private void refrescar() {
        productos = contexto.productos();
        modelo.fireTableDataChanged();
        Object seleccion = categoria.getSelectedItem();
        categoria.removeAllItems();
        contexto.categorias().forEach(categoria::addItem);
        if (seleccion != null)
            categoria.setSelectedItem(seleccion);
        filtrar();
    }

    private void filtrar() {
        String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
        filtro.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ModeloRecepcion, ? extends Integer> e) {
                Producto p = productos.get(e.getIdentifier());
                return (p.nombre() + " " + p.codigo() + " " + p.categoria()).toLowerCase(Locale.ROOT).contains(texto);
            }
        });
    }

    private void buscarCodigo() {
        Producto p = contexto.producto(buscar.getText());
        if (p == null && tabla.getRowCount() == 1)
            p = productos.get(tabla.convertRowIndexToModel(0));
        if (p == null)
            return;
        int fila = tabla.convertRowIndexToView(productos.indexOf(p));
        if (fila >= 0) {
            tabla.setRowSelectionInterval(fila, fila);
            tabla.scrollRectToVisible(tabla.getCellRect(fila, 2, true));
            tabla.editCellAt(fila, 2);
            if (tabla.getEditorComponent() instanceof JTextField campo) {
                campo.requestFocusInWindow();
                campo.selectAll();
            }
        }
    }

    private void guardar() {
        try {
            Object cat = categoria.getEditor().getItem();
            Producto p = new Producto(codigo.getText(), nombre.getText(), UI.numero(precio.getText()),
                    cat == null ? "" : cat.toString(), (UnidadVenta) unidad.getSelectedItem(), sinCodigo.isSelected());
            contexto.guardarProducto(p, editando);
            // La imagen se guarda después del producto: si falla, se permite reintentar su
            // aplicación.
            try {
                if (imagenElegida != null)
                    contexto.guardarImagenProducto(p.codigo(), imagenElegida);
                else if (quitarImagen)
                    contexto.quitarImagenProducto(p.codigo());
            } catch (Exception imagenError) {
                editando = true;
                codigo.setEditable(false);
                estado.setText("Producto guardado; falta aplicar su imagen. Reintenta Guardar producto.");
                UI.error(this, imagenError);
                return;
            }
            estado.setText("Producto guardado: " + p.nombre() + ". Indica la cantidad recibida en el catálogo.");
            limpiar();
        } catch (Exception e) {
            UI.error(this, e);
        }
    }

    private void limpiar() {
        imagenElegida = null;
        quitarImagen = false;
        nombre.setText("");
        precio.setText("");
        codigo.setText("");
        codigo.setEditable(true);
        unidad.setSelectedItem(UnidadVenta.PIEZA);
        sinCodigo.setSelected(false);
        editando = false;
        actualizarImagen();
        nombre.requestFocusInWindow();
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0)
            return;
        Producto p = productos.get(tabla.convertRowIndexToModel(fila));
        imagenElegida = null;
        quitarImagen = false;
        nombre.setText(p.nombre());
        precio.setText(p.precio().toPlainString());
        codigo.setText(p.codigo());
        categoria.setSelectedItem(p.categoria());
        unidad.setSelectedItem(p.unidad());
        sinCodigo.setSelected(p.sinCodigoFisico());
        editando = true;
        codigo.setEditable(false);
        actualizarImagen();
        nombre.requestFocusInWindow();
    }

    private void recibir() {
        try {
            // Incluye la última celda editada antes de construir el movimiento definitivo.
            if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing())
                return;
            List<DetalleVenta> detalles = new ArrayList<>();
            for (Producto p : productos)
                if (confirmados.contains(p.codigo()))
                    detalles.add(DetalleVenta.de(p, cantidades.get(p.codigo())));
            var registro = contexto.registrar(MovimientoInventario.Tipo.ENTRADA, detalles, BigDecimal.ZERO,
                    "Recepción de productos");
            for (String c : confirmados)
                cantidades.remove(c);
            confirmados.clear();
            modelo.fireTableDataChanged();
            estado.setText("Recepción guardada · Folio " + registro.folio() + " · " + detalles.size() + " productos.");
        } catch (Exception e) {
            UI.error(this, e);
        }
    }

    private class ModeloRecepcion extends AbstractTableModel {
        private final String[] nombres = { "PRODUCTO — CÓDIGO", "Categoría", "Cantidad a recibir", "Recibir" };

        public int getRowCount() {
            return productos.size();
        }

        public int getColumnCount() {
            return nombres.length;
        }

        public String getColumnName(int c) {
            return nombres[c];
        }

        public Class<?> getColumnClass(int c) {
            return c == 3 ? Boolean.class : String.class;
        }

        public boolean isCellEditable(int r, int c) {
            return c >= 2;
        }

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
                    if (valor.toString().isBlank()) {
                        cantidades.remove(p.codigo());
                        confirmados.remove(p.codigo());
                    } else {
                        BigDecimal q = UI.numero(valor.toString());
                        p.validarCantidad(q);
                        cantidades.put(p.codigo(), q);
                        confirmados.add(p.codigo());
                    }
                } else if (Boolean.TRUE.equals(valor)) {
                    if (!cantidades.containsKey(p.codigo()))
                        throw new IllegalArgumentException("Primero indica la cantidad a recibir.");
                    confirmados.add(p.codigo());
                } else
                    confirmados.remove(p.codigo());
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> UI.error(PanelCompras.this, e));
            }
            fireTableRowsUpdated(r, r);
        }
    }
}
