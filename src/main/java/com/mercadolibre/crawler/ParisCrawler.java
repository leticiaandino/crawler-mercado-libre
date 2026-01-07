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

@Component
public class ParisCrawler implements Crawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public Producto crawlProducto(String url) {
        // Implementar lógica para Paris.cl
        throw new UnsupportedOperationException("ParisCrawler no implementado aún");
    }

    @Override
    public Categoria crawlMetadataCategoria(String urlCategoria) {
        // Implementar lógica para Paris.cl
        throw new UnsupportedOperationException("ParisCrawler no implementado aún");
    }

    @Override
    public List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas) {
        // Implementar lógica para Paris.cl
        throw new UnsupportedOperationException("ParisCrawler no implementado aún");
    }
}