package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.CompanionTipContext;
import com.colorinchi.app.dto.chat.WardrobeContext;

@Service
public class CompanionTipService {

    private final WardrobeContextAssembler wardrobeContextAssembler;

    public CompanionTipService(WardrobeContextAssembler wardrobeContextAssembler) {
        this.wardrobeContextAssembler = wardrobeContextAssembler;
    }

    public CompanionTipContext assemble() {
        return create(wardrobeContextAssembler.assemble());
    }

    public CompanionTipContext create(WardrobeContext context) {
        if (context.totalGarments() == 0) {
            return new CompanionTipContext(
                    "Todavía no hay prendas cargadas en el armario.",
                    List.of(
                            "Cargá algunas prendas base para que el companion pueda detectar combinaciones y huecos reales.",
                            "Priorizá prendas de uso frecuente con color y material completos para mejorar las sugerencias.",
                            "Armá al menos un día del plan semanal para que el companion pueda detectar repeticiones o faltantes."));
        }

        List<String> tips = new ArrayList<>();

        if (context.plannedDays() == 0) {
            tips.add("No hay días planificados todavía: armá aunque sea 1 o 2 días para que el companion pueda detectar repeticiones y huecos.");
        } else if (context.plannedDays() < 3) {
            tips.add("La planificación semanal todavía es corta: sumar más días te va a dar mejor continuidad entre looks.");
        }

        if (context.todayPlan() != null && context.todayPlan().garmentCount() == 0 && context.plannedDays() > 0) {
            tips.add("Hay prendas planificadas en la semana, pero hoy no tiene look armado: conviene cerrar ese hueco primero.");
        }

        if (context.colors().isEmpty()) {
            tips.add("Faltan colores registrados en las prendas: completar ese dato mejora mucho las recomendaciones del companion.");
        } else if (hasDominantColor(context)) {
            ColorInfo dominant = context.colors().get(0);
            tips.add("El armario está muy concentrado en " + dominant.colorName()
                    + ": conviene sumar acentos o neutros de apoyo para ampliar combinaciones.");
        }

        if (context.favoritesCount() == 0) {
            tips.add("Todavía no hay prendas favoritas marcadas: eso le va a dar al companion una señal clara de preferencia real.");
        }

        if (context.seasons().isEmpty()) {
            tips.add("No hay temporadas cargadas en las prendas: completar ese dato ayuda a filtrar mejor qué combinar según clima y uso.");
        }

        if (tips.isEmpty()) {
            tips.add("El armario ya tiene buena base de datos: el companion puede enfocarse en optimizar rotación, contraste y planificación fina.");
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

        return summary.toString();
    }
}
