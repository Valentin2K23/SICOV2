package com.sicov.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ArchivoCsv {
    private ArchivoCsv() {}
    public static List<List<String>> leer(Path ruta, String cabecera) throws IOException {
        return leer(ruta, cabecera, new String[0]);
    }
    public static List<List<String>> leer(Path ruta, String cabecera, String... anteriores) throws IOException {
        if (!Files.exists(ruta)) return new ArrayList<>();
        String texto = Files.readString(ruta, StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "");
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"') {
                if (comillas && i + 1 < texto.length() && texto.charAt(i + 1) == '"') { campo.append('"'); i++; }
                else comillas = !comillas;
            } else if (c == ',' && !comillas) { fila.add(campo.toString()); campo.setLength(0); }
            else if ((c == '\n' || c == '\r') && !comillas) {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                fila.add(campo.toString()); campo.setLength(0);
                if (fila.stream().anyMatch(s -> !s.isEmpty())) filas.add(fila);
                fila = new ArrayList<>();
            } else campo.append(c);
        }
        if (comillas) throw new IOException("CSV incompleto: " + ruta);
        if (campo.length() > 0 || !fila.isEmpty()) { fila.add(campo.toString()); filas.add(fila); }
        if (filas.isEmpty()) return new ArrayList<>();
        List<String> encabezado = filas.remove(0);
        // Los archivos originales vacíos pueden adoptar la nueva cabecera al guardar.
        if (!String.join(",", encabezado).equals(cabecera)
                && !Arrays.asList(anteriores).contains(String.join(",", encabezado)) && !filas.isEmpty())
            throw new IOException("Formato no reconocido en " + ruta + ". Conserva una copia antes de migrar sus datos.");
        return filas;
    }
    public static void guardar(Path ruta, String cabecera, List<List<String>> filas) throws IOException {
        Files.createDirectories(ruta.toAbsolutePath().getParent());
        StringBuilder texto = new StringBuilder(cabecera).append('\n');
        for (List<String> fila : filas) {
            for (int i = 0; i < fila.size(); i++) {
                if (i > 0) texto.append(',');
                texto.append('"').append(fila.get(i).replace("\"", "\"\"")).append('"');
            }
            texto.append('\n');
        }
        Path temporal = Files.createTempFile(ruta.toAbsolutePath().getParent(), ".sicov-", ".tmp");
        try {
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            try { Files.move(temporal, ruta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporal); }
    }
}
