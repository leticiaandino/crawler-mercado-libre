package com.mercadolibre.service.imp;

import com.mercadolibre.crawler.MercadoLibreCrawler;
import com.mercadolibre.crawler.ParisCrawler;
import com.mercadolibre.crawler.AbcCrawler;
import com.mercadolibre.model.*;
import com.mercadolibre.repository.*;
import com.mercadolibre.service.CrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@Transactional
public class CrawlerServiceImpl implements CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerServiceImpl.class);

    @Autowired
    private MercadoLibreCrawler mercadoLibreCrawler;
    
    @Autowired
    private ParisCrawler parisCrawler;
    
    @Autowired
    private AbcCrawler abcCrawler;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ImagenProductoRepository imagenProductoRepository;

    @Autowired
    private ProductoCategoriaRepository productoCategoriaRepository;

    @Override
    public Producto extraerFichaProducto(String url) {
        Producto producto;
        
        if (url.contains("mercadolibre.com")) {
            producto = mercadoLibreCrawler.crawlProducto(url);
        } else if (url.contains("paris.cl")) {
            producto = parisCrawler.crawlProducto(url);
        } else if (url.contains("abc.cl")) {
            producto = abcCrawler.crawlProducto(url);
        } else {
            throw new IllegalArgumentException("URL no soportada: " + url);
        }

        // Guardar o actualizar producto
        Optional<Producto> existing = productoRepository.findBySku(producto.getSku());
        if (existing.isPresent()) {
            producto.setId(existing.get().getId());
            // Limpiar imágenes existentes
            imagenProductoRepository.deleteByProductoId(producto.getId());
        }

        // Guardar producto (las imágenes se guardan automáticamente por CascadeType.ALL)
        Producto savedProducto = productoRepository.save(producto);
        
        return savedProducto;
    }

    @Override
    public List<Producto> extraerListadoProductos(String urlCategoria) {
        urlCategoria = urlCategoria.trim();

        // Determinar qué crawler usar
        if (!urlCategoria.contains("paris.cl") && !urlCategoria.contains("abc.cl")) {
            throw new IllegalArgumentException("Solo se soporta crawling de Paris y ABC. URL recibida: " + urlCategoria);
        }

        // 1. Obtener metadata según el sitio
        Categoria categoria;
        if (urlCategoria.contains("paris.cl")) {
            categoria = parisCrawler.crawlMetadataCategoria(urlCategoria);
        } else {
            categoria = abcCrawler.crawlMetadataCategoria(urlCategoria);
        }

        logger.info("Metadata obtenida del crawler: {} páginas", categoria.getCantidadPaginas());
        
        Optional<Categoria> existingCatOpt = categoriaRepository.findByRuta(categoria.getRuta());
        Categoria savedCategoria;
        
        if (existingCatOpt.isPresent()) {
            savedCategoria = existingCatOpt.get();
            logger.info("Categoría existente en BD con {} páginas. Actualizando...", savedCategoria.getCantidadPaginas());
            savedCategoria.setCantidadPaginas(categoria.getCantidadPaginas());
            savedCategoria.setProductosPorPagina(categoria.getProductosPorPagina());
            savedCategoria.setUrlCategoria(categoria.getUrlCategoria());
            savedCategoria = categoriaRepository.save(savedCategoria);
            logger.info("Categoría actualizada a {} páginas", savedCategoria.getCantidadPaginas());
        } else {
            savedCategoria = categoriaRepository.save(categoria);
            logger.info("Nueva categoría guardada con {} páginas", savedCategoria.getCantidadPaginas());
        }

        int totalProductosExtraidos = 0;
        List<Producto> todosLosProductos = new ArrayList<>();

        // 2. Iterar páginas (0-based)
        for (int pagina = 0; pagina < savedCategoria.getCantidadPaginas(); pagina++) {
            logger.info("Extrayendo página {} de {}", pagina, savedCategoria.getCantidadPaginas());

            List<Producto> productosPagina;
            if (urlCategoria.contains("paris.cl")) {
                productosPagina = parisCrawler.crawlListadoProductos(urlCategoria, pagina, savedCategoria.getCantidadPaginas());
            } else {
                productosPagina = abcCrawler.crawlListadoProductos(urlCategoria, pagina, savedCategoria.getCantidadPaginas());
            }

            if (productosPagina.isEmpty()) {
                logger.warn("Página {} vacía. Deteniendo.", pagina);
                break;
            }

            // Guardar productos y relaciones
            List<Producto> toSave = new ArrayList<>();
            List<ProductoCategoria> relaciones = new ArrayList<>();

            for (Producto p : productosPagina) {
                Optional<Producto> existing = productoRepository.findBySku(p.getSku());
                if (existing.isPresent()) {
                    p.setId(existing.get().getId());
                    // Opcional: actualizar campos si cambian
                }
                toSave.add(p);
            }

            List<Producto> savedProductos = productoRepository.saveAll(toSave);

            for (Producto p : savedProductos) {
                ProductoCategoriaId id = new ProductoCategoriaId();
                id.setProductoId(p.getId());
                id.setCategoriaId(savedCategoria.getId());

                ProductoCategoria pc = new ProductoCategoria();
                pc.setId(id);
                pc.setProducto(p);
                pc.setCategoria(savedCategoria);
                relaciones.add(pc);
            }

            productoCategoriaRepository.saveAll(relaciones);
            totalProductosExtraidos += productosPagina.size();
            todosLosProductos.addAll(savedProductos);
        }

        logger.info("Extracción completada: {} productos", totalProductosExtraidos);
        return todosLosProductos;
    }

    @Override
    public Categoria obtenerMetadataCategoria(String urlCategoria) {
        if (urlCategoria.contains("mercadolibre.com")) {
            return mercadoLibreCrawler.crawlMetadataCategoria(urlCategoria);
        } else if (urlCategoria.contains("paris.cl")) {
            return parisCrawler.crawlMetadataCategoria(urlCategoria);
        } else if (urlCategoria.contains("abc.cl")) {
            return abcCrawler.crawlMetadataCategoria(urlCategoria);
        } else {
            throw new IllegalArgumentException("URL de categoría no soportada: " + urlCategoria);
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL no puede ser nula o vacía");
        }
        
        String normalized = url.trim();
        
        // Remover parámetros de tracking de MercadoLibre
        if (normalized.contains("?pdp_filters")) {
            normalized = normalized.substring(0, normalized.indexOf("?pdp_filters"));
        }
        if (normalized.contains("#polycard_client")) {
            normalized = normalized.substring(0, normalized.indexOf("#polycard_client"));
        }
        
        return normalized;
    }

}
