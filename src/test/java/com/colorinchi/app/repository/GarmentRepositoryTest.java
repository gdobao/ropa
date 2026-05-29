package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.colorinchi.app.model.Garment;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GarmentRepositoryTest {

    @Autowired
    private GarmentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveAndFindById() {
        Garment g = createGarment("Top Rojo", "Top", "Rojo", "#FF0000", null, false);
        Garment saved = repository.save(g);

        assertThat(saved.getId()).isNotNull();

        Optional<Garment> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Top Rojo");
    }

    @Test
    void findByCategoryOrderByCreatedAtDescReturnsOnlyMatchingCategory() {
        repository.save(createGarment("Top Rojo", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("Pantalón Azul", "Pantalón", "Azul", "#0000FF", null, false));
        repository.save(createGarment("Top Verde", "Top", "Verde", "#00FF00", null, false));

        List<Garment> tops = repository.findByCategoryOrderByCreatedAtDesc("Top");

        assertThat(tops).hasSize(2);
        assertThat(tops).allMatch(g -> "Top".equals(g.getCategory()));
    }

    @Test
    void findByFavoriteTrueOrderByCreatedAtDescReturnsOnlyFavorites() {
        repository.save(createGarment("Fav 1", "Top", "Rojo", "#FF0000", null, true));
        repository.save(createGarment("Not Fav", "Pantalón", "Azul", "#0000FF", null, false));
        repository.save(createGarment("Fav 2", "Chaqueta", "Negro", "#000000", null, true));

        List<Garment> favorites = repository.findByFavoriteTrueOrderByCreatedAtDesc();

        assertThat(favorites).hasSize(2);
        assertThat(favorites).allMatch(Garment::isFavorite);
    }

    @Test
    void findAllByOrderByCreatedAtDescReturnsAll() {
        repository.save(createGarment("A", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("B", "Pantalón", "Azul", "#0000FF", null, false));

        List<Garment> all = repository.findAllByOrderByCreatedAtDesc();

        assertThat(all).hasSize(2);
    }

    @Test
    void countByCategoryGroupedReturnsCorrectCounts() {
        repository.save(createGarment("T1", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("T2", "Top", "Verde", "#00FF00", null, false));
        repository.save(createGarment("P1", "Pantalón", "Azul", "#0000FF", null, false));

        List<Object[]> counts = repository.countByCategoryGrouped();

        assertThat(counts).hasSize(2);
        for (Object[] row : counts) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            if ("Top".equals(category)) {
                assertThat(count).isEqualTo(2);
            } else if ("Pantalón".equals(category)) {
                assertThat(count).isEqualTo(1);
            }
        }
    }

    @Test
    void countByColorGroupedReturnsCorrectCounts() {
        repository.save(createGarment("T1", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("T2", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("P1", "Pantalón", "Azul", "#0000FF", null, false));

        List<Object[]> counts = repository.countByColorGrouped();

        assertThat(counts).hasSize(2);
        assertThat((String) counts.get(0)[0]).isEqualTo("Rojo");
        assertThat((String) counts.get(0)[1]).isEqualTo("#FF0000");
        assertThat((Long) counts.get(0)[2]).isEqualTo(2);
    }

    @Test
    void countReturnsTotalNumberOfGarments() {
        repository.save(createGarment("A", "Top", "Rojo", "#FF0000", null, false));
        repository.save(createGarment("B", "Pantalón", "Azul", "#0000FF", null, false));

        long count = repository.count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByFavoriteTrueReturnsOnlyFavoriteCount() {
        repository.save(createGarment("A", "Top", "Rojo", "#FF0000", null, true));
        repository.save(createGarment("B", "Pantalón", "Azul", "#0000FF", null, false));

        long favCount = repository.countByFavoriteTrue();

        assertThat(favCount).isEqualTo(1);
    }

    @Test
    void findTop12ByOrderByCreatedAtDescReturnsLimitedResults() {
        for (int i = 1; i <= 15; i++) {
            repository.save(createGarment("G" + i, "Top", "Color" + i, "#000000", null, false));
        }

        List<Garment> top = repository.findTop12ByOrderByCreatedAtDesc();

        assertThat(top).hasSize(12);
    }

    @Test
    void deleteByIdRemovesGarment() {
        Garment saved = repository.save(createGarment("To Delete", "Top", "Rojo", "#FF0000", null, false));
        assertThat(repository.findById(saved.getId())).isPresent();

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isNotPresent();
    }

    // --- Helper ---

    private Garment createGarment(String name, String category, String colorName, String colorHex, String material, boolean favorite) {
        Garment g = new Garment();
        g.setName(name);
        g.setCategory(category);
        g.setColorName(colorName);
        g.setColorHex(colorHex);
        if (material != null) g.setMaterial(material);
        g.setImageUrl("/uploads/" + name + ".jpg");
        g.setFavorite(favorite);
        g.setUserConfirmed(true);
        return g;
    }
}
