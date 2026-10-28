package com.sicov;

import com.sicov.servicio.Autorizacion;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class PruebasRecuperacion {
    private static int comprobaciones;
    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
    private static char[] clave(String texto) { return texto.toCharArray(); }
    private static char[][] respuestas(String... textos) {
        return Arrays.stream(textos).map(String::toCharArray).toArray(char[][]::new);
    }
    private static char[][] correctas() { return respuestas("Luna de papel 🐾", "casa azul s/n #2", "cuando nací"); }
    private static void comprobar(boolean condicion, String mensaje) {
        comprobaciones++; if (!condicion) throw new AssertionError(mensaje);
    }
    private static void rechaza(Accion accion) throws Exception {
        boolean rechazado = false;
        try { accion.ejecutar(); } catch (IllegalArgumentException e) { rechazado = true; }
        comprobar(rechazado, "Debía rechazar una recuperación o configuración no autorizada");
    }
    private static Properties leer(Path ruta) throws IOException {
        Properties p = new Properties(); try (Reader r = Files.newBufferedReader(ruta)) { p.load(r); } return p;
    }
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("sicov-recuperacion-"), ruta = carpeta.resolve("configuracion.properties");
        Files.writeString(ruta, "tienda.nombre=Prueba\n");
        Autorizacion auth = new Autorizacion(ruta);
        comprobar(!auth.configurada() && !auth.recuperacionConfigurada(), "Comienza sin credenciales");
        comprobar(Autorizacion.PREGUNTAS_RECUPERACION.size() == 3, "Tres preguntas fijas");
        rechaza(() -> auth.recuperar(correctas(), clave("nueva")));
        rechaza(() -> auth.cambiar(clave(""), clave("inicial")));
        rechaza(() -> auth.cambiar(clave(""), clave("inicial"), null));
        rechaza(() -> auth.cambiar(clave(""), clave("inicial"), respuestas("solo", "dos")));
        for (int i = 0; i < 3; i++) {
            char[][] incompletas = correctas(); incompletas[i] = clave("  ");
            rechaza(() -> auth.cambiar(clave(""), clave("inicial"), incompletas));
        }
        comprobar(!auth.configurada(), "El alta incompleta no crea una contraseña");
        auth.cambiar(clave(""), clave("inicial"), correctas());
        comprobar(auth.configurada() && auth.recuperacionConfigurada(), "Alta completa");
        comprobar(auth.verificar(clave("inicial")), "Funciona la contraseña inicial");
        comprobar(leer(ruta).getProperty("tienda.nombre").equals("Prueba"), "Preserva otras opciones");
        String archivo = Files.readString(ruta);
        for (String secreto : new String[]{"inicial", "Luna de papel", "casa azul", "cuando nací"})
            comprobar(!archivo.contains(secreto), "No almacena secretos en texto plano");
        Properties hashes = leer(ruta);
        Set<String> sales = new HashSet<>();
        for (String p : new String[]{"admin", "admin.recuperacion.mascota", "admin.recuperacion.casa", "admin.recuperacion.nacimiento"})
            sales.add(hashes.getProperty(p + ".sal"));
        comprobar(sales.size() == 4, "Cada secreto tiene su propia sal");
        for (int i = 0; i < 3; i++) {
            char[][] incorrectas = correctas(); incorrectas[i] = clave("incorrecta");
            byte[] anterior = Files.readAllBytes(ruta);
            rechaza(() -> auth.recuperar(incorrectas, clave("intrusa")));
            comprobar(Arrays.equals(anterior, Files.readAllBytes(ruta)), "Una respuesta errónea no modifica la configuración");
        }
        rechaza(() -> auth.recuperar(respuestas("", "casa azul s/n #2", "cuando nací"), clave("nueva")));
        rechaza(() -> auth.recuperar(correctas(), clave("abc")));
        rechaza(() -> auth.cambiar(clave("erronea"), clave("intrusa"), respuestas("x", "y", "z")));
        comprobar(auth.verificar(clave("inicial")), "Los intentos fallidos conservan la contraseña");
        Autorizacion recargada = new Autorizacion(ruta);
        recargada.recuperar(respuestas("  LUNA DE PAPEL 🐾  ", "CASA AZUL S/N #2", "CUANDO NACI\u0301"), clave("recuperada"));
        comprobar(recargada.verificar(clave("recuperada")) && !recargada.verificar(clave("inicial")), "Recuperación tras reinicio y normalización");
        comprobar(recargada.recuperacionConfigurada(), "La recuperación mantiene las preguntas");
        comprobar(!Files.readString(ruta).contains("recuperada"), "La nueva contraseña también está protegida");
        Autorizacion persistida = new Autorizacion(ruta);
        comprobar(persistida.verificar(clave("recuperada")), "Persiste la contraseña recuperada");
        persistida.cambiar(clave("recuperada"), clave("cambiada"));
        persistida.recuperar(correctas(), clave("otra-clave"));
        comprobar(persistida.verificar(clave("otra-clave")), "Cambiar solo la contraseña conserva las respuestas");
        persistida.cambiar(clave("otra-clave"), clave("final"), respuestas("1", "@", "texto libre"));
        rechaza(() -> persistida.recuperar(correctas(), clave("anterior")));
        persistida.recuperar(respuestas("1", "@", "TEXTO LIBRE"), clave("ultima"));
        comprobar(persistida.verificar(clave("ultima")), "Se pueden actualizar las respuestas con la contraseña actual");
        Properties legado = leer(ruta);
        legado.keySet().removeIf(k -> k.toString().startsWith("admin.recuperacion."));
        Path rutaLegado = carpeta.resolve("legado.properties");
        try (Writer w = Files.newBufferedWriter(rutaLegado)) { legado.store(w, "Prueba de versión anterior"); }
        Autorizacion antigua = new Autorizacion(rutaLegado);
        comprobar(antigua.configurada() && !antigua.recuperacionConfigurada(), "Compatibilidad con cuentas anteriores");
        rechaza(() -> antigua.recuperar(correctas(), clave("nueva")));
        rechaza(() -> antigua.cambiar(clave("ultima"), clave("nueva")));
        antigua.cambiar(clave("ultima"), clave("migrada"), correctas());
        comprobar(antigua.recuperacionConfigurada() && antigua.verificar(clave("migrada")), "Migración autorizada registra las tres respuestas");
        System.out.println("OK: " + comprobaciones + " verificaciones de recuperación.");
    }
}
