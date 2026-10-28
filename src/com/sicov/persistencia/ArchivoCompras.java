package com.sicov.persistencia;
import java.nio.file.Path;
public final class ArchivoCompras extends ArchivoOperaciones {
    public ArchivoCompras(Path directorio) { super(directorio.resolve("compras.csv")); }
}
