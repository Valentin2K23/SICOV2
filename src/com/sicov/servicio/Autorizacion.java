package com.sicov.servicio;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.Normalizer;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Contraseña y recuperación local con hashes y sales independientes. */
public final class Autorizacion {
    public static final List<String> PREGUNTAS_RECUPERACION = List.of(
            "¿Nombre de mascota?", "¿Número de casa?", "¿Año de nacimiento?");
    private static final List<String> CLAVES_RECUPERACION = List.of("mascota", "casa", "nacimiento");
    private final Path ruta;
    private final Properties config = new Properties();

    public Autorizacion(Path ruta) throws IOException {
        this.ruta = ruta;
        if (Files.exists(ruta)) try (Reader r = Files.newBufferedReader(ruta)) { config.load(r); }
    }
    public boolean configurada() { return tieneSecreto("admin"); }
    public boolean recuperacionConfigurada() {
        return configurada() && CLAVES_RECUPERACION.stream().allMatch(c -> tieneSecreto("admin.recuperacion." + c));
    }
    private boolean tieneSecreto(String prefijo) {
        return config.containsKey(prefijo + ".hash") && config.containsKey(prefijo + ".sal");
    }
    private byte[] derivar(char[] clave, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, 210000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    private boolean verificarSecreto(String prefijo, char[] valor) {
        return MessageDigest.isEqual(Base64.getDecoder().decode(config.getProperty(prefijo + ".hash")),
                derivar(valor, Base64.getDecoder().decode(config.getProperty(prefijo + ".sal"))));
    }
    public boolean verificar(char[] clave) {
        return configurada() && clave != null && verificarSecreto("admin", clave);
    }
    private void validarCambio(char[] anterior, char[] nueva) {
        if (configurada() && !verificar(anterior))
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        validarNueva(nueva);
    }
    private void validarNueva(char[] nueva) {
        if (nueva == null || nueva.length < 4)
            throw new IllegalArgumentException("Usa una contraseña de al menos 4 caracteres.");
    }
    private void validarRespuestas(char[][] respuestas) {
        if (respuestas == null || respuestas.length != PREGUNTAS_RECUPERACION.size())
            throw new IllegalArgumentException("Debes responder las tres preguntas de recuperación.");
        for (char[] respuesta : respuestas)
            if (respuesta == null || new String(respuesta).isBlank())
                throw new IllegalArgumentException("Las tres respuestas son obligatorias y no pueden quedar en blanco.");
    }
    private char[] normalizar(char[] respuesta) {
        return Normalizer.normalize(new String(respuesta).strip(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).toCharArray();
    }
    private Properties copia() {
        Properties siguientes = new Properties(); siguientes.putAll(config); return siguientes;
    }
    private void ponerSecreto(Properties siguientes, String prefijo, char[] valor) {
        byte[] sal = new byte[16]; new SecureRandom().nextBytes(sal);
        siguientes.setProperty(prefijo + ".sal", Base64.getEncoder().encodeToString(sal));
        siguientes.setProperty(prefijo + ".hash", Base64.getEncoder().encodeToString(derivar(valor, sal)));
    }
    /** Cambia únicamente la contraseña de una cuenta con recuperación ya configurada. */
    public void cambiar(char[] anterior, char[] nueva) throws IOException {
        validarCambio(anterior, nueva);
        if (!recuperacionConfigurada())
            throw new IllegalArgumentException("Configura las tres respuestas de recuperación junto con la contraseña.");
        Properties siguientes = copia(); ponerSecreto(siguientes, "admin", nueva); guardar(siguientes);
    }
    /** Alta inicial o actualización autorizada de contraseña y de las tres respuestas. */
    public void cambiar(char[] anterior, char[] nueva, char[][] respuestas) throws IOException {
        validarCambio(anterior, nueva); validarRespuestas(respuestas);
        Properties siguientes = copia(); ponerSecreto(siguientes, "admin", nueva);
        for (int i = 0; i < respuestas.length; i++) {
            char[] normalizada = normalizar(respuestas[i]);
            try { ponerSecreto(siguientes, "admin.recuperacion." + CLAVES_RECUPERACION.get(i), normalizada); }
            finally { Arrays.fill(normalizada, '\0'); }
        }
        guardar(siguientes);
    }
    /** Solo sustituye la contraseña cuando coinciden las tres respuestas registradas. */
    public void recuperar(char[][] respuestas, char[] nueva) throws IOException {
        if (!recuperacionConfigurada())
            throw new IllegalArgumentException("Aún no están configuradas las tres preguntas de recuperación.");
        validarRespuestas(respuestas); validarNueva(nueva);
        boolean correctas = true;
        for (int i = 0; i < respuestas.length; i++) {
            char[] normalizada = normalizar(respuestas[i]);
            try { correctas &= verificarSecreto("admin.recuperacion." + CLAVES_RECUPERACION.get(i), normalizada); }
            finally { Arrays.fill(normalizada, '\0'); }
        }
        if (!correctas) throw new IllegalArgumentException("Las respuestas de recuperación no son correctas.");
        Properties siguientes = copia(); ponerSecreto(siguientes, "admin", nueva); guardar(siguientes);
    }
    private void guardar(Properties siguientes) throws IOException {
        Files.createDirectories(ruta.toAbsolutePath().getParent());
        Path temporal = Files.createTempFile(ruta.toAbsolutePath().getParent(), ".clave-", ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(temporal)) { siguientes.store(w, "Configuración de SICOV"); }
            try { Files.move(temporal, ruta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING); }
            config.clear(); config.putAll(siguientes);
        } finally { Files.deleteIfExists(temporal); }
    }
}
