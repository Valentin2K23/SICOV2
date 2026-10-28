package com.sicov.vista.promociones;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelPromociones extends JPanel {
    private final ContextoAplicacion contexto;
    private final JTextField nombre = new JTextField(), seccion = new JTextField(), precio = new JTextField();
    private final JComboBox<Producto> producto = new JComboBox<>();
    private final JTextField cantidad = new JTextField("1", 5);
    private final JCheckBox activa = new JCheckBox("Activa", true);
    private final LinkedHashMap<String, BigDecimal> componentes = new LinkedHashMap<>();
    private final DefaultTableModel partes = UI.tabla("Producto", "Cantidad");
    private final DefaultTableModel promociones = UI.tabla("Sección", "Promoción", "Precio paquete", "Estado");
    private List<Promocion> lista = List.of();
    private String id;
    public PanelPromociones(ContextoAplicacion contexto) {
        super(new BorderLayout()); this.contexto = contexto;
        JPanel pagina = UI.pagina("Descuentos y promociones");
        JPanel editor = new JPanel(); editor.setLayout(new BoxLayout(editor, BoxLayout.Y_AXIS));
        editor.add(UI.campo("Nombre · ejemplo: 2 leches por $25", nombre)); editor.add(UI.campo("Sección · ejemplo: Lácteos o Alcohol", seccion));
        editor.add(UI.campo("Precio del paquete", precio)); editor.add(activa);
        JPanel agregar = new JPanel(new FlowLayout(FlowLayout.LEFT)); producto.setPreferredSize(new Dimension(240, 34));
        agregar.add(producto); agregar.add(cantidad); agregar.add(UI.boton("Agregar", () -> {
            try {
                Producto p = (Producto) producto.getSelectedItem();
                if (p == null) throw new IllegalArgumentException("Registra primero los productos en Compras.");
                BigDecimal q = UI.numero(cantidad.getText()); p.validarCantidad(q);
                componentes.merge(p.codigo(), q, BigDecimal::add); refrescarPartes();
            } catch (Exception e) { UI.error(this, e); }
        })); editor.add(agregar);
        JTable tablaPartes = new JTable(partes); tablaPartes.setRowHeight(34);
        JScrollPane scroll = new JScrollPane(tablaPartes); scroll.setPreferredSize(new Dimension(400, 140)); editor.add(scroll);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(UI.boton("Quitar producto", () -> {
            int f = tablaPartes.getSelectedRow(); if (f >= 0) { componentes.remove(new ArrayList<>(componentes.keySet()).get(f)); refrescarPartes(); }
        }));
        acciones.add(UI.boton("Guardar", this::guardar)); acciones.add(UI.boton("Nueva", this::limpiar)); editor.add(acciones);
        JTable tabla = new JTable(promociones); tabla.setRowHeight(38);
        JPanel catalogo = new JPanel(new BorderLayout(8, 8)); catalogo.add(new JScrollPane(tabla));
        catalogo.add(UI.boton("Editar seleccionada", () -> {
            int f = tabla.getSelectedRow(); if (f < 0) return;
            Promocion p = lista.get(f); id = p.id(); nombre.setText(p.nombre()); seccion.setText(p.categoria());
            precio.setText(p.precio().toPlainString()); activa.setSelected(p.activa());
            componentes.clear(); componentes.putAll(p.componentes()); refrescarPartes();
        }), BorderLayout.SOUTH);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor, catalogo);
        division.setResizeWeight(0.5); division.setDividerLocation(490); pagina.add(division);
        pagina.add(new JLabel("Se aplican automáticamente. Cada unidad participa una vez; si coinciden reglas, se prioriza el mayor ahorro por paquete."), BorderLayout.SOUTH);
        add(pagina); contexto.escuchar(this::refrescar); refrescar();
    }
    private void refrescar() {
        Object seleccionado = producto.getSelectedItem(); producto.removeAllItems(); contexto.productos().forEach(producto::addItem);
        if (seleccionado != null) producto.setSelectedItem(seleccionado);
        lista = contexto.promociones(); promociones.setRowCount(0);
        for (Promocion p : lista) promociones.addRow(new Object[]{p.categoria(), p.nombre(), UI.dinero(p.precio()), p.activa() ? "Activa" : "Inactiva"});
    }
    private void refrescarPartes() {
        partes.setRowCount(0);
        componentes.forEach((c, q) -> {
            Producto p = contexto.producto(c);
            partes.addRow(new Object[]{p == null ? c : p.nombre(), UI.cantidad(q) + " " + (p == null ? "" : p.unidad().simbolo())});
        });
    }
    private void guardar() {
        try {
            contexto.guardarPromocion(new Promocion(id == null ? UUID.randomUUID().toString() : id, nombre.getText(),
                    seccion.getText(), UI.numero(precio.getText()), activa.isSelected(), componentes));
            limpiar();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void limpiar() {
        id = null; nombre.setText(""); seccion.setText(""); precio.setText(""); activa.setSelected(true);
        componentes.clear(); refrescarPartes();
    }
    public static void consultar(Component padre, ContextoAplicacion contexto) {
        JTabbedPane secciones = new JTabbedPane();
        Map<String, List<Promocion>> grupos = new LinkedHashMap<>();
        for (var p : contexto.promociones()) if (p.activa()) grupos.computeIfAbsent(p.categoria(), k -> new ArrayList<>()).add(p);
        grupos.forEach((categoria, lista) -> {
            DefaultListModel<String> modelo = new DefaultListModel<>();
            for (var p : lista) {
                String productos = p.componentes().entrySet().stream().map(c -> {
                    Producto producto = contexto.producto(c.getKey());
                    return UI.cantidad(c.getValue()) + " × " + (producto == null ? c.getKey() : producto.nombre());
                }).reduce((a, b) -> a + " + " + b).orElse("");
                modelo.addElement(p.nombre() + " — " + productos + " = " + UI.dinero(p.precio()));
            }
            JList<String> vista = new JList<>(modelo); vista.setFixedCellHeight(48);
            secciones.addTab(categoria, new JScrollPane(vista));
        });
        if (grupos.isEmpty()) JOptionPane.showMessageDialog(padre, "Aún no hay promociones activas. Configúralas desde Gestión.");
        else { secciones.setPreferredSize(new Dimension(760, 380)); JOptionPane.showMessageDialog(padre, secciones, "Promociones automáticas", JOptionPane.PLAIN_MESSAGE); }
    }
}
