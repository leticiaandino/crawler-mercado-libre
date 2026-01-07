package com.mercadolibre.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ruta", nullable = false, length = 500, unique = true)
    private String ruta;

    @Lob
    @Column(name = "url_categoria", nullable = false)
    private String urlCategoria;

    @Column(name = "cantidad_paginas", nullable = false)
    private Integer cantidadPaginas;

    @Column(name = "productos_por_pagina", nullable = false)
    private Integer productosPorPagina;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.fechaActualizacion = Instant.now();
    }


}