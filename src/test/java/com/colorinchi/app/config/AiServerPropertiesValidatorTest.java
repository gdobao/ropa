package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiServerPropertiesValidatorTest {

    @Test
    void skipsValidationWhenDisabled() {
        var props = new AiServerProperties(null, null, null, null, 0, false, null, null);
        var validator = new AiServerPropertiesValidator(props);
        validator.run(new DefaultApplicationArguments());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        var props = new AiServerProperties("http://localhost", "/chat", "gpt-4", null, 2000, true, null, null);
        var validator = new AiServerPropertiesValidator(props);
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API_KEY");
    }

    @Test
    void throwsWhenBaseUrlMissing() {
        var props = new AiServerProperties(null, "/chat", "gpt-4", "sk-123", 2000, true, null, null);
        var validator = new AiServerPropertiesValidator(props);
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base-url");
    }

    @Test
    void throwsWhenChatPathMissing() {
        var props = new AiServerProperties("http://localhost", null, "gpt-4", "sk-123", 2000, true, null, null);
        var validator = new AiServerPropertiesValidator(props);
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chat-path");
    }

    @Test
    void throwsWhenModelMissing() {
        var props = new AiServerProperties("http://localhost", "/chat", null, "sk-123", 2000, true, null, null);
        var validator = new AiServerPropertiesValidator(props);
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("model");
    }

    @Test
    void succeedsWithAllFieldsPresent() {
        var props = new AiServerProperties("http://localhost", "/chat", "gpt-4", "sk-123", 2000, true, null, null);
        var validator = new AiServerPropertiesValidator(props);
        validator.run(new DefaultApplicationArguments());
    }
}
