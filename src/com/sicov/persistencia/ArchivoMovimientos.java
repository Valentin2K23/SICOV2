package com.sicov.persistencia;
import java.nio.file.Path;
/** Registros de merma, sin cálculo de existencias. */
public final class ArchivoMovimientos extends ArchivoOperaciones {
    public ArchivoMovimientos(Path directorio) { super(directorio.resolve("movimientos_inventario.csv")); }
}
