package com.colorinchi.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.dto.chat.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatResponseValidatorTest {

    private ChatResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChatResponseValidator();
    }

    @Test
    void nullResponseReturnsInvalid() {
        ValidationResult result = validator.validate(null);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void blankResponseReturnsInvalid() {
        ValidationResult result = validator.validate("   ");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void safeAdviceResponseReturnsValid() {
        String response = "Podrías combinar ese pantalón azul con una camisa blanca "
                + "para un look fresco. Otra opción es usar tonos beige que también armonizan.";

        ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isTrue();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void prescriptiveResponseDetected() {
        String response = "Te recomiendo que uses el pantalón azul con la camisa blanca.";

        ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.warnings()).anyMatch(w -> w.contains("prescriptiva"));
    }

    @Test
    void strongPrescriptiveResponseDetected() {
        String response = "Ponete el vestido rojo con los zapatos negros.";

        ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.warnings()).anyMatch(w -> w.contains("prescriptiva"));
    }

    @Test
    void suggestionWithConditionalIsNotPrescriptive() {
        String response = "Una opción podría ser combinar el pantalón con una camisa "
                + "clara. ¿Qué te parece?";

        ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void expectedRefusalDetected() {
        assertThat(validator.isExpectedRefusal("No puedo elegir un outfit por vos, "
                + "pero puedo darte ideas.")).isTrue();

        assertThat(validator.isExpectedRefusal("La decisión final es tuya, "
                + "pero te cuento qué opciones tenés.")).isTrue();

        assertThat(validator.isExpectedRefusal("Mi rol es aconsejarte, "
                + "no elegir por vos.")).isTrue();
    }

    @Test
    void normalResponseIsNotRefusal() {
        assertThat(validator.isExpectedRefusal("Ese pantalón es de algodón, "
                + "ideal para el verano.")).isFalse();
    }

    @Test
    void nullResponseIsNotRefusal() {
        assertThat(validator.isExpectedRefusal(null)).isFalse();
    }
}
