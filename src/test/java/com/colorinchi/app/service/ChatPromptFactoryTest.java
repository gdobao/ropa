package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.dto.chat.CategoryInfo;
import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.DailyPlanInfo;
import com.colorinchi.app.dto.chat.MaterialInfo;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.ChatSurface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatPromptFactoryTest {

    @Mock
    private WardrobeContextAssembler wardrobeContextAssembler;

    private ChatPromptFactory chatPromptFactory;

    @BeforeEach
    void setUp() {
        CompanionTipService companionTipService = new CompanionTipService(wardrobeContextAssembler);
        chatPromptFactory = new ChatPromptFactory(wardrobeContextAssembler, companionTipService);

        when(wardrobeContextAssembler.assemble()).thenReturn(new WardrobeContext(
                6,
                List.of(new CategoryInfo("Tops", 3)),
                List.of(new ColorInfo("Negro", "#000000", 4)),
                List.of(new MaterialInfo("Algodón", 3)),
                Map.of("Invierno", 4L),
                0,
                2,
                3,
                new DailyPlanInfo("Lunes", 0, List.of()),
                List.of(new DailyPlanInfo("Martes", 2, List.of("Remera negra", "Jean"))),
                List.of()));
    }

    @Test
    void buildSystemPromptKeepsMainChatBehavior() {
        String prompt = chatPromptFactory.buildSystemPrompt();

        assertThat(prompt).contains("Eres Colorín, un asesor de moda experto y conversacional.");
        assertThat(prompt).doesNotContain("Eres Colorín, el companion personal de estilo de Rebeca.");
        assertThat(prompt).doesNotContain("SEÑALES CALCULADAS PARA EL COMPANION");
    }

    @Test
    void buildSystemPromptForCompanionAddsCompanionSpecificGuidance() {
        String prompt = chatPromptFactory.buildSystemPrompt(ChatSurface.COMPANION);

        assertThat(prompt).contains("Eres Colorín, el companion personal de estilo de Rebeca.");
        assertThat(prompt).contains("SEÑALES CALCULADAS PARA EL COMPANION:");
        assertThat(prompt).contains("Resumen: Armario con 6 prendas, dominado por Negro.");
        assertThat(prompt).contains("REGLAS DE COMPATIBILIDAD ENTRE CATEGORÍAS");
    }
}
