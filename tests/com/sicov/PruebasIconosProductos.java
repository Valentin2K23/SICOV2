package com.sicov;

import com.sicov.modelo.Producto;
import com.sicov.modelo.UnidadVenta;
import com.sicov.vista.ImagenProducto;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import javax.imageio.ImageIO;
import javax.swing.Icon;

/** Fotos e iconos usando imágenes sintéticas y datos temporales, sin abrir ventanas. */
public final class PruebasIconosProductos {
    private static int verificaciones;

    private static void comprobar(boolean condicion, String mensaje) {
        verificaciones++;
        if (!condicion) throw new AssertionError(mensaje);
    }
    private static Producto producto(String categoria) {
        return new Producto("H", "Huevo", new BigDecimal("40.00"), categoria, UnidadVenta.KILOGRAMO, true);
    }
    private static void probarFamilias() {
        String[][] familias = {
                {"FRUTAS Y VERDURAS", "frutas"}, {"LÁCTEOS", "lacteos"},
                {"Salchichonería", "carnes"}, {"Jamón", "carnes"}, {"HUEVOS", "huevos"},
                {"Jugos y néctares", "bebidas"}, {"Refrescos", "bebidas"}, {"Alcohol", "alcohol"},
                {"Pan", "pan"}, {"Galletas", "pan"}, {"Dulces y chocolates", "dulces"},
                {"Detergentes", "limpieza"}, {"Papel higiénico", "higiene"},
                {"Arroz y cereales", "granos"}, {"Frijoles", "granos"}, {"Botana", "botanas"},
                {"Tecnología", "tecnologia"}, {"Farmacia", "farmacia"}, {"Helados", "helados"},
                {"Tabacos", "tabacos"}, {"Juguetes", "juguetes"}, {"Atún y conservas", "abarrotes"}
        };
        for (String[] ejemplo : familias)
            comprobar(ImagenProducto.familia(ejemplo[0], "").equals(ejemplo[1]),
                    "Reconoce la categoría: " + ejemplo[0]);
        comprobar(ImagenProducto.familia("FRUTAS Y VERDURAS", "Lácteos").equals("frutas"),
                "La categoría específica tiene prioridad sobre la sección");
        comprobar(ImagenProducto.familia("Nombre personalizado", "Lácteos").equals("lacteos"),
                "La sección cubre categorías personalizadas");
        comprobar(ImagenProducto.familia("Desconocida", "Sin sección").equals("abarrotes"),
                "Una categoría desconocida usa el símbolo general");
        comprobar(ImagenProducto.familia(null, null).equals("abarrotes"),
                "La falta de categoría y sección permite un símbolo general");
    }
    private static BufferedImage imagen(Color color, int ancho, int alto) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(color); g.fillRect(0, 0, ancho, alto); g.dispose();
        return imagen;
    }
    private static BufferedImage pintar(Icon icono) {
        BufferedImage imagen = new BufferedImage(icono.getIconWidth(), icono.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics(); icono.paintIcon(null, g, 0, 0); g.dispose();
        return imagen;
    }
    private static boolean parecido(int pixel, Color color) {
        Color actual = new Color(pixel, true);
        return actual.getAlpha() > 240 && Math.abs(actual.getRed() - color.getRed()) < 25
                && Math.abs(actual.getGreen() - color.getGreen()) < 25
                && Math.abs(actual.getBlue() - color.getBlue()) < 25;
    }
    private static void probarFotografias(Path carpeta) throws Exception {
        Producto producto = producto("FRUTAS Y VERDURAS");
        Path foto = carpeta.resolve("vertical.png");
        ImageIO.write(imagen(Color.RED, 40, 120), "png", foto.toFile());
        comprobar(ImagenProducto.esImagenValida(foto), "Una foto PNG es válida");
        Icon icono = ImagenProducto.icono(producto, foto, "Lácteos", 90);
        comprobar(icono.getIconWidth() == 90 && icono.getIconHeight() == 90,
                "La miniatura usa exactamente el tamaño solicitado");
        comprobar(icono == ImagenProducto.icono(producto, foto, "Lácteos", 90),
                "Una foto sin cambios reutiliza su miniatura");
        comprobar(icono == ImagenProducto.icono(producto, carpeta.resolve(".").resolve("vertical.png"), "", 90),
                "Las rutas equivalentes comparten la miniatura");
        BufferedImage vista = pintar(icono);
        comprobar(parecido(vista.getRGB(45, 45), Color.RED), "La foto aparece en el centro del icono");
        comprobar(!parecido(vista.getRGB(10, 45), Color.RED) && !parecido(vista.getRGB(80, 45), Color.RED),
                "Una foto vertical conserva espacio lateral");
        int minX = 90, maxX = -1, minY = 90, maxY = -1;
        for (int y = 0; y < vista.getHeight(); y++) for (int x = 0; x < vista.getWidth(); x++)
            if (parecido(vista.getRGB(x, y), Color.RED)) {
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        double proporcion = (maxX - minX + 1.0) / (maxY - minY + 1.0);
        comprobar(proporcion > .28 && proporcion < .39,
                "La imagen conserva la proporción vertical de 1:3 sin estirarse");
        comprobar(!parecido(vista.getRGB(45, 1), Color.RED), "El marco conserva un margen alrededor de la foto");
        Icon pequeno = ImagenProducto.icono(producto, foto, "", 32);
        comprobar(pequeno != icono && pequeno.getIconWidth() == 32 && pequeno.getIconHeight() == 32,
                "Cada tamaño recibe su propia miniatura");

        long modificacion = Files.getLastModifiedTime(foto).toMillis();
        ImageIO.write(imagen(Color.BLUE, 40, 120), "png", foto.toFile());
        Files.setLastModifiedTime(foto, FileTime.fromMillis(modificacion + 10000));
        Icon actualizada = ImagenProducto.icono(producto, foto, "", 90);
        comprobar(actualizada != icono && parecido(pintar(actualizada).getRGB(45, 45), Color.BLUE),
                "Reemplazar una foto invalida la miniatura y muestra el contenido nuevo");

        Path jpeg = carpeta.resolve("horizontal.jpg");
        ImageIO.write(imagen(Color.GREEN, 120, 40), "jpeg", jpeg.toFile());
        comprobar(ImagenProducto.esImagenValida(jpeg), "Una foto JPEG es válida");
        BufferedImage horizontal = pintar(ImagenProducto.icono(producto, jpeg, "", 90));
        comprobar(parecido(horizontal.getRGB(45, 45), Color.GREEN)
                && !parecido(horizontal.getRGB(45, 10), Color.GREEN),
                "Una foto horizontal conserva su imagen centrada y el espacio superior");

        Path gif = carpeta.resolve("gif-disfrazado.png");
        ImageIO.write(imagen(Color.RED, 40, 60), "gif", gif.toFile());
        comprobar(!ImagenProducto.esImagenValida(gif), "Un GIF no se acepta aunque tenga extensión PNG");
        comprobar(ImagenProducto.icono(producto, gif, "", 90) == ImagenProducto.icono(producto, null, "", 90),
                "Los formatos que no se pueden importar usan el icono de categoría");
    }
    private static void probarRespaldo(Path carpeta) throws Exception {
        Producto producto = producto("FRUTAS Y VERDURAS");
        Icon respaldo = ImagenProducto.icono(producto, null, "Lácteos", 64);
        comprobar(respaldo.getIconWidth() == 64 && respaldo.getIconHeight() == 64,
                "El icono de categoría respeta el tamaño solicitado");
        comprobar(respaldo == ImagenProducto.icono(producto, null, "", 64),
                "El mismo símbolo se reutiliza independientemente de una sección menos específica");
        comprobar(respaldo == ImagenProducto.icono(producto, carpeta.resolve("inexistente.png"), "", 64),
                "Una foto inexistente usa el símbolo de categoría");
        comprobar(!ImagenProducto.esImagenValida(null) && !ImagenProducto.esImagenValida(carpeta.resolve("inexistente.png")),
                "Validar una ruta ausente no produce errores");
        Path rota = carpeta.resolve("rota.png");
        Files.writeString(rota, "Archivo que no es una imagen");
        comprobar(!ImagenProducto.esImagenValida(rota), "Los archivos corruptos no son fotos válidas");
        comprobar(respaldo == ImagenProducto.icono(producto, rota, "", 64),
                "Una foto corrupta usa el símbolo de categoría");
        ImageIO.write(imagen(Color.BLUE, 40, 40), "png", rota.toFile());
        comprobar(parecido(pintar(ImagenProducto.icono(producto, rota, "", 64)).getRGB(32, 32), Color.BLUE),
                "Reparar la foto permite verla sin reiniciar la aplicación");
        Path vacia = Files.createFile(carpeta.resolve("vacia.png"));
        comprobar(!ImagenProducto.esImagenValida(vacia)
                && ImagenProducto.icono(producto, vacia, "", 64) == respaldo,
                "Los archivos vacíos mantienen el respaldo de categoría");
        Path muyAncha = carpeta.resolve("muy-ancha.png");
        ImageIO.write(imagen(Color.RED, 12001, 1), "png", muyAncha.toFile());
        comprobar(!ImagenProducto.esImagenValida(muyAncha)
                && ImagenProducto.icono(producto, muyAncha, "", 64) == respaldo,
                "Las dimensiones excesivas se rechazan antes de decodificar la foto");
        comprobar(ImagenProducto.icono(null, null, null, 40).getIconWidth() == 40,
                "El símbolo general también funciona sin un producto seleccionado");
    }
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("sicov-iconos-productos-");
        try {
            probarFamilias(); probarFotografias(carpeta); probarRespaldo(carpeta);
        } finally {
            try (var archivos = Files.walk(carpeta)) {
                for (Path archivo : archivos.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(archivo);
            }
        }
        System.out.println("OK: " + verificaciones + " verificaciones de iconos y miniaturas de productos.");
    }
}
