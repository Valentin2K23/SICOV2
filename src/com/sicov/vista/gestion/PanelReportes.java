package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioReportes;
import com.sicov.vista.UI;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelReportes extends JPanel {
    private final ContextoAplicacion contexto;
    private final DefaultTableModel modelo = UI.tabla("Folio", "Día", "Hora", "Tipo", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final JTextField buscar = new JTextField(22);
    private final JTextArea detalle = new JTextArea();
    private List<MovimientoInventario> visibles = List.of();
    private String tipo = "AMBOS";
    public PanelReportes(ContextoAplicacion contexto) {
        super(new BorderLayout()); this.contexto = contexto;
        JPanel pagina = UI.pagina("Reportes · entradas y salidas");
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String t : new String[]{"ENTRADAS", "SALIDAS", "AMBOS"})
            barra.add(UI.boton(t, () -> { tipo = t; refrescar(); }));
        barra.add(new JLabel("Buscar folio")); barra.add(buscar);
        centro.add(barra, BorderLayout.NORTH);
        tabla.setRowHeight(34); tabla.getColumnModel().getColumn(0).setPreferredWidth(230);
        detalle.setEditable(false); detalle.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detalle.setMargin(new Insets(12, 12, 12, 12));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), new JScrollPane(detalle));
        division.setResizeWeight(0.5); division.setDividerLocation(230); centro.add(division);
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pie.add(new JLabel("Selecciona varios folios con Ctrl para ver un reporte combinado."));
        pie.add(UI.boton("Cancelar venta seleccionada", this::cancelar));
        centro.add(pie, BorderLayout.SOUTH); pagina.add(centro); add(pagina);
        tabla.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) mostrar(); });
        UI.escuchar(buscar, this::refrescar);
        contexto.escuchar(this::refrescar); refrescar();
    }
    private void refrescar() {
        String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
        visibles = contexto.registros().stream().filter(r -> r.folio().toLowerCase(Locale.ROOT).contains(texto))
                .filter(r -> tipo.equals("AMBOS") || (tipo.equals("ENTRADAS") == (r.tipo() == MovimientoInventario.Tipo.ENTRADA)))
                .sorted(Comparator.comparingInt((MovimientoInventario r) -> r.tipo() == MovimientoInventario.Tipo.ENTRADA ? 0 : 1)
                        .thenComparing(MovimientoInventario::fecha, Comparator.reverseOrder())).toList();
        modelo.setRowCount(0);
        for (var r : visibles) modelo.addRow(new Object[]{r.folio(), r.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                r.fecha().format(DateTimeFormatter.ofPattern("HH:mm:ss")), r.tipo() == MovimientoInventario.Tipo.ENTRADA
                ? "Entrada" : r.tipo() == MovimientoInventario.Tipo.VENTA ? "Salida · venta" : "Salida · merma",
                r.cancelado() ? "CANCELADA" : "Vigente"});
        if (!visibles.isEmpty()) tabla.setRowSelectionInterval(0, 0); else detalle.setText("Aún no hay registros para esta búsqueda.");
    }
    private void mostrar() {
        StringBuilder texto = new StringBuilder();
        List<MovimientoInventario> seleccion = new ArrayList<>();
        for (int fila : tabla.getSelectedRows()) if (fila < visibles.size()) seleccion.add(visibles.get(fila));
        seleccion.sort(Comparator.comparingInt(r -> r.tipo() == MovimientoInventario.Tipo.ENTRADA ? 0 : 1));
        for (var r : seleccion) {
            texto.append(r.tipo() == MovimientoInventario.Tipo.ENTRADA ? "ENTRADA" : "SALIDA")
                    .append(" · ").append(r.folio()).append(r.cancelado() ? " · CANCELADA" : "")
                    .append("\n").append(r.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
            if (!r.motivo().isBlank()) texto.append(r.motivo()).append("\n");
            if (r.tipo() == MovimientoInventario.Tipo.VENTA) {
                Map<String, java.math.BigDecimal> categorias = new TreeMap<>();
                for (var d : r.detalles()) categorias.merge(d.categoria(), d.importe(), java.math.BigDecimal::add);
                categorias.forEach((c, t) -> texto.append("  ").append(c).append("  ").append(UI.dinero(t)).append("\n"));
                texto.append("Total: ").append(UI.dinero(r.total())).append("  Recibido: ").append(UI.dinero(r.recibido()))
                        .append("  Cambio: ").append(UI.dinero(r.cambio())).append("\n");
            } else {
                for (var d : r.detalles()) texto.append("  ").append(d.nombre()).append(" — ").append(d.codigo())
                        .append(" | ").append(UI.cantidad(d.cantidad())).append(' ').append(d.unidad().simbolo()).append("\n");
            }
            texto.append("\n");
        }
        if (!seleccion.isEmpty()) {
            texto.append("VENTAS VIGENTES POR CATEGORÍA EN ESTA SELECCIÓN\n");
            new ServicioReportes().ventasPorCategoria(seleccion).forEach((c, t) -> texto.append(c).append(": ").append(UI.dinero(t)).append("\n"));
        }
        detalle.setText(texto.toString()); detalle.setCaretPosition(0);
    }
    private void cancelar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || tabla.getSelectedRowCount() != 1) { UI.error(this, new Exception("Selecciona una sola venta.")); return; }
        var r = visibles.get(fila);
        if (r.tipo() != MovimientoInventario.Tipo.VENTA || r.cancelado()) { UI.error(this, new Exception("Selecciona una venta vigente.")); return; }
        if (!contexto.autorizacion.configurada()) { UI.error(this, new Exception("Configura la contraseña en Administradores para autorizar cancelaciones.")); return; }
        JPasswordField clave = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Cancelar " + r.folio() + ". Contraseña de autorización:", clave},
                "Cancelar venta", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] valor = clave.getPassword();
        try { contexto.cancelarVenta(r.folio(), valor); } catch (Exception e) { UI.error(this, e); }
        finally { Arrays.fill(valor, '\0'); }
    }
}
