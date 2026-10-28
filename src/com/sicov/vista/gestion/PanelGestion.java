package com.sicov.vista.gestion;
import com.sicov.vista.UI;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public class PanelGestion extends JPanel {
    public PanelGestion(Consumer<String> navegar) {
        super(new BorderLayout());
        JPanel pagina = UI.pagina("Gestión");
        JPanel botones = new JPanel(new GridLayout(1, 6, 14, 0));
        String[][] secciones = {{"Reportes", "reportes"}, {"Compras", "compras"}, {"Salidas", "salidas"},
                {"Categorías", "categorias"}, {"Descuentos / Promociones", "promociones"}, {"Administradores", "administradores"}};
        for (String[] s : secciones) {
            JButton b = UI.boton("<html><center>" + s[0].replace(" / ", "<br>") + "</center></html>", () -> navegar.accept(s[1]));
            b.setPreferredSize(new Dimension(160, 130)); botones.add(b);
        }
        JPanel contenido = new JPanel(new BorderLayout()); contenido.add(botones, BorderLayout.NORTH);
        pagina.add(contenido); add(pagina);
    }
}
