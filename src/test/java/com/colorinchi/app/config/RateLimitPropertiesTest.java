package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPropertiesTest {

    @Test
    void holdsAnalyzeAndRecommendationAndChatConfig() {
        var analyze = new RateLimitProperties.EndpointConfig(5, 1);
        var recommend = new RateLimitProperties.EndpointConfig(10, 5);
        var chat = new RateLimitProperties.EndpointConfig(30, 1);
        var chatPerOwner = new RateLimitProperties.EndpointConfig(30, 1);
        var props = new RateLimitProperties(analyze, recommend, chat, chatPerOwner);

        assertThat(props.analyze().capacity()).isEqualTo(5);
        assertThat(props.analyze().refillMinutes()).isEqualTo(1);
        assertThat(props.recommendation().capacity()).isEqualTo(10);
        assertThat(props.recommendation().refillMinutes()).isEqualTo(5);
        assertThat(props.chat().capacity()).isEqualTo(30);
        assertThat(props.chat().refillMinutes()).isEqualTo(1);
        assertThat(props.chatPerOwner().capacity()).isEqualTo(30);
        assertThat(props.chatPerOwner().refillMinutes()).isEqualTo(1);
    }
}
