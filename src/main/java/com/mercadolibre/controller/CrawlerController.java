package com.mercadolibre.controller;

import com.mercadolibre.model.Producto;
import com.mercadolibre.service.CrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerController.class);

    @Autowired
    private CrawlerService crawlerService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Crawler service is running");
    }

    @PostMapping("/producto")
    public ResponseEntity<?> crawlProducto(@RequestBody String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: URL requerida en el body");
            }
            
            String cleanUrl = url.trim().replace("\"", "");
            logger.info("Iniciando extracción de producto: {}", cleanUrl);
            Producto producto = crawlerService.extraerFichaProducto(cleanUrl);
            logger.info("Producto extraído exitosamente: SKU={}, Nombre={}", 
                       producto.getSku(), producto.getNombre());
            
            // Crear respuesta JSON con los datos extraídos
            var response = new java.util.HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Producto extraído exitosamente");
            response.put("data", new java.util.HashMap<String, Object>() {{
                put("sku", producto.getSku());
                put("nombre", producto.getNombre());
                put("precioActual", producto.getPrecioActual());
                put("precioAnterior", producto.getPrecioAnterior());
                put("disponibilidad", producto.getDisponibilidad());
                put("urlFicha", producto.getUrlFicha());
                put("imagenes", producto.getImagenes().stream()
                    .map(img -> new java.util.HashMap<String, Object>() {{
                        put("url", img.getUrlImagen());
                        put("orden", img.getOrden());
                    }}).toList());
                put("fechaCreacion", producto.getFechaCreacion());
                put("fechaActualizacion", producto.getFechaActualizacion());
            }});
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al extraer producto", e);
            var errorResponse = new java.util.HashMap<String, Object>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/categoria")
    public ResponseEntity<String> crawlCategoria(@RequestParam String url) {
        try {
            logger.info("Iniciando extracción de categoría: {}", url);
            crawlerService.extraerListadoProductos(url);
            logger.info("Categoría procesada exitosamente: {}", url);
            return ResponseEntity.ok("Categoría procesada exitosamente");
        } catch (Exception e) {
            logger.error("Error al procesar categoría: {}", url, e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}