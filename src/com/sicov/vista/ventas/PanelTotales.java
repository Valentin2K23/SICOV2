package com.sicov.vista.ventas;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;

public class PanelTotales extends JPanel {
    private final JLabel total = new JLabel("$0.00", SwingConstants.RIGHT);
    public PanelTotales() {
        super(new BorderLayout(0, 8)); setBackground(UI.VERDE);
        JLabel titulo = new JLabel("TOTAL A PAGAR"); titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 38f)); total.setForeground(Color.WHITE);
        add(total); setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    }
    public void actualizar(BigDecimal importe) { total.setText(UI.dinero(importe)); }
}
