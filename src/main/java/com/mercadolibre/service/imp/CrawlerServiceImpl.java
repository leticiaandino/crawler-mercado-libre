package com.mercadolibre.service.imp;

import com.mercadolibre.crawler.MercadoLibreCrawler;
import com.mercadolibre.model.*;
import com.mercadolibre.repository.*;
import com.mercadolibre.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@Transactional
public class CrawlerServiceImpl implements CrawlerService {

    @Autowired
    private MercadoLibreCrawler mercadoLibreCrawler;

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
        Producto producto = mercadoLibreCrawler.crawlProducto(url);

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
        Categoria categoria = mercadoLibreCrawler.crawlMetadataCategoria(urlCategoria);
        
        // Guardar o actualizar categoría
        Optional<Categoria> existingCategoria = categoriaRepository.findByRuta(categoria.getRuta());
        if (existingCategoria.isPresent()) {
            categoria.setId(existingCategoria.get().getId());
        }
        categoria = categoriaRepository.save(categoria);

        List<Producto> productos = mercadoLibreCrawler.crawlListadoProductos(urlCategoria, 1, categoria.getCantidadPaginas());

        // Procesar productos en lote
        List<Producto> productosToSave = new ArrayList<>();
        List<ProductoCategoria> relacionesToSave = new ArrayList<>();
        
        final Categoria finalCategoria = categoria;
        productos.forEach(producto -> {
            Optional<Producto> existingProducto = productoRepository.findBySku(producto.getSku());
            if (existingProducto.isPresent()) {
                producto.setId(existingProducto.get().getId());
            }
            productosToSave.add(producto);
        });
        
        // Guardar productos en lote
        List<Producto> savedProductos = productoRepository.saveAll(productosToSave);
        
        // Crear relaciones producto-categoría en lote
        savedProductos.forEach(savedProducto -> {
            ProductoCategoriaId id = new ProductoCategoriaId();
            id.setProductoId(savedProducto.getId());
            id.setCategoriaId(finalCategoria.getId());
            
            ProductoCategoria productoCategoria = new ProductoCategoria();
            productoCategoria.setId(id);
            productoCategoria.setProducto(savedProducto);
            productoCategoria.setCategoria(finalCategoria);
            
            relacionesToSave.add(productoCategoria);
        });
        
        // Guardar relaciones en lote
        productoCategoriaRepository.saveAll(relacionesToSave);

        return productos;
    }

    @Override
    public Categoria obtenerMetadataCategoria(String urlCategoria) {
        return mercadoLibreCrawler.crawlMetadataCategoria(urlCategoria);
    }
}
