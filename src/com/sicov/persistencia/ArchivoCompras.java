package com.sicov.persistencia;

import java.nio.file.Path;

/**
 * Historial de recepciones de mercancía, con el mismo formato de los demás movimientos.
 */
public final class ArchivoCompras extends ArchivoOperaciones {
    public ArchivoCompras(Path directorio) {
        super(directorio.resolve("compras.csv"));
    }
}
