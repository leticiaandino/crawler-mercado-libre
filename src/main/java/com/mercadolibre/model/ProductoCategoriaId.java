package com.mercadolibre.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class ProductoCategoriaId implements Serializable {
    private static final long serialVersionUID = -5129179540254251008L;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;


}