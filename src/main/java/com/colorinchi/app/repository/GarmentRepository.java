package com.colorinchi.app.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.Garment;

public interface GarmentRepository extends JpaRepository<Garment, Long> {

    List<Garment> findTop12ByOrderByCreatedAtDesc();

    List<Garment> findByCategoryOrderByCreatedAtDesc(String category);

    List<Garment> findByFavoriteTrueOrderByCreatedAtDesc();

    List<Garment> findAllByOrderByCreatedAtDesc();

    Page<Garment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT g FROM Garment g WHERE g.category IN :categories ORDER BY g.createdAt DESC")
    List<Garment> findByCategoryIn(@Param("categories") Collection<String> categories);

    long count();

    long countByFavoriteTrue();

    @Query("SELECT g.category, COUNT(g) FROM Garment g GROUP BY g.category")
    List<Object[]> countByCategoryGrouped();

    @Query("SELECT g.colorName, g.colorHex, COUNT(g) FROM Garment g WHERE g.colorHex IS NOT NULL GROUP BY g.colorName, g.colorHex ORDER BY COUNT(g) DESC")
    List<Object[]> countByColorGrouped();
}
