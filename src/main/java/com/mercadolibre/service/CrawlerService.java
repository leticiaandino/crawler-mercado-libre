package com.mercadolibre.service;

import com.mercadolibre.model.Categoria;
import com.mercadolibre.model.Producto;

import java.util.List;

public interface CrawlerService {
    Producto extraerFichaProducto(String url);
    List<Producto> extraerListadoProductos(String urlCategoria);
    Categoria obtenerMetadataCategoria(String urlCategoria);
}