package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.colorinchi.app.colorimetry.service.ColorSeasonClassifier;
import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.CompanionTipContext;
import com.colorinchi.app.dto.chat.WardrobeContext;

@Service
public class CompanionTipService {

    private final WardrobeContextAssembler wardrobeContextAssembler;
    private final ColorSeasonClassifier classifier;

    public CompanionTipService(WardrobeContextAssembler wardrobeContextAssembler,
            ColorSeasonClassifier classifier) {
        this.wardrobeContextAssembler = wardrobeContextAssembler;
        this.classifier = classifier;
    }

    public CompanionTipContext assemble() {
        return create(wardrobeContextAssembler.assemble());
    }

    public CompanionTipContext create(WardrobeContext context) {
        if (context.totalGarments() == 0) {
            return new CompanionTipContext(
                    "Todavía no hay prendas cargadas en el armario.",
                    List.of(
                            "Carga algunas prendas base para que el asistente pueda detectar combinaciones y huecos reales.",
                            "Prioriza prendas de uso frecuente con color y material completos para mejorar las sugerencias.",
                            "Prepara al menos un día del plan semanal para que el asistente pueda detectar repeticiones o faltantes."));
        }

        List<String> tips = new ArrayList<>();

        if (context.plannedDays() == 0) {
            tips.add("No hay días planificados todavía: prepara al menos 1 o 2 días para que el asistente pueda detectar repeticiones y huecos.");
        } else if (context.plannedDays() < 3) {
            tips.add("La planificación semanal todavía es corta: sumar más días te dará mejor continuidad entre looks.");
        }

        if (context.todayPlan() != null && context.todayPlan().garmentCount() == 0 && context.plannedDays() > 0) {
            tips.add("Hay prendas planificadas en la semana, pero hoy no tiene look armado: conviene cerrar ese hueco primero.");
        }

        if (context.colors().isEmpty()) {
            tips.add("Faltan colores registrados en las prendas: completar ese dato mejora mucho las recomendaciones del asistente.");
        } else if (hasDominantColor(context)) {
            ColorInfo dominant = context.colors().get(0);
            tips.add("El armario está muy concentrado en " + dominant.colorName()
                    + ": conviene sumar acentos o neutros de apoyo para ampliar combinaciones.");
        }

        if (!context.colorSeasons().isEmpty()) {
            Map.Entry<String, Long> unknownEntry = null;
            Long unknownCount = 0L;
            for (Map.Entry<String, Long> e : context.colorSeasons().entrySet()) {
                if ("Sin estación".equals(e.getKey())) {
                    unknownEntry = e;
                    unknownCount = e.getValue();
                }
            }

            if (unknownCount > 0) {
                tips.add(unknownCount + " prenda(s) no tienen una estación colorimétrica definida: "
                        + "revisa sus colores para obtener mejores sugerencias.");
            }

            List<Map.Entry<String, Long>> realSeasons = context.colorSeasons().entrySet().stream()
                    .filter(e -> !"Sin estación".equals(e.getKey()))
                    .toList();

            if (realSeasons.size() == 1) {
                Map.Entry<String, Long> only = realSeasons.get(0);
                tips.add("Todas tus prendas con estación definida son de " + only.getKey()
                        + ": sumar alguna de otra estación puede ampliar mucho tus combinaciones.");
            } else if (realSeasons.size() >= 2) {
                Map.Entry<String, Long> max = realSeasons.stream()
                        .max(Map.Entry.comparingByValue()).orElse(null);
                Map.Entry<String, Long> min = realSeasons.stream()
                        .min(Map.Entry.comparingByValue()).orElse(null);
                if (max != null && min != null && max.getValue() > min.getValue() * 3) {
                    tips.add("Hay muchas más prendas de " + max.getKey()
                            + " que de " + min.getKey()
                            + ": equilibrar las estaciones te dará más variedad de combinaciones.");
                }
            }
        }

        if (context.favoritesCount() == 0) {
            tips.add("Todavía no hay prendas favoritas marcadas: eso le dará al asistente una señal clara de preferencia real.");
        }

        if (context.seasons().isEmpty()) {
            tips.add("No hay temporadas cargadas en las prendas: completar ese dato ayuda a filtrar mejor qué combinar según clima y uso.");
        }

        if (tips.isEmpty()) {
            tips.add("El armario ya tiene buena base de datos: el asistente puede enfocarse en optimizar rotación, contraste y planificación fina.");
        }

        return new CompanionTipContext(buildSummary(context), tips.stream().distinct().limit(3).toList());
    }

    private boolean hasDominantColor(WardrobeContext context) {
        if (context.colors().isEmpty()) {
            return false;
        }
        ColorInfo dominant = context.colors().get(0);
        return dominant.count() >= 3 && dominant.count() * 2 >= context.totalGarments();
    }

    private String buildSummary(WardrobeContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("Armario con ").append(context.totalGarments()).append(" prendas");
        if (!context.colors().isEmpty()) {
            summary.append(", dominado por ").append(context.colors().get(0).colorName());
        }
        summary.append(".");

        if (context.plannedDays() > 0) {
            summary.append(" Hay ").append(context.plannedDays()).append(" día(s) planificados esta semana");
            if (context.todayPlan() != null && context.todayPlan().garmentCount() > 0) {
                summary.append(" y hoy ya tiene ").append(context.todayPlan().garmentCount()).append(" prenda(s) asignadas");
            }
            summary.append(".");
        } else {
            summary.append(" Todavía no hay planificación semanal cargada.");
        }

        if (context.favoritesCount() > 0) {
            summary.append(" Hay ").append(context.favoritesCount()).append(" favorita(s) marcadas.");
        }

        if (!context.colorSeasons().isEmpty()) {
            summary.append(" Distribución colorimétrica: ");
            context.colorSeasons().forEach((season, count) ->
                summary.append(season).append("=").append(count).append(", "));
            summary.setLength(summary.length() - 2); // remove trailing ", "
            summary.append(".");
        }

        return summary.toString();
    }
}
