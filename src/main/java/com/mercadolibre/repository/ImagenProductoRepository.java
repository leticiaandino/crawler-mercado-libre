package com.mercadolibre.repository;

import com.mercadolibre.model.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, Long> {
    List<ImagenProducto> findByProductoIdOrderByOrden(Long productoId);
    void deleteByProductoId(Long productoId);
}