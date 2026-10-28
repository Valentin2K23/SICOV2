package com.sicov.vista.gestion;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Pantalla de Consultas. */
public class PanelConsultas extends JPanel {
    private static final long serialVersionUID = 1L;

    public PanelConsultas() {
        super(new BorderLayout());
        add(new JLabel("Consultas", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
