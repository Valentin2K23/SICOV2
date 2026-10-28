package com.sicov.vista.gestion;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Pantalla de Entradas. */
public class PanelEntradas extends JPanel {
    private static final long serialVersionUID = 1L;

    public PanelEntradas() {
        super(new BorderLayout());
        add(new JLabel("Entradas", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
