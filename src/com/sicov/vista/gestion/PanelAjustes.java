package com.sicov.vista.gestion;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Pantalla de Ajustes. */
public class PanelAjustes extends JPanel {
    private static final long serialVersionUID = 1L;

    public PanelAjustes() {
        super(new BorderLayout());
        add(new JLabel("Ajustes", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
