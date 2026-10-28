package com.sicov.vista.gestion;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Pantalla de Productos. */
public class PanelProductos extends JPanel {
    private static final long serialVersionUID = 1L;

    public PanelProductos() {
        super(new BorderLayout());
        add(new JLabel("Productos", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
