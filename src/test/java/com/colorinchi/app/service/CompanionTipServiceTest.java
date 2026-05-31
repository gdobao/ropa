package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.colorinchi.app.dto.chat.CategoryInfo;
import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.DailyPlanInfo;
import com.colorinchi.app.dto.chat.MaterialInfo;
import com.colorinchi.app.dto.chat.WardrobeContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CompanionTipServiceTest {

    private final CompanionTipService service = new CompanionTipService(mock(WardrobeContextAssembler.class));

    @Test
    void createReturnsBootstrapTipsWhenWardrobeIsEmpty() {
        WardrobeContext context = new WardrobeContext(
                0,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                0,
                0,
                0,
                new DailyPlanInfo("Lunes", 0, List.of()),
                List.of(),
                List.of());

        var result = service.create(context);

        assertThat(result.summary()).isEqualTo("Todavía no hay prendas cargadas en el armario.");
        assertThat(result.tips()).hasSize(3);
        assertThat(result.tips().get(0)).contains("Cargá algunas prendas base");
    }

    @Test
    void createBuildsDeterministicPlanningAndColorTips() {
        WardrobeContext context = new WardrobeContext(
                6,
                List.of(new CategoryInfo("Tops", 3)),
                List.of(
                        new ColorInfo("Negro", "#000000", 4),
                        new ColorInfo("Blanco", "#FFFFFF", 2)),
                List.of(new MaterialInfo("Algodón", 3)),
                Map.of("Invierno", 4L),
                0,
                2,
                3,
                new DailyPlanInfo("Lunes", 0, List.of()),
                List.of(new DailyPlanInfo("Martes", 2, List.of("Remera negra", "Jean"))),
                List.of());

        var result = service.create(context);

        assertThat(result.summary()).contains("Armario con 6 prendas, dominado por Negro.");
        assertThat(result.tips()).containsExactly(
                "La planificación semanal todavía es corta: sumar más días te va a dar mejor continuidad entre looks.",
                "Hay prendas planificadas en la semana, pero hoy no tiene look armado: conviene cerrar ese hueco primero.",
                "El armario está muy concentrado en Negro: conviene sumar acentos o neutros de apoyo para ampliar combinaciones.");
    }

    @Test
    void createReturnsPlanEmptyTipWhenPlannedDaysIsZero() {
        WardrobeContext context = new WardrobeContext(
                3,
                List.of(new CategoryInfo("Tops", 2), new CategoryInfo("Bottoms", 1)),
                List.of(new ColorInfo("Negro", "#000000", 2), new ColorInfo("Blanco", "#FFFFFF", 1)),
                List.of(),
                Map.of("Invierno", 2L),
                0,  // favoritesCount == 0 — triggers favorites tip
                0,  // plannedDays == 0 — triggers "plan empty" tip
                0,
                new DailyPlanInfo("Lunes", 0, List.of()),
                List.of(),
                List.of());

        var result = service.create(context);

        assertThat(result.summary()).contains("Armario con 3 prendas, dominado por Negro.");
        assertThat(result.summary()).endsWith("Todavía no hay planificación semanal cargada.");
        assertThat(result.tips()).contains(
                "No hay días planificados todavía: armá aunque sea 1 o 2 días para que el companion pueda detectar repeticiones y huecos.",
                "Todavía no hay prendas favoritas marcadas: eso le va a dar al companion una señal clara de preferencia real.");
        assertThat(result.tips()).hasSize(2);
    }

    @Test
    void createReturnsSeasonAndColorTipsWhenSeasonsAndColorsEmpty() {
        WardrobeContext context = new WardrobeContext(
                5,
                List.of(new CategoryInfo("Tops", 3), new CategoryInfo("Bottoms", 2)),
                List.of(),  // colors empty — triggers "Faltan colores" tip
                List.of(),
                Map.of(),   // seasons empty — triggers "No hay temporadas" tip
                2,          // favoritesCount > 0 — no favorites tip generated
                4,          // plannedDays >= 3 — no planning tip
                7,
                new DailyPlanInfo("Lunes", 2, List.of("Remera", "Jean")),
                List.of(),
                List.of());

        var result = service.create(context);

        assertThat(result.summary()).contains("Armario con 5 prendas");
        assertThat(result.summary()).contains("Hay 4 día(s) planificados esta semana");
        assertThat(result.summary()).contains("Hay 2 favorita(s) marcadas");
        assertThat(result.tips()).containsExactly(
                "Faltan colores registrados en las prendas: completar ese dato mejora mucho las recomendaciones del companion.",
                "No hay temporadas cargadas en las prendas: completar ese dato ayuda a filtrar mejor qué combinar según clima y uso.");
    }
}
