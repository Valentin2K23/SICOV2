package com.sicov.vista.ventas;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;

public class PanelTotales extends JPanel {
    private final JLabel total = new JLabel("$0.00", SwingConstants.RIGHT);
    public PanelTotales() {
        super(new BorderLayout(8, 0));
        add(new JLabel("TOTAL"), BorderLayout.WEST);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 32f)); total.setForeground(UI.ACENTO);
        add(total); setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
    }
    public void actualizar(BigDecimal importe) { total.setText(UI.dinero(importe)); }
}
