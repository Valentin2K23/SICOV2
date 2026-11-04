package com.sicov;

import com.sicov.controlador.*;
import com.sicov.modelo.*;
import com.sicov.vista.UI;
import com.sicov.vista.ventas.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.*;
import javax.swing.*;

/**
 * Interacciones del carrito y del cobro, sin abrir ventanas ni usar datos reales.
 */
public final class PruebasInteraccionVenta {
    private static int verificaciones;

    private static void comprobar(boolean valor, String mensaje) {
        verificaciones++;
        if (!valor)
            throw new AssertionError(mensaje);
    }

    private static <T> T campo(Object objeto, String nombre, Class<T> tipo) throws Exception {
        Field campo = PanelVentas.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return tipo.cast(campo.get(objeto));
    }

    private static JButton boton(Container panel, String texto) {
        for (Component hijo : panel.getComponents()) {
            if (hijo instanceof JButton b && b.getText().equals(texto))
                return b;
            if (hijo instanceof Container c) {
                JButton b = boton(c, texto);
                if (b != null)
                    return b;
            }
        }
        return null;
    }

    private static Runnable atajo(PanelVentas panel, Component origen, int tecla, char caracter, int modificadores)
            throws Exception {
        Method metodo = PanelVentas.class.getDeclaredMethod("accionAtajo", KeyEvent.class);
        metodo.setAccessible(true);
        return (Runnable) metodo.invoke(panel,
                new KeyEvent(origen, KeyEvent.KEY_PRESSED, 0, modificadores, tecla, caracter));
    }

    private static DetalleVenta detalle(VentaControlador controlador, String codigo) {
        return controlador.detalles().stream().filter(d -> d.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static void seleccionar(JList<?> lista, String codigo) {
        for (int i = 0; i < lista.getModel().getSize(); i++) {
            if (((DetalleVenta) lista.getModel().getElementAt(i)).codigo().equals(codigo)) {
                lista.setSelectedIndex(i);
                return;
            }
        }
        throw new AssertionError("No está en el carrito: " + codigo);
    }

    private static void papelera(JList<?> lista) {
        Rectangle fila = lista.getCellBounds(lista.getSelectedIndex(), lista.getSelectedIndex());
        MouseEvent click = new MouseEvent(lista, MouseEvent.MOUSE_CLICKED, 0, 0, fila.x + fila.width - 20,
                fila.y + fila.height / 2, 1, false, MouseEvent.BUTTON1);
        for (MouseListener listener : lista.getMouseListeners())
            listener.mouseClicked(click);
    }

    private static void probarGranel() throws Exception {
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-granel-"));
        contexto.guardarProducto(new Producto("J", "Jamón de pavo", new BigDecimal("120"), "Salchichonería",
                UnidadVenta.KILOGRAMO, true), false);
        contexto.guardarProducto(
                new Producto("Q", "Queso fresco", new BigDecimal("160"), "Lácteos", UnidadVenta.KILOGRAMO, true),
                false);
        contexto.guardarProducto(
                new Producto("H", "Huevo por pieza", new BigDecimal("3"), "Abarrotes", UnidadVenta.PIEZA, true), false);
        contexto.guardarProducto(
                new Producto("L", "Crema a granel", new BigDecimal("40"), "Lácteos", UnidadVenta.LITRO, true), false);
        PanelVentas panel = new PanelVentas(contexto);
        JTextField codigo = campo(panel, "codigo", JTextField.class);
        JTextField cantidad = campo(panel, "cantidad", JTextField.class);
        VentaControlador controlador = campo(panel, "controlador", VentaControlador.class);
        JList<?> lista = campo(panel, "lista", JList.class);
        JButton deshacer = campo(panel, "deshacer", JButton.class);
        codigo.setText("J*0.250");
        codigo.postActionEvent();
        comprobar(cantidad.getText().equals("250"), "La captura de un cuarto de kilo muestra 250 gramos");
        comprobar(campo(panel, "unidadCantidad", JLabel.class).getText().equals("g"),
                "La cantidad indica gramos junto al campo");
        atajo(panel, codigo, KeyEvent.VK_ADD, '+', 0).run();
        comprobar(detalle(controlador, "J").cantidad().compareTo(new BigDecimal("0.350")) == 0,
                "+ agrega 100 gramos al jamón");
        atajo(panel, codigo, KeyEvent.VK_MINUS, '-', 0).run();
        comprobar(detalle(controlador, "J").cantidad().compareTo(new BigDecimal("0.250")) == 0,
                "- resta 100 gramos al jamón");
        atajo(panel, lista, KeyEvent.VK_MINUS, '-', 0).run();
        comprobar(detalle(controlador, "J").cantidad().compareTo(new BigDecimal("0.150")) == 0
                && !controlador.venta.vacia(), "Disminuir un cuarto de kilo conserva 150 gramos en el carrito");
        cantidad.setText("375");
        cantidad.postActionEvent();
        comprobar(detalle(controlador, "J").cantidad().compareTo(new BigDecimal("0.375")) == 0,
                "Enter convierte los 375 gramos editados a 0.375 kilogramos");
        comprobar(detalle(controlador, "J").importe().compareTo(new BigDecimal("45")) == 0,
                "El importe de 375 gramos usa el precio por kilogramo");
        codigo.setText("Q*0.500");
        codigo.postActionEvent();
        codigo.setText("H*6");
        codigo.postActionEvent();
        comprobar(cantidad.getText().equals("6") && detalle(controlador, "H").cantidad().intValueExact() == 6,
                "Los huevos se capturan como seis piezas completas");
        atajo(panel, codigo, KeyEvent.VK_ADD, '+', 0).run();
        comprobar(detalle(controlador, "H").cantidad().intValueExact() == 7, "+ agrega una pieza al huevo");
        atajo(panel, codigo, KeyEvent.VK_MINUS, '-', 0).run();
        comprobar(detalle(controlador, "H").cantidad().intValueExact() == 6, "- resta una pieza al huevo");
        codigo.setText("L*0.500");
        codigo.postActionEvent();
        comprobar(
                cantidad.getText().equals("500") && campo(panel, "unidadCantidad", JLabel.class).getText().equals("ml"),
                "Medio litro se edita como 500 mililitros");
        atajo(panel, codigo, KeyEvent.VK_ADD, '+', 0).run();
        comprobar(detalle(controlador, "L").cantidad().compareTo(new BigDecimal("0.600")) == 0,
                "+ agrega 100 mililitros a los productos por litro");
        cantidad.setText("500");
        cantidad.postActionEvent();
        comprobar(detalle(controlador, "L").cantidad().compareTo(new BigDecimal("0.500")) == 0,
                "Enter convierte mililitros a litros sin alterar el precio base");
        comprobar(controlador.detalles().size() == 4 && controlador.total().compareTo(new BigDecimal("163")) == 0,
                "Carrito mixto: jamón45 + queso80 + huevo18 + crema20 = 163 pesos");
        comprobar(UI.medida(new BigDecimal("0.250"), UnidadVenta.KILOGRAMO).equals("250 g"),
                "Las cantidades pequeñas de peso se leen en gramos");
        comprobar(UI.medida(new BigDecimal("0.500"), UnidadVenta.LITRO).equals("500 ml"),
                "Las cantidades pequeñas de volumen se leen en mililitros");
        comprobar(UI.medida(new BigDecimal("1.250"), UnidadVenta.KILOGRAMO).equals("1.25 kg"),
                "Las cantidades mayores conservan la unidad kilogramo");
        comprobar(
                PanelAccesosRapidos.normalizar("  JAMÓN DE PAVO  ").equals("jamon de pavo")
                        && PanelAccesosRapidos.normalizar("Lácteos").equals("lacteos"),
                "La búsqueda por nombre o categoría ignora acentos y mayúsculas");
        lista.setSize(700, 400);
        seleccionar(lista, "Q");
        papelera(lista);
        comprobar(
                controlador.detalles().size() == 3 && deshacer.isVisible()
                        && controlador.total().compareTo(new BigDecimal("83")) == 0,
                "La papelera quita únicamente el queso y ofrece Deshacer");
        deshacer.doClick();
        comprobar(
                controlador.detalles().size() == 4
                        && detalle(controlador, "Q").cantidad().compareTo(new BigDecimal("0.500")) == 0
                        && controlador.total().compareTo(new BigDecimal("163")) == 0 && !deshacer.isVisible(),
                "Deshacer recupera la cantidad y el importe originales del queso");
        deshacer.doClick();
        comprobar(controlador.total().compareTo(new BigDecimal("163")) == 0 && contexto.registros().isEmpty(),
                "Deshacer repetido no duplica productos ni registra ventas");
        seleccionar(lista, "H");
        papelera(lista);
        MovimientoInventario venta = controlador.confirmar(true);
        comprobar(venta.total().compareTo(new BigDecimal("145")) == 0 && contexto.registros().size() == 1,
                "El ticket mixto registra solo los productos conservados en el carrito");
        deshacer.doClick();
        comprobar(
                controlador.venta.calcular(contexto.promociones()).size() == 3
                        && controlador.total().compareTo(new BigDecimal("145")) == 0,
                "Deshacer no modifica el carrito mientras el ticket ya guardado está abierto");
        comprobar(
                atajo(panel, lista, KeyEvent.VK_ADD, '+', 0) == null
                        && atajo(panel, lista, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED, 0) == null,
                "Los atajos de cantidad y eliminación se bloquean durante el ticket");
        controlador.cerrarTicket();
        comprobar(controlador.venta.vacia() && controlador.cobro.recibido().signum() == 0,
                "Cerrar el ticket mixto deja lista la siguiente venta");
    }

    private static void probarImporte() throws Exception {
        Path directorio = Files.createTempDirectory("sicov-importe-");
        ContextoAplicacion contexto = new ContextoAplicacion(directorio);
        Producto huevo = new Producto("HP", "Huevo por kilogramo", new BigDecimal("38"), "Abarrotes",
                UnidadVenta.KILOGRAMO, true);
        contexto.guardarProducto(huevo, false);
        BigDecimal peso = PanelCantidad.convertir(huevo, new BigDecimal("20"), "pesos");
        PanelVentas panel = new PanelVentas(contexto);
        JTextField codigo = campo(panel, "codigo", JTextField.class);
        JTextField cantidad = campo(panel, "cantidad", JTextField.class);
        VentaControlador controlador = campo(panel, "controlador", VentaControlador.class);
        JList<?> lista = campo(panel, "lista", JList.class);
        codigo.setText("HP*" + peso.toPlainString());
        codigo.postActionEvent();
        comprobar(controlador.total().compareTo(new BigDecimal("20.00")) == 0,
                "Veinte pesos de huevo a 38 por kilo conservan los veinte pesos en el carrito");
        comprobar(cantidad.getText().equals(UI.cantidad(peso.movePointRight(3))),
                "El editor conserva todos los decimales del peso capturado por importe");
        comprobar(UI.medida(peso, UnidadVenta.KILOGRAMO).equals("≈ 526.316 g"),
                "La etiqueta aproxima el peso de forma legible sin modificar la cantidad editable");
        panel.activarAtajos(false);
        comprobar(
                atajo(panel, lista, KeyEvent.VK_B, 'b', 0) == null
                        && atajo(panel, lista, KeyEvent.VK_ADD, '+', 0) == null
                        && atajo(panel, lista, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED, 0) == null,
                "El menú de Gestión bloquea la búsqueda y las modificaciones por atajo de la venta de fondo");
        panel.activarAtajos(true);
        comprobar(atajo(panel, codigo, KeyEvent.VK_B, 'b', 0) != null, "Cerrar Gestión restablece los atajos de venta");
        cantidad.postActionEvent();
        comprobar(
                detalle(controlador, "HP").cantidad().compareTo(peso) == 0
                        && controlador.total().compareTo(new BigDecimal("20.00")) == 0,
                "Confirmar el editor sin cambios conserva el peso y los veinte pesos completos");
        lista.setSize(700, 100);
        papelera(lista);
        comprobar(controlador.venta.vacia(), "Eliminar el huevo retira su importe del carrito");
        campo(panel, "deshacer", JButton.class).doClick();
        comprobar(
                detalle(controlador, "HP").cantidad().compareTo(peso) == 0
                        && controlador.total().compareTo(new BigDecimal("20.00")) == 0,
                "Deshacer recupera el peso preciso y el importe solicitado");
        PanelCobro cobro = campo(panel, "cobro", PanelCobro.class);
        cobro.efectivo.setText("20");
        comprobar(boton(cobro, "Confirmar pago y ver ticket").isEnabled(),
                "Veinte pesos de efectivo cubren el huevo vendido por veinte pesos");
        boton(cobro, "Confirmar pago y ver ticket").doClick(0);
        MovimientoInventario venta = controlador.ticketPendiente();
        comprobar(
                venta.total().compareTo(new BigDecimal("20.00")) == 0
                        && venta.recibido().compareTo(new BigDecimal("20.00")) == 0 && venta.cambio().signum() == 0,
                "El cobro guarda total y efectivo de veinte pesos con cambio cero");
        comprobar(
                PanelTicket.texto(venta).contains("TOTAL:    $20.00")
                        && PanelTicket.texto(venta).contains("CAMBIO:   $0.00"),
                "El ticket presenta el importe solicitado sin perder ni devolver un centavo");
        MovimientoInventario guardada = new ContextoAplicacion(directorio).registros().get(0);
        comprobar(
                guardada.detalles().get(0).cantidad().compareTo(peso) == 0
                        && guardada.total().compareTo(new BigDecimal("20.00")) == 0,
                "Reabrir los archivos conserva el peso preciso y los veinte pesos cobrados");
    }

    private static void probarConfirmacion() throws Exception {
        Path directorio = Files.createTempDirectory("sicov-confirmacion-");
        ContextoAplicacion contexto = new ContextoAplicacion(directorio);
        contexto.guardarProducto(
                new Producto("CP", "Producto para cobro", new BigDecimal("25"), "Abarrotes", UnidadVenta.PIEZA, false),
                false);
        PanelVentas panel = new PanelVentas(contexto);
        JTextField codigo = campo(panel, "codigo", JTextField.class);
        VentaControlador controlador = campo(panel, "controlador", VentaControlador.class);
        PanelCobro cobro = campo(panel, "cobro", PanelCobro.class);
        JButton confirmar = boton(cobro, "Confirmar pago y ver ticket");
        String[] formas = { "efectivo escrito", "billete", "importe exacto", "Enter" };
        for (int i = 0; i < formas.length; i++) {
            codigo.setText("CP");
            codigo.postActionEvent();
            if (i == 0)
                for (String efectivo : new String[] { "importe inválido", "24.99", "-1", "100.001" }) {
                    cobro.efectivo.setText(efectivo);
                    comprobar(!confirmar.isEnabled(), "El pago rechaza efectivo inválido o insuficiente: " + efectivo);
                    confirmar.doClick(0);
                    cobro.efectivo.postActionEvent();
                    comprobar(contexto.registros().isEmpty() && !controlador.tieneTicketPendiente(),
                            "Ni clic ni Enter registran una venta con efectivo inválido o insuficiente");
                }
            if (i == 1)
                boton(cobro, "$100").doClick(0);
            else if (i == 2)
                boton(cobro, "Importe exacto").doClick(0);
            else
                cobro.efectivo.setText("100");
            comprobar(confirmar.isEnabled() && contexto.registros().size() == i && !controlador.tieneTicketPendiente(),
                    "Capturar " + formas[i] + " habilita confirmar sin registrar todavía la venta");
            if (i == 3)
                cobro.efectivo.postActionEvent();
            else
                confirmar.doClick(0);
            comprobar(contexto.registros().size() == i + 1 && controlador.tieneTicketPendiente(),
                    "La confirmación real con " + formas[i] + " guarda la venta y prepara el ticket");
            MovimientoInventario venta = controlador.ticketPendiente();
            BigDecimal recibido = new BigDecimal(i == 2 ? "25" : "100");
            comprobar(
                    venta.total().compareTo(new BigDecimal("25")) == 0 && venta.recibido().compareTo(recibido) == 0
                            && venta.cambio().compareTo(recibido.subtract(venta.total())) == 0,
                    "El ticket conserva el total, efectivo y cambio del pago con " + formas[i]);
            String ticket = PanelTicket.texto(venta);
            comprobar(ticket.contains(venta.folio()) && ticket.contains("Producto para cobro")
                    && ticket.contains("TOTAL:    $25.00") && ticket.contains("EFECTIVO: " + UI.dinero(recibido))
                    && ticket.contains("CAMBIO:   " + UI.dinero(venta.cambio())),
                    "El ticket muestra los datos de la venta confirmada con " + formas[i]);
            confirmar.doClick(0);
            cobro.efectivo.postActionEvent();
            comprobar(
                    contexto.registros().size() == i + 1 && controlador.ticketPendiente().folio().equals(venta.folio()),
                    "Repetir clic o Enter conserva una sola venta y el mismo ticket");
            MovimientoInventario guardada = new ContextoAplicacion(directorio).registros().get(i);
            comprobar(
                    guardada.folio().equals(venta.folio()) && guardada.recibido().compareTo(recibido) == 0
                            && guardada.cambio().compareTo(venta.cambio()) == 0,
                    "El pago con " + formas[i] + " queda guardado al reabrir los archivos");
            controlador.cerrarTicket();
            comprobar(controlador.venta.vacia() && !controlador.tieneTicketPendiente(),
                    "Cerrar el ticket permite comenzar la siguiente venta");
        }
    }

    public static void main(String[] args) throws Exception {
        ContextoAplicacion contexto = new ContextoAplicacion(Files.createTempDirectory("sicov-interacciones-"));
        contexto.guardarProducto(
                new Producto("123", "Leche", new BigDecimal("25"), "Lácteos", UnidadVenta.PIEZA, false), false);
        PanelVentas[] vista = new PanelVentas[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                probarGranel();
                probarImporte();
                probarConfirmacion();
                PanelVentas panel = new PanelVentas(contexto) {
                    @Override
                    public boolean isShowing() {
                        return true;
                    }
                };
                vista[0] = panel;
                JTextField codigo = campo(panel, "codigo", JTextField.class);
                JTextField cantidad = campo(panel, "cantidad", JTextField.class);
                VentaControlador controlador = campo(panel, "controlador", VentaControlador.class);
                JList<?> lista = campo(panel, "lista", JList.class);
                comprobar(atajo(panel, codigo, KeyEvent.VK_B, 'b', 0) != null, "B abre búsqueda con código vacío");
                codigo.setText("AB");
                comprobar(atajo(panel, codigo, KeyEvent.VK_B, 'b', 0) == null,
                        "No intercepta letras de un código en edición");
                comprobar(atajo(panel, cantidad, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED, 0) == null,
                        "Delete respeta edición de cantidad");
                codigo.setText("123*2");
                codigo.postActionEvent();
                comprobar(lista.getSelectedIndex() == 0 && codigo.getText().isEmpty(),
                        "Selecciona el producto agregado sin llenar el campo de código");
                atajo(panel, codigo, KeyEvent.VK_ADD, '+', 0).run();
                comprobar(controlador.detalles().get(0).cantidad().intValueExact() == 3,
                        "+ aumenta producto seleccionado desde campo vacío");
                atajo(panel, lista, KeyEvent.VK_MINUS, '-', 0).run();
                comprobar(controlador.detalles().get(0).cantidad().intValueExact() == 2,
                        "- disminuye producto seleccionado");
                comprobar(atajo(panel, codigo, KeyEvent.VK_B, 'b', InputEvent.CTRL_DOWN_MASK) == null,
                        "No intercepta combinaciones de edición");
                atajo(panel, lista, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED, 0).run();
                comprobar(controlador.venta.vacia(), "Delete elimina sin registrar una venta");
                codigo.setText("123");
                codigo.postActionEvent();
                lista.setSize(700, 100);
                MouseEvent click = new MouseEvent(lista, MouseEvent.MOUSE_CLICKED, 0, 0, 680, 40, 1, false,
                        MouseEvent.BUTTON1);
                for (MouseListener listener : lista.getMouseListeners())
                    listener.mouseClicked(click);
                comprobar(controlador.venta.vacia(), "La papelera retira el producto con el ratón");
                codigo.setText("123");
                codigo.postActionEvent();
                PanelCobro cobro = campo(panel, "cobro", PanelCobro.class);
                boton(cobro, "$50").doClick();
                comprobar(!controlador.venta.vacia() && contexto.registros().isEmpty(),
                        "Agregar efectivo no guarda ni limpia el carrito");
                comprobar(boton(cobro, "Confirmar pago y ver ticket").isEnabled(),
                        "Pago suficiente habilita confirmación");
                boton(cobro, "$100").doClick();
                boton(cobro, "$50").doClick();
                comprobar(controlador.cobro.recibido().compareTo(new BigDecimal("50")) == 0,
                        "Elegir otro billete cambia el total recibido sin acumular el importe anterior");
                cobro.efectivo.setText("10");
                comprobar(!boton(cobro, "Confirmar pago y ver ticket").isEnabled(),
                        "El efectivo escrito insuficiente impide confirmar");
                cobro.efectivo.setText("100");
                comprobar(
                        boton(cobro, "Confirmar pago y ver ticket").isEnabled() && contexto.registros().isEmpty()
                                && !controlador.venta.vacia(),
                        "Escribir efectivo suficiente habilita el pago sin guardar ni limpiar la venta");
                comprobar(
                        java.util.Arrays.stream(cobro.getComponents())
                                .anyMatch(c -> c instanceof JLabel etiqueta && etiqueta.getText().contains("Cambio:")
                                        && etiqueta.getText().contains("$75.00")),
                        "Escribir 100 pesos previsualiza los 75 pesos de cambio antes de confirmar");
                boton(cobro, "Borrar efectivo").doClick();
                comprobar(controlador.cobro.recibido().signum() == 0 && !controlador.venta.vacia(),
                        "Borrar efectivo conserva productos");
                boton(cobro, "Importe exacto").doClick();
                comprobar(contexto.registros().isEmpty(), "Importe exacto tampoco cobra automáticamente");
                MovimientoInventario venta = controlador.confirmar(false);
                String ticket = PanelTicket.texto(venta);
                comprobar(
                        ticket.contains(venta.folio()) && ticket.contains("Leche") && ticket.contains("TOTAL:")
                                && ticket.contains("EFECTIVO:") && ticket.contains("CAMBIO:"),
                        "Ticket incluye operación, productos y desglose del pago");
                comprobar(!controlador.venta.vacia() && controlador.tieneTicketPendiente(),
                        "Carrito permanece mientras se revisa ticket");
                comprobar(atajo(panel, lista, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED, 0) == null,
                        "No permite editar después de guardar");
                controlador.confirmar(false);
                comprobar(contexto.registros().size() == 1, "Confirmar repetidamente guarda una sola venta");
                controlador.cerrarTicket();
                controlador.cerrarTicket();
                comprobar(controlador.venta.vacia() && controlador.cobro.recibido().signum() == 0,
                        "Cerrar ticket limpia una sola vez");
                codigo.setText("123");
                codigo.postActionEvent();
                KeyEventDispatcher dispatcher = campo(panel, "atajo", KeyEventDispatcher.class);
                KeyEvent pulsacion = new KeyEvent(codigo, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ADD, '+');
                comprobar(dispatcher.dispatchKeyEvent(pulsacion) && pulsacion.isConsumed(),
                        "Dispatcher intercepta + antes de la edición del campo");
                KeyEvent caracter = new KeyEvent(codigo, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, '+');
                comprobar(dispatcher.dispatchKeyEvent(caracter) && caracter.isConsumed(),
                        "El atajo no escribe + accidentalmente en código");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // La acción del dispatcher se encola en el EDT; esta llamada espera a que se
        // ejecute.
        SwingUtilities.invokeAndWait(() -> {
            try {
                VentaControlador controlador = campo(vista[0], "controlador", VentaControlador.class);
                comprobar(controlador.detalles().get(0).cantidad().intValueExact() == 2,
                        "La pulsación procesada por el dispatcher modifica la cantidad");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("OK: " + verificaciones + " verificaciones de interacción de ventas.");
    }
}
