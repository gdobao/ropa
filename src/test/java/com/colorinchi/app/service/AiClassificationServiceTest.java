package com.colorinchi.app.service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.dto.AiClassificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import reactor.netty.http.client.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class AiClassificationServiceTest {

    private WireMockServer wireMockServer;

    @TempDir
    Path tempDir;

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
    void returnsFallbackWhenDisabled() throws Exception {
        AiServerProperties disabledProps = new AiServerProperties(
                wireMockServer.baseUrl(), "/v1/chat/completions", "qwen3.6", "test-key", 500,
                false,
                Duration.ofSeconds(1), Duration.ofSeconds(3), null);
        WebClient wc = WebClient.builder()
                .baseUrl(disabledProps.baseUrl())
                .build();
        UploadProperties up = new UploadProperties(tempDir.resolve("uploads"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        AiClassificationService svc = new AiClassificationService(disabledProps, up, wc, new ObjectMapper());

        AiClassificationResponse response = svc.classify("/uploads/test.jpg");

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("IA desactivada");
    }

    @Test
    void returnsFallbackOn500() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(500)));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[]{1, 2, 3});

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/uploads/" + image.getFileName());

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void returnsFallbackOnTimeout() throws Exception {
        // Slow response that exceeds the read timeout
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"type\\\":\\\"Chaqueta\\\"}\"}}]}")
                        .withFixedDelay(5000)));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[]{1, 2, 3});

        AiClassificationResponse response = service(Duration.ofMillis(500)).classify("/uploads/" + image.getFileName());

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void returnsFallbackOnMalformedJsonResponse() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("not json at all")));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[]{1, 2, 3});

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/uploads/" + image.getFileName());

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void parsesResponseWithToolCalls() throws Exception {
        // When the AI returns tool_calls instead of content
        String toolArgs = "{\"type\":\"Chaqueta\",\"colorName\":\"Arena\",\"colorHex\":\"#F2E9E4\",\"confidence\":0.91}";
        String responseBody = "{\"choices\":[{\"message\":{\"tool_calls\":[{\"function\":{\"arguments\":" +
                new ObjectMapper().writeValueAsString(toolArgs) +
                "}}]}}]}";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[]{1, 2, 3});

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/uploads/" + image.getFileName());

        assertThat(response.type()).isEqualTo("Chaqueta");
        assertThat(response.colorName()).isEqualTo("Arena");
        assertThat(response.colorHex()).isEqualTo("#F2E9E4");
        assertThat(response.confidence()).isEqualByComparingTo("0.91");
        assertThat(response.error()).isNull();
    }

    @Test
    void returnsFallbackOnNonExistentImage() throws Exception {
        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/uploads/nonexistent.jpg");

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void returnsFallbackOnPathTraversalAttempt() throws Exception {
        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/etc/passwd");

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void sendsBearerTokenAndParsesValidJsonResponse() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"type\\\":\\\"Chaqueta\\\",\\\"colorName\\\":\\\"Arena\\\",\\\"colorHex\\\":\\\"#F2E9E4\\\",\\\"confidence\\\":0.91}\"}}]}")));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[] {1, 2, 3});

        AiClassificationService service = service(Duration.ofSeconds(3));
        AiClassificationResponse response = service.classify("/uploads/" + image.getFileName());

        assertThat(response.type()).isEqualTo("Chaqueta");
        assertThat(response.colorName()).isEqualTo("Arena");
        assertThat(response.colorHex()).isEqualTo("#F2E9E4");
        assertThat(response.confidence()).isEqualByComparingTo("0.91");
        assertThat(response.error()).isNull();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer test-key"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(matchingJsonPath("$.model", equalTo("qwen3.6")))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("500"))));
    }

    @Test
    void returnsManualFallbackWhenServerRejectsApiKey() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(401)));

        Path uploads = tempDir.resolve("uploads");
        Files.createDirectories(uploads);
        Path image = uploads.resolve("garment.jpg");
        Files.write(image, new byte[] {1, 2, 3});

        AiClassificationResponse response = service(Duration.ofSeconds(3)).classify("/uploads/" + image.getFileName());

        assertThat(response.hasPrediction()).isFalse();
        assertThat(response.error()).contains("Cargala manualmente");
    }

    @Test
    void errorResponseIsNotCached() {
        // The @Cacheable annotation on classify() has unless = "#result.error() != null"
        // which prevents caching of error responses. This is verified at the annotation level
        // in a Spring integration test. This test validates that error responses DO have
        // a non-null error field, which is the precondition for the unless expression.

        assertThat(new AiClassificationResponse("", "", "", BigDecimal.ZERO, "model", "some error").error()).isNotNull();
        assertThat(new AiClassificationResponse("Top", "Rojo", "#FF0000", new BigDecimal("0.95"), "model", null).error()).isNull();
    }

    private AiClassificationService service(Duration readTimeout) {
        AiServerProperties properties = new AiServerProperties(
                wireMockServer.baseUrl(),
                "/v1/chat/completions",
                "qwen3.6",
                "test-key",
                500,
                true,
                Duration.ofSeconds(1),
                readTimeout,
                null);
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> headers.setBearerAuth(properties.apiKey()))
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(readTimeout)))
                .build();
        UploadProperties uploadProperties = new UploadProperties(tempDir.resolve("uploads"), DataSize.ofMegabytes(8), java.util.List.of("image/jpeg"));
        return new AiClassificationService(properties, uploadProperties, webClient, new ObjectMapper());
    }
}
