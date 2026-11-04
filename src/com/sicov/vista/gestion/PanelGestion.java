package com.sicov.vista.gestion;

import com.sicov.vista.UI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

/** Menú lateral que delega la navegación y el cierre a la ventana principal. */
public class PanelGestion extends JPanel {
    public PanelGestion(Consumer<String> navegar, Runnable cerrar) {
        super(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(320, 0));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UI.BORDE),
                BorderFactory.createEmptyBorder(20, 16, 14, 16)));
        JPanel cabecera = new JPanel(new BorderLayout(8, 8));
        cabecera.setOpaque(false);
        cabecera.add(UI.titulo("Gestión"));
        JButton cerrarMenu = UI.boton("×", cerrar);
        cerrarMenu.setFont(cerrarMenu.getFont().deriveFont(22f));
        cerrarMenu.setBorder(BorderFactory.createEmptyBorder(0, 10, 2, 10));
        cerrarMenu.setToolTipText("Cerrar menú · Esc");
        cerrarMenu.getAccessibleContext().setAccessibleName("Cerrar menú de Gestión");
        cabecera.add(cerrarMenu, BorderLayout.EAST);
        cabecera.add(UI.discreto("Administra tu negocio"), BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);
        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.Y_AXIS));
        String[][] secciones = { { "Reportes", "reportes", "Consulta las operaciones" },
                { "Compras", "compras", "Productos y recepción de mercancía" },
                { "Salidas", "salidas", "Ventas por categoría y merma" },
                { "Categorías", "categorias", "Organiza productos y unidades" },
                { "Descuentos / Promociones", "promociones", "Paquetes y precios especiales" },
                { "Administradores", "administradores", "Contraseña y recuperación" },
                { "Cancelar ventas", "cancelaciones", "Busca y autoriza una cancelación" } };
        for (String[] seccion : secciones) {
            JButton boton = UI.boton("<html><b>" + seccion[0] + "</b><br><small>" + seccion[2] + "</small></html>",
                    () -> navegar.accept(seccion[1]));
            boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.setAlignmentX(LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            boton.setPreferredSize(new Dimension(276, 60));
            boton.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            botones.add(boton);
            botones.add(Box.createVerticalStrut(7));
        }
        JScrollPane desplazamiento = new JScrollPane(botones);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamiento.getViewport().setBackground(Color.WHITE);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(24);
        add(desplazamiento);
        add(UI.discreto("Esc para cerrar el menú"), BorderLayout.SOUTH);
    }
}
