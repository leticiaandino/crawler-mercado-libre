package com.mercadolibre.mapper;

import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public Producto fromApi(ParisProductsResponse.Result apiProduct, String categoryUrl) {
        Producto producto = new Producto();
        
        var variant = apiProduct.getMasterVariant();
        producto.setSku(variant.getSku());
        
        String nombre = apiProduct.getName() != null && apiProduct.getName().containsKey("es-CL") 
            ? apiProduct.getName().get("es-CL") 
            : apiProduct.getBrand();
        producto.setNombre(nombre);
        producto.setUrlFicha(categoryUrl);
        
        var prices = variant.getPrices();
        if (prices.getOffer() != null && prices.getOffer().getValue() != null) {
            producto.setPrecioActual(BigDecimal.valueOf(prices.getOffer().getValue().getCentAmount()));
            producto.setPrecioAnterior(BigDecimal.valueOf(prices.getRegular().getValue().getCentAmount()));
        } else {
            producto.setPrecioActual(BigDecimal.valueOf(prices.getRegular().getValue().getCentAmount()));
            producto.setPrecioAnterior(null);
        }
        
        producto.setDisponibilidad(apiProduct.isPublished() ? "stock_disponible" : "agotado");
        
        List<ImagenProducto> imagenes = new ArrayList<>();
        if (variant.getImages() != null) {
            int orden = 1;
            for (var img : variant.getImages()) {
                ImagenProducto imagen = new ImagenProducto();
                imagen.setUrlImagen(img.getUrl());
                imagen.setOrden(orden++);
                imagen.setProducto(producto);
                imagenes.add(imagen);
            }
        }
        producto.setImagenes(imagenes);
        
        return producto;
    }
}
