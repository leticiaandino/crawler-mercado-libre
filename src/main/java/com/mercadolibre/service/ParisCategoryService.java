package com.mercadolibre.service;

import com.mercadolibre.client.ParisApiClient;
import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.mapper.ProductMapper;
import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import com.mercadolibre.repository.CategoriaRepository;
import com.mercadolibre.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParisCategoryService {

    private final ParisApiClient apiClient;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductMapper mapper;
    private final com.mercadolibre.repository.ImagenProductoRepository imagenProductoRepository;

    @Transactional
    public Categoria importCategory(String categoryUrl) {
        String categoryPath = extractCategoryPath(categoryUrl);
        log.info("Starting import for category: {}", categoryPath);

        int page = 1;
        int totalProducts = 0;
        List<Producto> allProducts = new ArrayList<>();
        java.util.Set<String> processedSkus = new java.util.HashSet<>();

        while (true) {
            ParisProductsResponse response = apiClient.fetchCategory(categoryPath, page);
            
            if (page == 1) {
                totalProducts = response.getTotal();
                log.info("Total products in category: {}", totalProducts);
            }

            if (response.getResults() == null || response.getResults().isEmpty()) {
                log.info("No more products, stopping at page {}", page);
                break;
            }

            int newProductsCount = 0;
            for (var apiProduct : response.getResults()) {
                String sku = apiProduct.getMasterVariant().getSku();
                
                if (processedSkus.contains(sku)) {
                    continue;
                }
                
                Producto producto = mapper.fromApi(apiProduct, categoryUrl);
                
                productoRepository.findBySku(sku).ifPresentOrElse(
                    existing -> {
                        existing.setNombre(producto.getNombre());
                        existing.setPrecioActual(producto.getPrecioActual());
                        existing.setPrecioAnterior(producto.getPrecioAnterior());
                        existing.setDisponibilidad(producto.getDisponibilidad());
                        
                        imagenProductoRepository.deleteByProductoId(existing.getId());
                        
                        for (ImagenProducto img : producto.getImagenes()) {
                            img.setProducto(existing);
                            imagenProductoRepository.save(img);
                        }
                        
                        productoRepository.save(existing);
                        allProducts.add(existing);
                    },
                    () -> {
                        productoRepository.save(producto);
                        allProducts.add(producto);
                    }
                );
                
                processedSkus.add(sku);
                newProductsCount++;
            }

            log.info("Processed page {} - {} new products (total: {})", page, newProductsCount, allProducts.size());
            
            if (newProductsCount == 0 || allProducts.size() >= totalProducts) {
                break;
            }
            
            page++;
        }

        int totalPages = (int) Math.ceil((double) totalProducts / 30);
        Categoria categoria = saveCategoria(categoryUrl, categoryPath, totalPages, 30);
        log.info("Import completed. Total products: {}", allProducts.size());
        
        return categoria;
    }

    private Categoria saveCategoria(String url, String path, int totalPages, int pageSize) {
        Categoria categoria = categoriaRepository.findByRuta(path)
            .orElse(new Categoria());
        
        categoria.setRuta(path);
        categoria.setUrlCategoria(url);
        categoria.setCantidadPaginas(totalPages);
        categoria.setProductosPorPagina(pageSize);
        
        return categoriaRepository.save(categoria);
    }

    private String extractCategoryPath(String url) {
        return url.replaceFirst("https://www\\.paris\\.cl/", "").replaceAll("/$", "");
    }
}
