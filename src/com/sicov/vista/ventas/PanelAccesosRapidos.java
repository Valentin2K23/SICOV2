package com.sicov.vista.ventas;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.Producto;
import com.sicov.vista.UI;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;

public class PanelAccesosRapidos extends JPanel {
    public static void abrir(Component padre, ContextoAplicacion contexto, Consumer<Producto> agregar) {
        Window ventana = SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(ventana, "Buscar mercancía", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel contenido = UI.pagina("Mercancía sin código físico");
        JTextField buscar = new JTextField();
        JCheckBox todos = new JCheckBox("Mostrar también productos con código físico");
        DefaultListModel<Producto> modelo = new DefaultListModel<>(); JList<Producto> lista = new JList<>(modelo);
        lista.setFixedCellHeight(44);
        Runnable filtrar = () -> {
            modelo.clear(); String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
            List<Producto> productos = contexto.productos().stream()
                    .filter(p -> todos.isSelected() || p.sinCodigoFisico())
                    .filter(p -> (p.nombre() + " " + p.codigo()).toLowerCase(Locale.ROOT).contains(texto)).toList();
            productos.forEach(modelo::addElement); if (!modelo.isEmpty()) lista.setSelectedIndex(0);
        };
        Runnable elegir = () -> { Producto p = lista.getSelectedValue(); if (p != null) { dialogo.dispose(); agregar.accept(p); } };
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        JPanel campos = new JPanel(new BorderLayout(6, 6)); campos.add(buscar); campos.add(todos, BorderLayout.SOUTH);
        centro.add(campos, BorderLayout.NORTH); centro.add(new JScrollPane(lista)); centro.add(UI.boton("Agregar a la venta", elegir), BorderLayout.SOUTH);
        contenido.add(centro); UI.escuchar(buscar, filtrar); UI.enter(buscar, elegir); todos.addActionListener(e -> filtrar.run());
        lista.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) elegir.run(); } });
        lista.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elegir");
        lista.getActionMap().put("elegir", new AbstractAction() { public void actionPerformed(ActionEvent e) { elegir.run(); } });
        dialogo.setContentPane(contenido); dialogo.setSize(720, 480); dialogo.setLocationRelativeTo(padre);
        filtrar.run(); dialogo.setVisible(true);
    }
}
