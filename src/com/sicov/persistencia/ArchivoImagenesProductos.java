package com.sicov.persistencia;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

/** Imágenes opcionales, independientes del catálogo y del archivo elegido por el usuario. */
public final class ArchivoImagenesProductos {
    private static final int LADO_MAXIMO = 512;
    private static final long BYTES_MAXIMOS = 20L * 1024 * 1024;
    private static final long PIXELES_MAXIMOS = 25_000_000;
    private final Path directorio, imagenes, archivo;
    private final Properties asociaciones = new Properties();

    public ArchivoImagenesProductos(Path directorio) throws IOException {
        this.directorio = directorio.toAbsolutePath().normalize();
        imagenes = this.directorio.resolve("imagenes");
        archivo = this.directorio.resolve("imagenes_productos.properties");
        if (Files.exists(archivo)) {
            try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                asociaciones.load(lector);
            } catch (IllegalArgumentException e) {
                throw new IOException("No se pudo leer la configuración de imágenes de productos.", e);
            }
        }
    }

    /** Devuelve null si falta la imagen o la referencia no es válida o sale del directorio de imágenes. */
    public Path imagen(String codigo) {
        if (codigo == null) return null;
        String referencia = asociaciones.getProperty(codigo);
        if (referencia == null || referencia.isBlank()) return null;
        try {
            Path relativa = Path.of(referencia);
            if (relativa.isAbsolute() || relativa.getNameCount() < 2
                    || !relativa.getName(0).toString().equals("imagenes")) return null;
            for (Path parte : relativa) if (parte.toString().equals("..")) return null;
            Path ruta = directorio.resolve(relativa).normalize();
            if (!ruta.startsWith(imagenes) || !Files.isRegularFile(ruta)) return null;
            Path baseReal = directorio.toRealPath(), imagenesReal = imagenes.toRealPath();
            if (!imagenesReal.startsWith(baseReal) || !ruta.toRealPath().startsWith(imagenesReal)) return null;
            revisarImagen(ruta, false);
            return ruta;
        } catch (IOException | InvalidPathException | SecurityException e) {
            return null;
        }
    }

    /** Importa PNG/JPEG reales y conserva una copia PNG de hasta 512 píxeles por lado. */
    public void guardar(String codigo, Path origen) throws IOException {
        validarCodigo(codigo);
        BufferedImage original = revisarImagen(origen, true);
        BufferedImage copia = reducir(original);
        try {
            Files.createDirectories(directorio);
            Files.createDirectories(imagenes);
            if (!imagenes.toRealPath().startsWith(directorio.toRealPath()))
                throw new IOException("La carpeta de imágenes debe estar dentro de la carpeta de datos.");
            String nombre = UUID.randomUUID() + ".png";
            Path destino = imagenes.resolve(nombre);
            Properties siguientes = copiarAsociaciones();
            siguientes.setProperty(codigo, "imagenes/" + nombre);
            try {
                try (var salida = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW)) {
                    if (!ImageIO.write(copia, "png", salida))
                        throw new IOException("No se pudo preparar la imagen del producto.");
                }
                guardarAsociaciones(siguientes);
            } catch (IOException e) {
                Files.deleteIfExists(destino);
                throw e;
            }
            asociaciones.clear();
            asociaciones.putAll(siguientes);
        } finally {
            if (copia != original) copia.flush();
            original.flush();
        }
    }

    /** Quita la asociación; conserva las copias anteriores y el archivo original del usuario. */
    public void quitar(String codigo) throws IOException {
        validarCodigo(codigo);
        if (!asociaciones.containsKey(codigo)) return;
        Properties siguientes = copiarAsociaciones();
        siguientes.remove(codigo);
        guardarAsociaciones(siguientes);
        asociaciones.clear();
        asociaciones.putAll(siguientes);
    }

    private static void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Selecciona un producto para asignar su imagen.");
    }

    private Properties copiarAsociaciones() {
        Properties copia = new Properties();
        copia.putAll(asociaciones);
        return copia;
    }

    private static BufferedImage revisarImagen(Path ruta, boolean decodificar) throws IOException {
        if (ruta == null || !Files.isRegularFile(ruta)) throw new IOException("Selecciona una imagen PNG o JPG existente.");
        if (Files.size(ruta) > BYTES_MAXIMOS) throw new IOException("La imagen debe pesar como máximo 20 MB.");
        try (ImageInputStream entrada = new FileImageInputStream(ruta.toFile())) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new IOException("El archivo no contiene una imagen PNG o JPG válida.");
            ImageReader lector = lectores.next();
            try {
                String formato = lector.getFormatName();
                if (!formato.equalsIgnoreCase("png") && !formato.equalsIgnoreCase("jpeg") && !formato.equalsIgnoreCase("jpg"))
                    throw new IOException("Selecciona una imagen PNG o JPG.");
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho > 12_000 || alto > 12_000
                        || (long) ancho * alto > PIXELES_MAXIMOS)
                    throw new IOException("La imagen es demasiado grande: usa hasta 25 megapíxeles y 12 000 píxeles por lado.");
                if (!decodificar) return null;
                BufferedImage imagen = lector.read(0);
                if (imagen == null) throw new IOException("No se pudo leer la imagen del producto.");
                return imagen;
            } finally { lector.dispose(); }
        } catch (RuntimeException e) {
            throw new IOException("No se pudo leer la imagen. Elige un archivo PNG o JPG válido.", e);
        }
    }

    private static BufferedImage reducir(BufferedImage original) {
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(original.getWidth(), original.getHeight()));
        if (escala == 1.0) return original;
        int ancho = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(original.getHeight() * escala));
        BufferedImage copia = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copia.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, ancho, alto, null);
        } finally { g.dispose(); }
        return copia;
    }

    private void guardarAsociaciones(Properties siguientes) throws IOException {
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, ".sicov-imagenes-", ".tmp");
        try {
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                siguientes.store(escritor, "Imágenes opcionales de productos");
            }
            try { Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporal); }
    }
}
