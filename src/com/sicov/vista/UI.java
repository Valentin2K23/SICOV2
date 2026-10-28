package com.sicov.vista;

import java.awt.*;
import java.math.*;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

public final class UI {
    public static final Color FONDO = new Color(243, 246, 249), AZUL = new Color(24, 49, 67), ACENTO = new Color(0, 120, 123);
    private UI() {}
    public static void tema() {
        for (Object clave : UIManager.getDefaults().keySet().toArray())
            if (UIManager.get(clave) instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(clave, new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 15));
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.selectionBackground", new Color(204, 233, 235));
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
        JButton b = new JButton(texto); b.setFocusPainted(false);
        b.setMargin(new Insets(9, 16, 9, 16)); b.addActionListener(e -> accion.run()); return b;
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
        p.add(new JLabel(texto), BorderLayout.NORTH); p.add(campo, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72)); return p;
    }
    public static void enter(JTextField campo, Runnable accion) { campo.addActionListener(e -> accion.run()); }
}
