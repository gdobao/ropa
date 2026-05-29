package com.colorinchi.app.controller;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.service.AiClassificationService;
import com.colorinchi.app.service.AiRecommendationService;
import com.colorinchi.app.service.GarmentCompatibilityService;
import com.colorinchi.app.service.GarmentService;
import com.colorinchi.app.service.InspirationService;
import com.colorinchi.app.service.WeekPlanService;
import com.colorinchi.app.upload.ImageStorageService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GarmentController.class)
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

    @Test
    void newGarmentRendersUploadScreen() throws Exception {
        mockMvc.perform(get("/wardrobe/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("garment-new"));
    }

    @TestConfiguration
    static class TestPropertiesConfig {

        @Bean
        UploadProperties uploadProperties() {
            return new UploadProperties(Path.of("uploads"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        }
    }
}
