package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.*;
import com.sicov.servicio.*;
import com.sicov.vista.gestion.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Cancelaciones autorizadas, historial y recuperación ante errores de guardado. */
public final class PruebasCancelaciones {
    private static int verificaciones;
    private static final String CLAVE = "clave-prueba";
    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
    private static BigDecimal n(String valor) { return new BigDecimal(valor); }
    private static char[] clave() { return CLAVE.toCharArray(); }
    private static void comprobar(boolean cumple, String mensaje) {
        verificaciones++; if (!cumple) throw new AssertionError(mensaje);
    }
    private static void igual(BigDecimal actual, String esperado) {
        comprobar(actual != null && actual.compareTo(n(esperado)) == 0,
                "Esperado " + esperado + ", obtenido " + actual);
    }
    private static void rechaza(Accion accion, String mensaje) throws Exception {
        boolean rechazo = false;
        try { accion.ejecutar(); } catch (IllegalArgumentException e) { rechazo = true; }
        comprobar(rechazo, mensaje);
    }
    private static MovimientoInventario registro(ContextoAplicacion contexto, String folio) {
        return contexto.registros().stream().filter(r -> r.folio().equals(folio)).findFirst().orElseThrow();
    }
    private static Map<String, BigDecimal> cantidades(ContextoAplicacion contexto) {
        return new ServicioInventario().cantidadesEstimadas(contexto.productos(), contexto.registros());
    }
    private static void configurar(ContextoAplicacion contexto) throws IOException {
        contexto.autorizacion.cambiar(new char[0], clave(),
                new char[][]{"Luna".toCharArray(), "42".toCharArray(), "2000".toCharArray()});
    }
    public static void main(String[] args) throws Exception {
        Path datos = Files.createTempDirectory("sicov-cancelaciones-");
        Path archivo = datos.resolve("ventas.csv");
        ContextoAplicacion contexto = new ContextoAplicacion(datos);
        Producto huevo = new Producto("001", "Huevo", n("5"), "Abarrotes", UnidadVenta.PIEZA, true);
        Producto queso = new Producto("002", "Queso", n("100"), "Lácteos", UnidadVenta.KILOGRAMO, true);
        contexto.guardarProducto(huevo, false); contexto.guardarProducto(queso, false);
        var entrada = contexto.registrar(MovimientoInventario.Tipo.ENTRADA,
                List.of(DetalleVenta.de(huevo, n("30")), DetalleVenta.de(queso, n("2"))), n("0"), "Recepción");
        var merma = contexto.registrar(MovimientoInventario.Tipo.MERMA,
                List.of(DetalleVenta.de(queso, n("0.1"))), n("0"), "Producto retirado");
        var venta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(huevo, n("6")), DetalleVenta.de(queso, n("0.25"))), n("100"), "Entrega en mostrador");
        var otraVenta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(huevo, n("2"))), n("10"), "");
        List<MovimientoInventario> anteriores = contexto.registros();
        byte[] anterior = Files.readAllBytes(archivo);
        int[] avisos = {0}; contexto.escuchar(() -> avisos[0]++);

        rechaza(() -> contexto.cancelarVenta(venta.folio(), clave(), "Cliente devolvió la compra"),
                "No permite cancelar antes de configurar una contraseña");
        comprobar(contexto.registros().equals(anteriores) && Arrays.equals(anterior, Files.readAllBytes(archivo)),
                "Sin autorización conserva la memoria y el archivo");
        configurar(contexto);
        rechaza(() -> contexto.cancelarVenta(venta.folio(), "incorrecta".toCharArray(), "Compra duplicada"),
                "Rechaza una contraseña incorrecta");
        rechaza(() -> contexto.cancelarVenta(venta.folio(), clave(), "   \n "), "El motivo es obligatorio");
        rechaza(() -> contexto.cancelarVenta(venta.folio(), clave(), null), "El motivo nulo no es válido");
        rechaza(() -> contexto.cancelarVenta(entrada.folio(), clave(), "Error"), "No cancela entradas como ventas");
        rechaza(() -> contexto.cancelarVenta(merma.folio(), clave(), "Error"), "No cancela mermas como ventas");
        rechaza(() -> contexto.cancelarVenta("V-INEXISTENTE", clave(), "Error"), "No cancela un folio inexistente");
        comprobar(contexto.registros().equals(anteriores), "Los intentos rechazados conservan todos los registros");
        comprobar(Arrays.equals(anterior, Files.readAllBytes(archivo)), "Los intentos rechazados no modifican ventas.csv");
        comprobar(avisos[0] == 0, "Los intentos rechazados no notifican cambios inexistentes");
        igual(cantidades(contexto).get(huevo.codigo()), "22");
        igual(cantidades(contexto).get(queso.codigo()), "1.65");
        igual(new ServicioReportes().ventasPorCategoria(contexto.registros()).get("Abarrotes"), "40");

        String motivo = "Cliente devolvió la compra, \"sin abrir\"\nRevisada en mostrador";
        contexto.cancelarVenta(venta.folio(), clave(), motivo);
        MovimientoInventario cancelada = registro(contexto, venta.folio());
        comprobar(cancelada.cancelado(), "Marca la venta como cancelada");
        comprobar(contexto.registros().size() == anteriores.size(), "Conserva la venta en el historial");
        comprobar(cancelada.folio().equals(venta.folio()) && cancelada.fecha().equals(venta.fecha())
                && cancelada.tipo() == venta.tipo(), "Conserva la identidad de la operación");
        comprobar(cancelada.detalles().equals(venta.detalles()), "Conserva cantidades, unidades y precios históricos");
        igual(cancelada.total(), "55"); igual(cancelada.recibido(), "100"); igual(cancelada.cambio(), "45");
        comprobar(cancelada.motivo().contains("Entrega en mostrador")
                && cancelada.motivo().contains("Cancelación: " + motivo), "Conserva el motivo previo y registra la cancelación");
        comprobar(registro(contexto, otraVenta.folio()).equals(otraVenta), "La otra venta permanece vigente e intacta");
        comprobar(registro(contexto, entrada.folio()).equals(entrada) && registro(contexto, merma.folio()).equals(merma),
                "No modifica entradas ni merma");
        comprobar(avisos[0] == 1, "Notifica una vez después de guardar la cancelación");
        Map<String, BigDecimal> resumen = new ServicioReportes().ventasPorCategoria(contexto.registros());
        igual(resumen.get("Abarrotes"), "10");
        comprobar(!resumen.containsKey("Lácteos"), "El reporte excluye los importes cancelados");
        igual(cantidades(contexto).get(huevo.codigo()), "28");
        igual(cantidades(contexto).get(queso.codigo()), "1.9");
        byte[] cancelacionGuardada = Files.readAllBytes(archivo);
        rechaza(() -> contexto.cancelarVenta(venta.folio(), clave(), "Segunda cancelación"), "No cancela dos veces");
        comprobar(registro(contexto, venta.folio()).equals(cancelada)
                && Arrays.equals(cancelacionGuardada, Files.readAllBytes(archivo)) && avisos[0] == 1,
                "La segunda cancelación conserva el primer motivo y el archivo");

        ContextoAplicacion recarga = new ContextoAplicacion(datos);
        comprobar(recarga.registros().equals(contexto.registros()), "Persiste el historial completo, incluido el motivo con comas y saltos");
        igual(cantidades(recarga).get(huevo.codigo()), "28");
        comprobar(recarga.autorizacion.verificar(clave()), "La autorización persiste al reiniciar");
        recarga.cancelarVenta(otraVenta.folio(), clave());
        comprobar(registro(recarga, otraVenta.folio()).cancelado(), "Conserva compatibilidad con la cancelación de dos argumentos");

        comprobarFalloGuardado();
        SwingUtilities.invokeAndWait(() -> {
            try { comprobarInterfaz(); } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("OK: " + verificaciones + " verificaciones de cancelaciones.");
    }

    private static void comprobarFalloGuardado() throws Exception {
        Path datos = Files.createTempDirectory("sicov-cancelacion-fallo-");
        ContextoAplicacion contexto = new ContextoAplicacion(datos);
        configurar(contexto);
        Producto producto = new Producto("001", "Leche", n("25"), "Lácteos", UnidadVenta.PIEZA, false);
        var venta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(producto, n("1"))), n("50"), "");
        Path archivo = datos.resolve("ventas.csv");
        byte[] anterior = Files.readAllBytes(archivo);
        Files.delete(archivo);
        Files.createDirectory(archivo);
        Path bloqueo = archivo.resolve("bloqueo");
        Files.writeString(bloqueo, "Impide sustituir el directorio por el archivo de ventas");
        int[] avisos = {0}; contexto.escuchar(() -> avisos[0]++);
        boolean error = false;
        try { contexto.cancelarVenta(venta.folio(), clave(), "Compra duplicada"); }
        catch (IOException esperada) { error = true; }
        comprobar(error, "Informa el error de persistencia");
        comprobar(registro(contexto, venta.folio()).equals(venta), "El fallo de escritura conserva la venta vigente en memoria");
        comprobar(avisos[0] == 0, "Un guardado fallido no notifica una cancelación");
        comprobar(Files.exists(bloqueo), "El fallo no reemplaza el destino bloqueado");
        Files.delete(bloqueo); Files.delete(archivo); Files.write(archivo, anterior);
        comprobar(registro(new ContextoAplicacion(datos), venta.folio()).equals(venta), "El archivo anterior conserva la venta vigente");
        contexto.cancelarVenta(venta.folio(), clave(), "Compra duplicada");
        comprobar(registro(contexto, venta.folio()).cancelado() && avisos[0] == 1,
                "Permite reintentar la cancelación después de resolver el fallo");
    }

    private static <T> T campo(Object panel, String nombre, Class<T> tipo) throws Exception {
        Field campo = panel.getClass().getDeclaredField(nombre);
        campo.setAccessible(true); return tipo.cast(campo.get(panel));
    }
    private static void seleccionar(JTable tabla, String folio) {
        for (int fila = 0; fila < tabla.getRowCount(); fila++) {
            if (tabla.getValueAt(fila, 0).equals(folio)) {
                tabla.setRowSelectionInterval(fila, fila); return;
            }
        }
        throw new AssertionError("El folio no aparece en la tabla: " + folio);
    }
    private static void comprobarInterfaz() throws Exception {
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-cancelaciones-ui-"));
        Producto queso = new Producto("001", "Queso fresco", n("100"), "Lácteos", UnidadVenta.KILOGRAMO, true);
        var entrada = contexto.registrar(MovimientoInventario.Tipo.ENTRADA,
                List.of(DetalleVenta.de(queso, n("5"))), n("0"), "Recepción");
        var merma = contexto.registrar(MovimientoInventario.Tipo.MERMA,
                List.of(DetalleVenta.de(queso, n("0.1"))), n("0"), "Producto retirado");
        var venta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(queso, n("0.25"))), n("50"), "");
        var otraVenta = contexto.registrar(MovimientoInventario.Tipo.VENTA,
                List.of(DetalleVenta.de(queso, n("0.5"))), n("100"), "");
        int[] abrirAdministracion = {0};
        PanelCancelaciones panel = new PanelCancelaciones(contexto, () -> abrirAdministracion[0]++);
        JTable tabla = campo(panel, "tabla", JTable.class);
        JTextField buscar = campo(panel, "buscar", JTextField.class);
        JComboBox<?> filtro = campo(panel, "filtro", JComboBox.class);
        JTextArea detalle = campo(panel, "detalle", JTextArea.class);
        JButton cancelar = campo(panel, "cancelar", JButton.class);
        JButton configurar = campo(panel, "configurar", JButton.class);
        comprobar(tabla.getRowCount() == 2 && filtro.getSelectedItem().equals("Vigentes"),
                "Cancelaciones inicia con ventas vigentes y excluye entradas y mermas");
        comprobar(tabla.getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION,
                "La pantalla de cancelaciones permite seleccionar una sola venta");
        comprobar(!cancelar.isEnabled() && configurar.isVisible(), "Sin contraseña ofrece configurarla y bloquea la cancelación");
        configurar.doClick(); comprobar(abrirAdministracion[0] == 1, "El botón de configuración abre Administradores");
        PanelReportes reportes = new PanelReportes(contexto);
        JTable tablaReportes = campo(reportes, "tabla", JTable.class);
        JTextField buscarReporte = campo(reportes, "buscar", JTextField.class);
        JTextArea detalleReporte = campo(reportes, "detalle", JTextArea.class);
        JButton cancelarReporte = campo(reportes, "cancelar", JButton.class);
        seleccionar(tablaReportes, venta.folio());
        comprobar(!cancelarReporte.isEnabled(), "Reportes también exige tener contraseña configurada");

        configurar(contexto);
        filtro.setSelectedItem("Todas");
        comprobar(!configurar.isVisible() && cancelar.isEnabled(), "Con contraseña permite cancelar ventas vigentes");
        filtro.setSelectedItem("Canceladas");
        comprobar(tabla.getRowCount() == 0 && !cancelar.isEnabled(), "El filtro canceladas sin resultados desactiva el botón");
        filtro.setSelectedItem("Vigentes");
        comprobar(tabla.getRowCount() == 2, "El filtro vigentes recupera las dos ventas");
        buscar.setText("folio-inexistente");
        comprobar(tabla.getRowCount() == 0 && !cancelar.isEnabled() && detalle.getText().contains("No hay ventas"),
                "Una búsqueda sin resultados explica el estado y no permite cancelar");
        buscar.setText(venta.folio().toLowerCase(Locale.ROOT));
        comprobar(tabla.getRowCount() == 1 && tabla.getValueAt(0, 0).equals(venta.folio()),
                "La búsqueda por folio ignora mayúsculas");
        comprobar(detalle.getText().contains("Queso fresco") && detalle.getText().contains("250 g")
                && detalle.getText().contains("$25.00") && detalle.getText().contains("$50.00"),
                "El detalle muestra producto a granel, importe, recibido y cambio");
        buscar.setText("");

        comprobar(tablaReportes.getRowCount() == 4, "Reportes conserva entradas, ventas y merma");
        seleccionar(tablaReportes, entrada.folio());
        comprobar(!cancelarReporte.isEnabled(), "Reportes no ofrece cancelar una entrada");
        seleccionar(tablaReportes, merma.folio());
        comprobar(!cancelarReporte.isEnabled(), "Reportes no ofrece cancelar una merma");
        seleccionar(tablaReportes, venta.folio());
        comprobar(cancelarReporte.isEnabled(), "Reportes permite cancelar una venta vigente individual");
        int primera = tablaReportes.getSelectedRow();
        seleccionar(tablaReportes, otraVenta.folio());
        tablaReportes.addRowSelectionInterval(primera, primera);
        comprobar(tablaReportes.getSelectedRowCount() == 2 && !cancelarReporte.isEnabled(),
                "Reportes desactiva la cancelación en una selección múltiple");
        seleccionar(tablaReportes, venta.folio());

        String motivo = "El cliente devolvió el queso";
        contexto.cancelarVenta(venta.folio(), clave(), motivo);
        comprobar(tabla.getRowCount() == 1 && tabla.getValueAt(0, 0).equals(otraVenta.folio()),
                "Una cancelación actualiza automáticamente las ventas vigentes");
        comprobar(!cancelarReporte.isEnabled() && detalleReporte.getText().contains("CANCELADA")
                && detalleReporte.getText().contains(motivo), "Reportes recibe el cambio y muestra la cancelación con su motivo");
        comprobar(tablaReportes.getValueAt(tablaReportes.getSelectedRow(), 4).equals("CANCELADA"),
                "Reportes conserva seleccionado el folio y actualiza su estado");
        filtro.setSelectedItem("Canceladas");
        comprobar(tabla.getRowCount() == 1 && tabla.getValueAt(0, 0).equals(venta.folio()) && !cancelar.isEnabled(),
                "El filtro canceladas conserva el historial y evita una segunda cancelación");
        comprobar(detalle.getText().contains(motivo) && detalle.getText().contains("excluida"),
                "El ticket cancelado muestra el motivo y su exclusión de los totales");
        filtro.setSelectedItem("Todas");
        comprobar(tabla.getRowCount() == 2, "Todas muestra ventas vigentes y canceladas sin entradas ni merma");
        buscarReporte.setText("folio-inexistente");
        comprobar(tablaReportes.getRowCount() == 0 && !cancelarReporte.isEnabled()
                && detalleReporte.getText().contains("Aún no hay registros"), "Reportes gestiona una búsqueda sin resultados");
        buscarReporte.setText(venta.folio());
        comprobar(tablaReportes.getRowCount() == 1 && !cancelarReporte.isEnabled()
                && detalleReporte.getText().contains(motivo), "Reportes encuentra también una venta cancelada por folio");
        List<MovimientoInventario> anteriores = contexto.registros();
        cancelarReporte.doClick();
        seleccionar(tabla, venta.folio()); cancelar.doClick();
        comprobar(contexto.registros().equals(anteriores), "Los botones desactivados no modifican el historial");
    }
}
