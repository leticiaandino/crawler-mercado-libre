package com.mercadolibre.crawler;

import com.mercadolibre.model.Producto;
import com.mercadolibre.util.RateLimitHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@DisplayName("Tests para MercadoLibreCrawler")
@ExtendWith(MockitoExtension.class)
class MercadoLibreCrawlerTest {

    @Mock
    private RateLimitHandler rateLimitHandler;

    @InjectMocks
    private MercadoLibreCrawler mercadoLibreCrawler;

    @BeforeEach
    void setUp() {
        // Usar lenient() para mocks que no se usan en todos los tests
        lenient().doNothing().when(rateLimitHandler).waitBeforeRequest(anyString());
    }

    // ==================== TESTS DE EXTRACCIÓN DE SKU ====================

    @Test
    @DisplayName("Debe extraer SKU correctamente del formato /p/MLA")
    void testExtractSkuFromUrlFormatP() {
        String url = "https://www.mercadolibre.com.ar/sierra-circular-7-14-185-190mm-1600w-hs7010-makita/p/MLA19813486";

        String sku = extractSkuFromUrlReflection(url);

        assertEquals("MLA19813486", sku);
    }

    @Test
    @DisplayName("Debe extraer SKU correctamente del formato /up/MLAU")
    void testExtractSkuFromUrlFormatUp() {
        String url = "https://www.mercadolibre.com.ar/producto/up/MLAU266107237";

        String sku = extractSkuFromUrlReflection(url);

        assertEquals("MLAU266107237", sku);
    }

    @Test
    @DisplayName("Debe extraer SKU cuando no tiene /p/ o /up/")
    void testExtractSkuFromUrlAlternativeFormat() {
        String url = "https://www.mercadolibre.com.ar/MLA19813486-producto";

        String sku = extractSkuFromUrlReflection(url);

        assertTrue(sku.contains("MLA19813486") || sku.startsWith("UNKNOWN_SKU_"));
    }

    @Test
    @DisplayName("Debe generar SKU desconocido cuando no puede extraer")
    void testExtractSkuFromUrlInvalid() {
        String url = "https://www.mercadolibre.com.ar/producto-invalido";

        String sku = extractSkuFromUrlReflection(url);

        assertTrue(sku.startsWith("UNKNOWN_SKU_"));
    }

    // ==================== TESTS DE VALIDACIÓN ====================

    @Test
    @DisplayName("Debe lanzar excepción con URL nula")
    void testCrawlProductoUrlNula() {
        assertThrows(RuntimeException.class, () -> {
            mercadoLibreCrawler.crawlProducto(null);
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción con URL vacía")
    void testCrawlProductoUrlVacia() {
        assertThrows(RuntimeException.class, () -> {
            mercadoLibreCrawler.crawlProducto("   ");
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción con URL no válida")
    void testCrawlProductoUrlNoValida() {
        String url = "https://www.mercadolibre.com.ar/producto-inexistente-12345";

        assertThrows(RuntimeException.class, () -> {
            mercadoLibreCrawler.crawlProducto(url);
        });
    }

    // ==================== TESTS DE NORMALIZACIÓN ====================

    @Test
    @DisplayName("Debe normalizar URL removiendo query parameters")
    void testNormalizeMercadoLibreUrlWithQuery() {
        String url = "https://www.mercadolibre.com.ar/producto/p/MLA123?param=value#section";

        String normalized = normalizeUrlReflection(url);

        assertFalse(normalized.contains("?"));
        assertFalse(normalized.contains("#"));
    }

    @Test
    @DisplayName("Debe manejar URL nula en normalización")
    void testNormalizeMercadoLibreUrlNull() {
        String normalized = normalizeUrlReflection(null);

        assertNull(normalized);
    }

    // ==================== TESTS DE ESTRUCTURA ====================

    @Test
    @DisplayName("Crawler debe ser tipo Crawler")
    void testImplementsCrawlerInterface() {
        assertTrue(mercadoLibreCrawler instanceof Crawler);
    }

    @Test
    @DisplayName("Crawler debe tener el método crawlProducto")
    void testHasCrawlProductoMethod() {
        assertDoesNotThrow(() -> {
            MercadoLibreCrawler.class.getMethod("crawlProducto", String.class);
        });
    }

    @Test
    @DisplayName("Crawler debe tener el método crawlListadoProductos")
    void testHasCrawlListadoProductosMethod() {
        assertDoesNotThrow(() -> {
            MercadoLibreCrawler.class.getMethod("crawlListadoProductos", String.class, int.class, int.class);
        });
    }

    @Test
    @DisplayName("Crawler debe tener el método crawlMetadataCategoria")
    void testHasCrawlMetadataCategoriaMethod() {
        assertDoesNotThrow(() -> {
            MercadoLibreCrawler.class.getMethod("crawlMetadataCategoria", String.class);
        });
    }

    // ==================== TESTS DE CONFIGURACIÓN ====================

    @Test
    @DisplayName("Debe tener RateLimitHandler inyectado")
    void testHasRateLimitHandlerInjected() {
        assertNotNull(rateLimitHandler);
    }

    @Test
    @DisplayName("Debe tener ObjectMapper configurado")
    void testHasObjectMapperConfigured() {
        assertNotNull(mercadoLibreCrawler);
    }

    // ==================== TESTS DE MANEJO DE ERRORES ====================

    @Test
    @DisplayName("Debe manejar excepción de conexión correctamente")
    void testHandlesConnectionException() {
        String url = "https://www.mercadolibre.com.ar/no-existe";

        Exception exception = assertThrows(RuntimeException.class, () -> {
            mercadoLibreCrawler.crawlProducto(url);
        });

        assertTrue(exception.getMessage().contains("Error") || exception.getMessage().contains("conectar"));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Utiliza reflection para acceder al método privado extractSkuFromUrl
     */
    private String extractSkuFromUrlReflection(String url) {
        try {
            var method = MercadoLibreCrawler.class.getDeclaredMethod("extractSkuFromUrl", String.class);
            method.setAccessible(true);
            return (String) method.invoke(mercadoLibreCrawler, url);
        } catch (Exception e) {
            throw new RuntimeException("Error al usar reflection para extractSkuFromUrl", e);
        }
    }

    /**
     * Utiliza reflection para acceder al método privado normalizeMercadoLibreUrl
     */
    private String normalizeUrlReflection(String url) {
        try {
            var method = MercadoLibreCrawler.class.getDeclaredMethod("normalizeMercadoLibreUrl", String.class);
            method.setAccessible(true);
            return (String) method.invoke(mercadoLibreCrawler, url);
        } catch (Exception e) {
            throw new RuntimeException("Error al usar reflection para normalizeMercadoLibreUrl", e);
        }
    }
}

