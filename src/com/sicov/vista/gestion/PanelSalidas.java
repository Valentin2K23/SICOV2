package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioReportes;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelSalidas extends JPanel {
    private final ContextoAplicacion contexto;
    private final DefaultTableModel resumen = UI.tabla("Categoría", "Total vendido");
    private final DefaultTableModel modeloMerma = UI.tabla("Producto", "Código", "Cantidad", "Unidad");
    private final JComboBox<Producto> producto = new JComboBox<>();
    private final JTextField codigo = new JTextField(16), cantidad = new JTextField("1", 8), motivo = new JTextField(30);
    private final List<DetalleVenta> borrador = new ArrayList<>();
    private final JLabel estado = new JLabel("Registra productos retirados sin modificar existencias.");
    public PanelSalidas(ContextoAplicacion contexto) {
        super(new BorderLayout()); this.contexto = contexto;
        JPanel pagina = UI.pagina("Salidas");
        JTabbedPane pestanas = new JTabbedPane();
        JTable ventas = new JTable(resumen); ventas.setRowHeight(38);
        JPanel panelResumen = new JPanel(new BorderLayout(10, 10));
        panelResumen.add(new JLabel("Acumulado de todas las ventas vigentes. Las canceladas están excluidas."), BorderLayout.NORTH);
        panelResumen.add(new JScrollPane(ventas)); pestanas.addTab("Ventas por categoría", panelResumen);
        JPanel merma = new JPanel(new BorderLayout(10, 10));
        JPanel captura = new JPanel(new FlowLayout(FlowLayout.LEFT));
        producto.setPreferredSize(new Dimension(280, 36));
        captura.add(producto); captura.add(new JLabel("Código")); captura.add(codigo);
        captura.add(new JLabel("Cantidad")); captura.add(cantidad); captura.add(UI.boton("Agregar", this::agregar));
        merma.add(captura, BorderLayout.NORTH);
        JTable tabla = new JTable(modeloMerma); tabla.setRowHeight(36); merma.add(new JScrollPane(tabla));
        JPanel pie = new JPanel(); pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(new JLabel("Motivo")); acciones.add(motivo);
        acciones.add(UI.boton("Quitar seleccionado", () -> {
            int f = tabla.getSelectedRow(); if (f >= 0) { borrador.remove(f); refrescarBorrador(); }
        }));
        acciones.add(UI.boton("Registrar merma", this::guardar)); pie.add(acciones); pie.add(estado); merma.add(pie, BorderLayout.SOUTH);
        pestanas.addTab("Registrar merma", merma); pagina.add(pestanas); add(pagina);
        UI.enter(codigo, () -> {
            Producto p = contexto.producto(codigo.getText());
            if (p == null) UI.error(this, new Exception("Producto no encontrado"));
            else { producto.setSelectedItem(p); cantidad.requestFocusInWindow(); cantidad.selectAll(); }
        });
        UI.enter(cantidad, this::agregar); contexto.escuchar(this::refrescar); refrescar();
    }
    private void refrescar() {
        Object seleccionado = producto.getSelectedItem();
        producto.removeAllItems(); contexto.productos().forEach(producto::addItem);
        if (seleccionado != null) producto.setSelectedItem(seleccionado);
        resumen.setRowCount(0);
        new ServicioReportes().ventasPorCategoria(contexto.registros()).forEach((c, t) -> resumen.addRow(new Object[]{c, UI.dinero(t)}));
    }
    private void agregar() {
        try {
            Producto p = (Producto) producto.getSelectedItem();
            if (p == null) throw new IllegalArgumentException("Registra primero un producto en Compras.");
            borrador.add(DetalleVenta.de(p, UI.numero(cantidad.getText())));
            refrescarBorrador(); cantidad.setText("1"); codigo.setText(""); codigo.requestFocusInWindow();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void refrescarBorrador() {
        modeloMerma.setRowCount(0);
        for (var d : borrador) modeloMerma.addRow(new Object[]{d.nombre(), d.codigo(), UI.cantidad(d.cantidad()), d.unidad().simbolo()});
    }
    private void guardar() {
        try {
            var registro = contexto.registrar(MovimientoInventario.Tipo.MERMA, borrador, BigDecimal.ZERO, motivo.getText().strip());
            borrador.clear(); refrescarBorrador(); motivo.setText(""); estado.setText("Merma registrada · " + registro.folio());
        } catch (Exception e) { UI.error(this, e); }
    }
}
