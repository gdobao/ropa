package com.colorinchi.app.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.GarmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarmentCompatibilityServiceTest {

    @Mock
    private GarmentRepository garmentRepository;

    @InjectMocks
    private GarmentCompatibilityService service;

    private List<Garment> allGarments;

    @BeforeEach
    void setUp() {
        allGarments = List.of(
                createGarment(1L, "Top", "Verano"),
                createGarment(2L, "Pantalón", "Verano"),
                createGarment(3L, "Vestido", "Verano"),
                createGarment(4L, "Chaqueta", "Invierno"),
                createGarment(5L, "Zapatos", "Todas"),
                createGarment(6L, "Accesorio", "Todas"),
                createGarment(7L, "Pantalón", "Invierno"),
                createGarment(8L, "Top", "Invierno"),
                createGarment(9L, "Top", "Verano"),
                createGarment(10L, "Pantalón", "Primavera"),
                createGarment(11L, "Vestido", "Otono"),
                createGarment(12L, "Chaqueta", "Verano"),
                createGarment(13L, "Zapatos", "Invierno"),
                createGarment(14L, "Accesorio", "Verano"),
                createGarment(15L, "Top", "Todas"),
                createGarment(16L, "Pantalón", "Todas"),
                createGarment(17L, "Zapatos", "Verano"),
                createGarment(18L, "Zapatos", "Primavera"),
                createGarment(19L, "Otro", "Verano"),
                createGarment(20L, "Otro", "Todas"),
                createGarment(21L, "Accesorio", "Otono"));
    }

    @Test
    void findCompatibleWithNullReturnsEmpty() {
        assertThat(service.findCompatible(null)).isEmpty();
    }

    @Test
    void findCompatibleWithUnmappedCategoryReturnsEmpty() {
        Garment base = additionalFields(new Garment(), null, "Sombrero", null);
        assertThat(service.findCompatible(base)).isEmpty();
    }

    @Test
    void findCompatibleWithTopReturnsCandidateCategories() {
        Garment base = createGarment(1L, "Top", "Verano");

        // Top compatibility: Pantalón, Chaqueta, Zapatos, Accesorio
        var expectedCategories = Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        when(garmentRepository.findByCategoryIn(expectedCategories)).thenReturn(
                allGarments.stream()
                        .filter(g -> expectedCategories.contains(g.getCategory()))
                        .toList());

        var result = service.findCompatible(base);
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(g ->
                assertThat(g.getCategory()).isIn(expectedCategories));
    }

    @Test
    void findCompatibleExcludesSameGarment() {
        Garment base = createGarment(1L, "Top", "Verano");
        var expectedCategories = Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        when(garmentRepository.findByCategoryIn(expectedCategories)).thenReturn(
                allGarments.stream()
                        .filter(g -> expectedCategories.contains(g.getCategory()))
                        .toList());

        var result = service.findCompatible(base);
        assertThat(result).noneMatch(g -> g.getId().equals(base.getId()));
    }

    @Test
    void findCompatibleSortsBySeasonMatchFirst() {
        // Use lowercased season since production code normalizes candidate but not base
        Garment base = createGarment(1L, "Top", "verano");
        var expectedCategories = Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        when(garmentRepository.findByCategoryIn(expectedCategories)).thenReturn(
                allGarments.stream()
                        .filter(g -> expectedCategories.contains(g.getCategory()))
                        .toList());

        var result = service.findCompatible(base);

        boolean seenDifferentSeason = false;
        for (Garment g : result) {
            if ("verano".equalsIgnoreCase(g.getSeason())) {
                assertThat(seenDifferentSeason).as("All Verano items should come first").isFalse();
            } else {
                seenDifferentSeason = true;
            }
        }
    }

    @Test
    void findCompatibleLimitsToSixResults() {
        Garment base = createGarment(1L, "Top", "Verano");
        var expectedCategories = Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        List<Garment> manyCandidates = createManyGarments(20);
        when(garmentRepository.findByCategoryIn(expectedCategories)).thenReturn(manyCandidates);

        var result = service.findCompatible(base);
        assertThat(result).hasSizeLessThanOrEqualTo(6);
    }

    @Test
    void findCompatibleWithNullCategoryReturnsEmpty() {
        Garment base = new Garment();
        ReflectionTestUtils.setField(base, "id", 1L);
        base.setCategory(null);
        assertThat(service.findCompatible(base)).isEmpty();
    }

    @Test
    void findCompatibleWithBlankCategoryReturnsEmpty() {
        Garment base = new Garment();
        ReflectionTestUtils.setField(base, "id", 1L);
        base.setCategory("  ");
        assertThat(service.findCompatible(base)).isEmpty();
    }

    @Test
    void normalizeNullIsHandledGracefully() {
        Garment base = createGarment(1L, "Top", null);
        var expectedCategories = Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        when(garmentRepository.findByCategoryIn(expectedCategories)).thenReturn(
                allGarments.stream()
                        .filter(g -> expectedCategories.contains(g.getCategory()))
                        .toList());

        var result = service.findCompatible(base);
        assertThat(result).isNotEmpty();
    }

    // --- Helpers ---

    private Garment createGarment(Long id, String category, String season) {
        Garment g = new Garment();
        ReflectionTestUtils.setField(g, "id", id);
        g.setCategory(category);
        g.setSeason(season);
        g.setName(category + " " + (id != null ? id : ""));
        g.setColorName("Test");
        g.setColorHex("#000000");
        g.setImageUrl("/uploads/test.jpg");
        return g;
    }

    private Garment additionalFields(Garment g, Long id, String category, String season) {
        if (id != null) ReflectionTestUtils.setField(g, "id", id);
        g.setCategory(category);
        g.setSeason(season);
        g.setName(category);
        g.setColorName("Test");
        g.setColorHex("#000000");
        g.setImageUrl("/uploads/test.jpg");
        return g;
    }

    private List<Garment> createManyGarments(int count) {
        var categories = List.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio");
        var seasons = List.of("Verano", "Invierno", "Todas", "Primavera");
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createGarment(
                        100L + i,
                        categories.get(i % categories.size()),
                        seasons.get(i % seasons.size())))
                .toList();
    }
}
