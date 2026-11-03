package com.sicov.vista;

import com.sicov.modelo.Producto;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Miniaturas opcionales y símbolos de categoría, sin depender de fuentes o emojis. */
public final class ImagenProducto {
    private static final Color CREMA = new Color(248, 246, 237);
    private static final Color VERDE = new Color(56, 112, 74);
    private static final Color DORADO = new Color(230, 183, 78);
    private static final long MAXIMO_BYTES = 20L * 1024 * 1024;
    private static final Map<Object, Icon> CACHE = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Object, Icon> entrada) {
            return size() > 128;
        }
    };
    private record ClaveFoto(Path ruta, long modificacion, long bytes, int tamano) {}
    private record ClaveCategoria(String familia, int tamano) {}

    private ImagenProducto() {}

    /** La lectura y el escalado se hacen aquí; pintar el Icon no accede al disco. */
    public static Icon icono(Producto producto, Path foto, String seccion, int tamano) {
        if (tamano <= 0) throw new IllegalArgumentException("El tamaño de la imagen debe ser positivo.");
        if (foto != null) {
            try {
                Path ruta = foto.toAbsolutePath().normalize();
                BasicFileAttributes datos = Files.readAttributes(ruta, BasicFileAttributes.class);
                if (archivoValido(datos)) {
                    ClaveFoto clave = new ClaveFoto(ruta, datos.lastModifiedTime().toMillis(), datos.size(), tamano);
                    Icon guardado = obtener(clave);
                    if (guardado != null) return guardado;
                    BufferedImage imagen = leerFoto(ruta, tamano);
                    if (imagen != null) return guardar(clave, miniatura(imagen, tamano));
                }
            } catch (IOException | SecurityException | IllegalArgumentException e) {
                // Una foto retirada o dañada no debe impedir vender el producto.
            }
        }
        String familia = familia(producto == null ? "" : producto.categoria(), seccion);
        ClaveCategoria clave = new ClaveCategoria(familia, tamano);
        Icon guardado = obtener(clave);
        return guardado != null ? guardado : guardar(clave, simbolo(familia, tamano));
    }

    /** Revisa formato y dimensiones sin decodificar toda la foto seleccionada. */
    public static boolean esImagenValida(Path foto) {
        if (foto == null) return false;
        try {
            if (!archivoValido(Files.readAttributes(foto, BasicFileAttributes.class))) return false;
            try (ImageInputStream entrada = ImageIO.createImageInputStream(foto.toFile())) {
                if (entrada == null) return false;
                Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
                if (!lectores.hasNext()) return false;
                ImageReader lector = lectores.next();
                try {
                    lector.setInput(entrada, true, true);
                    return formatoValido(lector) && dimensionesValidas(lector.getWidth(0), lector.getHeight(0));
                } finally { lector.dispose(); }
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) { return false; }
    }

    private static boolean archivoValido(BasicFileAttributes datos) {
        return datos.isRegularFile() && datos.size() > 0 && datos.size() <= MAXIMO_BYTES;
    }
    private static boolean dimensionesValidas(int ancho, int alto) {
        return ancho > 0 && alto > 0 && ancho <= 12000 && alto <= 12000 && (long) ancho * alto <= 25_000_000;
    }
    private static boolean formatoValido(ImageReader lector) throws IOException {
        String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
        return formato.equals("png") || formato.equals("jpeg") || formato.equals("jpg");
    }
    private static BufferedImage leerFoto(Path ruta, int tamano) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(ruta.toFile())) {
            if (entrada == null) return null;
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) return null;
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                if (!formatoValido(lector) || !dimensionesValidas(ancho, alto)) return null;
                ImageReadParam parametros = lector.getDefaultReadParam();
                int paso = (int) Math.max(1, Math.min(ancho, alto) / Math.max(1L, tamano * 2L));
                parametros.setSourceSubsampling(paso, paso, 0, 0);
                return lector.read(0, parametros);
            } finally { lector.dispose(); }
        }
    }

    /** La categoría específica tiene preferencia; la sección sirve como respaldo. */
    public static String familia(String categoria, String seccion) {
        String resultado = reconocer(normalizar(categoria));
        if (resultado == null) resultado = reconocer(normalizar(seccion));
        return resultado == null ? "abarrotes" : resultado;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    private static String reconocer(String texto) {
        if (contiene(texto, "fruta", "verdura", "vegetal", "hortaliza")) return "frutas";
        if (contiene(texto, "lacteo", "leche", "queso", "yogur", "crema", "mantequilla")) return "lacteos";
        if (contiene(texto, "salchich", "carne", "jamon", "embutid", "tocino", "pollo")) return "carnes";
        if (contiene(texto, "huevo")) return "huevos";
        if (contiene(texto, "detergente", "limpia", "lavatrast", "suavizante", "cloro", "hogar")) return "limpieza";
        if (contiene(texto, "higien", "jabon", "papel", "dental", "shampoo", "champu")) return "higiene";
        if (contiene(texto, "alcohol", "cerveza", "vino", "licor", "tequila", "mezcal")) return "alcohol";
        if (contiene(texto, "bebida", "refresco", "jugo", "nectar", "agua", "cafe", "te ")) return "bebidas";
        if (contiene(texto, "reposter", "panader", "galleta", "tortilla") || (" " + texto + " ").contains(" pan ")) return "pan";
        if (contiene(texto, "dulce", "chocolate", "caramelo", "chicle")) return "dulces";
        if (contiene(texto, "arroz", "cereal", "frijol", "grano", "semilla", "lenteja", "harina")) return "granos";
        if (contiene(texto, "botana", "snack", "fritura", "papas")) return "botanas";
        if (contiene(texto, "tecnolog", "electronic", "pila", "bateria")) return "tecnologia";
        if (contiene(texto, "farmacia", "medicament", "curacion")) return "farmacia";
        if (contiene(texto, "helado", "paleta")) return "helados";
        if (contiene(texto, "tabaco", "cigarro")) return "tabacos";
        if (contiene(texto, "juguete")) return "juguetes";
        if (contiene(texto, "abarrote", "conserva", "atun", "aderezo", "mayonesa", "salsa", "condimento")) return "abarrotes";
        return null;
    }
    private static boolean contiene(String texto, String... opciones) {
        for (String opcion : opciones) if (texto.contains(opcion)) return true;
        return false;
    }
    private static synchronized Icon obtener(Object clave) { return CACHE.get(clave); }
    private static synchronized Icon guardar(Object clave, Icon icono) { CACHE.put(clave, icono); return icono; }

    private static BufferedImage lienzo(int tamano) {
        return new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
    }
    private static Graphics2D preparar(BufferedImage imagen) {
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
    private static Icon miniatura(BufferedImage foto, int tamano) {
        BufferedImage imagen = lienzo(tamano);
        Graphics2D g = preparar(imagen);
        double margen = tamano * .06;
        double espacio = tamano - 2 * margen;
        g.setColor(CREMA);
        g.fill(new RoundRectangle2D.Double(0, 0, tamano, tamano, tamano * .2, tamano * .2));
        g.setClip(new RoundRectangle2D.Double(margen, margen, espacio, espacio, tamano * .14, tamano * .14));
        double escala = Math.min(espacio / foto.getWidth(), espacio / foto.getHeight());
        int ancho = Math.max(1, (int) Math.round(foto.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(foto.getHeight() * escala));
        g.drawImage(foto, (tamano - ancho) / 2, (tamano - alto) / 2, ancho, alto, null);
        g.dispose();
        return new ImageIcon(imagen);
    }
    private static Icon simbolo(String familia, int tamano) {
        BufferedImage imagen = lienzo(tamano);
        Graphics2D g = preparar(imagen);
        g.scale(tamano / 100.0, tamano / 100.0);
        g.setColor(CREMA); g.fillRoundRect(0, 0, 100, 100, 20, 20);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (familia) {
            case "frutas" -> {
                g.setColor(new Color(204, 84, 59));
                g.fillOval(22, 34, 38, 43); g.fillOval(42, 34, 38, 43);
                g.setColor(VERDE); g.fillOval(50, 20, 24, 12); g.drawLine(50, 37, 47, 24);
                g.setColor(new Color(243, 168, 126)); g.drawArc(28, 40, 15, 24, 90, 90);
            }
            case "lacteos" -> {
                g.setColor(DORADO); poligono(g, 20, 64, 20, 45, 65, 24, 81, 45, 81, 73);
                g.setColor(new Color(249, 217, 126)); poligono(g, 20, 45, 65, 24, 81, 45);
                g.setColor(new Color(191, 137, 45));
                g.fillOval(30, 53, 9, 9); g.fillOval(57, 58, 12, 10); g.fillOval(66, 48, 6, 6);
            }
            case "carnes" -> {
                g.setColor(new Color(231, 151, 140)); g.fillOval(20, 27, 62, 49);
                g.setColor(new Color(180, 74, 66)); g.drawOval(23, 30, 56, 43);
                g.setColor(new Color(255, 227, 207)); g.fillOval(48, 42, 18, 14);
                g.setColor(new Color(243, 196, 178)); g.fillOval(31, 39, 9, 7); g.fillOval(32, 58, 13, 6);
            }
            case "huevos" -> {
                g.setColor(new Color(209, 173, 129)); g.fillOval(38, 20, 36, 56);
                g.setColor(new Color(241, 220, 184)); g.fillOval(21, 31, 36, 49);
                g.setColor(new Color(255, 245, 224)); g.drawArc(28, 40, 18, 27, 95, 75);
            }
            case "bebidas", "alcohol" -> {
                g.setColor(familia.equals("alcohol") ? new Color(125, 97, 52) : new Color(81, 152, 158));
                g.fillRoundRect(38, 18, 24, 21, 5, 5); g.fillRoundRect(29, 34, 42, 49, 12, 12);
                g.setColor(familia.equals("alcohol") ? DORADO : VERDE); g.fillRoundRect(37, 16, 26, 8, 3, 3);
                g.setColor(new Color(247, 237, 207)); g.fillRoundRect(33, 48, 34, 22, 4, 4);
                g.setColor(VERDE); g.drawLine(42, 59, 58, 59);
            }
            case "pan" -> {
                g.setColor(new Color(193, 129, 66)); g.fillRoundRect(17, 36, 67, 38, 30, 30);
                g.setColor(new Color(236, 189, 119)); g.fillRoundRect(20, 30, 61, 37, 27, 27);
                g.setColor(new Color(166, 107, 49));
                g.drawLine(34, 39, 30, 49); g.drawLine(49, 36, 45, 47); g.drawLine(64, 38, 60, 49);
            }
            case "dulces" -> {
                g.setColor(new Color(224, 169, 110));
                poligono(g, 33, 43, 16, 33, 18, 52, 15, 68, 34, 58);
                poligono(g, 67, 43, 84, 33, 82, 52, 85, 68, 66, 58);
                g.setColor(new Color(194, 72, 67)); g.fillRoundRect(29, 34, 42, 34, 17, 17);
                g.setColor(new Color(247, 204, 157)); g.drawLine(42, 40, 52, 61); g.drawLine(53, 39, 62, 59);
            }
            case "limpieza" -> {
                g.setColor(new Color(78, 143, 112)); g.fillRoundRect(26, 32, 50, 50, 12, 12);
                g.fillRoundRect(28, 21, 28, 26, 6, 6);
                g.setColor(DORADO); g.fillRoundRect(27, 18, 30, 10, 3, 3);
                g.setColor(CREMA); g.fillRoundRect(57, 39, 11, 16, 5, 5); g.fillRoundRect(33, 55, 21, 18, 5, 5);
                g.setColor(VERDE); g.drawLine(39, 64, 48, 64);
            }
            case "higiene" -> {
                g.setColor(new Color(217, 222, 212)); g.fillRoundRect(25, 32, 50, 46, 12, 12);
                g.setColor(Color.WHITE); g.fillOval(25, 22, 50, 25); g.fillRect(56, 40, 21, 39);
                g.setColor(new Color(155, 169, 153)); g.fillOval(40, 29, 20, 10);
                g.setColor(new Color(211, 217, 206)); g.drawLine(61, 63, 74, 63); g.drawLine(61, 69, 74, 69);
            }
            case "granos", "botanas" -> {
                g.setColor(familia.equals("granos") ? new Color(212, 185, 131) : new Color(220, 147, 71));
                poligono(g, 28, 21, 72, 21, 69, 36, 77, 80, 23, 80, 31, 36);
                g.setColor(VERDE); g.drawLine(31, 27, 69, 27); g.drawLine(28, 74, 72, 74);
                if (familia.equals("granos")) {
                    g.setColor(new Color(255, 244, 210));
                    for (int i = 0; i < 3; i++) {
                        g.fillOval(34 + i * 10, 43 + i % 2 * 5, 7, 13);
                        g.fillOval(39 + i * 10, 58 - i % 2 * 4, 7, 11);
                    }
                } else {
                    g.setColor(new Color(255, 218, 124)); g.fillOval(35, 40, 21, 26); g.fillOval(49, 48, 18, 22);
                }
            }
            case "tecnologia" -> {
                g.setColor(new Color(71, 102, 117)); g.fillRoundRect(30, 17, 40, 67, 9, 9);
                g.setColor(new Color(189, 219, 212)); g.fillRoundRect(35, 25, 30, 45, 4, 4);
                g.setColor(CREMA); g.fillOval(47, 74, 6, 6);
            }
            case "farmacia" -> {
                g.setColor(new Color(237, 234, 215)); g.fillRoundRect(22, 29, 56, 49, 10, 10);
                g.setColor(VERDE); g.drawRoundRect(39, 21, 22, 14, 4, 4);
                g.setColor(new Color(187, 76, 63)); g.fillRect(44, 39, 12, 29); g.fillRect(35, 48, 30, 12);
            }
            case "helados" -> {
                g.setColor(new Color(219, 169, 99)); poligono(g, 29, 45, 72, 45, 50, 86);
                g.setColor(new Color(240, 195, 183)); g.fillOval(28, 18, 45, 40);
                g.setColor(new Color(255, 226, 199)); g.drawArc(35, 25, 26, 22, 45, 100);
            }
            case "tabacos", "juguetes" -> {
                g.setColor(familia.equals("juguetes") ? DORADO : new Color(230, 217, 185));
                g.fillRoundRect(25, 25, 50, 52, 6, 6);
                g.setColor(VERDE); g.fillRoundRect(25, 25, 50, 15, 5, 5);
                if (familia.equals("juguetes")) {
                    g.setColor(new Color(193, 78, 63)); g.fillOval(43, 51, 14, 14);
                } else { g.setColor(new Color(175, 137, 81)); g.drawLine(34, 57, 66, 57); }
            }
            default -> {
                g.setColor(new Color(208, 174, 117)); poligono(g, 22, 35, 50, 21, 78, 35, 78, 69, 50, 83, 22, 69);
                g.setColor(new Color(231, 202, 149)); poligono(g, 22, 35, 50, 21, 78, 35, 50, 50);
                g.setColor(new Color(168, 132, 80)); g.drawLine(50, 50, 50, 79); g.drawLine(37, 28, 64, 42);
            }
        }
        g.dispose();
        return new ImageIcon(imagen);
    }
    private static void poligono(Graphics2D g, int... puntos) {
        Path2D forma = new Path2D.Double(); forma.moveTo(puntos[0], puntos[1]);
        for (int i = 2; i < puntos.length; i += 2) forma.lineTo(puntos[i], puntos[i + 1]);
        forma.closePath(); g.fill(forma);
    }
}
