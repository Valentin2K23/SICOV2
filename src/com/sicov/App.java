package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.vista.*;
import java.nio.file.Path;
import javax.swing.*;

public final class App {
    private App() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UI.tema();
                Path datos = directorioDatos();
                new VentanaPrincipal(new ContextoAplicacion(datos)).setVisible(true);
            } catch (Exception e) { UI.error(null, new Exception("No se pudo abrir SICOV: " + e.getMessage(), e)); }
        });
    }
    public static Path directorioDatos() throws Exception {
        String personalizado = System.getProperty("sicov.datos");
        if (personalizado != null) return Path.of(personalizado);
        Path ubicacion = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path base = java.nio.file.Files.isDirectory(ubicacion) ? ubicacion : ubicacion.getParent();
        if (base.getFileName() != null && (base.getFileName().toString().equals("bin") || base.getFileName().toString().equals("dist")))
            base = base.getParent();
        return base.resolve("datos");
    }
}
