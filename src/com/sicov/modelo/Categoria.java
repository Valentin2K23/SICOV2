package com.sicov.modelo;

import java.util.List;

public record Categoria(String nombre, String seccion) {
    public static final List<String> SECCIONES = List.of("Repostería", "Dulces", "Bebidas", "Refrescos",
            "Lácteos", "Salchichonería", "Carnes", "Tecnología", "Farmacia", "Higiene", "Hogar",
            "Snacks", "Botana", "Alcohol", "Abarrotes", "Helados", "Tabacos", "Juguetes");
    public Categoria {
        nombre = nombre.strip(); seccion = seccion.strip();
        if (nombre.isEmpty()) throw new IllegalArgumentException("Escribe el nombre de la categoría.");
        if (!seccion.isEmpty() && !SECCIONES.contains(seccion))
            throw new IllegalArgumentException("Selecciona una sección de la lista.");
    }
    public String seccionVisible() { return seccion.isEmpty() ? "Sin sección" : seccion; }
}
