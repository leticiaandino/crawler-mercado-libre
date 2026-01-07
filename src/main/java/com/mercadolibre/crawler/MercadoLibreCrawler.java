package com.mercadolibre.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MercadoLibreCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(MercadoLibreCrawler.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String normalizeMercadoLibreUrl(String url) {
        if (url == null) return null;
        return url.split("[?#]")[0];
    }

    @Override
    public Producto crawlProducto(String url) {
        try {
            url = normalizeMercadoLibreUrl(url.trim());
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
            logger.debug("URL final cargada por Jsoup: {}", doc.location());

            // Extraer JSON del script __PRELOADED_STATE__
            Element jsonElement = doc.selectFirst("script#__PRELOADED_STATE__");
            if (jsonElement == null) {
                throw new RuntimeException("No se encontró el JSON __PRELOADED_STATE__ en la página");
            }

            String jsonData = jsonElement.data();
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode pageState = root.path("pageState").path("initialState");

            // SKU: usar el de la URL original
            String sku = extractSkuFromUrl(url);
            logger.debug("SKU extraído de la URL: {}", sku);

            // Nombre: desde el título
            String nombre = pageState.path("components").path("header").path("title").asText();
            logger.debug("Nombre extraído: {}", nombre);

            // Precios: desde el componente price
            JsonNode priceComponent = pageState.path("components").path("price").path("price");
            BigDecimal precioActual = null;
            BigDecimal precioAnterior = null;

            if (priceComponent.has("value")) {
                precioActual = new BigDecimal(priceComponent.path("value").asText());
            }
            if (priceComponent.has("original_value")) {
                precioAnterior = new BigDecimal(priceComponent.path("original_value").asText());
            }
            logger.debug("Precio actual extraído: {} (anterior: {})", precioActual, precioAnterior);

            // Disponibilidad: extraer desde HTML ya que no está en JSON
            String disponibilidad = "en_stock";
            
            // Buscar "¡Última disponible!" en el HTML
            Element ultimaDisponible = doc.selectFirst("span:contains(¡Última disponible!)");
            if (ultimaDisponible != null) {
                disponibilidad = "ultima_unidad";
                logger.debug("Detectado: última unidad disponible");
            } else {
                // Buscar "Stock disponible"
                Element stockDisponible = doc.selectFirst("span:contains(Stock disponible)");
                if (stockDisponible != null) {
                    disponibilidad = "stock_disponible";
                    logger.debug("Detectado: stock disponible");
                } else {
                    // Buscar indicadores de sin stock
                    Element sinStock = doc.selectFirst("span:contains(Sin stock), span:contains(No disponible)");
                    if (sinStock != null) {
                        disponibilidad = "agotado";
                        logger.debug("Detectado: producto agotado");
                    }
                }
            }
            
            logger.debug("Disponibilidad final determinada: {}", disponibilidad);

            // Imágenes: solo desde la galería principal (evitar vertical_gallery u otros duplicados)
            java.util.Set<String> seenIds = new java.util.LinkedHashSet<>();
            List<String> imagenesUrls = new ArrayList<>();

            // Extraer solo del primer "gallery" explícito bajo components
            JsonNode galleryNode = pageState.path("components").path("gallery");
            if (!galleryNode.isMissingNode()) {
                JsonNode pictures = galleryNode.path("pictures");
                if (pictures.isArray()) {
                    for (JsonNode picture : pictures) {
                        String id = picture.path("id").asText("").trim();
                        if (!id.isEmpty() && seenIds.add(id)) { // add() devuelve false si ya existía
                            String cleanId = id.replaceAll("\\s+", "");
                            String imgUrl = "https://http2.mlstatic.com/D_NQ_NP_" + cleanId + "-O.webp";
                            imagenesUrls.add(imgUrl);
                        }
                    }
                }
            }

            logger.debug("Imágenes únicas extraídas: {} URLs", imagenesUrls.size());

            Producto producto = new Producto();
            producto.setSku(sku);
            producto.setNombre(nombre);
            producto.setPrecioActual(precioActual);
            producto.setPrecioAnterior(precioAnterior);
            producto.setDisponibilidad(disponibilidad);
            producto.setUrlFicha(url);

            // Crear objetos ImagenProducto
            List<ImagenProducto> imagenes = new ArrayList<>();
            for (int i = 0; i < imagenesUrls.size(); i++) {
                ImagenProducto imagen = new ImagenProducto();
                imagen.setUrlImagen(imagenesUrls.get(i));
                imagen.setOrden(i + 1);
                imagen.setProducto(producto);
                imagenes.add(imagen);
            }
            producto.setImagenes(imagenes);

            return producto;

        } catch (Exception e) {
            logger.error("Error al extraer datos: {}", e.getMessage());
            throw new RuntimeException("Error al scrapear producto: " + url, e);
        }
    }

    @Override
    public Categoria crawlMetadataCategoria(String urlCategoria) {
        try {
            Document doc = Jsoup.connect(urlCategoria).userAgent(USER_AGENT).get();

            // Ruta de categoría
            String ruta = doc.select("nav.a-breadcrumb span").stream()
                    .map(Element::text)
                    .reduce((a, b) -> a + " > " + b)
                    .orElse("Sin categoría");

            // Cantidad de páginas
            String paginationText = doc.select("li.pagination__page--next").prev().text();
            int cantidadPaginas = Integer.parseInt(paginationText);

            // Productos por página (ej: 50)
            int productosPorPagina = 50; // Ajusta según el sitio

            Categoria categoria = new Categoria();
            categoria.setRuta(ruta);
            categoria.setUrlCategoria(urlCategoria);
            categoria.setCantidadPaginas(cantidadPaginas);
            categoria.setProductosPorPagina(productosPorPagina);

            return categoria;

        } catch (IOException e) {
            throw new RuntimeException("Error al scrapear metadata de categoría: " + urlCategoria, e);
        }
    }

    @Override
    public List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas) {
        List<Producto> productos = new ArrayList<>();

        try {
            String urlPaginada = urlCategoria + "?page=" + paginaActual;
            Document doc = Jsoup.connect(urlPaginada).userAgent(USER_AGENT).get();

            Elements productElements = doc.select("div.ui-search-result__content");

            for (Element prod : productElements) {
                String productUrl = prod.select("a.ui-search-link").attr("href");
                String sku = extractSkuFromUrl(productUrl);
                String nombre = prod.select("h2.ui-search-item__title").text();
                String precioActualStr = prod.select("span.price-tag-fraction").text();
                BigDecimal precioActual = new BigDecimal(precioActualStr.replace(".", "").replace(",", "."));

                String disponibilidad = "en_stock"; // Simplificado

                Producto producto = new Producto();
                producto.setSku(sku);
                producto.setNombre(nombre);
                producto.setPrecioActual(precioActual);
                producto.setDisponibilidad(disponibilidad);
                producto.setUrlFicha(productUrl);

                productos.add(producto);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al scrapear página " + paginaActual + " de: " + urlCategoria, e);
        }

        return productos;
    }

    private String extractSkuFromUrl(String url) {
        url = normalizeMercadoLibreUrl(url.trim());
        // Patrón mejorado para URLs como: /p/MLA19813486
        Pattern pattern = Pattern.compile("/p/(MLA[A-Z0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Patrón alternativo: /MLA19813486
        pattern = Pattern.compile("/(MLA[A-Z0-9]+)");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "UNKNOWN_SKU_" + System.currentTimeMillis();
    }
}