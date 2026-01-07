package com.mercadolibre.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "imagen_producto", indexes = {
    @Index(name = "idx_producto_id", columnList = "producto_id")
})
public class ImagenProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Lob
    @Column(name = "url_imagen", nullable = false)
    private String urlImagen;

    @Column(name = "orden")
    private Integer orden;
}