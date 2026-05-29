package com.colorinchi.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.colorinchi.app.dto.DashboardStats;
import com.colorinchi.app.dto.GarmentReviewForm;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.GarmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarmentServiceTest {

    @Mock
    private GarmentRepository repository;

    @InjectMocks
    private GarmentService service;

    private Garment sampleGarment;

    @BeforeEach
    void setUp() {
        sampleGarment = new Garment();
        ReflectionTestUtils.setField(sampleGarment, "id", 1L);
        sampleGarment.setName("Top Rojo");
        sampleGarment.setCategory("Top");
        sampleGarment.setColorName("Rojo");
        sampleGarment.setColorHex("#FF0000");
        sampleGarment.setMaterial("Algodon");
        sampleGarment.setSeason("Verano");
        sampleGarment.setImageUrl("/uploads/test.jpg");
        sampleGarment.setAiType("Top");
        sampleGarment.setAiColorName("Rojo");
        sampleGarment.setAiColorHex("#FF0000");
        sampleGarment.setAiConfidence(new BigDecimal("0.95"));
        sampleGarment.setAiModel("qwen3.6");
        sampleGarment.setFavorite(false);
        sampleGarment.setUserConfirmed(true);
    }

    @Test
    void allReturnsAllGarmentsOrderedByCreatedAtDesc() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleGarment));

        var result = service.all();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Top Rojo");
    }

    @Test
    void latestReturnsTop12() {
        when(repository.findTop12ByOrderByCreatedAtDesc()).thenReturn(List.of(sampleGarment));

        var result = service.latest();

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByCategoryReturnsFilteredResults() {
        when(repository.findByCategoryOrderByCreatedAtDesc("Top")).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory("Top");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Top");
    }

    @Test
    void filterByEmptyCategoryReturnsAll() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory("");

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByNullCategoryReturnsAll() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void favoritesReturnsFavoritesOnly() {
        when(repository.findByFavoriteTrueOrderByCreatedAtDesc()).thenReturn(List.of(sampleGarment));

        var result = service.favorites();

        assertThat(result).hasSize(1);
    }

    @Test
    void getDashboardStatsWithGarmentsReturnsCorrectStats() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByFavoriteTrue()).thenReturn(3L);
        when(repository.countByCategoryGrouped()).thenReturn(List.of(
                new Object[]{"Top", 5L},
                new Object[]{"Pantalón", 3L}));

        DashboardStats stats = service.getDashboardStats(3L, 6L);

        assertThat(stats.totalGarments()).isEqualTo(10L);
        assertThat(stats.favoriteCount()).isEqualTo(3L);
        assertThat(stats.usagePercent()).isEqualTo(42); // (3 * 100) / 7
        assertThat(stats.plannedDays()).isEqualTo(3L);
        assertThat(stats.plannedItems()).isEqualTo(6L);
        assertThat(stats.categoryBreakdown()).hasSize(2);
        assertThat(stats.categoryBreakdown().get(0).category()).isEqualTo("Top");
        assertThat(stats.categoryBreakdown().get(0).count()).isEqualTo(5L);
    }

    @Test
    void getDashboardStatsWithZeroGarmentsReturnsZeroStats() {
        when(repository.count()).thenReturn(0L);
        when(repository.countByFavoriteTrue()).thenReturn(0L);
        when(repository.countByCategoryGrouped()).thenReturn(List.of());

        DashboardStats stats = service.getDashboardStats(0L, 0L);

        assertThat(stats.totalGarments()).isEqualTo(0L);
        assertThat(stats.favoriteCount()).isEqualTo(0L);
        assertThat(stats.usagePercent()).isEqualTo(0);
        assertThat(stats.plannedDays()).isEqualTo(0L);
        assertThat(stats.plannedItems()).isEqualTo(0L);
        assertThat(stats.categoryBreakdown()).isEmpty();
    }

    @Test
    void getTopColorsReturnsParsedResults() {
        List<Object[]> rawColors = List.of(
                new Object[]{"Rojo", "#FF0000", 3L},
                new Object[]{"Azul", "#0000FF", 2L});
        when(repository.countByColorGrouped()).thenReturn(rawColors);

        List<Object[]> colors = service.getTopColors();

        assertThat(colors).hasSize(2);
        assertThat((String) colors.get(0)[0]).isEqualTo("Rojo");
        assertThat((String) colors.get(0)[1]).isEqualTo("#FF0000");
        assertThat((Long) colors.get(0)[2]).isEqualTo(3L);
    }

    @Test
    void getWhenExistsReturnsGarment() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleGarment));

        Garment result = service.get(1L);

        assertThat(result).isEqualTo(sampleGarment);
    }

    @Test
    void getWhenNotFoundThrowsIllegalArgument() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prenda no encontrada");
    }

    @Test
    void toggleFavoriteFlipsAndSaves() {
        sampleGarment.setFavorite(false);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleGarment));
        when(repository.save(any(Garment.class))).thenReturn(sampleGarment);

        boolean result = service.toggleFavorite(1L);

        assertThat(result).isTrue();
        assertThat(sampleGarment.isFavorite()).isTrue();
        verify(repository).save(sampleGarment);
    }

    @Test
    void toggleFavoriteWhenAlreadyFavoriteFlipsToFalse() {
        sampleGarment.setFavorite(true);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleGarment));
        when(repository.save(any(Garment.class))).thenReturn(sampleGarment);

        boolean result = service.toggleFavorite(1L);

        assertThat(result).isFalse();
        assertThat(sampleGarment.isFavorite()).isFalse();
        verify(repository).save(sampleGarment);
    }

    @Test
    void createMapsFormAndSaves() {
        GarmentReviewForm form = new GarmentReviewForm();
        form.setName("Pantalón Azul");
        form.setCategory("Pantalón");
        form.setColorName("Azul");
        form.setColorHex("#0000FF");
        form.setMaterial("Jean");
        form.setSeason("Todas");
        form.setImageUrl("/uploads/pantalon.jpg");
        form.setAiType("Pantalón");
        form.setAiColorName("Azul");
        form.setAiColorHex("#0000FF");
        form.setAiConfidence(new BigDecimal("0.88"));
        form.setAiModel("qwen3.6");

        when(repository.save(any(Garment.class))).thenAnswer(invocation -> {
            Garment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        Garment result = service.create(form);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Pantalón Azul");
        assertThat(result.getCategory()).isEqualTo("Pantalón");
        assertThat(result.getColorName()).isEqualTo("Azul");
        assertThat(result.getColorHex()).isEqualTo("#0000FF");
        assertThat(result.getMaterial()).isEqualTo("Jean");
        assertThat(result.getSeason()).isEqualTo("Todas");
        assertThat(result.getImageUrl()).isEqualTo("/uploads/pantalon.jpg");
        assertThat(result.getAiType()).isEqualTo("Pantalón");
        assertThat(result.isUserConfirmed()).isTrue();
    }

    @Test
    void updateMapsFormFieldsAndSaves() {
        GarmentReviewForm form = new GarmentReviewForm();
        form.setName("Updated Name");
        form.setCategory("Chaqueta");
        form.setColorName("Negro");
        form.setColorHex("#000000");
        form.setMaterial("Cuero");
        form.setSeason("Invierno");

        Garment existing = new Garment();
        ReflectionTestUtils.setField(existing, "id", 1L);
        existing.setImageUrl("/uploads/original.jpg");
        existing.setAiType("Original");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Garment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Garment result = service.update(1L, form);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getCategory()).isEqualTo("Chaqueta");
        assertThat(result.getColorName()).isEqualTo("Negro");
        assertThat(result.getColorHex()).isEqualTo("#000000");
        assertThat(result.getMaterial()).isEqualTo("Cuero");
        assertThat(result.getSeason()).isEqualTo("Invierno");

        // AI fields and imageUrl are preserved
        assertThat(result.getImageUrl()).isEqualTo("/uploads/original.jpg");
        assertThat(result.getAiType()).isEqualTo("Original");
    }

    @Test
    void deleteCallsRepositoryDeleteById() {
        service.delete(1L);
        verify(repository).deleteById(1L);
    }
}
