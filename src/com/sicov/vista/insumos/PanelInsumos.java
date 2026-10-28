package com.sicov.vista.insumos;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Pantalla de Insumos. */
public class PanelInsumos extends JPanel {
    private static final long serialVersionUID = 1L;

    public PanelInsumos() {
        super(new BorderLayout());
        add(new JLabel("Insumos", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
