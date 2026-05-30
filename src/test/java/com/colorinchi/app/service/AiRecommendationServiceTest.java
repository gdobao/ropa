package com.colorinchi.app.service;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.dto.AiRecommendationResponse;
import com.colorinchi.app.model.Garment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import reactor.netty.http.client.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    private WireMockServer wireMockServer;

    @Mock
    private GarmentService garmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void generateWithLessThanThreeGarmentsReturnsEmpty() {
        when(garmentService.all()).thenReturn(List.of(createGarment(1L)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).isEmpty();

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    @Test
    void generateWithDisabledReturnsEmpty() {
        AiRecommendationService service = createService(false, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).isEmpty();
    }

    @Test
    void generateWithValidGarmentsAndWireMock200ReturnsResponse() throws Exception {
        when(garmentService.all()).thenReturn(createThreeGarments());

        String outfitsJson = "{\"outfits\":[{\"name\":\"Look 1\",\"description\":\"Un look elegante\",\"pieces\":[{\"category\":\"Top\",\"colorName\":\"Rojo\",\"colorHex\":\"#FF0000\"}]}]}";
        String contentField = objectMapper.writeValueAsString(outfitsJson);
        String responseJson = "{\"choices\":[{\"message\":{\"content\":" + contentField + "}}]}";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).hasSize(1);
        assertThat(result.outfits().get(0).name()).isEqualTo("Look 1");
        assertThat(result.outfits().get(0).pieces()).hasSize(1);
    }

    @Test
    void generateWithWireMock500ReturnsEmpty() {
        when(garmentService.all()).thenReturn(createThreeGarments());

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(500)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).isEmpty();
    }

    @Test
    void buildPromptContainsGarmentInfo() throws Exception {
        when(garmentService.all()).thenReturn(createThreeGarments());

        String outfitsJson = "{\"outfits\":[]}";
        String contentField = objectMapper.writeValueAsString(outfitsJson);
        String responseJson = "{\"choices\":[{\"message\":{\"content\":" + contentField + "}}]}";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        service.generate();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("Guardarropa"))
                .withRequestBody(containing("Top"))
                .withRequestBody(containing("Pantalón"))
                .withRequestBody(containing("Zapatos")));
    }

    @Test
    void parseResponseWithValidJsonReturnsPopulatedResponse() throws Exception {
        when(garmentService.all()).thenReturn(createThreeGarments());

        String outfitsJson = "{\"outfits\":[{\"name\":\"Outfit\",\"description\":\"Desc\",\"pieces\":[{\"category\":\"Top\",\"colorName\":\"Azul\",\"colorHex\":\"#0000FF\"}]}]}";
        String contentField = objectMapper.writeValueAsString(outfitsJson);
        String responseJson = "{\"choices\":[{\"message\":{\"content\":" + contentField + "}}]}";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).isNotEmpty();
        assertThat(result.outfits().get(0).name()).isEqualTo("Outfit");
    }

    @Test
    void parseResponseWithInvalidJsonReturnsEmpty() throws Exception {
        when(garmentService.all()).thenReturn(createThreeGarments());

        String responseJson = "{\"choices\":[{\"message\":{\"content\":\"not valid json\"}}]}";

        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)));

        AiRecommendationService service = createService(true, Duration.ofSeconds(3));
        AiRecommendationResponse result = service.generate();

        assertThat(result.outfits()).isEmpty();
    }

    // --- Helpers ---

    private AiRecommendationService createService(boolean enabled, Duration readTimeout) {
        AiServerProperties properties = new AiServerProperties(
                wireMockServer.baseUrl(),
                "/v1/chat/completions",
                "qwen3.6",
                "test-key",
                500,
                enabled,
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
        return new AiRecommendationService(webClient, properties, objectMapper, garmentService);
    }

    private List<Garment> createThreeGarments() {
        return List.of(
                createGarmentWithCategory(1L, "Top", "Rojo", "#FF0000", "Algodon"),
                createGarmentWithCategory(2L, "Pantalón", "Azul", "#0000FF", "Jean"),
                createGarmentWithCategory(3L, "Zapatos", "Negro", "#000000", "Cuero"));
    }

    private Garment createGarment(Long id) {
        return createGarmentWithCategory(id, "Top", "Rojo", "#FF0000", null);
    }

    private Garment createGarmentWithCategory(Long id, String category, String colorName, String colorHex, String material) {
        Garment g = new Garment();
        ReflectionTestUtils.setField(g, "id", id);
        g.setName(category + " " + colorName);
        g.setCategory(category);
        g.setColorName(colorName);
        g.setColorHex(colorHex);
        g.setMaterial(material);
        g.setImageUrl("/uploads/" + id + ".jpg");
        return g;
    }
}
