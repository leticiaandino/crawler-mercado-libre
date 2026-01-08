package com.mercadolibre.service;

import com.mercadolibre.client.ParisApiClient;
import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.mapper.ProductMapper;
import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.Producto;
import com.mercadolibre.repository.CategoriaRepository;
import com.mercadolibre.repository.ImagenProductoRepository;
import com.mercadolibre.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParisCategoryServiceTest {

    @Mock
    private ParisApiClient apiClient;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ImagenProductoRepository imagenProductoRepository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ParisCategoryService service;

    private ParisProductsResponse mockResponse;
    private Producto mockProducto;

    @BeforeEach
    void setUp() {
        mockResponse = new ParisProductsResponse();
        mockResponse.setTotal(60);
        mockResponse.setCount(30);
        mockResponse.setLimit(30);
        
        List<ParisProductsResponse.Result> results = new ArrayList<>();
        ParisProductsResponse.Result result = new ParisProductsResponse.Result();
        ParisProductsResponse.MasterVariant variant = new ParisProductsResponse.MasterVariant();
        variant.setSku("TEST123");
        result.setMasterVariant(variant);
        results.add(result);
        mockResponse.setResults(results);

        mockProducto = new Producto();
        mockProducto.setSku("TEST123");
        mockProducto.setNombre("Test Product");
        mockProducto.setPrecioActual(BigDecimal.valueOf(10000));
        mockProducto.setImagenes(new ArrayList<>());
    }

    @Test
    void importCategory_Success() {
        when(apiClient.fetchCategory(anyString(), anyInt())).thenReturn(mockResponse);
        when(mapper.fromApi(any(), anyString())).thenReturn(mockProducto);
        when(productoRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(productoRepository.save(any())).thenReturn(mockProducto);
        when(categoriaRepository.findByRuta(anyString())).thenReturn(Optional.empty());
        when(categoriaRepository.save(any())).thenReturn(new Categoria());

        Categoria result = service.importCategory("https://www.paris.cl/tecnologia/celulares/smartphone/");

        assertNotNull(result);
        verify(apiClient, atLeastOnce()).fetchCategory(anyString(), anyInt());
        verify(productoRepository, atLeastOnce()).save(any());
    }

    @Test
    void importCategory_UpdatesExistingProduct() {
        Producto existing = new Producto();
        existing.setId(1L);
        existing.setSku("TEST123");
        existing.setImagenes(new ArrayList<>());

        when(apiClient.fetchCategory(anyString(), anyInt())).thenReturn(mockResponse);
        when(mapper.fromApi(any(), anyString())).thenReturn(mockProducto);
        when(productoRepository.findBySku("TEST123")).thenReturn(Optional.of(existing));
        when(productoRepository.save(any())).thenReturn(existing);
        when(categoriaRepository.findByRuta(anyString())).thenReturn(Optional.empty());
        when(categoriaRepository.save(any())).thenReturn(new Categoria());

        service.importCategory("https://www.paris.cl/tecnologia/celulares/smartphone/");

        verify(imagenProductoRepository).deleteByProductoId(1L);
        verify(productoRepository).save(existing);
    }
}
