package com.mercadolibre.controller;

import com.mercadolibre.dto.ApiResponse;
import com.mercadolibre.model.Producto;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.service.CrawlerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    /**
     * Endpoint estandarizado que detecta automáticamente:
     * - Tipo: producto (ficha individual) o categoría (listado)
     * - Sitio: mercadolibre, paris o abc
     *
     * Ejemplos:
     * POST /api/crawler/extract
     * Body: https://www.mercadolibre.com.ar/sierra/p/MLA123        (Ficha ML)
     * Body: https://www.paris.cl/tecnologia/celulares/smartphone/  (Listado Paris)
     * Body: https://www.abc.cl/hombre/accesorios/                  (Listado ABC)
     */
    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody String url) {
        try {
            String cleanUrl = url.trim().replace("\"", "");

            if (cleanUrl == null || cleanUrl.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, null, "URL requerida")
                );
            }

            logger.info("Iniciando extracción desde: {}", cleanUrl);

            // Detectar tipo (producto o categoría) y sitio
            ExtractType extractType = detectType(cleanUrl);
            String site = detectSite(cleanUrl);

            logger.info("Tipo detectado: {}, Sitio: {}", extractType, site);

            if (extractType == ExtractType.PRODUCTO) {
                return extractProducto(cleanUrl);
            } else {
                return extractListado(cleanUrl);
            }

        } catch (Exception e) {
            logger.error("Error en extracción", e);
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, null, e.getMessage())
            );
        }
    }

    /**
     * Endpoint para extraer ficha de producto
     * URL debe ser de producto individual
     *
     * DEPRECADO: Usar /api/crawler/extract en su lugar
     */
    @PostMapping("/producto")
    @Deprecated
    public ResponseEntity<?> crawlProducto(@RequestBody String url) {
        logger.warn("Endpoint /api/crawler/producto está DEPRECADO. Usar /api/crawler/extract en su lugar");
        return extract(url);
    }

    /**
     * Endpoint para extraer listado de productos
     * URL debe ser de categoría
     *
     * DEPRECADO: Usar /api/crawler/extract en su lugar
     */
    @PostMapping("/listado-productos")
    @Deprecated
    public ResponseEntity<?> crawlListadoProductos(@RequestBody String url) {
        logger.warn("Endpoint /api/crawler/listado-productos está DEPRECADO. Usar /api/crawler/extract en su lugar");
        return extract(url);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private ResponseEntity<?> extractProducto(String url) {
        try {
            String cleanUrl = url.trim().replace("\"", "");
            logger.info("Extrayendo ficha de producto: {}", cleanUrl);

            Producto producto = crawlerService.extraerFichaProducto(cleanUrl);

            logger.info("Ficha extraída: SKU={}, Nombre={}",
                       producto.getSku(), producto.getNombre());

            Map<String, Object> productData = new HashMap<>();
            productData.put("sku", producto.getSku());
            productData.put("nombre", producto.getNombre());
            productData.put("precioActual", producto.getPrecioActual());
            productData.put("precioAnterior", producto.getPrecioAnterior());
            productData.put("disponibilidad", producto.getDisponibilidad());
            productData.put("urlFicha", producto.getUrlFicha());
            productData.put("imagenes", producto.getImagenes().stream()
                .map(img -> {
                    Map<String, Object> imgData = new HashMap<>();
                    imgData.put("url", img.getUrlImagen());
                    imgData.put("orden", img.getOrden());
                    return imgData;
                }).toList());
            productData.put("fechaCreacion", producto.getFechaCreacion());
            productData.put("fechaActualizacion", producto.getFechaActualizacion());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Producto extraído exitosamente", productData, null)
            );

        } catch (Exception e) {
            logger.error("Error al extraer producto", e);
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, null, e.getMessage())
            );
        }
    }

    private ResponseEntity<?> extractListado(String url) {
        try {
            String cleanUrl = url.trim().replace("\"", "");
            logger.info("Extrayendo listado de productos: {}", cleanUrl);

            List<Producto> productos = crawlerService.extraerListadoProductos(cleanUrl);

            logger.info("Listado extraído: {} productos", productos.size());

            List<Map<String, Object>> productosData = productos.stream()
                .map(producto -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("sku", producto.getSku());
                    data.put("nombre", producto.getNombre());
                    data.put("precioActual", producto.getPrecioActual());
                    data.put("precioAnterior", producto.getPrecioAnterior());
                    data.put("disponibilidad", producto.getDisponibilidad());
                    data.put("imagenes", producto.getImagenes().stream()
                        .map(ImagenProducto::getUrlImagen)
                        .toList());
                    return data;
                }).toList();

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado extraído exitosamente", productosData, null)
            );

        } catch (Exception e) {
            logger.error("Error al extraer listado", e);
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, null, e.getMessage())
            );
        }
    }

    /**
     * Detecta automáticamente si la URL es de producto o categoría
     */
    private ExtractType detectType(String url) {
        // Productos: contienen /p/ o terminan en .html
        if (url.contains("/p/") || url.endsWith(".html")) {
            return ExtractType.PRODUCTO;
        }
        // Categorías: terminan en / o contienen nombres de categorías
        return ExtractType.CATEGORIA;
    }

    /**
     * Detecta automáticamente el sitio de la URL
     */
    private String detectSite(String url) {
        if (url.contains("mercadolibre")) {
            return "mercadolibre";
        } else if (url.contains("paris")) {
            return "paris";
        } else if (url.contains("abc")) {
            return "abc";
        }
        return "desconocido";
    }

    /**
     * Enum para tipos de extracción
     */
    private enum ExtractType {
        PRODUCTO,
        CATEGORIA
    }
}


