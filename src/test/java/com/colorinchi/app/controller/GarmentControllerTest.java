package com.colorinchi.app.controller;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.config.RateLimitExceededException;
import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.dto.AiClassificationResponse;
import com.colorinchi.app.dto.AiRecommendationResponse;
import com.colorinchi.app.dto.DashboardStats;
import com.colorinchi.app.dto.OutfitPiece;
import com.colorinchi.app.dto.OutfitSuggestion;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.service.AiClassificationService;
import com.colorinchi.app.service.AiRecommendationService;
import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.GarmentCompatibilityService;
import com.colorinchi.app.service.GarmentService;
import com.colorinchi.app.service.InspirationService;
import com.colorinchi.app.service.WeekPlanService;
import com.colorinchi.app.upload.ImageStorageService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GarmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GarmentControllerTest.TestPropertiesConfig.class)
class GarmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GarmentService garmentService;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private AiClassificationService aiClassificationService;

    @MockitoBean
    private AiRecommendationService aiRecommendationService;

    @MockitoBean
    private WeekPlanService weekPlanService;

    @MockitoBean
    private GarmentCompatibilityService garmentCompatibilityService;

    @MockitoBean
    private InspirationService inspirationService;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @MockitoBean
    private AnonymousOwnerService anonymousOwnerService;

    private Garment sampleGarment;
    private AiClassificationResponse sampleAiResponse;
    private DashboardStats sampleStats;

    @BeforeEach
    void setUp() {
        sampleGarment = new Garment();
        ReflectionTestUtils.setField(sampleGarment, "id", 1L);
        sampleGarment.setName("Top Rojo");
        sampleGarment.setCategory("Top");
        sampleGarment.setColorName("Rojo");
        sampleGarment.setColorHex("#FF0000");
        sampleGarment.setImageUrl("/uploads/test.jpg");
        sampleGarment.setFavorite(false);

        sampleAiResponse = new AiClassificationResponse(
                "Top", "Rojo", "#FF0000", new BigDecimal("0.95"), "qwen3.6", null);

        sampleStats = new DashboardStats(
                10L, 3L, 42, 3L, 6L,
                List.of(new DashboardStats.CategoryCount("Top", 5L)));

        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // --- 1. GET / ---

    @Test
    void rootRedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // --- 2. GET /dashboard ---

    @Test
    void dashboardShowsStats() throws Exception {
        when(weekPlanService.countDistinctDaysPlanned()).thenReturn(3L);
        when(weekPlanService.countPlanned()).thenReturn(6L);
        when(garmentService.getDashboardStats(3L, 6L)).thenReturn(sampleStats);
        when(garmentService.latest()).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/dashboard").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("latestGarments"))
                .andExpect(model().attribute("usagePercent", 42));
    }

    // --- 3. GET /wardrobe ---

    @Test
    void wardrobeShowsAllGarments() throws Exception {
        when(garmentService.all()).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/wardrobe").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("wardrobe"))
                .andExpect(model().attributeExists("garments"))
                .andExpect(model().attributeExists("categories"));
    }

    // --- 4. GET /wardrobe/filter?category=Top ---

    @Test
    void wardrobeFilterByCategoryReturnsGridFragment() throws Exception {
        when(garmentService.filterByCategory("Top")).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/wardrobe/filter").param("category", "Top").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("wardrobe :: grid"));
    }

    // --- 5. GET /wardrobe/filter?category= (empty) ---

    @Test
    void wardrobeFilterWithEmptyCategoryReturnsAll() throws Exception {
        when(garmentService.filterByCategory("")).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/wardrobe/filter").param("category", "").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("wardrobe :: grid"));
    }

    // --- 6. GET /wardrobe/new ---

    @Test
    void newGarmentRendersUploadScreen() throws Exception {
        mockMvc.perform(get("/wardrobe/new").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-new"));
    }

    // --- 7. POST /wardrobe/analyze with valid file ---

    @Test
    void analyzeWithValidFileReturnsConfirmScreen() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        when(imageStorageService.store(any())).thenReturn("/uploads/test.jpg");
        when(aiClassificationService.classify("/uploads/test.jpg")).thenReturn(sampleAiResponse);

        mockMvc.perform(multipart("/wardrobe/analyze").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-confirm"))
                .andExpect(model().attributeExists("garmentReviewForm"));
    }

    // --- 8. POST /wardrobe/analyze with invalid file (store throws) ---

    @Test
    void analyzeWithInvalidFileRedirectsToNew() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        when(imageStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("Selecciona una imagen de la prenda."));

        mockMvc.perform(multipart("/wardrobe/analyze").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wardrobe/new"))
                .andExpect(flash().attribute("error", "Selecciona una imagen de la prenda."));
    }

    // --- 9. POST /wardrobe with valid form ---

    @Test
    void createWithValidFormRedirectsToDetail() throws Exception {
        when(garmentService.create(any())).thenReturn(sampleGarment);

        mockMvc.perform(post("/wardrobe").with(csrf())
                        .param("imageUrl", "/uploads/test.jpg")
                        .param("name", "Top Rojo")
                        .param("category", "Top")
                        .param("colorName", "Rojo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wardrobe/1"));
    }

    // --- 10. POST /wardrobe with invalid form ---

    @Test
    void createWithInvalidFormReturnsConfirm() throws Exception {
        mockMvc.perform(post("/wardrobe").with(csrf())
                        .param("name", "Incomplete"))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-confirm"));
    }

    // --- 11. GET /wardrobe/1 ---

    @Test
    void detailShowsGarment() throws Exception {
        when(garmentService.get(1L)).thenReturn(sampleGarment);
        when(garmentCompatibilityService.findCompatible(sampleGarment)).thenReturn(List.of());
        when(weekPlanService.findCompanionGarments(1L)).thenReturn(List.of());

        mockMvc.perform(get("/wardrobe/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-detail"))
                .andExpect(model().attributeExists("garment"))
                .andExpect(model().attributeExists("compatibleGarments"))
                .andExpect(model().attributeExists("companionGarments"));
    }

    // --- 12. GET /wardrobe/1/edit ---

    @Test
    void editFormShowsGarmentData() throws Exception {
        when(garmentService.get(1L)).thenReturn(sampleGarment);

        mockMvc.perform(get("/wardrobe/1/edit").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-edit"))
                .andExpect(model().attributeExists("garmentReviewForm"))
                .andExpect(model().attributeExists("garment"));
    }

    // --- 13. PUT /wardrobe/1 ---

    @Test
    void updateWithValidFormRedirects() throws Exception {
        when(garmentService.update(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(sampleGarment);

        mockMvc.perform(put("/wardrobe/1").with(csrf())
                        .param("imageUrl", "/uploads/test.jpg")
                        .param("name", "Updated Top")
                        .param("category", "Top")
                        .param("colorName", "Rojo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wardrobe/1"));
    }

    // --- 14. DELETE /wardrobe/1 with source=detail ---

    @Test
    void deleteGarmentWithDetailSourceSetsHxRedirect() throws Exception {
        mockMvc.perform(delete("/wardrobe/1").with(csrf())
                        .param("source", "detail"))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/wardrobe"))
                .andExpect(content().string(""));
    }

    @Test
    void deleteGarmentWithDefaultSourceReturnsEmpty() throws Exception {
        mockMvc.perform(delete("/wardrobe/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // --- 15. POST /wardrobe/1/favorite ---

    @Test
    void toggleFavoriteUpdatesAndReturnsGrid() throws Exception {
        when(garmentService.toggleFavorite(1L)).thenReturn(true);
        when(garmentService.get(1L)).thenReturn(sampleGarment);
        when(garmentService.all()).thenReturn(List.of(sampleGarment));

        mockMvc.perform(post("/wardrobe/1/favorite").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("wardrobe :: grid"));
    }

    @Test
    void toggleFavoriteWithDetailVariantReturnsDetailButton() throws Exception {
        when(garmentService.toggleFavorite(1L)).thenReturn(true);
        when(garmentService.get(1L)).thenReturn(sampleGarment);

        mockMvc.perform(post("/wardrobe/1/favorite").with(csrf())
                        .param("variant", "detail"))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-detail :: favDetailButton"));
    }

    // --- 16. GET /profile ---

    @Test
    void profileShowsStats() throws Exception {
        when(weekPlanService.countDistinctDaysPlanned()).thenReturn(3L);
        when(weekPlanService.countPlanned()).thenReturn(6L);
        when(garmentService.getDashboardStats(3L, 6L)).thenReturn(sampleStats);

        java.util.List<Object[]> mockTopColors = new java.util.ArrayList<>();
        mockTopColors.add(new Object[]{"Rojo", "#FF0000", 3L});
        when(garmentService.getTopColors()).thenReturn(mockTopColors);

        mockMvc.perform(get("/profile").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-stats"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("topColors"));
    }

    // --- 17. GET /weekly-plan ---

    @Test
    void weeklyPlanShowsPlan() throws Exception {
        Map<String, List<com.colorinchi.app.model.WeekPlan>> plansByDay = new java.util.LinkedHashMap<>();
        plansByDay.put("Lunes", java.util.Collections.emptyList());
        plansByDay.put("Martes", java.util.Collections.emptyList());

        when(weekPlanService.getPlansByDay()).thenReturn(plansByDay);
        when(garmentService.all()).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/weekly-plan").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("weekly-plan"))
                .andExpect(model().attributeExists("days"))
                .andExpect(model().attributeExists("plans"))
                .andExpect(model().attributeExists("allGarments"));
    }

    // --- 18. POST /weekly-plan/assign ---

    @Test
    void assignGarmentReturnsOk() throws Exception {
        mockMvc.perform(post("/weekly-plan/assign").with(csrf())
                        .param("garmentId", "1")
                        .param("dayOfWeek", "Lunes")
                        .param("position", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(weekPlanService).assignGarment(1L, "Lunes", 0);
    }

    // --- 19. PUT /weekly-plan/reorder ---

    @Test
    void reorderDayReturnsOk() throws Exception {
        mockMvc.perform(put("/weekly-plan/reorder").with(csrf())
                        .param("dayOfWeek", "Lunes")
                        .param("order", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(weekPlanService).reorderDay("Lunes", List.of(1L, 2L, 3L));
    }

    // --- 20. DELETE /weekly-plan/1 ---

    @Test
    void removeFromPlanReturnsOk() throws Exception {
        mockMvc.perform(delete("/weekly-plan/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(weekPlanService).remove(1L);
    }

    // --- Extra: GET /inspiration ---

    @Test
    void inspirationShowsLooks() throws Exception {
        when(inspirationService.getAll()).thenReturn(java.util.Collections.emptyList());
        when(inspirationService.getAllTags()).thenReturn(java.util.Collections.emptySet());

        mockMvc.perform(get("/inspiration").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("inspiration"))
                .andExpect(model().attributeExists("inspirations"))
                .andExpect(model().attributeExists("allTags"));
    }

    // --- Extra: GET /recommendation ---

    @Test
    void recommendationShowsOutfits() throws Exception {
        when(aiRecommendationService.generate()).thenReturn(
                new AiRecommendationResponse(List.of(
                        new OutfitSuggestion("Look 1", "Desc", 8,
                                List.of(new OutfitPiece("Top", "Rojo", "#FF0000",
                                        OutfitPiece.zoneFor("Top"), OutfitPiece.isLightText("#FF0000")))))));

        mockMvc.perform(get("/recommendation").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation"))
                .andExpect(model().attributeExists("recommendations"));
    }

    // --- Extra: filter by favoritos ---

    @Test
    void wardrobeFilterByFavoritos() throws Exception {
        when(garmentService.favorites()).thenReturn(List.of(sampleGarment));

        mockMvc.perform(get("/wardrobe/filter").param("category", "favoritos").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("wardrobe :: grid"));
    }

    // --- Extra: POST /wardrobe/{id}/update with invalid BindingResult ---

    @Test
    void updateWithInvalidFormReturnsEdit() throws Exception {
        when(garmentService.get(1L)).thenReturn(sampleGarment);

        mockMvc.perform(put("/wardrobe/1").with(csrf())
                        .param("name", "Only name"))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-edit"));
    }

    // --- Rate limiting: POST /wardrobe/analyze ---

    @Test
    void analyzeReturns429WhenRateLimited() throws Exception {
        when(rateLimitingInterceptor.preHandle(any(), any(), any()))
                .thenThrow(new RateLimitExceededException(
                        "Demasiadas solicitudes. Esper\u00E1 1 minutos antes de intentar de nuevo."));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        mockMvc.perform(multipart("/wardrobe/analyze").file(file).with(csrf()))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()))
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorTitle"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // --- Rate limiting: GET /recommendation ---

    @Test
    void recommendationReturns429WhenRateLimited() throws Exception {
        when(rateLimitingInterceptor.preHandle(any(), any(), any()))
                .thenThrow(new RateLimitExceededException(
                        "Demasiadas solicitudes. Esper\u00E1 1 minutos antes de intentar de nuevo."));

        mockMvc.perform(get("/recommendation").with(csrf()))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()))
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorTitle"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // --- Rate limiting: /dashboard is NOT rate-limited ---

    @Test
    void dashboardWorksWithoutRateLimitCheck() throws Exception {
        when(weekPlanService.countDistinctDaysPlanned()).thenReturn(0L);
        when(weekPlanService.countPlanned()).thenReturn(0L);
        when(garmentService.getDashboardStats(anyLong(), anyLong())).thenReturn(
                new DashboardStats(0L, 0L, 0, 0L, 0L, List.of()));
        when(garmentService.latest()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @TestConfiguration
    static class TestPropertiesConfig {

        @Bean
        UploadProperties uploadProperties() {
            return new UploadProperties(Path.of("uploads"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        }

        @Bean
        WardrobeProperties wardrobeProperties() {
            return new WardrobeProperties(
                    List.of("Top", "Pantalón", "Vestido", "Falda", "Chaqueta", "Abrigo", "Camisa", "Sudadera", "Zapatos", "Accesorio", "Otro"),
                    List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"),
                    5, 30, 3);
        }
    }
}
