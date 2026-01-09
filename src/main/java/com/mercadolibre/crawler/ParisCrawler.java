package com.mercadolibre.crawler;

import com.mercadolibre.client.ParisApiClient;
import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.mapper.ProductMapper;
import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.Producto;
import com.mercadolibre.util.RateLimitHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Crawler para Paris.cl
 * Usa la API interna de Paris para extraer productos de categorías
 * Estructura correcta de API:
 * - applicationId
 * - filters[].key="group_id", .stringValues=["tecCelSmartphones"]
 * - pagination{page, pageSize}
 * - sortBy, sponsoredProducts, term
 */
@Slf4j
@Component
public class ParisCrawler implements Crawler {

    @Autowired
    private ParisApiClient apiClient;

    @Autowired
    private ProductMapper mapper;

    @Autowired
    private RateLimitHandler rateLimitHandler;

    private static final int PAGE_SIZE = 30;

    @Override
    public Producto crawlProducto(String url) {
        throw new UnsupportedOperationException("No implementado para Paris");
    }

    @Override
    public Categoria crawlMetadataCategoria(String urlCategoria) {
        try {
            log.debug("Obteniendo metadata de categoría: {}", urlCategoria);
            String categoryPath = extractCategoryPath(urlCategoria);

            // Aplicar rate limiting antes de hacer el request
            String apiUrl = "https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/";
            log.debug("Aplicando rate limiting para: {}", apiUrl);
            rateLimitHandler.waitBeforeRequest(apiUrl);

            // Obtener primera página para calcular total
            ParisProductsResponse response = apiClient.fetchCategory(categoryPath, 1);

            if (response == null) {
                log.warn("Respuesta nula de API");
                return createDefaultCategoria(urlCategoria);
            }

            int totalProducts = response.getTotal();
            if (totalProducts <= 0) {
                log.warn("Total productos inválido: {}", totalProducts);
                return createDefaultCategoria(urlCategoria);
            }

            int totalPages = (int) Math.ceil((double) totalProducts / PAGE_SIZE);

            log.info("Categoría: {}, Total productos: {}, Total páginas: {}",
                categoryPath, totalProducts, totalPages);

            Categoria categoria = new Categoria();
            categoria.setRuta(categoryPath);
            categoria.setUrlCategoria(urlCategoria);
            categoria.setCantidadPaginas(totalPages);
            categoria.setProductosPorPagina(PAGE_SIZE);

            return categoria;

        } catch (Exception e) {
            log.error("Error obteniendo metadata: {}", e.getMessage(), e);
            return createDefaultCategoria(urlCategoria);
        }
    }

    @Override
    public List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas) {
        try {
            if (paginaActual < 0 || paginaActual >= totalPaginas) {
                log.warn("Página {} fuera de rango [0-{})", paginaActual, totalPaginas);
                return Collections.emptyList();
            }

            log.debug("Extrayendo página {} de {}", paginaActual, totalPaginas);
            String categoryPath = extractCategoryPath(urlCategoria);

            // Aplicar rate limiting antes de hacer el request
            String apiUrl = "https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/";
            log.debug("Aplicando rate limiting para: {}", apiUrl);
            rateLimitHandler.waitBeforeRequest(apiUrl);

            // API de Paris usa páginas 1-based
            int pageNumber = paginaActual + 1;
            ParisProductsResponse response = apiClient.fetchCategory(categoryPath, pageNumber);

            if (response == null || response.getResults() == null) {
                log.warn("Respuesta nula o sin resultados en página {}", paginaActual);
                return Collections.emptyList();
            }

            List<Producto> productos = response.getResults().stream()
                .map(result -> mapper.fromApi(result, urlCategoria))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            log.debug("Página {} procesada: {} productos", paginaActual, productos.size());
            return productos;

        } catch (Exception e) {
            log.error("Error extrayendo listado página {}: {}", paginaActual, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String extractCategoryPath(String url) {
        try {
            return url.replace("https://www.paris.cl/", "")
                      .replace("http://www.paris.cl/", "")
                      .replaceAll("/$", "");
        } catch (Exception e) {
            log.error("Error extrayendo categoría de URL: {}", url, e);
            return "";
        }
    }

    private Categoria createDefaultCategoria(String urlCategoria) {
        Categoria categoria = new Categoria();
        categoria.setRuta(extractCategoryPath(urlCategoria));
        categoria.setUrlCategoria(urlCategoria);
        categoria.setCantidadPaginas(1);
        categoria.setProductosPorPagina(PAGE_SIZE);
        return categoria;
    }
}

