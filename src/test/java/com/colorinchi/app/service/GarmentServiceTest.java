package com.colorinchi.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.colorinchi.app.dto.DashboardStats;
import com.colorinchi.app.dto.GarmentReviewForm;
import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.GarmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GarmentServiceTest {

    @Mock
    private GarmentRepository repository;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    @Mock
    private WardrobeProperties wardrobeProperties;

    @InjectMocks
    private GarmentService service;

    private Garment sampleGarment;
    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
        sampleGarment.setOwnerId(ownerId);
        sampleGarment.setUserConfirmed(true);
        lenient().when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
        lenient().when(wardrobeProperties.categories()).thenReturn(List.of(
                "Top", "Pantalón", "Vestido", "Falda", "Chaqueta", "Abrigo",
                "Camisa", "Sudadera", "Zapatos", "Accesorio", "Otro"));
    }

    @Test
    void allReturnsAllGarmentsOrderedByCreatedAtDesc() {
        when(repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(sampleGarment));

        var result = service.all();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Top Rojo");
    }

    @Test
    void latestReturnsTop12() {
        when(repository.findTop12ByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(sampleGarment));

        var result = service.latest();

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByCategoryReturnsFilteredResults() {
        when(repository.findByOwnerIdAndCategoryOrderByCreatedAtDesc(ownerId, "Top")).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory("Top");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Top");
    }

    @Test
    void filterByEmptyCategoryReturnsAll() {
        when(repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory("");

        assertThat(result).hasSize(1);
    }

    @Test
    void filterByNullCategoryReturnsAll() {
        when(repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(sampleGarment));

        var result = service.filterByCategory(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void favoritesReturnsFavoritesOnly() {
        when(repository.findByOwnerIdAndFavoriteTrueOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(sampleGarment));

        var result = service.favorites();

        assertThat(result).hasSize(1);
    }

    @Test
    void getDashboardStatsWithGarmentsReturnsCorrectStats() {
        when(repository.countByOwnerId(ownerId)).thenReturn(10L);
        when(repository.countByOwnerIdAndFavoriteTrue(ownerId)).thenReturn(3L);
        when(repository.countByCategoryGrouped(ownerId)).thenReturn(List.of(
                new Object[]{"Top", 5L},
                new Object[]{"Pantalón", 3L}));

        DashboardStats stats = service.getDashboardStats(3L, 6L);

        assertThat(stats.totalGarments()).isEqualTo(10L);
        assertThat(stats.favoriteCount()).isEqualTo(3L);
        assertThat(stats.usagePercent()).isEqualTo(42); // (3 * 100) / 7
        assertThat(stats.plannedDays()).isEqualTo(3L);
        assertThat(stats.plannedItems()).isEqualTo(6L);
        assertThat(stats.plannedCoveragePercent()).isEqualTo(60L);
        assertThat(stats.nonFavoriteCount()).isEqualTo(7L);
        assertThat(stats.categoryBreakdown()).hasSize(2);
        assertThat(stats.categoryBreakdown().get(0).category()).isEqualTo("Top");
        assertThat(stats.categoryBreakdown().get(0).count()).isEqualTo(5L);
    }

    @Test
    void getDashboardStatsWithZeroGarmentsReturnsZeroStats() {
        when(repository.countByOwnerId(ownerId)).thenReturn(0L);
        when(repository.countByOwnerIdAndFavoriteTrue(ownerId)).thenReturn(0L);
        when(repository.countByCategoryGrouped(ownerId)).thenReturn(List.of());

        DashboardStats stats = service.getDashboardStats(0L, 0L);

        assertThat(stats.totalGarments()).isEqualTo(0L);
        assertThat(stats.favoriteCount()).isEqualTo(0L);
        assertThat(stats.usagePercent()).isEqualTo(0);
        assertThat(stats.plannedDays()).isEqualTo(0L);
        assertThat(stats.plannedItems()).isEqualTo(0L);
        assertThat(stats.plannedCoveragePercent()).isEqualTo(0L);
        assertThat(stats.nonFavoriteCount()).isEqualTo(0L);
        assertThat(stats.categoryBreakdown()).isEmpty();
    }

    @Test
    void getTopColorsReturnsParsedResults() {
        List<Object[]> rawColors = List.of(
                new Object[]{"Rojo", "#FF0000", 3L},
                new Object[]{"Azul", "#0000FF", 2L});
        when(repository.countByColorGrouped(ownerId)).thenReturn(rawColors);

        List<Object[]> colors = service.getTopColors();

        assertThat(colors).hasSize(2);
        assertThat((String) colors.get(0)[0]).isEqualTo("Rojo");
        assertThat((String) colors.get(0)[1]).isEqualTo("#FF0000");
        assertThat((Long) colors.get(0)[2]).isEqualTo(3L);
    }

    @Test
    void getWhenExistsReturnsGarment() {
        when(repository.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(sampleGarment));

        Garment result = service.get(1L);

        assertThat(result).isEqualTo(sampleGarment);
    }

    @Test
    void getWhenNotFoundThrowsIllegalArgument() {
        when(repository.findByIdAndOwnerId(999L, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prenda no encontrada");
    }

    @Test
    void toggleFavoriteFlipsAndSaves() {
        sampleGarment.setFavorite(false);
        when(repository.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(sampleGarment));
        when(repository.save(any(Garment.class))).thenReturn(sampleGarment);

        boolean result = service.toggleFavorite(1L);

        assertThat(result).isTrue();
        assertThat(sampleGarment.isFavorite()).isTrue();
        verify(repository).save(sampleGarment);
    }

    @Test
    void toggleFavoriteWhenAlreadyFavoriteFlipsToFalse() {
        sampleGarment.setFavorite(true);
        when(repository.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(sampleGarment));
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
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.isUserConfirmed()).isTrue();
    }

    @Test
    void createRejectsInvalidCategory() {
        GarmentReviewForm form = new GarmentReviewForm();
        form.setName("Bad");
        form.setCategory("Invalid");
        form.setColorName("Rojo");
        form.setImageUrl("/uploads/test.jpg");

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoría no válida");
    }

    @Test
    void createRejectsNonUploadImageUrl() {
        GarmentReviewForm form = new GarmentReviewForm();
        form.setName("Top");
        form.setCategory("Top");
        form.setColorName("Rojo");
        form.setImageUrl("https://example.com/test.jpg");

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Imagen no válida");
    }

    @Test
    void createNormalizesBlankOptionalFields() {
        GarmentReviewForm form = new GarmentReviewForm();
        form.setName("  Top Azul  ");
        form.setCategory("Top");
        form.setColorName("  Azul  ");
        form.setColorHex("  #0000ff  ");
        form.setMaterial("   ");
        form.setSeason("   ");
        form.setImageUrl("/uploads/top.jpg");

        when(repository.save(any(Garment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Garment result = service.create(form);

        assertThat(result.getName()).isEqualTo("Top Azul");
        assertThat(result.getColorName()).isEqualTo("Azul");
        assertThat(result.getColorHex()).isEqualTo("#0000FF");
        assertThat(result.getMaterial()).isNull();
        assertThat(result.getSeason()).isNull();
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

        existing.setOwnerId(ownerId);
        when(repository.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(existing));
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
    void deleteCallsRepositoryDeleteByIdAndOwnerId() {
        when(repository.deleteByIdAndOwnerId(1L, ownerId)).thenReturn(1L);

        service.delete(1L);

        verify(repository).deleteByIdAndOwnerId(1L, ownerId);
    }

    @Test
    void deleteThrowsWhenGarmentBelongsToAnotherOwner() {
        when(repository.deleteByIdAndOwnerId(1L, ownerId)).thenReturn(0L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prenda no encontrada");
    }
}
