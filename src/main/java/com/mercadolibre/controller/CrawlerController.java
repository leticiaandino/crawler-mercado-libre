package com.mercadolibre.controller;

import com.mercadolibre.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    @Autowired
    private CrawlerService crawlerService;

    @GetMapping("/health")
    public String health() {
        return "Crawler service is running";
    }

    @PostMapping("/producto")
    public String crawlProducto(@RequestParam String url) {
        try {
            crawlerService.extraerFichaProducto(url);
            return "Producto extraído exitosamente";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/categoria")
    public String crawlCategoria(@RequestParam String url) {
        try {
            crawlerService.extraerListadoProductos(url);
            return "Categoría procesada exitosamente";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}