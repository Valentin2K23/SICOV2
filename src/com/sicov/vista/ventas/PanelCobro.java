package com.sicov.vista.ventas;

import com.sicov.vista.UI;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javax.swing.*;

/** Captura de efectivo; agregar dinero nunca registra la venta. */
public class PanelCobro extends JPanel {
    public final JTextField efectivo = new JTextField(9);
    private final JLabel estado = new JLabel();
    private final JButton confirmar;

    public PanelCobro(Consumer<BigDecimal> recibir, Runnable reiniciar, Runnable exacto, Runnable pagar) {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        controles.setOpaque(false);
        JPanel billetes = new JPanel(new GridLayout(2, 3, 8, 8));
        billetes.setOpaque(false);
        for (int billete : new int[] { 20, 50, 100, 200, 500, 1000 })
            billetes.add(UI.boton("$" + billete, () -> recibir.accept(BigDecimal.valueOf(billete))));
        JPanel opcionesBilletes = new JPanel(new BorderLayout(0, 7));
        opcionesBilletes.setOpaque(false);
        opcionesBilletes.add(UI.discreto("¿Con cuánto paga? Elige o escribe el total recibido."), BorderLayout.NORTH);
        opcionesBilletes.add(billetes);
        controles.add(opcionesBilletes);
        controles.add(Box.createVerticalStrut(12));
        efectivo.setFont(efectivo.getFont().deriveFont(22f));
        efectivo.setPreferredSize(new Dimension(efectivo.getPreferredSize().width, 40));
        efectivo.getAccessibleContext().setAccessibleName("Total de efectivo recibido del cliente");
        JPanel captura = new JPanel(new BorderLayout(8, 0));
        captura.setOpaque(false);
        captura.add(UI.campo("Efectivo recibido ($)", efectivo));
        UI.enter(efectivo, this::confirmarConEnter);
        controles.add(captura);
        controles.add(Box.createVerticalStrut(10));
        JPanel opciones = new JPanel(new GridLayout(1, 2, 8, 0));
        opciones.setOpaque(false);
        for (JButton boton : new JButton[] { UI.boton("Importe exacto", exacto),
                UI.boton("Borrar efectivo", reiniciar) }) {
            boton.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
            boton.setPreferredSize(new Dimension(boton.getPreferredSize().width, 34));
            opciones.add(boton);
        }
        for (JPanel fila : new JPanel[] { opcionesBilletes, captura, opciones }) {
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
        }
        controles.add(opciones);
        add(controles, BorderLayout.NORTH);
        estado.setFont(estado.getFont().deriveFont(20f));
        estado.setForeground(UI.VERDE);
        add(estado);
        confirmar = UI.primario("Confirmar pago y ver ticket", pagar);
        confirmar.setEnabled(false);
        confirmar.setToolTipText("Enter · Registra la venta una sola vez y muestra el ticket.");
        add(confirmar, BorderLayout.SOUTH);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmarPago");
        getActionMap().put("confirmarPago", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmarConEnter();
            }
        });
    }

    public void confirmarConEnter() {
        if (confirmar.isEnabled())
            confirmar.doClick(0);
    }

    /**
     * Valida lo escrito para la vista previa; el controlador recibe ese efectivo al confirmar.
     */
    public void actualizar(BigDecimal recibido, BigDecimal total) {
        boolean valido = true;
        if (!efectivo.getText().isBlank()) {
            try {
                recibido = UI.numero(efectivo.getText());
                valido = recibido.signum() >= 0 && recibido.stripTrailingZeros().scale() <= 2;
            } catch (IllegalArgumentException e) {
                valido = false;
            }
        }
        BigDecimal diferencia = recibido.subtract(total);
        estado.setText(valido
                ? "<html>Recibido: <b>" + UI.dinero(recibido) + "</b><br>"
                        + (diferencia.signum() < 0 ? "Falta: <b>" + UI.dinero(diferencia.negate())
                                : "Cambio: <b>" + UI.dinero(diferencia))
                        + "</b></html>"
                : "<html>Escribe un importe válido,<br>con máximo dos decimales.</html>");
        confirmar.setEnabled(valido && diferencia.signum() >= 0);
    }
}
