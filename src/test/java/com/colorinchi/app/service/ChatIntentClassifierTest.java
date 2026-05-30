package com.colorinchi.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.service.ChatIntentClassifier.Intent;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatIntentClassifierTest {

    private ChatIntentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ChatIntentClassifier();
    }

    @Test
    void nullMessageReturnsGeneralChat() {
        assertThat(classifier.classify(null)).isEqualTo(Intent.GENERAL_CHAT);
    }

    @Test
    void blankMessageReturnsGeneralChat() {
        assertThat(classifier.classify("   ")).isEqualTo(Intent.GENERAL_CHAT);
    }

    @Test
    void generalChatReturnsGeneralChat() {
        assertThat(classifier.classify("Hola, ¿cómo estás?")).isEqualTo(Intent.GENERAL_CHAT);
        assertThat(classifier.classify("gracias por la ayuda")).isEqualTo(Intent.GENERAL_CHAT);
        assertThat(classifier.classify("chau")).isEqualTo(Intent.GENERAL_CHAT);
    }

    @Test
    void outfitRequestInSpanishDetected() {
        assertThat(classifier.classify("elige un outfit para mí")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("armame un look")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("dame un outfit completo")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("qué me pongo hoy")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("elegí qué ropa ponerme")).isEqualTo(Intent.OUTFIT_REQUEST);
    }

    @Test
    void outfitRequestInEnglishDetected() {
        assertThat(classifier.classify("pick an outfit for me")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("what should I wear today")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("choose an outfit for the party")).isEqualTo(Intent.OUTFIT_REQUEST);
        assertThat(classifier.classify("suggest an outfit")).isEqualTo(Intent.OUTFIT_REQUEST);
    }

    @Test
    void wardrobeQuestionInSpanishDetected() {
        assertThat(classifier.classify("qué prendas tengo en mi armario")).isEqualTo(Intent.WARDROBE_QUESTION);
        assertThat(classifier.classify("mostrame mi guardarropa")).isEqualTo(Intent.WARDROBE_QUESTION);
        assertThat(classifier.classify("cuántas prendas tengo")).isEqualTo(Intent.WARDROBE_QUESTION);
        assertThat(classifier.classify("enseñame la ropa que tengo")).isEqualTo(Intent.WARDROBE_QUESTION);
    }

    @Test
    void wardrobeQuestionInEnglishDetected() {
        assertThat(classifier.classify("what garments do I have")).isEqualTo(Intent.WARDROBE_QUESTION);
        assertThat(classifier.classify("do I have a blue shirt")).isEqualTo(Intent.WARDROBE_QUESTION);
        assertThat(classifier.classify("show me my wardrobe")).isEqualTo(Intent.WARDROBE_QUESTION);
    }

    @Test
    void stylingAdviceInSpanishDetected() {
        assertThat(classifier.classify("cómo combino esta camisa")).isEqualTo(Intent.STYLING_ADVICE);
        assertThat(classifier.classify("qué me recomendas con este pantalón")).isEqualTo(Intent.STYLING_ADVICE);
        assertThat(classifier.classify("consejos de estilo para esta chaqueta")).isEqualTo(Intent.STYLING_ADVICE);
    }

    @Test
    void stylingAdviceInEnglishDetected() {
        assertThat(classifier.classify("how to style this shirt")).isEqualTo(Intent.STYLING_ADVICE);
        assertThat(classifier.classify("styling advice for my wardrobe")).isEqualTo(Intent.STYLING_ADVICE);
        assertThat(classifier.classify("what goes with a red skirt")).isEqualTo(Intent.STYLING_ADVICE);
    }

    @Test
    void colorAdviceInSpanishDetected() {
        assertThat(classifier.classify("qué colores combinan con el azul")).isEqualTo(Intent.COLOR_ADVICE);
        assertThat(classifier.classify("teoría del color para mi armario")).isEqualTo(Intent.COLOR_ADVICE);
        assertThat(classifier.classify("qué tonos me quedan mejor")).isEqualTo(Intent.COLOR_ADVICE);
    }

    @Test
    void colorAdviceInEnglishDetected() {
        assertThat(classifier.classify("what colors match with green")).isEqualTo(Intent.COLOR_ADVICE);
        assertThat(classifier.classify("color palette advice")).isEqualTo(Intent.COLOR_ADVICE);
        assertThat(classifier.classify("what shade of blue is best")).isEqualTo(Intent.COLOR_ADVICE);
    }

    @Test
    void isOutfitRequestReturnsTrueForOutfitRequests() {
        assertThat(classifier.isOutfitRequest("pick an outfit")).isTrue();
        assertThat(classifier.isOutfitRequest("hola")).isFalse();
    }

    @Test
    void outfitRequestTakesPriorityOverStylingAdvice() {
        // Even if it could match styling, outfit_request takes priority
        assertThat(classifier.classify("pick an outfit and tell me how to style it"))
                .isEqualTo(Intent.OUTFIT_REQUEST);
    }
}
