package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.Producto;
import com.sicov.modelo.UnidadVenta;
import com.sicov.vista.gestion.PanelCompras;

import java.math.BigDecimal;
import java.nio.file.Files;
import javax.swing.*;

public final class PruebasCompras {
    private static int verificaciones;

    private static void comprobar(boolean cumple, String mensaje) {
        verificaciones++;
        if (!cumple)
            throw new AssertionError(mensaje);
    }

    private static <T> T campo(PanelCompras panel, String nombre, Class<T> tipo) throws Exception {
        var campo = PanelCompras.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return tipo.cast(campo.get(panel));
    }

    private static void accion(PanelCompras panel, String nombre) throws Exception {
        var metodo = PanelCompras.class.getDeclaredMethod(nombre);
        metodo.setAccessible(true);
        metodo.invoke(panel);
    }

    private static void completar(PanelCompras panel, String nombre, UnidadVenta unidad) throws Exception {
        campo(panel, "nombre", JTextField.class).setText(nombre);
        campo(panel, "precio", JTextField.class).setText("38.50");
        campo(panel, "categoria", JComboBox.class).setSelectedItem("Abarrotes");
        campo(panel, "unidad", JComboBox.class).setSelectedItem(unidad);
    }

    private static void probarCompras() throws Exception {
        var datos = Files.createTempDirectory("sicov-compras-");
        var contexto = new ContextoAplicacion(datos);
        contexto.guardarCategoria("Abarrotes", "Abarrotes");
        contexto.guardarProducto(new Producto("INT-000001", "Existente", BigDecimal.ONE, "Abarrotes",
                UnidadVenta.PIEZA, true), false);
        var panel = new PanelCompras(contexto);
        var categoria = campo(panel, "categoria", JComboBox.class);
        comprobar(!categoria.isEditable(), "La categoría solo permite seleccionar opciones existentes");
        categoria.setSelectedItem("Categoría inventada");
        comprobar(categoria.getSelectedItem().equals("Abarrotes"), "No admite una categoría escrita fuera de la lista");

        completar(panel, "Huevo a granel", UnidadVenta.KILOGRAMO);
        var codigo = campo(panel, "codigo", JTextField.class);
        var sinCodigo = campo(panel, "sinCodigo", JCheckBox.class);
        comprobar(sinCodigo.isSelected() && !codigo.isEnabled(),
                "Granel activa Sin código físico y no solicita un código de barras");
        accion(panel, "guardar");
        Producto huevo = contexto.producto("INT-000002");
        comprobar(huevo != null && huevo.nombre().equals("Huevo a granel") && huevo.sinCodigoFisico(),
                "Guardar con código vacío asigna un identificador interno sin colisiones");
        comprobar(contexto.producto("INT-000001").nombre().equals("Existente"),
                "Generar un identificador interno conserva los productos existentes");
        comprobar(codigo.getText().isBlank() && codigo.isEnabled() && !sinCodigo.isSelected(),
                "El formulario vuelve al alta con código físico después de guardar");

        var recarga = new ContextoAplicacion(datos);
        var segundoPanel = new PanelCompras(recarga);
        completar(segundoPanel, "Pieza sin empaque", UnidadVenta.PIEZA);
        campo(segundoPanel, "sinCodigo", JCheckBox.class).setSelected(true);
        accion(segundoPanel, "guardar");
        comprobar(recarga.producto("INT-000003") != null && recarga.producto("INT-000003").sinCodigoFisico(),
                "Una pieza sin código físico recibe otro identificador único al reiniciar");

        var tabla = campo(segundoPanel, "tabla", JTable.class);
        var buscar = campo(segundoPanel, "buscar", JTextField.class);
        buscar.setText("INT-000002");
        tabla.setRowSelectionInterval(0, 0);
        accion(segundoPanel, "editar");
        campo(segundoPanel, "nombre", JTextField.class).setText("Huevo fresco");
        accion(segundoPanel, "guardar");
        comprobar(recarga.producto("INT-000002").nombre().equals("Huevo fresco") && recarga.productos().size() == 3,
                "Editar conserva el identificador interno sin duplicar el producto");

        accion(segundoPanel, "limpiar");
        completar(segundoPanel, "Producto con empaque", UnidadVenta.PIEZA);
        campo(segundoPanel, "codigo", JTextField.class).setText("7501234567890");
        accion(segundoPanel, "guardar");
        comprobar(recarga.producto("7501234567890") != null && !recarga.producto("7501234567890").sinCodigoFisico(),
                "Los productos con código físico conservan el código capturado");

        var registrar = campo(segundoPanel, "registrarRecepcion", JButton.class);
        comprobar(!registrar.isEnabled(), "No se registra una recepción vacía");
        buscar.setText("INT-000002");
        tabla.setValueAt("1.25", 0, 2);
        comprobar(Boolean.TRUE.equals(tabla.getValueAt(0, 3)) && registrar.isEnabled(),
                "Capturar una cantidad marca el producto y habilita Registrar recepción");
        buscar.setText("7501234567890");
        tabla.setValueAt("6", 0, 2);
        comprobar(campo(segundoPanel, "resumenRecepcion", JLabel.class).getText().startsWith("2 productos"),
                "El resumen incluye productos seleccionados aunque estén ocultos por la búsqueda");
        accion(segundoPanel, "recibir");
        var registro = new ContextoAplicacion(datos).registros().getFirst();
        comprobar(registro.detalles().size() == 2 && registro.detalles().stream().anyMatch(d ->
                d.codigo().equals("INT-000002") && d.cantidad().compareTo(new BigDecimal("1.25")) == 0),
                "La recepción guarda todos los seleccionados con sus cantidades precisas");
        comprobar(!registrar.isEnabled() && campo(segundoPanel, "resumenRecepcion", JLabel.class).getText()
                .equals("Sin productos seleccionados"), "Una recepción guardada limpia la selección y el resumen");
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                probarCompras();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("OK: " + verificaciones + " verificaciones de compras y productos sin código.");
    }
}
