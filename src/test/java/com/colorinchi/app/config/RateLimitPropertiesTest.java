package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPropertiesTest {

    @Test
    void holdsAnalyzeAndRecommendationConfig() {
        var analyze = new RateLimitProperties.EndpointConfig(5, 1);
        var recommend = new RateLimitProperties.EndpointConfig(10, 5);
        var props = new RateLimitProperties(analyze, recommend);

        assertThat(props.analyze().capacity()).isEqualTo(5);
        assertThat(props.analyze().refillMinutes()).isEqualTo(1);
        assertThat(props.recommendation().capacity()).isEqualTo(10);
        assertThat(props.recommendation().refillMinutes()).isEqualTo(5);
    }
}
