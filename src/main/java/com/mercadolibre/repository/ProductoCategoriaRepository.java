package com.mercadolibre.repository;

import com.mercadolibre.model.ProductoCategoria;
import com.mercadolibre.model.ProductoCategoriaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCategoriaRepository extends JpaRepository<ProductoCategoria, ProductoCategoriaId> {
    List<ProductoCategoria> findByProductoId(Long productoId);
    List<ProductoCategoria> findByCategoriaId(Long categoriaId);
    void deleteByProductoId(Long productoId);
}