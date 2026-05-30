package com.colorinchi.app.service;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.config.AiServerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelRouterTest {

    private ModelRouter router;

    @BeforeEach
    void setUp() {
        var models = List.of(
                model("gemma4", "Gemma 4", false),
                model("qwen3.6", "Qwen 3.6", true),
                model("deepseek-v4-flash", "DeepSeek V4 Flash", false));
        var props = new AiServerProperties(
                "https://api.test", "/v1/chat/completions", "qwen3.6",
                "sk-test", 2000, true,
                Duration.ofSeconds(5), Duration.ofSeconds(30),
                models);
        router = new ModelRouter(props);
    }

    @Test
    void resolveNullReturnsDefault() {
        var result = router.resolve(null);
        assertThat(result.getId()).isEqualTo("qwen3.6");
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void resolveEmptyReturnsDefault() {
        var result = router.resolve("");
        assertThat(result.getId()).isEqualTo("qwen3.6");
    }

    @Test
    void resolveValidModelReturnsConfig() {
        var result = router.resolve("gemma4");
        assertThat(result.getId()).isEqualTo("gemma4");
        assertThat(result.getProvider()).isEqualTo("google");
    }

    @Test
    void resolveInvalidModelThrows() {
        assertThatThrownBy(() -> router.resolve("gpt-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gpt-4")
                .hasMessageContaining("gemma4")
                .hasMessageContaining("qwen3.6")
                .hasMessageContaining("deepseek-v4-flash");
    }

    @Test
    void findDefaultReturnsQwen() {
        var result = router.findDefault();
        assertThat(result.getId()).isEqualTo("qwen3.6");
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void getAvailableModelsReturnsAll() {
        var available = router.getAvailableModels();
        assertThat(available).hasSize(3);
        assertThat(available).extracting(AiModelConfig::getId)
                .containsExactly("gemma4", "qwen3.6", "deepseek-v4-flash");
    }

    @Test
    void getAvailableModelsIsUnmodifiable() {
        var available = router.getAvailableModels();
        assertThatThrownBy(() -> available.add(model("new-model", "New", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Helpers ---

    private static AiModelConfig model(String id, String name, boolean isDefault) {
        var m = new AiModelConfig();
        m.setId(id);
        m.setName(name);
        m.setProvider(id.contains("gemma") ? "google" : id.contains("qwen") ? "alibaba" : "deepseek");
        m.setDefault(isDefault);
        return m;
    }
}
