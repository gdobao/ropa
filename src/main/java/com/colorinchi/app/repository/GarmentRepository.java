package com.colorinchi.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.Garment;

public interface GarmentRepository extends JpaRepository<Garment, Long> {

    List<Garment> findTop12ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Garment> findByOwnerIdAndCategoryOrderByCreatedAtDesc(UUID ownerId, String category);

    List<Garment> findByOwnerIdAndFavoriteTrueOrderByCreatedAtDesc(UUID ownerId);

    List<Garment> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Page<Garment> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    Optional<Garment> findByIdAndOwnerId(Long id, UUID ownerId);

    long deleteByIdAndOwnerId(Long id, UUID ownerId);

    @Query("SELECT g FROM Garment g WHERE g.ownerId = :ownerId AND g.category IN :categories ORDER BY g.createdAt DESC")
    List<Garment> findByOwnerIdAndCategoryIn(@Param("ownerId") UUID ownerId, @Param("categories") Collection<String> categories);

    long countByOwnerId(UUID ownerId);

    long countByOwnerIdAndFavoriteTrue(UUID ownerId);

    @Query("SELECT g.category, COUNT(g) FROM Garment g WHERE g.ownerId = :ownerId GROUP BY g.category")
    List<Object[]> countByCategoryGrouped(@Param("ownerId") UUID ownerId);

    @Query("SELECT g.colorName, g.colorHex, COUNT(g) FROM Garment g WHERE g.ownerId = :ownerId AND g.colorHex IS NOT NULL GROUP BY g.colorName, g.colorHex ORDER BY COUNT(g) DESC")
    List<Object[]> countByColorGrouped(@Param("ownerId") UUID ownerId);
}
