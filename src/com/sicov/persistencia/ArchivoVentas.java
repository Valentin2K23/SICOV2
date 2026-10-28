package com.sicov.persistencia;
import java.nio.file.Path;
public final class ArchivoVentas extends ArchivoOperaciones {
    public ArchivoVentas(Path directorio) { super(directorio.resolve("ventas.csv")); }
}
