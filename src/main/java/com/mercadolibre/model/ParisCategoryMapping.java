package com.mercadolibre.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mapeo de categorías de Paris
 * URL de categoría en Paris → groupId en API de Paris
 *
 * Ejemplo:
 * categoryPath: "mujer/ropa-interior/pantuflas"
 * groupId: "mujIntPantuflas"
 */
@Entity
@Table(name = "paris_category_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParisCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_path", nullable = false, unique = true, length = 255)
    private String categoryPath;

    @Column(name = "group_id", nullable = false, length = 100)
    private String groupId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

