package com.mercadolibre.crawler;

import com.mercadolibre.model.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MercadoLibreCrawlerTest {

    @InjectMocks
    private MercadoLibreCrawler crawler;

    @Test
    void testExtractSkuFromRealUrl1() throws Exception {
        String url = "https://www.mercadolibre.com.ar/lector-codigos-de-barras--control-de-stock--ventas--caja/up/MLAU250092924#polycard_client=search-nordic";
        
        java.lang.reflect.Method method = MercadoLibreCrawler.class.getDeclaredMethod("extractSkuFromUrl", String.class);
        method.setAccessible(true);
        String sku = (String) method.invoke(crawler, url);
        
        assertEquals("MLAU250092924", sku);
    }

    @Test
    void testExtractSkuFromRealUrl2() throws Exception {
        String url = "https://www.mercadolibre.com.ar/canilla-cocina-monocomando-griferia-extensible-krumm-cromo-acabado-satinado-color-niquel/p/MLA26043589?pdp_filters=item_id:MLA1479119202#polycard_client=recommendations";
        
        java.lang.reflect.Method method = MercadoLibreCrawler.class.getDeclaredMethod("extractSkuFromUrl", String.class);
        method.setAccessible(true);
        String sku = (String) method.invoke(crawler, url);
        
        assertEquals("MLA26043589", sku);
    }

    @Test
    void testExtractSkuFromRealUrl3() throws Exception {
        String url = "https://www.mercadolibre.com.ar/software-ferreteria-libreria-venta-de-repuestos-corralon/up/MLAU266107237#polycard_client=recommendations";
        
        java.lang.reflect.Method method = MercadoLibreCrawler.class.getDeclaredMethod("extractSkuFromUrl", String.class);
        method.setAccessible(true);
        String sku = (String) method.invoke(crawler, url);
        
        assertEquals("MLAU266107237", sku);
    }

    @Test
    void testNormalizeMercadoLibreUrlWithRealUrls() throws Exception {
        java.lang.reflect.Method method = MercadoLibreCrawler.class.getDeclaredMethod("normalizeMercadoLibreUrl", String.class);
        method.setAccessible(true);
        
        String url1 = "https://www.mercadolibre.com.ar/canilla-cocina-monocomando-griferia-extensible-krumm-cromo-acabado-satinado-color-niquel/p/MLA26043589?pdp_filters=item_id:MLA1479119202#polycard_client=recommendations";
        String normalized1 = (String) method.invoke(crawler, url1);
        
        assertEquals("https://www.mercadolibre.com.ar/canilla-cocina-monocomando-griferia-extensible-krumm-cromo-acabado-satinado-color-niquel/p/MLA26043589", normalized1);
    }

    @Test
    void testNormalizeMercadoLibreUrlNull() throws Exception {
        java.lang.reflect.Method method = MercadoLibreCrawler.class.getDeclaredMethod("normalizeMercadoLibreUrl", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(crawler, (String) null);
        
        assertNull(result);
    }

    @Test
    void testCrawlProductoWithInvalidUrl() {
        String invalidUrl = "https://invalid-url.com";
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crawler.crawlProducto(invalidUrl);
        });
        
        assertTrue(exception.getMessage().contains("Error al scrapear producto"));
    }
}