package com.mercadolibre.service;

import com.mercadolibre.crawler.MercadoLibreCrawler;
import com.mercadolibre.model.Producto;
import com.mercadolibre.repository.ProductoRepository;
import com.mercadolibre.service.imp.CrawlerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    @Mock
    private MercadoLibreCrawler mercadoLibreCrawler;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CrawlerServiceImpl crawlerService;

    @Test
    void testExtraerFichaProductoNuevo() {
        // Arrange
        String url = "https://www.mercadolibre.com.ar/producto/p/MLA123";
        Producto producto = createTestProducto();
        
        when(mercadoLibreCrawler.crawlProducto(url)).thenReturn(producto);
        when(productoRepository.findBySku("MLA123")).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        // Act
        Producto result = crawlerService.extraerFichaProducto(url);

        // Assert
        assertNotNull(result);
        assertEquals("MLA123", result.getSku());
        verify(mercadoLibreCrawler).crawlProducto(url);
        verify(productoRepository).save(producto);
    }

    @Test
    void testExtraerFichaProductoExistente() {
        // Arrange
        String url = "https://www.mercadolibre.com.ar/producto/p/MLA123";
        Producto productoExistente = createTestProducto();
        productoExistente.setId(1L);
        
        Producto productoNuevo = createTestProducto();
        
        when(mercadoLibreCrawler.crawlProducto(url)).thenReturn(productoNuevo);
        when(productoRepository.findBySku("MLA123")).thenReturn(Optional.of(productoExistente));
        when(productoRepository.save(any(Producto.class))).thenReturn(productoNuevo);

        // Act
        Producto result = crawlerService.extraerFichaProducto(url);

        // Assert
        assertNotNull(result);
        assertEquals(1L, productoNuevo.getId()); // Debe mantener el ID existente
        verify(productoRepository).save(productoNuevo);
    }

    private Producto createTestProducto() {
        Producto producto = new Producto();
        producto.setSku("MLA123");
        producto.setNombre("Producto Test");
        producto.setPrecioActual(new BigDecimal("100.00"));
        producto.setDisponibilidad("stock_disponible");
        producto.setUrlFicha("https://www.mercadolibre.com.ar/producto/p/MLA123");
        return producto;
    }
}