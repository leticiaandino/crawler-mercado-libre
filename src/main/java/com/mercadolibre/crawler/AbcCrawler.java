package com.mercadolibre.crawler;

import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.ImagenProducto;
import com.mercadolibre.model.Producto;
import com.mercadolibre.util.RateLimitHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AbcCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(AbcCrawler.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Autowired
    private RateLimitHandler rateLimitHandler;

    @Override
    public Producto crawlProducto(String url) {
        try {
            // Validar URL
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL no puede estar vacía");
            }

            // Aplicar rate limiting antes de hacer el request
            logger.debug("Aplicando rate limiting para: {}", url);
            rateLimitHandler.waitBeforeRequest(url);

            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();

            String sku = extractSku(doc, url);

            Element nombreElement = doc.selectFirst("h1[itemprop=name].product-name");
            String nombre = nombreElement != null
                    ? nombreElement.text().trim()
                    : doc.select("title").text().split("\\|")[0].trim();

            BigDecimal precioActual = extractPrecioActual(doc);
            BigDecimal precioAnterior = extractPrecioAnterior(doc);
            List<ImagenProducto> imagenes = extractImages(doc);
            String disponibilidad = extractDisponibilidad(doc);

            Producto producto = new Producto();
            producto.setSku(sku);
            producto.setNombre(nombre);
            producto.setPrecioActual(precioActual);
            producto.setPrecioAnterior(precioAnterior);
            producto.setDisponibilidad(disponibilidad);
            producto.setUrlFicha(url);
            producto.setImagenes(imagenes);

            imagenes.forEach(img -> img.setProducto(producto));

            return producto;

        } catch (Exception e) {
            logger.error("Error al scrapear ABC: {}", e.getMessage());
            throw new RuntimeException("Error al scrapear producto ABC: " + url, e);
        }
    }

    private String extractSku(Document doc, String url) {
        Element skuElement = doc.selectFirst("span[itemprop=sku]");
        if (skuElement != null) {
            return skuElement.text().trim();
        }

        Pattern urlPattern = Pattern.compile("/(\\d+)\\.html");
        Matcher matcher = urlPattern.matcher(url);
        return matcher.find() ? matcher.group(1) : "UNKNOWN_SKU";
    }

    private BigDecimal extractPrecioActual(Document doc) {
        Element precioElement = doc.selectFirst("p.internet .price-value[data-value]");
        if (precioElement != null) {
            String valor = precioElement.attr("data-value");
            return new BigDecimal(valor.replaceAll("\\.0$", ""));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal extractPrecioAnterior(Document doc) {
        Element precioElement = doc.selectFirst("p.normal .price-value[data-value]");
        if (precioElement != null) {
            String valor = precioElement.attr("data-value");
            return new BigDecimal(valor.replaceAll("\\.0$", ""));
        }
        return null;
    }

    private List<ImagenProducto> extractImages(Document doc) {
        List<ImagenProducto> imagenes = new ArrayList<>();

        Elements thumbnails = doc.select(".primary-thumbnail:not(.slick-cloned) img");

        int orden = 1;
        for (Element img : thumbnails) {
            String src = img.absUrl("src").trim();
            if (src.contains("/images/large/")) {
                ImagenProducto imagen = new ImagenProducto();
                imagen.setUrlImagen(src);
                imagen.setOrden(orden++);
                imagenes.add(imagen);
            }
        }

        return imagenes;
    }

    private String extractDisponibilidad(Document doc) {
        // Busca el botón principal de "Agregar a la bolsa"
        Element boton = doc.selectFirst("button.add-to-cart");
        if (boton != null && !boton.hasAttr("disabled")) {
            return "en_stock";
        }

        // Opcional: detecta mensajes explícitos de agotamiento
        if (!doc.select("div:contains(agotado), span:contains(sin stock), p:contains(no disponible)").isEmpty()) {
            return "sin_stock";
        }

        // Por defecto, si no hay indicios claros de agotamiento, asume disponible
        return "en_stock";
    }

    @Override
    public Categoria crawlMetadataCategoria(String urlCategoria) {
        try {
            // Obtener la primera página para extraer metadata
            Document doc = Jsoup.connect(urlCategoria).userAgent(USER_AGENT).get();

            // Extraer nombre de la categoría de la ruta URL
            String ruta = extractCategoryName(urlCategoria);

            // Extraer cantidad total de páginas
            int totalPaginas = extractTotalPaginas(doc);

            // Extraer cantidad de productos por página
            int productosPorPagina = extractProductosPorPagina(doc);

            Categoria categoria = new Categoria();
            categoria.setRuta(ruta);
            categoria.setUrlCategoria(urlCategoria.trim());
            categoria.setCantidadPaginas(totalPaginas);
            categoria.setProductosPorPagina(productosPorPagina);

            logger.info("Metadata ABC obtenida - Categoría: {}, Páginas: {}, Productos/Página: {}",
                       ruta, totalPaginas, productosPorPagina);

            return categoria;
        } catch (Exception e) {
            logger.error("Error al extraer metadata de ABC: {}", e.getMessage());
            throw new RuntimeException("Error al extraer metadata de ABC: " + urlCategoria, e);
        }
    }

    @Override
    public List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas) {
        try {
            // Construir URL con paginación
            String urlPagina = construirUrlPaginada(urlCategoria, paginaActual);
            logger.info("Extrayendo página {} de {}: {}", paginaActual, totalPaginas, urlPagina);

            // Aplicar rate limiting antes de hacer el request
            rateLimitHandler.waitBeforeRequest(urlPagina);

            Document doc = Jsoup.connect(urlPagina).userAgent(USER_AGENT).get();
            List<Producto> prods = new ArrayList<>();

            // Seleccionar los artículos principales de la página
            Elements productElements = doc.select("div.product-tile__item");

            logger.debug("Selectores encontrados para product-tile__item: {}", productElements.size());

            if (productElements.isEmpty()) {
                logger.warn("No se encontraron productos con selector 'div.product-tile__item' en la página {}. Total elementos en DOM: {}",
                           paginaActual, doc.select("*").size());
                logger.debug("Intentando selectores alternativos...");

                // Intentar selectores alternativos
                productElements = doc.select("[class*='product-tile']");
                logger.debug("Selector alternativo [class*='product-tile']: {}", productElements.size());

                if (productElements.isEmpty()) {
                    productElements = doc.select("div.col-xs-6.col-sm-3");
                    logger.debug("Selector alternativo div.col-xs-6.col-sm-3: {}", productElements.size());
                }

                if (productElements.isEmpty()) {
                    return prods;
                }
            }

            logger.info("Encontrados {} productos en la página {}", productElements.size(), paginaActual);

            for (Element prodElement : productElements) {
                try {
                    Producto producto = extractProductoFromListItem(prodElement, urlCategoria);
                    if (producto != null) {
                        prods.add(producto);
                    }
                } catch (Exception e) {
                    logger.warn("Error al extraer producto individual: {}", e.getMessage());
                }
            }

            logger.info("Página {} procesada: {} productos extraídos", paginaActual, prods.size());

            // Agregar delay entre requests para evitar rate limiting
            if (paginaActual < totalPaginas - 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupción durante delay: {}", e.getMessage());
                }
            }

            return prods;


        } catch (Exception e) {
            logger.error("Error al extraer listado de productos de página {}: {}", paginaActual, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String extractCategoryName(String urlCategoria) {
        // Ejemplo: https://www.abc.cl/hombre/accesorios/ -> hombre/accesorios
        String path = urlCategoria.replaceAll("https?://[^/]+/?", "").replaceAll("/$", "");
        return path.isEmpty() ? "categorias" : path;
    }

    private int extractTotalPaginas(Document doc) {
        // Buscar el contenedor de paginación
        Element paginador = doc.selectFirst("div.search-results__load-more.pagination");

        if (paginador != null) {
            // Buscar todos los botones de página
            Elements pageButtons = paginador.select("button.btn-page");

            if (!pageButtons.isEmpty()) {
                // El último botón antes del ">" es el número de última página
                int maxPage = 0;
                for (Element btn : pageButtons) {
                    String text = btn.text().trim();
                    // Saltar el botón ">"
                    if (!text.equals(">") && !text.equals("<")) {
                        try {
                            int pageNum = Integer.parseInt(text);
                            maxPage = Math.max(maxPage, pageNum);
                        } catch (NumberFormatException e) {
                            // Ignorar si no es número
                        }
                    }
                }

                if (maxPage > 0) {
                    logger.info("Total de páginas detectadas: {}", maxPage);
                    return maxPage;
                }
            }
        }

        // Por defecto, asumir 1 página
        logger.warn("No se encontró información de paginación, asumiendo 1 página");
        return 1;
    }

    private int extractProductosPorPagina(Document doc) {
        // Contar productos en la página actual con el selector correcto
        Elements productos = doc.select("div.product-tile__item");
        int count = productos.size();
        logger.info("Productos por página detectados: {}", count);
        return count > 0 ? count : 36; // Default 36 si no se puede extraer
    }

    private String construirUrlPaginada(String urlCategoria, int numeroPagina) {
        String baseUrl = urlCategoria.replaceAll("\\?.*", "").replaceAll("/$", "");

        if (numeroPagina == 0) {
            return baseUrl + "/";
        }

        // ABC usa parámetro 'start' para la paginación
        // Página 1 (índice 0): start=0
        // Página 2 (índice 1): start=36
        // Página 3 (índice 2): start=72
        int start = numeroPagina * 36;
        return baseUrl + "/?srule=Categoria&start=" + start + "&sz=36";
    }

    private Producto extractProductoFromListItem(Element prodElement, String urlCategoria) {
        try {
            Producto producto = new Producto();

            // Extraer SKU del data-pid o del URL del link
            String sku = prodElement.attr("data-pid");
            if (sku.isEmpty()) {
                Element linkElement = prodElement.selectFirst("a.image-link, a.pdp-link a, div.pdp-link a");
                if (linkElement != null) {
                    String href = linkElement.attr("href");
                    Pattern pattern = Pattern.compile("/(\\d+)\\.html");
                    Matcher matcher = pattern.matcher(href);
                    if (matcher.find()) {
                        sku = matcher.group(1);
                    }
                }
            }

            if (sku.isEmpty()) {
                logger.warn("No se pudo extraer SKU");
                return null;
            }

            // Extraer nombre del elemento con clase pdp-link o del alt de la imagen
            Element nameElement = prodElement.selectFirst("div.pdp-link a");
            String nombre = "";
            if (nameElement != null) {
                nombre = nameElement.text().trim();
            }
            if (nombre.isEmpty()) {
                Element imgElement = prodElement.selectFirst("img.tile-image");
                if (imgElement != null) {
                    nombre = imgElement.attr("alt").trim();
                }
            }
            if (nombre.isEmpty()) {
                nombre = "Producto Sin Nombre";
            }

            // Extraer URL de la ficha
            Element linkElement = prodElement.selectFirst("a.image-link, div.pdp-link a");
            String urlFicha = "";
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (!href.isEmpty()) {
                    urlFicha = href.startsWith("http") ? href : "https://www.abc.cl" + href;
                }
            }

            // Extraer precio actual (internet price)
            Element precioElement = prodElement.selectFirst("p.internet span.price-value");
            BigDecimal precioActual = BigDecimal.ZERO;
            if (precioElement != null) {
                String precioText = precioElement.attr("data-value");
                if (precioText.isEmpty()) {
                    precioText = precioElement.text();
                }
                try {
                    precioActual = new BigDecimal(precioText.replaceAll("[^0-9.]", ""));
                } catch (Exception e) {
                    logger.debug("Error parseando precio: {}", precioText);
                }
            }

            // Extraer precio anterior (normal price)
            Element precioPrevElement = prodElement.selectFirst("p.normal span.price-value");
            BigDecimal precioAnterior = null;
            if (precioPrevElement != null) {
                try {
                    String precioText = precioPrevElement.attr("data-value");
                    if (precioText.isEmpty()) {
                        precioText = precioPrevElement.text();
                    }
                    precioText = precioText.replaceAll("[^0-9.]", "");
                    if (!precioText.isEmpty()) {
                        precioAnterior = new BigDecimal(precioText);
                    }
                } catch (Exception e) {
                    logger.debug("Error parseando precio anterior");
                }
            }

            // Extraer imagen (tile-image)
            Element imgElement = prodElement.selectFirst("img.tile-image");
            List<ImagenProducto> imagenes = new ArrayList<>();
            if (imgElement != null) {
                String imgUrl = imgElement.attr("src");
                if (imgUrl.isEmpty()) {
                    imgUrl = imgElement.attr("data-src");
                }
                if (!imgUrl.isEmpty()) {
                    ImagenProducto imagen = new ImagenProducto();
                    imagen.setUrlImagen(imgUrl);
                    imagen.setOrden(1);
                    imagenes.add(imagen);
                }
            }

            // Extraer disponibilidad (verificar si tiene botón de agregar a bolsa disponible)
            String disponibilidad = "en_stock";
            Element addButton = prodElement.selectFirst("a.quickview-button");
            if (addButton == null) {
                disponibilidad = "sin_stock";
            }

            producto.setSku(sku);
            producto.setNombre(nombre);
            producto.setPrecioActual(precioActual);
            producto.setPrecioAnterior(precioAnterior);
            producto.setUrlFicha(urlFicha);
            producto.setDisponibilidad(disponibilidad);
            producto.setImagenes(imagenes);

            imagenes.forEach(img -> img.setProducto(producto));

            logger.debug("Producto extraído - SKU: {}, Nombre: {}", sku, nombre);

            return producto;

        } catch (Exception e) {
            logger.error("Error al procesar item de producto: {}", e.getMessage());
            return null;
        }
    }
}