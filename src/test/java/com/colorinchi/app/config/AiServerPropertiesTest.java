package com.colorinchi.app.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiServerPropertiesTest {

    @Test
    void holdsAllValues() {
        var props = new AiServerProperties(
                "http://localhost", "/chat", "gpt-4", "sk-123",
                2000, true, Duration.ofSeconds(5), Duration.ofSeconds(20)
        );
        assertThat(props.baseUrl()).isEqualTo("http://localhost");
        assertThat(props.chatPath()).isEqualTo("/chat");
        assertThat(props.model()).isEqualTo("gpt-4");
        assertThat(props.apiKey()).isEqualTo("sk-123");
        assertThat(props.maxTokens()).isEqualTo(2000);
        assertThat(props.enabled()).isTrue();
        assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void disabledWhenEnabledFalse() {
        var props = new AiServerProperties(null, null, null, null, 0, false, null, null);
        assertThat(props.enabled()).isFalse();
    }
}
