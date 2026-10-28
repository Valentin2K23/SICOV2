package com.sicov.vista.gestion;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.ServicioInventario;
import com.sicov.vista.UI;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class PanelCategorias extends JPanel {
    private final ContextoAplicacion contexto;
    private final Runnable antesDeNavegar, regresar;
    private String pantallaActual = "menu";
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JTextField nombre = new JTextField();
    private final JComboBox<String> seccion = new JComboBox<>(Categoria.SECCIONES.toArray(String[]::new));
    private final DefaultTableModel modeloCategorias = UI.tabla("Categoría", "Sección", "Productos registrados");
    private final JTable tablaCategorias = new JTable(modeloCategorias);
    private final DefaultListModel<Categoria> modeloConsulta = new DefaultListModel<>();
    private final JList<Categoria> lista = new JList<>(modeloConsulta);
    private final DefaultTableModel modeloProductos = UI.tabla("Producto", "Código", "Cantidad estimada", "Unidad");
    private final JLabel tituloProductos = new JLabel(), estado = new JLabel(" "), vacio = new JLabel();
    private String categoriaAbierta;
    private boolean refrescando;

    public PanelCategorias(ContextoAplicacion contexto, Runnable antesDeNavegar, Runnable regresar) {
        super(new BorderLayout()); this.contexto = contexto;
        this.antesDeNavegar = antesDeNavegar; this.regresar = regresar;
        JPanel menu = UI.pagina("Categorías");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 18));
        botones.add(cuadrado("<html><center>Configurar<br>categorías</center></html>", () -> mostrar("configurar")));
        botones.add(cuadrado("Ver categorías", () -> mostrar("consultar")));
        menu.add(botones); contenido.add(menu, "menu");
        contenido.add(configuracion(), "configurar");
        contenido.add(consulta(), "consultar");
        contenido.add(productos(), "productos");
        add(contenido); contexto.escuchar(this::refrescar); refrescar(); mostrar("menu");
    }
    private JButton cuadrado(String texto, Runnable accion) {
        JButton boton = UI.boton(texto, accion);
        Dimension tamano = new Dimension(210, 210);
        boton.setPreferredSize(tamano); boton.setMinimumSize(tamano); boton.setMaximumSize(tamano);
        return boton;
    }
    private void mostrar(String pantalla) {
        if (!pantallaActual.equals(pantalla)) antesDeNavegar.run();
        pantallaActual = pantalla; tarjetas.show(contenido, pantalla);
    }
    public void mostrarMenu() { pantallaActual = "menu"; tarjetas.show(contenido, "menu"); }
    public Runnable restauracion() {
        String pantalla = pantallaActual, categoria = categoriaAbierta;
        return () -> {
            pantallaActual = pantalla; categoriaAbierta = categoria;
            refrescarProductos(); tarjetas.show(contenido, pantalla);
        };
    }
    private JPanel configuracion() {
        JPanel pagina = UI.pagina("Configurar categorías");
        JPanel formulario = new JPanel(); formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setPreferredSize(new Dimension(340, 300));
        formulario.add(UI.campo("Nombre de la categoría", nombre)); formulario.add(Box.createVerticalStrut(12));
        formulario.add(UI.campo("Sección a la que pertenece", seccion)); formulario.add(Box.createVerticalStrut(12));
        formulario.add(new JLabel("Repostería: galletas, pan y similares."));
        formulario.add(Box.createVerticalStrut(16));
        formulario.add(UI.boton("Guardar categoría", this::guardar));
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(UI.boton("Eliminar seleccionada", this::eliminar));
        formulario.add(Box.createVerticalGlue());
        for (Component c : formulario.getComponents()) if (c instanceof JComponent jc) jc.setAlignmentX(LEFT_ALIGNMENT);
        tablaCategorias.setRowHeight(38); tablaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCategorias.getSelectionModel().addListSelectionListener(e -> {
            if (refrescando || e.getValueIsAdjusting() || tablaCategorias.getSelectedRow() < 0) return;
            int fila = tablaCategorias.getSelectedRow();
            nombre.setText(modeloCategorias.getValueAt(fila, 0).toString());
            String valor = modeloCategorias.getValueAt(fila, 1).toString();
            seccion.setSelectedItem(valor.equals("Sin sección") ? null : valor);
        });
        JPanel centro = new JPanel(new BorderLayout(20, 12));
        centro.add(formulario, BorderLayout.WEST); centro.add(new JScrollPane(tablaCategorias));
        pagina.add(centro);
        JPanel pie = new JPanel(new BorderLayout(12, 0));
        pie.add(UI.boton("Regresar a Categorías", regresar), BorderLayout.WEST);
        pie.add(estado); pagina.add(pie, BorderLayout.SOUTH);
        UI.enter(nombre, () -> seccion.requestFocusInWindow()); seccion.setSelectedIndex(-1);
        return pagina;
    }
    private JPanel consulta() {
        JPanel pagina = UI.pagina("Ver categorías");
        lista.setFixedCellHeight(54); lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object valor, int i, boolean seleccion, boolean foco) {
                Categoria categoria = (Categoria) valor;
                return super.getListCellRendererComponent(l, categoria.nombre() + "  ·  " + categoria.seccionVisible(), i, seleccion, foco);
            }
        });
        lista.addListSelectionListener(e -> {
            if (refrescando || e.getValueIsAdjusting() || lista.getSelectedValue() == null) return;
            categoriaAbierta = lista.getSelectedValue().nombre(); refrescarProductos(); mostrar("productos");
            refrescando = true; lista.clearSelection(); refrescando = false;
        });
        JPanel centro = new JPanel(new BorderLayout(8, 8)); centro.add(vacio, BorderLayout.NORTH);
        centro.add(new JScrollPane(lista)); pagina.add(centro);
        pagina.add(UI.boton("Regresar a Categorías", regresar), BorderLayout.SOUTH);
        return pagina;
    }
    private JPanel productos() {
        JPanel pagina = UI.pagina("Productos de la categoría");
        JTable tabla = new JTable(modeloProductos); tabla.setRowHeight(38); tabla.setAutoCreateRowSorter(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(400);
        JPanel centro = new JPanel(new BorderLayout(12, 12)); centro.add(tituloProductos, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla));
        centro.add(new JLabel("Estimación según los registros disponibles; puede ser negativa y no limita la venta."), BorderLayout.SOUTH);
        pagina.add(centro);
        pagina.add(UI.boton("Regresar a la lista de categorías", regresar), BorderLayout.SOUTH);
        return pagina;
    }
    private void guardar() {
        try {
            contexto.guardarCategoria(nombre.getText(), (String) seccion.getSelectedItem());
            estado.setText("Categoría guardada."); limpiar();
        } catch (Exception e) { UI.error(this, e); }
    }
    private void eliminar() {
        if (tablaCategorias.getSelectedRow() < 0) return;
        try {
            contexto.eliminarCategoria(modeloCategorias.getValueAt(tablaCategorias.getSelectedRow(), 0).toString());
            limpiar(); estado.setText("Categoría eliminada.");
        } catch (Exception e) { UI.error(this, e); }
    }
    private void limpiar() {
        tablaCategorias.clearSelection(); nombre.setEditable(true); nombre.setText("");
        seccion.setSelectedIndex(-1); nombre.requestFocusInWindow();
    }
    private void refrescar() {
        refrescando = true;
        try {
            modeloCategorias.setRowCount(0); modeloConsulta.clear();
            for (Categoria categoria : contexto.categoriasConfiguradas()) {
                long productos = contexto.productos().stream().filter(p -> p.categoria().equals(categoria.nombre())).count();
                modeloCategorias.addRow(new Object[]{categoria.nombre(), categoria.seccionVisible(), productos}); modeloConsulta.addElement(categoria);
            }
            vacio.setText(modeloConsulta.isEmpty() ? "Aún no hay categorías. Crea una en Configurar categorías."
                    : "Selecciona una categoría para ver sus productos.");
            refrescarProductos();
        } finally { refrescando = false; }
    }
    private void refrescarProductos() {
        modeloProductos.setRowCount(0);
        if (categoriaAbierta == null) return;
        Map<String, BigDecimal> cantidades = new ServicioInventario().cantidadesEstimadas(contexto.productos(), contexto.registros());
        for (Producto p : contexto.productos()) if (p.categoria().equals(categoriaAbierta)) {
            BigDecimal cantidad = cantidades.get(p.codigo());
            modeloProductos.addRow(new Object[]{p.nombre(), p.codigo(), cantidad == null
                    ? "Sin estimación: cambió la unidad" : UI.cantidad(cantidad), p.unidad().simbolo()});
        }
        Categoria categoria = contexto.categoriasConfiguradas().stream().filter(c -> c.nombre().equals(categoriaAbierta)).findFirst().orElse(null);
        tituloProductos.setText(categoriaAbierta + (categoria == null ? "" : " · " + categoria.seccionVisible())
                + (modeloProductos.getRowCount() == 0 ? " · Sin productos registrados" : " · " + modeloProductos.getRowCount() + " productos"));
    }
}
