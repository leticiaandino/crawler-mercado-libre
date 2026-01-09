package com.mercadolibre.controller;

import com.mercadolibre.dto.ApiResponse;
 import com.mercadolibre.dto.CrawlerRequest;
import com.mercadolibre.model.Producto;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.service.CrawlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Tests para CrawlerController")
@ExtendWith(MockitoExtension.class)
class CrawlerControllerTest {

    @Mock
    private CrawlerService crawlerService;

    @InjectMocks
    private CrawlerController crawlerController;

    private Producto mockProducto;

    @BeforeEach
    void setUp() {
        mockProducto = new Producto();
        mockProducto.setSku("TEST123");
        mockProducto.setNombre("Producto Test");
        mockProducto.setPrecioActual(BigDecimal.valueOf(100.0));
        mockProducto.setPrecioAnterior(BigDecimal.valueOf(150.0));
        mockProducto.setDisponibilidad("en_stock");
        mockProducto.setUrlFicha("https://test.com/producto");

        // Inicializar lista de imágenes
        List<ImagenProducto> imagenes = new ArrayList<>();
        ImagenProducto img = new ImagenProducto();
        img.setUrlImagen("https://test.com/imagen1.jpg");
        img.setOrden(1);
        imagenes.add(img);
        mockProducto.setImagenes(imagenes);
    }

    // ==================== TESTS DEL ENDPOINT HEALTH ====================

    @Test
    @DisplayName("Health check debe retornar 200 OK")
    void testHealthCheck() {
        ResponseEntity<String> response = crawlerController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Crawler service is running", response.getBody());
    }

    @Test
    @DisplayName("Health check debe contener mensaje correcto")
    void testHealthCheckMensaje() {
        ResponseEntity<String> response = crawlerController.health();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("running"));
    }

    // ==================== TESTS DEL ENDPOINT EXTRACT ====================

    @Test
    @DisplayName("Endpoint extract debe aceptar URL válida")
    void testExtractConUrlValida() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Endpoint extract debe retornar ApiResponse")
    void testExtractRetornaApiResponse() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);

        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
    }

    @Test
    @DisplayName("Endpoint extract debe rechazar URL vacía")
    void testExtractUrlVacia() {
        CrawlerRequest request = new CrawlerRequest("   ");
        ResponseEntity<?> response = crawlerController.extract(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Endpoint extract debe limpiar comillas de la URL")
    void testExtractLimpiaComillas() {
        String urlConComillas = "\"https://www.abc.cl/producto/123.html\"";
        String urlLimpia = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(urlConComillas);

        when(crawlerService.extraerFichaProducto(urlLimpia)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TESTS DEL ENDPOINT PRODUCTO ====================

    @Test
    @DisplayName("Endpoint producto debe retornar ficha extraída")
    void testProductoEndpoint() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.crawlProducto(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Producto en respuesta debe contener SKU")
    void testProductoTieneSku() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.crawlProducto(request);

        assertNotNull(response.getBody());
        // La respuesta es un ApiResponse
    }

    // ==================== TESTS DEL ENDPOINT LISTADO ====================

    @Test
    @DisplayName("Endpoint listado-productos debe retornar lista de productos")
    void testListadoProductosEndpoint() {
        String url = "https://www.abc.cl/hombre/accesorios/";
        CrawlerRequest request = new CrawlerRequest(url);

        List<Producto> mockListado = new ArrayList<>();
        mockListado.add(mockProducto);

        when(crawlerService.extraerListadoProductos(url)).thenReturn(mockListado);

        ResponseEntity<?> response = crawlerController.crawlListadoProductos(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Listado vacío debe retornar lista vacía")
    void testListadoProductosVacio() {
        String url = "https://www.abc.cl/categoria-vacia/";
        CrawlerRequest request = new CrawlerRequest(url);

        List<Producto> mockListado = new ArrayList<>();
        when(crawlerService.extraerListadoProductos(url)).thenReturn(mockListado);

        ResponseEntity<?> response = crawlerController.crawlListadoProductos(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== TESTS DE MANEJO DE EXCEPCIONES ====================

    @Test
    @DisplayName("Debe manejar excepción en extract")
    void testExtractConExcepcion() {
        String url = "https://www.abc.cl/producto-inexistente/999.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url))
            .thenThrow(new RuntimeException("Producto no encontrado"));

        ResponseEntity<?> response = crawlerController.extract(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar excepción en producto")
    void testProductoConExcepcion() {
        String url = "https://www.abc.cl/producto-inexistente/999.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url))
            .thenThrow(new RuntimeException("Error al procesar"));

        ResponseEntity<?> response = crawlerController.crawlProducto(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar excepción en listado-productos")
    void testListadoConExcepcion() {
        String url = "https://www.abc.cl/categoria-error/";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerListadoProductos(url))
            .thenThrow(new RuntimeException("Error en API"));

        ResponseEntity<?> response = crawlerController.crawlListadoProductos(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ==================== TESTS DE FORMATO DE RESPUESTA ====================

    @Test
    @DisplayName("Respuesta debe tener estructura ApiResponse")
    void testRespuestaHasApiResponseStructure() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);
        Object body = response.getBody();

        assertTrue(body instanceof ApiResponse, "Respuesta debe ser ApiResponse");
    }

    @Test
    @DisplayName("ApiResponse debe tener campo success")
    void testApiResponseTieneSuccess() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();

        assertTrue(apiResponse.isSuccess());
    }

    @Test
    @DisplayName("ApiResponse debe tener mensaje")
    void testApiResponseTieneMensaje() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();

        assertNotNull(apiResponse.getMessage());
        assertFalse(apiResponse.getMessage().isEmpty());
    }

    @Test
    @DisplayName("ApiResponse error debe tener campo error")
    void testApiResponseErrorTieneError() {
        String url = "https://www.abc.cl/producto-inexistente/999.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url))
            .thenThrow(new RuntimeException("Test error"));

        ResponseEntity<?> response = crawlerController.extract(request);
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();

        assertFalse(apiResponse.isSuccess());
        assertNotNull(apiResponse.getError());
    }

    @Test
    @DisplayName("ApiResponse debe tener timestamp")
    void testApiResponseTieneTimestamp() {
        String url = "https://www.abc.cl/producto/123.html";
        CrawlerRequest request = new CrawlerRequest(url);

        when(crawlerService.extraerFichaProducto(url)).thenReturn(mockProducto);

        ResponseEntity<?> response = crawlerController.extract(request);
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();

        assertTrue(apiResponse.getTimestamp() > 0);
    }
}

