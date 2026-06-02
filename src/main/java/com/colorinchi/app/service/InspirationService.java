package com.colorinchi.app.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.colorinchi.app.dto.InspirationLook;

@Service
public class InspirationService {

    private final List<InspirationLook> looks = seedLooks();

    public List<InspirationLook> getAll() {
        return looks;
    }

    public Set<String> getAllTags() {
        return looks.stream()
            .flatMap(l -> l.tags().stream())
            .collect(Collectors.toSet());
    }

    private List<InspirationLook> seedLooks() {
        return List.of(
            new InspirationLook(1, "Cápsula oficina", "6 piezas para toda la semana laboral. Combina neutros con un toque de color.",
                List.of("Minimal", "Oficina"), "Oficina",
                List.of("Top", "Pantalón", "Chaqueta", "Zapatos"),
                "var(--accent)", "var(--sand)", "var(--terracotta)"),
            new InspirationLook(2, "Cena elegante", "Look sofisticado con prendas que ya tienes. Vestido + tacón + bolso.",
                List.of("Elegante", "Cena"), "Cena",
                List.of("Vestido", "Zapatos", "Accesorio"),
                "var(--fg)", "var(--teal)", "var(--accent)"),
            new InspirationLook(3, "Finde casual", "Outfit relajado para el fin de semana. Jeans + remera + zapatillas.",
                List.of("Casual", "Finde"), "Casual",
                List.of("Top", "Pantalón", "Zapatos"),
                "var(--sand)", "var(--border)", "var(--muted)"),
            new InspirationLook(4, "Capas de invierno", "Superposiciones para el frío sin perder estilo.",
                List.of("Invierno", "Capas"), "Invierno",
                List.of("Top", "Chaqueta", "Pantalón", "Zapatos"),
                "var(--terracotta)", "var(--fg)", "var(--sand)"),
            new InspirationLook(5, "Minimalista total", "Menos es más. Paleta monocromática con texturas.",
                List.of("Minimal", "Elegante"), "Minimal",
                List.of("Top", "Pantalón", "Zapatos"),
                "var(--surface)", "var(--border)", "var(--muted)"),
            new InspirationLook(6, "Sport chic", "Comodidad y estilo para el día a día activo.",
                List.of("Sport", "Casual"), "Sport",
                List.of("Top", "Pantalón", "Zapatos", "Accesorio"),
                "var(--accent)", "var(--sand)", "var(--teal)")
        );
    }
}
