package com.colorinchi.app.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.mock;
import com.github.tomakehurst.wiremock.WireMockServer;

import reactor.netty.http.client.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamingChatClientTest {

    private WireMockServer wireMockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProviderRequestFactory requestFactory = new ProviderRequestFactory();
    private final ProviderResponseParser responseParser = new ProviderResponseParser(objectMapper);
    private final ChatAnalyticsService chatAnalyticsService = org.mockito.Mockito.mock(ChatAnalyticsService.class);
    private final ChatMetricsService chatMetricsService = org.mockito.Mockito.mock(ChatMetricsService.class);

    @BeforeEach
    void startServer() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterEach
    void stopServer() {
        wireMockServer.stop();
    }

    @Test
    void streamingReturnsChunks() {
        // SSE events MUST be separated by \n\n. This is what real providers send.
        String sseBody = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"},\"index\":0}]}\n"
                + "\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\" world\"},\"index\":0}]}\n"
                + "\n"
                + "data: [DONE]\n";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream")
                        .withBody(sseBody)));

        StreamingChatClient client = createClient(Duration.ofSeconds(5), true);
        List<String> chunks = client.stream("qwen3.6", sampleMessages())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).containsExactly("Hello", " world");
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void streamingWithDisabledAiReturnsEmpty() {
        StreamingChatClient client = createClient(Duration.ofSeconds(5), false);
        List<String> chunks = client.stream("qwen3.6", sampleMessages())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).isEmpty();
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void streamingWithTimeoutThrows() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream")
                        .withBody("data: {\"choices\":[{\"delta\":{\"content\":\"Slow\"},\"index\":0}]}")
                        .withFixedDelay(5000)));

        StreamingChatClient client = createClient(Duration.ofMillis(500), true);

        assertThatThrownBy(() -> client.stream("qwen3.6", sampleMessages())
                .collectList()
                .block(Duration.ofSeconds(10)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void streamingWithServerErrorThrows() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(500)));

        StreamingChatClient client = createClient(Duration.ofSeconds(5), true);

        assertThatThrownBy(() -> client.stream("qwen3.6", sampleMessages())
                .collectList()
                .block(Duration.ofSeconds(5)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void streamingWithInvalidResponseBodyReturnsEmpty() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream")
                        .withBody("not valid sse")));

        StreamingChatClient client = createClient(Duration.ofSeconds(5), true);
        List<String> chunks = client.stream("qwen3.6", sampleMessages())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).isEmpty();
    }

    // --- Helpers ---

    private StreamingChatClient createClient(Duration readTimeout, boolean enabled) {
        AiServerProperties props = new AiServerProperties(
                wireMockServer.baseUrl(),
                "/v1/chat/completions",
                "qwen3.6",
                "test-key",
                500,
                enabled,
                Duration.ofSeconds(1),
                readTimeout,
                null);

        var httpClient = HttpClient.create()
                .responseTimeout(readTimeout);

        var webClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> headers.setBearerAuth(props.apiKey()))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return new StreamingChatClient(webClient, props, requestFactory, responseParser, chatAnalyticsService, chatMetricsService);
    }

    private static List<Map<String, String>> sampleMessages() {
        return List.of(
                Map.of("role", "system", "content", "Eres un asistente de moda."),
                Map.of("role", "user", "content", "¿Qué me recomiendas?"));
    }
}
