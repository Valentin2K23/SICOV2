package com.sicov.modelo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record Promocion(String id, String nombre, String categoria, BigDecimal precio,
                        boolean activa, Map<String, BigDecimal> componentes) {
    public Promocion {
        nombre = nombre.strip(); categoria = categoria.strip();
        if (nombre.isBlank() || categoria.isBlank() || precio.signum() < 0 || precio.scale() > 2
                || componentes.isEmpty() || componentes.values().stream().anyMatch(q -> q.signum() <= 0))
            throw new IllegalArgumentException("Completa la promoción, su precio y las cantidades de sus productos.");
        componentes = Collections.unmodifiableMap(new LinkedHashMap<>(componentes));
    }
}
