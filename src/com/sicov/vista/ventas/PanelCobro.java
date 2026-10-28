package com.sicov.vista.ventas;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javax.swing.*;

public class PanelCobro extends JPanel {
    public final JTextField efectivo = new JTextField(9);
    private final JLabel estado = new JLabel("Recibido: $0.00");
    public PanelCobro(Consumer<BigDecimal> recibir, Runnable reiniciar) {
        super(new BorderLayout(0, 5));
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 5));
        for (int billete : new int[]{500, 200, 100, 50, 20})
            controles.add(UI.boton("$" + billete, () -> recibir.accept(BigDecimal.valueOf(billete))));
        controles.add(new JLabel("$")); controles.add(efectivo);
        Runnable ingresar = () -> {
            try { BigDecimal cantidad = UI.numero(efectivo.getText()); recibir.accept(cantidad); efectivo.setText(""); }
            catch (Exception e) { UI.error(this, e); }
        };
        UI.enter(efectivo, ingresar);
        controles.add(UI.boton("Agregar efectivo", ingresar)); controles.add(UI.boton("Reiniciar efectivo", reiniciar));
        add(controles, BorderLayout.CENTER); add(estado, BorderLayout.SOUTH);
    }
    public void actualizar(BigDecimal recibido, BigDecimal total) {
        estado.setText("Recibido: " + UI.dinero(recibido) + " · Falta: " + UI.dinero(total.subtract(recibido).max(BigDecimal.ZERO)));
    }
}
