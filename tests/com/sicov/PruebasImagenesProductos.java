package com.sicov;

import com.sicov.controlador.ContextoAplicacion;
import com.sicov.modelo.Producto;
import com.sicov.modelo.UnidadVenta;
import com.sicov.persistencia.ArchivoCsv;
import com.sicov.persistencia.ArchivoImagenesProductos;
import com.sicov.vista.gestion.PanelCompras;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Importación segura y persistencia de imágenes opcionales, sin tocar datos reales.
 */
public final class PruebasImagenesProductos {
    private static int verificaciones;

    private static void comprobar(boolean cumple, String mensaje) {
        verificaciones++;
        if (!cumple)
            throw new AssertionError(mensaje);
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private static void rechazar(Accion accion, String mensaje) throws Exception {
        boolean rechazo = false;
        try {
            accion.ejecutar();
        } catch (IOException e) {
            rechazo = true;
        }
        comprobar(rechazo, mensaje);
    }

    private static Path crearImagen(Path ruta, int ancho, int alto, String formato, boolean transparente)
            throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto,
                transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(30, 140, 70, transparente ? 100 : 255));
        g.fillRect(0, 0, ancho, alto);
        g.dispose();
        comprobar(ImageIO.write(imagen, formato, ruta.toFile()), "Prepara una imagen de prueba " + formato);
        imagen.flush();
        return ruta;
    }

    private static Properties leer(Path archivo) throws Exception {
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }
        return propiedades;
    }

    private static void probarImportacion() throws Exception {
        Path temporal = Files.createTempDirectory("sicov-imagenes-");
        Path datos = temporal.resolve("datos");
        ArchivoImagenesProductos imagenes = new ArchivoImagenesProductos(datos);
        comprobar(!Files.exists(datos), "Consultar imágenes opcionales no crea archivos ni carpetas");
        comprobar(imagenes.imagen("0304") == null && imagenes.imagen(null) == null,
                "Un producto sin imagen permite usar su icono");
        Path origen = crearImagen(temporal.resolve("foto-sin-extension"), 1000, 400, "png", true);
        byte[] contenidoOriginal = Files.readAllBytes(origen);
        imagenes.guardar("0304", origen);
        Path copia = imagenes.imagen("0304");
        comprobar(copia != null && copia.startsWith(datos.resolve("imagenes")), "Importa dentro de datos/imagenes");
        comprobar(!copia.equals(origen), "Conserva una copia independiente del archivo elegido");
        BufferedImage leida = ImageIO.read(copia.toFile());
        comprobar(leida.getWidth() == 512 && leida.getHeight() == 205, "Reduce la imagen manteniendo su proporción");
        comprobar((leida.getRGB(250, 100) >>> 24) == 100, "Conserva la transparencia de PNG");
        leida.flush();
        comprobar(java.util.Arrays.equals(contenidoOriginal, Files.readAllBytes(origen)),
                "No cambia la fotografía original");
        Path archivo = datos.resolve("imagenes_productos.properties");
        comprobar(
                leer(archivo).getProperty("0304").startsWith("imagenes/")
                        && !Path.of(leer(archivo).getProperty("0304")).isAbsolute(),
                "Persiste una ruta relativa portable");
        ArchivoImagenesProductos recarga = new ArchivoImagenesProductos(datos);
        comprobar(copia.equals(recarga.imagen("0304")), "Recupera la asociación al abrir nuevamente la aplicación");
        Files.delete(origen);
        comprobar(recarga.imagen("0304") != null, "Mover o borrar el original no rompe la imagen del producto");

        Path segundo = crearImagen(temporal.resolve("segunda.jpg"), 120, 80, "jpeg", false);
        recarga.guardar("Queso ñ", segundo);
        Path queso = recarga.imagen("Queso ñ");
        comprobar(queso != null && ImageIO.read(queso.toFile()).getWidth() == 120,
                "Importa JPEG sin ampliar una imagen pequeña");
        recarga.guardar("0304", segundo);
        Path nueva = recarga.imagen("0304");
        comprobar(nueva != null && !nueva.equals(copia) && Files.exists(copia),
                "Reemplaza la asociación conservando la copia anterior");
        comprobar(recarga.imagen("Queso ñ").equals(queso), "Reemplazar una imagen conserva las asociaciones ajenas");
        recarga.quitar("0304");
        comprobar(recarga.imagen("0304") == null && Files.exists(nueva) && Files.exists(segundo),
                "Quitar desvincula sin borrar imágenes ni originales");
        comprobar(new ArchivoImagenesProductos(datos).imagen("0304") == null, "Persiste quitar la imagen");
        comprobar(new ArchivoImagenesProductos(datos).imagen("Queso ñ").equals(queso),
                "Los códigos Unicode persisten en UTF-8");
        byte[] antes = Files.readAllBytes(archivo);
        Path invalido = temporal.resolve("falsa.png");
        Files.writeString(invalido, "Esto no es una imagen");
        rechazar(() -> recarga.guardar("Queso ñ", invalido), "Rechaza un archivo falso aunque termine en .png");
        comprobar(
                java.util.Arrays.equals(antes, Files.readAllBytes(archivo)) && recarga.imagen("Queso ñ").equals(queso),
                "Una importación inválida conserva archivo de asociaciones e imagen anterior");
        Path gif = crearImagen(temporal.resolve("fuera-de-formato.png"), 20, 20, "gif", false);
        rechazar(() -> recarga.guardar("Queso ñ", gif), "Valida el contenido y rechaza GIF con extensión PNG");
        rechazar(() -> recarga.guardar("Queso ñ", temporal.resolve("no-existe.png")), "Rechaza un archivo inexistente");
        rechazar(() -> recarga.guardar("Queso ñ", null), "Rechaza la ausencia de archivo sin perder la asociación");
        comprobar(Files.exists(segundo) && Files.exists(invalido) && Files.exists(gif),
                "Ninguna importación ni eliminación borra archivos del usuario");
        Files.delete(queso);
        comprobar(recarga.imagen("Queso ñ") == null,
                "Una imagen referenciada que ya no existe permite volver al icono");
    }

    private static void probarRutasYLimites() throws Exception {
        Path temporal = Files.createTempDirectory("sicov-rutas-imagenes-");
        Path datos = Files.createDirectory(temporal.resolve("datos"));
        Path imagenes = Files.createDirectory(datos.resolve("imagenes"));
        Path fuera = crearImagen(temporal.resolve("fuera.png"), 20, 20, "png", false);
        Path dentro = crearImagen(imagenes.resolve("valida.png"), 20, 20, "png", false);
        Files.writeString(imagenes.resolve("falsa.png"), "No es PNG");
        Files.writeString(datos.resolve("imagenes_productos.properties"),
                "subir=imagenes/../../fuera.png\n" + "retroceso=imagenes/../imagenes/valida.png\n" + "absoluta="
                        + fuera.toAbsolutePath() + "\n" + "faltante=imagenes/faltante.png\n"
                        + "falsa=imagenes/falsa.png\n" + "valida=imagenes/valida.png\n"
                        + "otra_clave=valor conservado\n",
                StandardCharsets.UTF_8);
        ArchivoImagenesProductos archivo = new ArchivoImagenesProductos(datos);
        comprobar(archivo.imagen("subir") == null && archivo.imagen("retroceso") == null,
                "Rechaza cualquier componente .. en los metadatos");
        comprobar(archivo.imagen("absoluta") == null && archivo.imagen("faltante") == null,
                "Rechaza referencias absolutas o inexistentes");
        comprobar(archivo.imagen("falsa") == null && archivo.imagen("valida").equals(dentro),
                "Solo usa referencias existentes con formato de imagen válido");
        archivo.guardar("nuevo", fuera);
        comprobar(leer(datos.resolve("imagenes_productos.properties")).getProperty("otra_clave")
                .equals("valor conservado"), "Guardar conserva claves ajenas de los metadatos");
        Path enlace = imagenes.resolve("enlace.png");
        try {
            Files.createSymbolicLink(enlace, fuera);
            Files.writeString(datos.resolve("imagenes_productos.properties"), "enlace=imagenes/enlace.png\n");
            comprobar(new ArchivoImagenesProductos(datos).imagen("enlace") == null,
                    "Un enlace simbólico no permite salir de imágenes");
        } catch (UnsupportedOperationException e) { /* Algunos sistemas no ofrecen enlaces simbólicos. */ }
        Path demasiadoAncha = crearImagen(temporal.resolve("ancha.png"), 12_001, 1, "png", false);
        rechazar(() -> archivo.guardar("nuevo", demasiadoAncha),
                "Rechaza dimensiones excesivas antes de decodificar píxeles");
        Path grande = temporal.resolve("grande.png");
        try (var canal = Files.newByteChannel(grande, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            canal.position(20L * 1024 * 1024);
            canal.write(java.nio.ByteBuffer.wrap(new byte[] { 0 }));
        }
        rechazar(() -> archivo.guardar("nuevo", grande), "Rechaza archivos mayores de 20 MB antes de decodificarlos");
    }

    private static void probarContexto() throws Exception {
        Path temporal = Files.createTempDirectory("sicov-contexto-imagenes-");
        Path datos = temporal.resolve("datos");
        Path foto = crearImagen(temporal.resolve("huevo.png"), 200, 140, "png", true);
        ContextoAplicacion contexto = new ContextoAplicacion(datos);
        contexto.guardarCategoria("Huevo", "Abarrotes");
        Producto huevo = new Producto("0304", "Huevo blanco a granel", new BigDecimal("42.50"), "Huevo",
                UnidadVenta.KILOGRAMO, true);
        contexto.guardarProducto(huevo, false);
        int[] avisos = { 0 };
        contexto.escuchar(() -> avisos[0]++);
        byte[] catalogoAntes = Files.readAllBytes(datos.resolve("productos.csv"));
        contexto.guardarImagenProducto("0304", foto);
        Path copia = contexto.imagenProducto("0304");
        comprobar(copia != null && avisos[0] == 1, "Asignar una imagen notifica una vez a las pantallas abiertas");
        comprobar(contexto.producto("0304").equals(huevo),
                "Asignar una imagen conserva código, precio, unidad y demás datos del producto");
        comprobar(java.util.Arrays.equals(catalogoAntes, Files.readAllBytes(datos.resolve("productos.csv"))),
                "Asignar una imagen no reescribe ni amplía productos.csv");
        comprobar(contexto.seccionCategoria("huevo").equals("Abarrotes"),
                "La categoría conserva su sección para elegir el icono por defecto");
        ContextoAplicacion recarga = new ContextoAplicacion(datos);
        comprobar(recarga.imagenProducto("0304").equals(copia) && recarga.producto("0304").equals(huevo),
                "El contexto recupera la fotografía y los mismos datos del catálogo");
        Producto editado = new Producto("0304", "Huevo fresco a granel", new BigDecimal("44.00"), "Huevo",
                UnidadVenta.KILOGRAMO, true);
        contexto.guardarProducto(editado, true);
        comprobar(contexto.imagenProducto("0304").equals(copia),
                "Editar un producto mediante su API usual conserva la imagen asignada");
        String cabecera = "codigo,nombre,precio,categoria,unidad,sin_codigo_fisico";
        comprobar(Files.readAllLines(datos.resolve("productos.csv"), StandardCharsets.UTF_8).get(0).equals(cabecera)
                && ArchivoCsv.leer(datos.resolve("productos.csv"), cabecera).stream().allMatch(f -> f.size() == 6),
                "El catálogo mantiene sus seis columnas originales");
        int avisosAntes = avisos[0];
        boolean rechazo = false;
        try {
            contexto.guardarImagenProducto("inexistente", foto);
        } catch (IllegalArgumentException e) {
            rechazo = true;
        }
        comprobar(rechazo && avisos[0] == avisosAntes && contexto.imagenProducto("inexistente") == null,
                "No asocia fotografías a productos que no existen ni anuncia cambios inexistentes");
        contexto.quitarImagenProducto("0304");
        comprobar(contexto.imagenProducto("0304") == null && avisos[0] == avisosAntes + 1,
                "Quitar la imagen notifica a las pantallas para recuperar el icono de categoría");
        comprobar(
                new ContextoAplicacion(datos).imagenProducto("0304") == null
                        && contexto.producto("0304").equals(editado) && Files.exists(copia) && Files.exists(foto),
                "Quitar persiste sin alterar el producto ni borrar copias u originales");
    }

    private static Field campo(String nombre) throws Exception {
        Field campo = PanelCompras.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo;
    }

    private static <T> T campo(PanelCompras panel, String nombre, Class<T> tipo) throws Exception {
        return tipo.cast(campo(nombre).get(panel));
    }

    private static void accion(PanelCompras panel, String nombre) throws Exception {
        var metodo = PanelCompras.class.getDeclaredMethod(nombre);
        metodo.setAccessible(true);
        metodo.invoke(panel);
    }

    private static void probarFormulario() throws Exception {
        Path temporal = Files.createTempDirectory("sicov-formulario-imagenes-");
        Path foto = crearImagen(temporal.resolve("queso.png"), 160, 120, "png", false);
        ContextoAplicacion contexto = new ContextoAplicacion(temporal.resolve("datos"));
        PanelCompras panel = new PanelCompras(contexto);
        campo(panel, "nombre", JTextField.class).setText("Queso fresco");
        campo(panel, "precio", JTextField.class).setText("120.00");
        campo(panel, "codigo", JTextField.class).setText("Q1");
        campo(panel, "categoria", JComboBox.class).setSelectedItem("Lácteos");
        campo(panel, "unidad", JComboBox.class).setSelectedItem(UnidadVenta.KILOGRAMO);
        campo("imagenElegida").set(panel, foto);
        accion(panel, "guardar");
        Path copia = contexto.imagenProducto("Q1");
        comprobar(contexto.producto("Q1") != null && copia != null,
                "Compras guarda un producto nuevo y su imagen opcional sin abrir un selector de archivos");
        comprobar(
                campo(panel, "codigo", JTextField.class).getText().isBlank()
                        && campo("imagenElegida").get(panel) == null,
                "Guardar limpia la selección de imagen para el próximo producto");
        JTable tabla = campo(panel, "tabla", JTable.class);
        tabla.setRowSelectionInterval(0, 0);
        accion(panel, "editar");
        comprobar(
                campo(panel, "estadoImagen", JLabel.class).getText().equals("Imagen del producto")
                        && campo(panel, "vistaImagen", JLabel.class).getIcon() != null,
                "Editar recupera la vista previa de la fotografía existente");
        campo(panel, "precio", JTextField.class).setText("125.00");
        accion(panel, "guardar");
        comprobar(
                contexto.imagenProducto("Q1").equals(copia)
                        && contexto.producto("Q1").precio().compareTo(new BigDecimal("125")) == 0,
                "Guardar una edición sin elegir imagen conserva la fotografía anterior");
        tabla.setRowSelectionInterval(0, 0);
        accion(panel, "editar");
        campo("quitarImagen").setBoolean(panel, true);
        accion(panel, "actualizarImagen");
        comprobar(
                campo(panel, "estadoImagen", JLabel.class).getText().equals("Icono de categoría")
                        && campo(panel, "vistaImagen", JLabel.class).getIcon() != null,
                "Usar icono de categoría muestra una vista previa antes de guardar");
        accion(panel, "guardar");
        comprobar(contexto.imagenProducto("Q1") == null && Files.exists(copia) && Files.exists(foto),
                "El formulario puede quitar la asociación sin borrar las imágenes");
    }

    public static void main(String[] args) throws Exception {
        probarImportacion();
        probarRutasYLimites();
        probarContexto();
        SwingUtilities.invokeAndWait(() -> {
            try {
                probarFormulario();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("OK: " + verificaciones + " verificaciones de imágenes de productos.");
    }
}
