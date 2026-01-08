package com.mercadolibre.crawler;

import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ParisCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(ParisCrawler.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final String API_PRODUCTS_URL = "https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/";
    private static final int PAGE_SIZE = 30;

    private final RestTemplate restTemplate = new RestTemplate();

    // Mapa de URLs a category IDs de breadcrumbs
    private static final Map<String, String> URL_TO_CATEGORY_ID = new HashMap<>();
    static {
        URL_TO_CATEGORY_ID.put("/tecnologia/celulares/smartphone/", "tecCelSmartphones");
        URL_TO_CATEGORY_ID.put("/tecnologia/impresoras/rotuladores/", "tecImpRotuladores");
    }

    @Override
    public Producto crawlProducto(String url) {
        throw new UnsupportedOperationException("No implementado");
    }

    @Override
    public Categoria crawlMetadataCategoria(String urlCategoria) {
        String categoryPath = extractCategoryPath(urlCategoria);
        String categoryId = URL_TO_CATEGORY_ID.get(categoryPath);
        
        Map<String, Object> response = fetchProductsPage(0, 30, categoryId);
        if (response == null || !response.containsKey("total")) {
            logger.warn("Respuesta inválida o sin campo 'total'. Usando valores por defecto.");
            return fallbackCategoria(urlCategoria);
        }

        Object totalObj = response.get("total");
        int total = 0;

        if (totalObj instanceof Number) {
            total = ((Number) totalObj).intValue();
        } else {
            logger.warn("Tipo inesperado en 'total': {}", totalObj != null ? totalObj.getClass() : "null");
            return fallbackCategoria(urlCategoria);
        }

        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        logger.info("Total productos reales: {}, páginas: {}", total, totalPages);

        Categoria cat = new Categoria();
        cat.setRuta(extractCategoryName(urlCategoria));
        cat.setUrlCategoria(urlCategoria.trim());
        cat.setCantidadPaginas(totalPages);
        cat.setProductosPorPagina(PAGE_SIZE);
        return cat;
    }

    @Override
    public List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas) {
        if (paginaActual < 0 || paginaActual >= totalPaginas) {
            return Collections.emptyList();
        }

        String categoryPath = extractCategoryPath(urlCategoria);
        String categoryId = URL_TO_CATEGORY_ID.get(categoryPath);
        
        int offset = paginaActual * PAGE_SIZE;
        Map<String, Object> response = fetchProductsPage(offset, PAGE_SIZE, categoryId);

        if (response == null) {
            logger.warn("Respuesta nula en página {}", paginaActual);
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            logger.warn("No hay 'results' en la página {}", paginaActual);
            return Collections.emptyList();
        }

        return results.stream()
                .map(item -> mapToProducto(item, urlCategoria))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> fetchProductsPage(int offset, int limit, String categoryId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", USER_AGENT);

        Map<String, Object> body = new HashMap<>();
        body.put("offset", offset);
        body.put("limit", limit);
        if (categoryId != null) {
            body.put("query", "category:" + categoryId);
        }

        if (offset == 0) {
            logger.info("Request body: {}", body);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_PRODUCTS_URL, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (offset == 0 && responseBody != null) {
                logger.info("Total productos: {}", responseBody.get("total"));
            }
            return responseBody;
        } catch (Exception e) {
            logger.error("Error API Paris (offset={}, limit={}): {}", offset, limit, e.getMessage());
            return null;
        }
    }

    private Producto mapToProducto(Map<String, Object> item, String urlCategoria) {
        try {
            // Nombre: name.es-CL
            Map<String, Object> nameMap = (Map<String, Object>) item.get("name");
            String nombre = nameMap != null ? (String) nameMap.get("es-CL") : null;

            // Master variant
            Map<String, Object> masterVariant = (Map<String, Object>) item.get("masterVariant");
            if (nombre == null || masterVariant == null) return null;

            String sku = (String) masterVariant.get("sku");
            if (sku == null) return null;

            // Precios
            Map<String, Object> prices = (Map<String, Object>) masterVariant.get("prices");
            BigDecimal precioActual = null;
            BigDecimal precioAnterior = null;

            if (prices != null) {
                Map<String, Object> offer = (Map<String, Object>) prices.get("offer");
                Map<String, Object> regular = (Map<String, Object>) prices.get("regular");

                if (offer != null) {
                    Map<String, Object> value = (Map<String, Object>) offer.get("value");
                    Integer centAmount = (Integer) value.get("centAmount");
                    precioActual = centAmount != null ? new BigDecimal(centAmount) : null;
                }

                if (regular != null) {
                    Map<String, Object> value = (Map<String, Object>) regular.get("value");
                    Integer centAmount = (Integer) value.get("centAmount");
                    precioAnterior = centAmount != null ? new BigDecimal(centAmount) : null;
                }

                if (precioActual == null && precioAnterior != null) {
                    precioActual = precioAnterior;
                    precioAnterior = null;
                }
            }

            if (precioActual == null) precioActual = BigDecimal.ZERO;

            // Imágenes
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> imageList = (List<Map<String, Object>>) masterVariant.get("images");
            List<ImagenProducto> imagenes = new ArrayList<>();
            if (imageList != null) {
                for (int i = 0; i < imageList.size(); i++) {
                    String url = (String) imageList.get(i).get("url");
                    if (url != null) {
                        ImagenProducto img = new ImagenProducto();
                        img.setUrlImagen(url.trim()); // Limpiar espacios
                        img.setOrden(i + 1);
                        imagenes.add(img);
                    }
                }
            }

            // Disponibilidad: asumimos "en_stock"
            Producto p = new Producto();
            p.setSku(sku);
            p.setNombre(nombre);
            p.setPrecioActual(precioActual);
            p.setPrecioAnterior(precioAnterior);
            p.setDisponibilidad("en_stock");
            p.setUrlFicha(urlCategoria);
            p.setImagenes(imagenes);

            // Establecer relación bidireccional
            imagenes.forEach(img -> img.setProducto(p));

            return p;

        } catch (Exception e) {
            logger.warn("Error al mapear producto: {}", e.getMessage());
            return null;
        }
    }

    private String extractCategoryPath(String url) {
        try {
            String path = url.replace("https://www.paris.cl", "").replace("http://www.paris.cl", "");
            if (!path.endsWith("/")) path += "/";
            return path;
        } catch (Exception e) {
            return "/";
        }
    }

    private String extractCategoryName(String url) {
        String path = extractCategoryPath(url);
        String[] parts = path.split("/");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                name.append(part.substring(0, 1).toUpperCase()).append(part.substring(1)).append(" > ");
            }
        }
        return name.length() > 3 ? name.substring(0, name.length() - 3) : "Categoría";
    }

    private Categoria fallbackCategoria(String urlCategoria) {
        Categoria cat = new Categoria();
        cat.setRuta(extractCategoryName(urlCategoria));
        cat.setUrlCategoria(urlCategoria);
        cat.setCantidadPaginas(0);
        cat.setProductosPorPagina(PAGE_SIZE);
        return cat;
    }
}