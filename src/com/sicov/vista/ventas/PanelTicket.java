package com.sicov.vista.ventas;

import com.sicov.modelo.*;
import com.sicov.vista.UI;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

/** Comprobante en pantalla construido exclusivamente con la venta ya guardada. */
public final class PanelTicket extends JPanel {
    public PanelTicket(MovimientoInventario venta, Runnable cerrar) {
        super(new BorderLayout(12, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 6));
        cabecera.add(UI.titulo("Pago realizado"));
        JLabel cambio = UI.titulo("Cambio: " + UI.dinero(venta.cambio())); cambio.setForeground(UI.ACENTO);
        cabecera.add(cambio); cabecera.add(new JLabel("Venta guardada · " + venta.folio()));
        add(cabecera, BorderLayout.NORTH);
        JTextArea ticket = new JTextArea(texto(venta)); ticket.setEditable(false); ticket.setLineWrap(true); ticket.setWrapStyleWord(true);
        ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        ticket.setBackground(Color.WHITE); ticket.setMargin(new Insets(16, 16, 16, 16));
        ticket.setCaretPosition(0); ticket.getAccessibleContext().setAccessibleName("Ticket de la venta");
        add(new JScrollPane(ticket));
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        pie.add(UI.primario("Cerrar ticket y comenzar nueva venta", cerrar));
        pie.add(UI.discreto("Enter / Esc · Cerrar ticket"), BorderLayout.SOUTH); add(pie, BorderLayout.SOUTH);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "cerrarTicket");
        getActionMap().put("cerrarTicket", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cerrar.run(); }
        });
    }
    public static String texto(MovimientoInventario venta) {
        StringBuilder texto = new StringBuilder("SICOV · ABARROTES\nCOMPROBANTE DE VENTA\n\n");
        texto.append(venta.folio()).append('\n')
                .append(venta.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
        for (DetalleVenta d : venta.detalles()) {
            texto.append(d.nombre()).append('\n').append("  ").append(UI.medida(d.cantidad(), d.unidad()))
                    .append(" a ").append(UI.dinero(d.precio())).append("/").append(d.unidad().simbolo())
                    .append("  =  ").append(UI.dinero(d.importe())).append('\n');
            if (!d.promocion().isBlank()) texto.append("  ").append(d.promocion()).append('\n');
        }
        return texto.append("\nTOTAL:    ").append(UI.dinero(venta.total()))
                .append("\nEFECTIVO: ").append(UI.dinero(venta.recibido()))
                .append("\nCAMBIO:   ").append(UI.dinero(venta.cambio()))
                .append("\n\n¡Gracias por tu compra!\n").toString();
    }
}
