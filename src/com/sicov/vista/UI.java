package com.sicov.vista;

import java.awt.*;
import java.math.*;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

public final class UI {
    // Verde de abarrotes, crema de papel y rojo como acento.
    public static final Color FONDO = new Color(248, 245, 237), VERDE = new Color(24, 83, 60),
            AZUL = VERDE, ACENTO = new Color(177, 49, 41), TEXTO = new Color(39, 53, 44),
            SUAVE = new Color(232, 240, 231), BORDE = new Color(221, 225, 215),
            MUTED = new Color(108, 119, 110), DORADO = new Color(212, 161, 67);
    private UI() {}
    public static void tema() {
        for (Object clave : UIManager.getDefaults().keySet().toArray())
            if (UIManager.get(clave) instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(clave, new javax.swing.plaf.FontUIResource(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.gridColor", BORDE);
        UIManager.put("TableHeader.background", SUAVE);
        UIManager.put("TableHeader.foreground", VERDE);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("List.selectionBackground", SUAVE);
        UIManager.put("List.selectionForeground", TEXTO);
        UIManager.put("TextField.selectionBackground", SUAVE);
        UIManager.put("Table.selectionBackground", SUAVE);
        UIManager.put("Table.selectionForeground", AZUL);
    }
    public static BigDecimal numero(String texto) {
        try { return new BigDecimal(texto.strip().replace(',', '.')); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Escribe un número válido."); }
    }
    public static String cantidad(BigDecimal n) { return n.stripTrailingZeros().toPlainString(); }
    public static String dinero(BigDecimal n) { return "$" + String.format(Locale.US, "%,.2f", n); }
    public static String html(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    public static JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D dibujo = (Graphics2D) g.create();
                dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fondo = isEnabled() ? getBackground() : SUAVE;
                if (isEnabled() && getModel().isRollover()) fondo = fondo.darker();
                dibujo.setColor(fondo); dibujo.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                dibujo.setColor(isFocusOwner() ? ACENTO : BORDE);
                dibujo.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                dibujo.dispose(); super.paintComponent(g);
            }
        };
        b.setFocusPainted(false); b.setContentAreaFilled(false); b.setOpaque(false);
        b.setRolloverEnabled(true); b.setBackground(Color.WHITE); b.setForeground(VERDE);
        b.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> accion.run()); return b;
    }
    public static JButton primario(String texto, Runnable accion) {
        JButton b = boton(texto, accion); b.setBackground(VERDE); b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD)); return b;
    }
    public static JLabel discreto(String texto) {
        JLabel l = new JLabel(texto); l.setFont(l.getFont().deriveFont(12f)); l.setForeground(MUTED); return l;
    }
    public static String medida(BigDecimal cantidad, com.sicov.modelo.UnidadVenta unidad) {
        if (unidad == com.sicov.modelo.UnidadVenta.KILOGRAMO && cantidad.abs().compareTo(BigDecimal.ONE) < 0)
            return cantidad(cantidad.multiply(new BigDecimal("1000"))) + " g";
        if (unidad == com.sicov.modelo.UnidadVenta.LITRO && cantidad.abs().compareTo(BigDecimal.ONE) < 0)
            return cantidad(cantidad.multiply(new BigDecimal("1000"))) + " ml";
        return cantidad(cantidad) + " " + unidad.simbolo();
    }
    public static JPanel tarjeta() {
        JPanel p = new JPanel(new BorderLayout(12, 12)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16))); return p;
    }
    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto); l.setFont(l.getFont().deriveFont(Font.BOLD, 24f)); l.setForeground(AZUL); return l;
    }
    public static JPanel pagina(String titulo) {
        JPanel p = new JPanel(new BorderLayout(16, 16));
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24)); p.add(titulo(titulo), BorderLayout.NORTH); return p;
    }
    public static void error(Component c, Exception e) {
        JOptionPane.showMessageDialog(c, e.getMessage() == null ? e.toString() : e.getMessage(), "SICOV", JOptionPane.ERROR_MESSAGE);
    }
    public static void escuchar(JTextField campo, Runnable accion) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { accion.run(); }
            public void removeUpdate(DocumentEvent e) { accion.run(); }
            public void changedUpdate(DocumentEvent e) { accion.run(); }
        });
    }
    public static DefaultTableModel tabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    public static JPanel campo(String texto, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false); p.add(new JLabel(texto), BorderLayout.NORTH); p.add(campo, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72)); return p;
    }
    public static void enter(JTextField campo, Runnable accion) { campo.addActionListener(e -> accion.run()); }
}
