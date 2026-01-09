package com.mercadolibre.mapper;

import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private ProductMapper mapper;
    private ParisProductsResponse.Result apiProduct;

    @BeforeEach
    void setUp() {
        mapper = new ProductMapper();
        
        apiProduct = new ParisProductsResponse.Result();
        apiProduct.setKey("625026999");
        apiProduct.setBrand("Samsung");
        apiProduct.setName(Map.of("es-CL", "Smartphone Galaxy S24 128GB"));
        apiProduct.setPublished(true);

        ParisProductsResponse.MasterVariant variant = new ParisProductsResponse.MasterVariant();
        variant.setSku("625026999");

        ParisProductsResponse.Prices prices = new ParisProductsResponse.Prices();
        ParisProductsResponse.Price regular = new ParisProductsResponse.Price();
        ParisProductsResponse.Value regularValue = new ParisProductsResponse.Value();
        regularValue.setCentAmount(979990L);
        regular.setValue(regularValue);
        prices.setRegular(regular);

        ParisProductsResponse.Price offer = new ParisProductsResponse.Price();
        ParisProductsResponse.Value offerValue = new ParisProductsResponse.Value();
        offerValue.setCentAmount(499990L);
        offer.setValue(offerValue);
        prices.setOffer(offer);

        variant.setPrices(prices);

        ParisProductsResponse.Image image = new ParisProductsResponse.Image();
        image.setUrl("https://example.com/image.jpg");
        variant.setImages(List.of(image));

        apiProduct.setMasterVariant(variant);
    }

    @Test
    void fromApi_MapsAllFields() {
        Producto result = mapper.fromApi(apiProduct, "https://www.paris.cl/test");

        assertEquals("625026999", result.getSku());
        assertEquals("Smartphone Galaxy S24 128GB", result.getNombre());
        assertEquals(BigDecimal.valueOf(499990), result.getPrecioActual());
        assertEquals(BigDecimal.valueOf(979990), result.getPrecioAnterior());
        assertEquals("stock_disponible", result.getDisponibilidad());
        assertEquals(1, result.getImagenes().size());
        assertEquals(1, result.getImagenes().get(0).getOrden());
    }

    @Test
    void fromApi_UsesRegularPrice_WhenNoOffer() {
        apiProduct.getMasterVariant().getPrices().setOffer(null);

        Producto result = mapper.fromApi(apiProduct, "https://www.paris.cl/test");

        assertEquals(BigDecimal.valueOf(979990), result.getPrecioActual());
        assertNull(result.getPrecioAnterior());
    }

    @Test
    void fromApi_UsesBrand_WhenNoName() {
        apiProduct.setName(null);

        Producto result = mapper.fromApi(apiProduct, "https://www.paris.cl/test");

        assertEquals("Samsung", result.getNombre());
    }

    @Test
    void fromApi_SetsAgotado_WhenNotPublished() {
        apiProduct.setPublished(false);

        Producto result = mapper.fromApi(apiProduct, "https://www.paris.cl/test");

        assertEquals("agotado", result.getDisponibilidad());
    }
}
