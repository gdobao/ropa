package com.colorinchi.app.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.net.URI;

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

    private AiClassificationService service(Duration readTimeout) {
        AiServerProperties properties = new AiServerProperties(
                wireMockServer.baseUrl(),
                "/v1/chat/completions",
                "qwen3.6",
                "test-key",
                500,
                true,
                Duration.ofSeconds(1),
                readTimeout);
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
