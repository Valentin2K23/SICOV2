package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.MovimientoInventario;
import com.sicov.servicio.ServicioReportes;
import com.sicov.vista.UI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Consulta el historial de movimientos y combina los folios seleccionados en un reporte.
 */
public class PanelReportes extends JPanel {
    private final ContextoAplicacion contexto;
    private final DefaultTableModel modelo = UI.tabla("Folio", "Día", "Hora", "Tipo", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final JTextField buscar = new JTextField(22);
    private final JTextArea detalle = new JTextArea();
    private final JLabel estado = new JLabel(" ");
    private final JButton cancelar;
    private List<MovimientoInventario> visibles = List.of();
    private String tipo = "AMBOS";
    private boolean actualizando;

    public PanelReportes(ContextoAplicacion contexto) {
        super(new BorderLayout());
        this.contexto = contexto;
        JPanel pagina = UI.pagina("Reportes · entradas y salidas");
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String t : new String[] { "ENTRADAS", "SALIDAS", "AMBOS" })
            barra.add(UI.boton(t, () -> {
                tipo = t;
                refrescar();
            }));
        barra.add(new JLabel("Buscar folio"));
        barra.add(buscar);
        centro.add(barra, BorderLayout.NORTH);
        tabla.setRowHeight(34);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(230);
        detalle.setEditable(false);
        detalle.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detalle.setMargin(new Insets(12, 12, 12, 12));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla),
                new JScrollPane(detalle));
        division.setResizeWeight(0.5);
        division.setDividerLocation(230);
        centro.add(division);
        JPanel pie = new JPanel(new BorderLayout(12, 8));
        pie.add(UI.discreto("Selecciona varios folios con Ctrl para ver un reporte combinado."), BorderLayout.CENTER);
        cancelar = UI.boton("Cancelar venta seleccionada", this::cancelar);
        pie.add(cancelar, BorderLayout.EAST);
        estado.setForeground(UI.MUTED);
        pie.add(estado, BorderLayout.SOUTH);
        centro.add(pie, BorderLayout.SOUTH);
        pagina.add(centro);
        add(pagina);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!actualizando && !e.getValueIsAdjusting())
                mostrar();
        });
        UI.escuchar(buscar, this::refrescar);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing())
                refrescar();
        });
        contexto.escuchar(this::refrescar);
        refrescar();
    }

    private void refrescar() {
        // Los folios permiten restaurar la selección aunque cambie el orden o se
        // recargue el historial.
        Set<String> seleccionados = new HashSet<>();
        for (int fila : tabla.getSelectedRows())
            if (fila < visibles.size())
                seleccionados.add(visibles.get(fila).folio());
        String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
        actualizando = true;
        visibles = contexto.registros().stream().filter(r -> r.folio().toLowerCase(Locale.ROOT).contains(texto))
                .filter(r -> tipo.equals("AMBOS")
                        || (tipo.equals("ENTRADAS") == (r.tipo() == MovimientoInventario.Tipo.ENTRADA)))
                .sorted(Comparator
                        .comparingInt((MovimientoInventario r) -> r.tipo() == MovimientoInventario.Tipo.ENTRADA ? 0 : 1)
                        .thenComparing(MovimientoInventario::fecha, Comparator.reverseOrder()))
                .toList();
        modelo.setRowCount(0);
        for (var r : visibles)
            modelo.addRow(new Object[] { r.folio(), r.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    r.fecha().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    r.tipo() == MovimientoInventario.Tipo.ENTRADA ? "Entrada"
                            : r.tipo() == MovimientoInventario.Tipo.VENTA ? "Salida · venta" : "Salida · merma",
                    r.cancelado() ? "CANCELADA" : "Vigente" });
        for (int i = 0; i < visibles.size(); i++)
            if (seleccionados.contains(visibles.get(i).folio()))
                tabla.addRowSelectionInterval(i, i);
        if (!visibles.isEmpty() && tabla.getSelectedRowCount() == 0)
            tabla.setRowSelectionInterval(0, 0);
        actualizando = false;
        mostrar();
        if (visibles.isEmpty())
            detalle.setText("Aún no hay registros para esta búsqueda.");
    }

    private void mostrar() {
        StringBuilder texto = new StringBuilder();
        List<MovimientoInventario> seleccion = new ArrayList<>();
        for (int fila : tabla.getSelectedRows())
            if (fila < visibles.size())
                seleccion.add(visibles.get(fila));
        cancelar.setEnabled(seleccion.size() == 1 && seleccion.get(0).tipo() == MovimientoInventario.Tipo.VENTA
                && !seleccion.get(0).cancelado() && contexto.autorizacion.configurada());
        cancelar.setToolTipText(!contexto.autorizacion.configurada()
                ? "Configura la contraseña en Administradores para autorizar cancelaciones."
                : null);
        seleccion.sort(Comparator.comparingInt(r -> r.tipo() == MovimientoInventario.Tipo.ENTRADA ? 0 : 1));
        for (var r : seleccion) {
            texto.append(r.tipo() == MovimientoInventario.Tipo.ENTRADA ? "ENTRADA" : "SALIDA").append(" · ")
                    .append(r.folio()).append(r.cancelado() ? " · CANCELADA" : "").append("\n")
                    .append(r.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
            if (!r.motivo().isBlank())
                texto.append(r.cancelado() ? "Motivo de cancelación: " : "Motivo: ").append(r.motivo()).append("\n");
            if (r.tipo() == MovimientoInventario.Tipo.VENTA) {
                // Se usan las categorías guardadas en la venta, no las actuales del catálogo.
                Map<String, BigDecimal> categorias = new TreeMap<>();
                for (var d : r.detalles())
                    categorias.merge(d.categoria(), d.importe(), BigDecimal::add);
                categorias
                        .forEach((c, t) -> texto.append("  ").append(c).append("  ").append(UI.dinero(t)).append("\n"));
                texto.append("Total: ").append(UI.dinero(r.total())).append("  Recibido: ")
                        .append(UI.dinero(r.recibido())).append("  Cambio: ").append(UI.dinero(r.cambio()))
                        .append("\n");
            } else {
                for (var d : r.detalles())
                    texto.append("  ").append(d.nombre()).append(" — ").append(d.codigo()).append(" | ")
                            .append(UI.cantidad(d.cantidad())).append(' ').append(d.unidad().simbolo()).append("\n");
            }
            texto.append("\n");
        }
        if (!seleccion.isEmpty()) {
            texto.append("VENTAS VIGENTES POR CATEGORÍA EN ESTA SELECCIÓN\n");
            new ServicioReportes().ventasPorCategoria(seleccion)
                    .forEach((c, t) -> texto.append(c).append(": ").append(UI.dinero(t)).append("\n"));
        }
        detalle.setText(texto.toString());
        detalle.setCaretPosition(0);
    }

    private void cancelar() {
        int fila = tabla.getSelectedRow();
        estado.setForeground(UI.ACENTO);
        if (fila < 0 || tabla.getSelectedRowCount() != 1) {
            estado.setText("Selecciona una sola venta.");
            return;
        }
        var r = visibles.get(fila);
        if (r.tipo() != MovimientoInventario.Tipo.VENTA || r.cancelado()) {
            estado.setText("Selecciona una venta vigente.");
            return;
        }
        if (!contexto.autorizacion.configurada()) {
            estado.setText("Configura la contraseña en Administradores para autorizar cancelaciones.");
            return;
        }
        PanelCancelaciones.solicitar(this, contexto, r);
        if (contexto.registros().stream().anyMatch(v -> v.folio().equals(r.folio()) && v.cancelado())) {
            estado.setText("Venta " + r.folio() + " cancelada. Se conserva en el historial.");
            estado.setForeground(UI.VERDE);
        }
    }
}
