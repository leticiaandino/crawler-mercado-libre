package com.mercadolibre.crawler;

import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.Producto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MercadoLibreCrawler implements Crawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public Producto crawlProducto(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();

            // SKU: Ej: MLA19813486
            String sku = extractSkuFromUrl(url);

            // Nombre
            String nombre = doc.select("h1.ui-pdp-title").text();

            // Precios
            String precioActualStr = doc.select("span.andes-money-amount__fraction").first().text();
            BigDecimal precioActual = new BigDecimal(precioActualStr.replace(".", "").replace(",", "."));

            String precioAnteriorStr = doc.select("span.andes-money-amount__previous-price").text();
            BigDecimal precioAnterior = null;
            if (!precioAnteriorStr.isEmpty()) {
                precioAnterior = new BigDecimal(precioAnteriorStr.replace(".", "").replace(",", "."));
            }

            // Disponibilidad
            String disponibilidad = "en_stock";
            if (doc.select("span.ui-pdp-buybox__quantity-label").isEmpty()) {
                disponibilidad = "agotado";
            }

            Producto producto = new Producto();
            producto.setSku(sku);
            producto.setNombre(nombre);
            producto.setPrecioActual(precioActual);
            producto.setPrecioAnterior(precioAnterior);
            producto.setDisponibilidad(disponibilidad);
            producto.setUrlFicha(url);

            return producto;

        } catch (IOException e) {
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
        Pattern pattern = Pattern.compile("/p/(\\w+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1); // Ej: MLA19813486
        }
        return "UNKNOWN_SKU";
    }
}