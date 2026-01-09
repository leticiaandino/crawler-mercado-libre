package com.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para recibir solicitudes de crawling
 * Formato JSON:
 * {
 *   "url": "https://www.example.com/producto"
 * }
 */
public class CrawlerRequest {
    @JsonProperty("url")
    private String url;

    // Constructor vacío para deserialización
    public CrawlerRequest() {
    }

    // Constructor con parámetro
    public CrawlerRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "CrawlerRequest{" +
                "url='" + url + '\'' +
                '}';
    }
}

