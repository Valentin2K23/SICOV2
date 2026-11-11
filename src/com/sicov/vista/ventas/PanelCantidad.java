package com.sicov.vista.ventas;

import com.sicov.modelo.*;
import com.sicov.vista.UI;

import java.awt.*;
import java.awt.event.*;
import java.math.*;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Captura piezas, peso, volumen o importe y devuelve cantidades en la unidad del catálogo.
 */
public final class PanelCantidad extends JPanel {
    private final Producto producto;
    private final JTextField entrada = new JTextField(10);
    private final JLabel unidad = new JLabel(), ayuda = new JLabel(), resumen = new JLabel(), importe = new JLabel(),
            aviso = new JLabel(" ");
    private final JButton agregar;
    private final JPanel modos = new JPanel(new GridLayout(1, 0, 8, 0));
    private String modo;

    private PanelCantidad(Producto producto, BigDecimal inicial, Consumer<BigDecimal> aceptar) {
        super(new BorderLayout(0, 18));
        this.producto = producto;
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        modo = producto.unidad() == UnidadVenta.PIEZA ? "piezas"
                : producto.unidad() == UnidadVenta.KILOGRAMO ? "gramos" : "mililitros";
        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 7));
        cabecera.setOpaque(false);
        cabecera.add(UI.titulo("¿Cuánto vas a vender?"));
        JLabel nombre = new JLabel("<html>" + UI.html(producto.nombre()) + "</html>");
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 20f));
        cabecera.add(nombre);
        cabecera.add(new JLabel(UI.dinero(producto.precio()) + " por " + switch (producto.unidad()) {
            case PIEZA -> "pieza";
            case KILOGRAMO -> "kilogramo";
            case LITRO -> "litro";
        }));
        add(cabecera, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        modos.setOpaque(false);
        if (producto.unidad() == UnidadVenta.PIEZA)
            modos.add(new JLabel("Cantidad de piezas"));
        else {
            modo("gramos", "Gramos", producto.unidad() == UnidadVenta.KILOGRAMO);
            modo("kilos", "Kilos", producto.unidad() == UnidadVenta.KILOGRAMO);
            modo("mililitros", "Mililitros", producto.unidad() == UnidadVenta.LITRO);
            modo("litros", "Litros", producto.unidad() == UnidadVenta.LITRO);
            modo("pesos", "Por importe $", producto.precio().signum() > 0);
        }
        centro.add(modos, BorderLayout.NORTH);
        JPanel captura = new JPanel(new BorderLayout(10, 9));
        captura.setOpaque(false);
        UI.soloNumeros(entrada);
        entrada.setFont(entrada.getFont().deriveFont(Font.BOLD, 34f));
        entrada.setMargin(new Insets(10, 12, 10, 12));
        entrada.getAccessibleContext().setAccessibleName("Cantidad del producto");
        unidad.setFont(unidad.getFont().deriveFont(Font.BOLD, 22f));
        unidad.setForeground(UI.VERDE);
        captura.add(entrada);
        captura.add(unidad, BorderLayout.EAST);
        ayuda.setFont(ayuda.getFont().deriveFont(12f));
        captura.add(ayuda, BorderLayout.NORTH);
        JPanel frecuentes = new JPanel(new GridLayout(1, 0, 7, 0));
        frecuentes.setOpaque(false);
        int[] cantidades = producto.unidad() == UnidadVenta.PIEZA ? new int[] { 1, 6, 12 }
                : new int[] { 100, 250, 500, 1000 };
        for (int n : cantidades) {
            String etiqueta = producto.unidad() == UnidadVenta.PIEZA ? n + " pzas"
                    : producto.unidad() == UnidadVenta.KILOGRAMO ? (n == 1000 ? "1 kg" : n + " g")
                            : (n == 1000 ? "1 L" : n + " ml");
            frecuentes.add(UI.boton(etiqueta, () -> {
                modo = producto.unidad() == UnidadVenta.PIEZA ? "piezas"
                        : producto.unidad() == UnidadVenta.KILOGRAMO ? "gramos" : "mililitros";
                entrada.setText(Integer.toString(n));
                actualizar();
                enfocar();
            }));
        }
        captura.add(frecuentes, BorderLayout.SOUTH);
        centro.add(captura);
        JPanel vistaPrevia = UI.tarjeta();
        resumen.setFont(resumen.getFont().deriveFont(Font.BOLD, 19f));
        vistaPrevia.add(resumen, BorderLayout.NORTH);
        importe.setFont(importe.getFont().deriveFont(Font.BOLD, 26f));
        importe.setForeground(UI.VERDE);
        vistaPrevia.add(importe);
        aviso.setFont(aviso.getFont().deriveFont(12f));
        aviso.setForeground(UI.ACENTO);
        vistaPrevia.add(aviso, BorderLayout.SOUTH);
        centro.add(vistaPrevia, BorderLayout.SOUTH);
        add(centro);
        agregar = UI.primario("Agregar a la venta",
                () -> aceptar.accept(convertir(producto, UI.numero(entrada.getText()), modo)));
        UI.enter(entrada, () -> {
            if (agregar.isEnabled())
                agregar.doClick();
        });
        UI.escuchar(entrada, this::actualizar);
        BigDecimal valor = inicial == null || inicial.signum() <= 0 ? BigDecimal.ONE : inicial;
        entrada.setText(
                UI.cantidad(modo.equals("gramos") || modo.equals("mililitros") ? valor.movePointRight(3) : valor));
        actualizar();
    }

    private void modo(String clave, String texto, boolean mostrar) {
        if (!mostrar)
            return;
        JButton boton = UI.boton(texto, () -> {
            BigDecimal anterior = null;
            try {
                anterior = convertir(producto, UI.numero(entrada.getText()), modo);
            } catch (IllegalArgumentException e) { /* La edición puede estar incompleta. */ }
            modo = clave;
            if (anterior != null)
                entrada.setText(UI.cantidad(switch (modo) {
                    case "gramos", "mililitros" -> anterior.movePointRight(3);
                    case "pesos" -> anterior.multiply(producto.precio()).setScale(2, RoundingMode.HALF_UP);
                    default -> anterior;
                }));
            actualizar();
            enfocar();
        });
        boton.setActionCommand(clave);
        modos.add(boton);
    }

    private void enfocar() {
        entrada.requestFocusInWindow();
        entrada.selectAll();
    }

    private void actualizar() {
        unidad.setText(switch (modo) {
            case "piezas" -> "pzas";
            case "gramos" -> "g";
            case "kilos" -> "kg";
            case "mililitros" -> "ml";
            case "litros" -> "L";
            default -> "$";
        });
        ayuda.setText(modo.equals("pesos") ? "¿Cuántos pesos quiere llevar? Se respeta el importe indicado."
                : producto.unidad() == UnidadVenta.PIEZA ? "Escribe cuántas piezas quiere llevar."
                        : "Escribe el peso o volumen que marca la báscula o el medidor.");
        for (Component c : modos.getComponents())
            if (c instanceof JButton b) {
                boolean elegido = b.getActionCommand().equals(modo);
                b.setBackground(elegido ? UI.VERDE : Color.WHITE);
                b.setForeground(elegido ? Color.WHITE : UI.VERDE);
            }
        try {
            BigDecimal cantidad = convertir(producto, UI.numero(entrada.getText()), modo);
            // La medida visible puede ser aproximada; no se reutiliza para calcular el
            // importe.
            resumen.setText(UI.medida(cantidad, producto.unidad()));
            importe.setText(
                    "Importe: " + UI.dinero(cantidad.multiply(producto.precio()).setScale(2, RoundingMode.HALF_UP)));
            aviso.setText(modo.equals("pesos") ? "El peso o volumen mostrado es aproximado." : " ");
            aviso.setForeground(new Color(102, 115, 105));
            agregar.setEnabled(true);
        } catch (IllegalArgumentException e) {
            resumen.setText("Cantidad por confirmar");
            importe.setText("Importe: —");
            aviso.setText(entrada.getText().isBlank() ? "Escribe una cantidad." : e.getMessage());
            aviso.setForeground(UI.ACENTO);
            agregar.setEnabled(false);
        }
    }

    /**
     * Convierte piezas, gramos, kilos, mililitros, litros o pesos a la unidad del producto.
     */
    public static BigDecimal convertir(Producto producto, BigDecimal entrada, String modo) {
        if (producto == null || entrada == null || entrada.signum() <= 0)
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        if (modo == null)
            throw new IllegalArgumentException("Selecciona cómo capturar la cantidad.");
        BigDecimal cantidad = switch (modo.strip().toLowerCase(Locale.ROOT)) {
            case "piezas" -> {
                if (producto.unidad() != UnidadVenta.PIEZA)
                    throw new IllegalArgumentException("Este producto no se vende por pieza.");
                yield entrada;
            }
            case "gramos", "kilos" -> {
                if (producto.unidad() != UnidadVenta.KILOGRAMO)
                    throw new IllegalArgumentException("Este producto no se vende por peso.");
                yield modo.strip().equalsIgnoreCase("gramos") ? entrada.movePointLeft(3) : entrada;
            }
            case "mililitros", "litros" -> {
                if (producto.unidad() != UnidadVenta.LITRO)
                    throw new IllegalArgumentException("Este producto no se vende por volumen.");
                yield modo.strip().equalsIgnoreCase("mililitros") ? entrada.movePointLeft(3) : entrada;
            }
            case "pesos" -> {
                if (producto.unidad() == UnidadVenta.PIEZA || producto.precio().signum() <= 0)
                    throw new IllegalArgumentException(
                            "El importe requiere un producto a granel con precio mayor que cero.");
                if (entrada.stripTrailingZeros().scale() > 2)
                    throw new IllegalArgumentException("El importe debe tener máximo dos decimales.");
                // Conserva precisión hasta el cobro: truncar a gramos perdía centavos.
                // CEILING evita quedar justo debajo de un umbral al acumular cantidades.
                int escala = Math.max(12, producto.precio().precision() - producto.precio().scale() + 10);
                yield entrada.divide(producto.precio(), escala, RoundingMode.CEILING);
            }
            default -> throw new IllegalArgumentException("Selecciona una unidad válida.");
        };
        producto.validarCantidad(cantidad);
        return cantidad;
    }

    /** Devuelve null si se cancela; cerrar el diálogo nunca altera el carrito. */
    public static BigDecimal pedir(Component padre, Producto producto, BigDecimal inicial) {
        return pedir(padre, producto, inicial, "Agregar a la venta");
    }

    public static BigDecimal pedir(Component padre, Producto producto, BigDecimal inicial, String accion) {
        Window ventana = padre instanceof Window w ? w : padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(ventana, "Cantidad · " + producto.nombre(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        BigDecimal[] resultado = new BigDecimal[1];
        PanelCantidad panel = new PanelCantidad(producto, inicial, valor -> {
            resultado[0] = valor;
            dialogo.dispose();
        });
        panel.agregar.setText(accion);
        JPanel acciones = new JPanel(new GridLayout(1, 2, 10, 0));
        acciones.setOpaque(false);
        acciones.add(UI.boton("Cancelar", dialogo::dispose));
        acciones.add(panel.agregar);
        JPanel pie = new JPanel(new BorderLayout(0, 9));
        pie.setOpaque(false);
        pie.add(acciones);
        JLabel teclas = new JLabel("Enter confirmar cantidad · Esc cancelar", SwingConstants.CENTER);
        teclas.setFont(teclas.getFont().deriveFont(11f));
        teclas.setForeground(new Color(110, 119, 109));
        pie.add(teclas, BorderLayout.SOUTH);
        panel.add(pie, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dialogo.add(scroll);
        dialogo.getRootPane().setDefaultButton(panel.agregar);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                panel.enfocar();
            }
        });
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        dialogo.pack();
        dialogo.setSize(Math.min(590, pantalla.width - 40), Math.min(dialogo.getHeight() + 16, pantalla.height - 80));
        dialogo.setLocationRelativeTo(padre);
        dialogo.setVisible(true);
        return resultado[0];
    }
}
