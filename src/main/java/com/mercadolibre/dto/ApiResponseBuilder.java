package com.mercadolibre.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad para crear respuestas API consistentes
 */
public class ApiResponseBuilder {

    /**
     * Crea una respuesta exitosa con datos
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * Crea una respuesta exitosa sin datos
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * Crea una respuesta de error
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return new ApiResponse<>(false, null, null, errorMessage);
    }

    /**
     * Crea una respuesta de error con mensaje descriptivo
     */
    public static <T> ApiResponse<T> error(String message, String errorMessage) {
        return new ApiResponse<>(false, message, null, errorMessage);
    }

    /**
     * Crea un mapa de datos de producto para respuesta consistente
     */
    public static Map<String, Object> productToMap(com.mercadolibre.model.Producto producto) {
        Map<String, Object> map = new HashMap<>();
        map.put("sku", producto.getSku());
        map.put("nombre", producto.getNombre());
        map.put("precioActual", producto.getPrecioActual());
        map.put("precioAnterior", producto.getPrecioAnterior());
        map.put("disponibilidad", producto.getDisponibilidad());
        map.put("urlFicha", producto.getUrlFicha());
        map.put("imagenes", producto.getImagenes().stream()
            .map(img -> {
                Map<String, Object> imgMap = new HashMap<>();
                imgMap.put("url", img.getUrlImagen());
                imgMap.put("orden", img.getOrden());
                return imgMap;
            }).toList());
        map.put("fechaCreacion", producto.getFechaCreacion());
        map.put("fechaActualizacion", producto.getFechaActualizacion());
        return map;
    }

    /**
     * Crea una lista de mapas de productos para respuesta consistente
     */
    public static List<Map<String, Object>> productsToMapList(
            List<com.mercadolibre.model.Producto> productos) {
        return productos.stream()
            .map(producto -> {
                Map<String, Object> data = new HashMap<>();
                data.put("sku", producto.getSku());
                data.put("nombre", producto.getNombre());
                data.put("precioActual", producto.getPrecioActual());
                data.put("precioAnterior", producto.getPrecioAnterior());
                data.put("disponibilidad", producto.getDisponibilidad());
                data.put("imagenes", producto.getImagenes().stream()
                    .map(com.mercadolibre.model.ImagenProducto::getUrlImagen)
                    .toList());
                return data;
            }).toList();
    }
}

