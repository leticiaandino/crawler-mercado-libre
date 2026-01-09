package com.mercadolibre.service;

import com.mercadolibre.client.ParisApiClient;
import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.mapper.ProductMapper;
import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import com.mercadolibre.repository.CategoriaRepository;
import com.mercadolibre.repository.ImagenProductoRepository;
import com.mercadolibre.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio para importar categorías de Paris.cl
 * Maneja la lógica de paginación, deduplicación y persistencia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParisCategoryService {

    private final ParisApiClient apiClient;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final ProductMapper mapper;

    /**
     * Importa todos los productos de una categoría de Paris.cl
     * Realiza:
     * - Paginación automática
     * - Deduplicación por SKU
     * - Upsert inteligente (actualiza existentes, crea nuevos)
     */
    @Transactional
    public Categoria importCategory(String categoryUrl) {
        try {
            String categoryPath = extractCategoryPath(categoryUrl);
            log.info("Starting import for Paris category: {}", categoryPath);

            int page = 1;
            int totalProducts = 0;
            List<Producto> allProducts = new ArrayList<>();
            Set<String> processedSkus = new HashSet<>();

            // Iterar páginas hasta que no haya más productos
            while (true) {
                log.info("Fetching page {} from Paris API", page);
                ParisProductsResponse response = apiClient.fetchCategory(categoryPath, page);

                // Primera página: obtener total
                if (page == 1) {
                    totalProducts = response.getTotal();
                    log.info("Total products in category: {}", totalProducts);
                }

                // Si no hay resultados, salir
                if (response.getResults() == null || response.getResults().isEmpty()) {
                    log.info("No more products, stopping at page {}", page);
                    break;
                }

                int newProductsCount = 0;

                // Procesar cada producto
                for (var apiProduct : response.getResults()) {
                    String sku = apiProduct.getMasterVariant().getSku();

                    // Evitar duplicados
                    if (processedSkus.contains(sku)) {
                        log.debug("SKU {} ya fue procesado, saltando", sku);
                        continue;
                    }

                    // Mapear API response a entidad
                    Producto producto = mapper.fromApi(apiProduct, categoryUrl);
                    if (producto == null) {
                        log.warn("Producto mapeo resultó nulo para SKU {}", sku);
                        continue;
                    }

                    // Upsert: actualizar si existe, crear si no
                    Optional<Producto> existing = productoRepository.findBySku(sku);

                    if (existing.isPresent()) {
                        Producto existingProduct = existing.get();
                        log.debug("Updating existing product with SKU: {}", sku);

                        // Actualizar campos
                        existingProduct.setNombre(producto.getNombre());
                        existingProduct.setPrecioActual(producto.getPrecioActual());
                        existingProduct.setPrecioAnterior(producto.getPrecioAnterior());
                        existingProduct.setDisponibilidad(producto.getDisponibilidad());
                        existingProduct.setUrlFicha(producto.getUrlFicha());

                        // Limpiar imágenes antiguas
                        imagenProductoRepository.deleteByProductoId(existingProduct.getId());

                        // Agregar nuevas imágenes
                        for (ImagenProducto img : producto.getImagenes()) {
                            img.setProducto(existingProduct);
                            imagenProductoRepository.save(img);
                        }

                        productoRepository.save(existingProduct);
                        allProducts.add(existingProduct);
                    } else {
                        log.debug("Creating new product with SKU: {}", sku);

                        // Crear nuevo producto
                        Producto saved = productoRepository.save(producto);

                        // Guardar imágenes
                        for (ImagenProducto img : producto.getImagenes()) {
                            img.setProducto(saved);
                            imagenProductoRepository.save(img);
                        }

                        allProducts.add(saved);
                    }

                    processedSkus.add(sku);
                    newProductsCount++;
                }

                log.info("Processed page {} - {} products (total so far: {})",
                    page, newProductsCount, allProducts.size());

                // Si no hay productos nuevos o alcanzamos el total, parar
                if (newProductsCount == 0 || allProducts.size() >= totalProducts) {
                    log.info("Stopping pagination: newProductsCount={}, totalProcessed={}",
                        newProductsCount, allProducts.size());
                    break;
                }

                page++;
            }

            // Guardar metadata de categoría
            int totalPages = (int) Math.ceil((double) totalProducts / 30.0);
            Categoria categoria = saveCategoria(categoryUrl, categoryPath, totalPages, 30);

            log.info("Import completed. Total products: {}, Total pages: {}", allProducts.size(), totalPages);
            return categoria;

        } catch (Exception e) {
            log.error("Error importing Paris category: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import category: " + e.getMessage(), e);
        }
    }

    /**
     * Guarda o actualiza la metadata de la categoría
     */
    private Categoria saveCategoria(String url, String path, int totalPages, int pageSize) {
        Optional<Categoria> existing = categoriaRepository.findByRuta(path);

        Categoria categoria;
        if (existing.isPresent()) {
            categoria = existing.get();
            log.info("Updating existing category: {}", path);
        } else {
            categoria = new Categoria();
            log.info("Creating new category: {}", path);
        }

        categoria.setRuta(path);
        categoria.setUrlCategoria(url);
        categoria.setCantidadPaginas(totalPages);
        categoria.setProductosPorPagina(pageSize);

        return categoriaRepository.save(categoria);
    }

    /**
     * Extrae la ruta de categoría de la URL
     */
    private String extractCategoryPath(String url) {
        return url.replaceFirst("https://www\\.paris\\.cl/", "")
                  .replaceFirst("http://www\\.paris\\.cl/", "")
                  .replaceAll("/$", "");
    }
}

