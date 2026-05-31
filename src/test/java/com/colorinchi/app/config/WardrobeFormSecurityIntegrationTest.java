package com.colorinchi.app.config;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.model.Garment;
import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.colorinchi.app.service.GarmentService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WardrobeFormSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    @MockitoBean
    private GarmentService garmentService;

    @Test
    void analyzeFormPostWithoutCsrfIsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        mockMvc.perform(multipart("/wardrobe/analyze").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFormPostWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/wardrobe")
                        .param("name", "Top Rojo")
                        .param("category", "Top")
                        .param("colorName", "Rojo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void seedFormPostWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/wardrobe/seed"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wardrobeNewContainsCsrfHiddenInput() throws Exception {
        mockMvc.perform(get("/wardrobe/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void wardrobeEmptyStateSeedFormContainsCsrfHiddenInput() throws Exception {
        when(currentOwnerAccessor.getCurrentOwnerId())
                .thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(garmentService.all()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/wardrobe"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/wardrobe/seed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void garmentEditContainsCsrfHiddenInput() throws Exception {
        Garment garment = new Garment();
        garment.setName("Top Rojo");
        garment.setCategory("Top");
        garment.setColorName("Rojo");
        garment.setImageUrl("/uploads/test.jpg");
        org.springframework.test.util.ReflectionTestUtils.setField(garment, "id", 1L);

        when(currentOwnerAccessor.getCurrentOwnerId())
                .thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(garmentService.get(1L)).thenReturn(garment);

        mockMvc.perform(get("/wardrobe/1/edit").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }
}
