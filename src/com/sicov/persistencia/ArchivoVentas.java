package com.sicov.persistencia;

import java.nio.file.Path;

/**
 * Historial de ventas y sus cancelaciones; comparte el formato de ArchivoOperaciones.
 */
public final class ArchivoVentas extends ArchivoOperaciones {
    public ArchivoVentas(Path directorio) {
        super(directorio.resolve("ventas.csv"));
    }
}
