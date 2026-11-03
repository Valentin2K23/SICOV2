package com.sicov.vista.gestion;
import com.sicov.vista.UI;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public class PanelGestion extends JPanel {
    public PanelGestion(Consumer<String> navegar) {
        super(new BorderLayout());
        JPanel pagina = UI.pagina("Gestión");
        JPanel botones = new JPanel(new GridLayout(0, 3, 16, 16));
        String[][] secciones = {{"Reportes", "reportes"}, {"Compras", "compras"}, {"Salidas", "salidas"},
                {"Categorías", "categorias"}, {"Descuentos / Promociones", "promociones"}, {"Administradores", "administradores"},
                {"Cancelar ventas", "cancelaciones"}};
        String[] detalles = {"Consulta y cancelación de ventas", "Productos y recepción de mercancía", "Ventas por categoría y merma", "Organiza productos y unidades", "Paquetes y precios especiales", "Contraseña y recuperación", "Busca una venta y autoriza su cancelación"};
        int indice = 0;
        for (String[] s : secciones) {
            JButton b = UI.boton("<html><center><b>" + s[0].replace(" / ", "<br>") + "</b><br><br><small>" + detalles[indice++] + "</small></center></html>", () -> navegar.accept(s[1]));
            b.setPreferredSize(new Dimension(200, 108)); botones.add(b);
        }
        JPanel contenido = new JPanel(new BorderLayout()); contenido.add(botones, BorderLayout.NORTH);
        pagina.add(contenido); add(pagina);
    }
}
