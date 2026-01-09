package com.mercadolibre.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para acceder a los mapeos de categorías de Paris
 */
@Repository
public interface ParisCategoryMappingRepository extends JpaRepository<ParisCategoryMapping, Long> {

    /**
     * Busca un mapeo por categoryPath
     */
    Optional<ParisCategoryMapping> findByCategoryPath(String categoryPath);

    /**
     * Busca todos los mapeos activos
     */
    @Query("SELECT p FROM ParisCategoryMapping p WHERE p.active = true ORDER BY p.categoryPath")
    List<ParisCategoryMapping> findAllActive();

    /**
     * Busca mapeos que contengan un patrón de búsqueda
     */
    @Query("SELECT p FROM ParisCategoryMapping p WHERE p.categoryPath LIKE %:pattern% AND p.active = true")
    List<ParisCategoryMapping> findByPattern(@Param("pattern") String pattern);

    /**
     * Verifica si existe un mapeo para una categoryPath
     */
    boolean existsByCategoryPath(String categoryPath);
}

