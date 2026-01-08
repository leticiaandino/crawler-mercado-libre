package com.mercadolibre.controller;

import com.mercadolibre.model.Categoria;
import com.mercadolibre.service.ParisCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CategoryController {

    private final ParisCategoryService categoryService;

    @PostMapping("/categoria")
    public ResponseEntity<?> extractCategory(@RequestBody Map<String, String> request) {
        try {
            String categoryUrl = request.get("url");
            
            if (categoryUrl == null || !categoryUrl.contains("paris.cl")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "URL de categoría de Paris.cl requerida"
                ));
            }

            Categoria categoria = categoryService.importCategory(categoryUrl);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "categoria", categoria.getRuta(),
                    "totalPaginas", categoria.getCantidadPaginas(),
                    "productosPorPagina", categoria.getProductosPorPagina(),
                    "message", "Extracción completada exitosamente"
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error al extraer categoría: " + e.getMessage()
            ));
        }
    }
}
