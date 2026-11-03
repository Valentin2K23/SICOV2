package com.sicov.vista.ventas;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.Producto;
import com.sicov.modelo.UnidadVenta;
import com.sicov.vista.UI;
import com.sicov.vista.ImagenProducto;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;

public class PanelAccesosRapidos extends JPanel {
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip();
    }

    public static void abrir(Component padre, ContextoAplicacion contexto, Consumer<Producto> agregar) {
        Window ventana = SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(ventana, "Buscar productos", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel contenido = UI.pagina("Buscar productos");
        JTextField buscar = new JTextField();
        buscar.getAccessibleContext().setAccessibleName("Buscar por nombre, código o categoría");
        buscar.setFont(buscar.getFont().deriveFont(20f));
        buscar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.BORDE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JToggleButton todos = new JToggleButton("Todos", true);
        JToggleButton granel = new JToggleButton("A granel");
        JToggleButton sinCodigo = new JToggleButton("Sin código");
        ButtonGroup grupo = new ButtonGroup();
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JToggleButton filtro : new JToggleButton[]{todos, granel, sinCodigo}) {
            grupo.add(filtro); filtros.add(filtro);
            filtro.setFocusPainted(false); filtro.setMargin(new Insets(8, 12, 8, 12));
            filtro.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        JComboBox<String> categoria = new JComboBox<>();
        categoria.addItem("Todas las categorías");
        contexto.productos().stream().map(Producto::categoria).distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER).forEach(categoria::addItem);
        categoria.getAccessibleContext().setAccessibleName("Filtrar por categoría");
        categoria.setPreferredSize(new Dimension(200, 38));

        JLabel resultados = new JLabel();
        DefaultListModel<Producto> modelo = new DefaultListModel<>(); JList<Producto> lista = new JList<>(modelo) {
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
        };
        lista.setFixedCellHeight(80); lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setBackground(Color.WHITE);
        lista.setCellRenderer((l, p, indice, seleccion, foco) -> {
            JPanel fila = new JPanel(new BorderLayout(12, 4));
            fila.setBackground(seleccion ? UI.SUAVE : Color.WHITE);
            fila.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, seleccion ? 4 : 0, 1, 0,
                            seleccion ? UI.VERDE : UI.BORDE),
                    BorderFactory.createEmptyBorder(10, seleccion ? 10 : 14, 10, 14)));
            JLabel nombre = new JLabel(p.nombre());
            nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 17f));
            JLabel detalle = new JLabel(p.categoria() + "  ·  Código " + p.codigo()
                    + (p.sinCodigoFisico() ? "  ·  Sin código físico" : ""));
            detalle.setFont(detalle.getFont().deriveFont(12f));
            detalle.setForeground(new Color(105, 112, 104));
            JLabel precio = new JLabel(UI.dinero(p.precio()) + " / " + p.unidad().simbolo());
            precio.setFont(precio.getFont().deriveFont(Font.BOLD, 18f)); precio.setForeground(UI.VERDE);
            JLabel unidad = new JLabel(p.unidad() == UnidadVenta.PIEZA ? "Por pieza" : "A granel", SwingConstants.RIGHT);
            unidad.setFont(unidad.getFont().deriveFont(12f)); unidad.setForeground(new Color(105, 112, 104));
            JPanel datos = new JPanel(new BorderLayout(4, 4)); datos.setOpaque(false);
            datos.add(nombre, BorderLayout.NORTH); datos.add(detalle, BorderLayout.SOUTH);
            JPanel importe = new JPanel(new BorderLayout(4, 4)); importe.setOpaque(false);
            importe.add(precio, BorderLayout.NORTH); importe.add(unidad, BorderLayout.SOUTH);
            JLabel foto = new JLabel(ImagenProducto.icono(p, contexto.imagenProducto(p.codigo()), contexto.seccionCategoria(p.categoria()), 48));
            fila.add(foto, BorderLayout.WEST); fila.add(datos); fila.add(importe, BorderLayout.EAST); return fila;
        });

        CardLayout tarjetas = new CardLayout(); JPanel productos = new JPanel(tarjetas);
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(UI.BORDE));
        productos.add(scroll, "lista");
        JPanel vacio = UI.tarjeta();
        JLabel mensaje = new JLabel("<html><center><b>No encontramos productos</b><br><br>Prueba otro nombre o cambia los filtros.</center></html>", SwingConstants.CENTER);
        vacio.add(mensaje); productos.add(vacio, "vacio");
        JButton agregarVenta = UI.primario("Agregar a la venta", () -> {
            Producto p = lista.getSelectedValue();
            if (p != null) { dialogo.dispose(); agregar.accept(p); }
        });
        Runnable elegir = agregarVenta::doClick;
        lista.addListSelectionListener(e -> {
            Producto p = lista.getSelectedValue(); agregarVenta.setEnabled(p != null);
            agregarVenta.setText(p != null && p.unidad() != UnidadVenta.PIEZA
                    ? "Elegir cantidad" : "Agregar a la venta");
        });
        Runnable filtrar = () -> {
            modelo.clear(); String texto = normalizar(buscar.getText());
            List<Producto> encontrados = contexto.productos().stream()
                    .filter(p -> !granel.isSelected() || p.unidad() != UnidadVenta.PIEZA)
                    .filter(p -> !sinCodigo.isSelected() || p.sinCodigoFisico())
                    .filter(p -> categoria.getSelectedIndex() == 0 || p.categoria().equals(categoria.getSelectedItem()))
                    .filter(p -> normalizar(p.nombre() + " " + p.codigo() + " " + p.categoria()).contains(texto))
                    .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre())).toList();
            encontrados.forEach(modelo::addElement);
            resultados.setText(encontrados.size() + (encontrados.size() == 1 ? " producto" : " productos"));
            tarjetas.show(productos, modelo.isEmpty() ? "vacio" : "lista");
            if (!modelo.isEmpty()) lista.setSelectedIndex(0);
            agregarVenta.setEnabled(!modelo.isEmpty());
        };
        JPanel centro = new JPanel(new BorderLayout(12, 12));
        JPanel campos = new JPanel(new BorderLayout(10, 10));
        campos.add(UI.campo("Nombre, código o categoría", buscar));
        JPanel opciones = new JPanel(new BorderLayout(8, 0));
        opciones.add(filtros); opciones.add(categoria, BorderLayout.EAST); campos.add(opciones, BorderLayout.SOUTH);
        centro.add(campos, BorderLayout.NORTH); centro.add(productos);
        JPanel pie = new JPanel(new BorderLayout(8, 8));
        resultados.setFont(resultados.getFont().deriveFont(12f));
        resultados.setForeground(new Color(105, 112, 104)); pie.add(resultados, BorderLayout.NORTH);
        JPanel acciones = new JPanel(new BorderLayout(8, 0));
        acciones.add(UI.boton("Cerrar", dialogo::dispose), BorderLayout.WEST);
        acciones.add(agregarVenta, BorderLayout.EAST); pie.add(acciones);
        JLabel ayuda = new JLabel("↑ ↓ seleccionar · Enter agregar · Esc cerrar");
        ayuda.setFont(ayuda.getFont().deriveFont(11f)); ayuda.setForeground(new Color(105, 112, 104));
        pie.add(ayuda, BorderLayout.SOUTH);
        centro.add(pie, BorderLayout.SOUTH);
        contenido.add(centro); UI.escuchar(buscar, filtrar); UI.enter(buscar, elegir);
        for (JToggleButton filtro : new JToggleButton[]{todos, granel, sinCodigo})
            filtro.addActionListener(e -> filtrar.run());
        categoria.addActionListener(e -> filtrar.run());
        lista.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int indice = lista.locationToIndex(e.getPoint());
                if (e.getClickCount() == 2 && indice >= 0 && lista.getCellBounds(indice, indice).contains(e.getPoint())) elegir.run();
            }
        });
        lista.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elegir");
        lista.getActionMap().put("elegir", new AbstractAction() { public void actionPerformed(ActionEvent e) { elegir.run(); } });
        for (int tecla : new int[]{KeyEvent.VK_DOWN, KeyEvent.VK_UP}) {
            String accion = "mover-" + tecla;
            buscar.getInputMap().put(KeyStroke.getKeyStroke(tecla, 0), accion);
            buscar.getActionMap().put(accion, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (modelo.isEmpty()) return;
                    int siguiente = Math.max(0, Math.min(modelo.size() - 1,
                            lista.getSelectedIndex() + (tecla == KeyEvent.VK_DOWN ? 1 : -1)));
                    lista.setSelectedIndex(siguiente); lista.ensureIndexIsVisible(siguiente);
                }
            });
        }
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { buscar.requestFocusInWindow(); }
        });
        dialogo.setContentPane(contenido); dialogo.setSize(780, 580); dialogo.setMinimumSize(new Dimension(680, 480));
        dialogo.setLocationRelativeTo(padre); dialogo.getRootPane().setDefaultButton(agregarVenta);
        filtrar.run(); dialogo.setVisible(true);
    }
}
