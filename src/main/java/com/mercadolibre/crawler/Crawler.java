package com.mercadolibre.crawler;

public class Crawler {
    // Extrae un solo producto desde su URL
    Producto crawlProducto(String url);

    // Extrae listado de productos desde URL de categoría (con paginación)
    List<Producto> crawlListadoProductos(String urlCategoria, int paginaActual, int totalPaginas);

    // Extrae metadata de categoría (ruta, paginas totales, productos por página)
    Categoria crawlMetadataCategoria(String urlCategoria);
}
