package com.mercadolibre.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manejador de Rate Limiting para respetar los límites de solicitudes
 * y evitar bloqueos de IP por parte de los sitios web.
 *
 * Implementa control de velocidad por dominio y throttling automático.
 */
@Component
public class RateLimitHandler {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitHandler.class);

    // Configuración de rate limiting por dominio
    private static final Map<String, Integer> REQUESTS_PER_MINUTE = new HashMap<>();
    private static final Map<String, Integer> REQUEST_DELAY_MS = new HashMap<>();

    static {
        // Configuración de límites por sitio
        REQUESTS_PER_MINUTE.put("mercadolibre.com", 10); // 10 requests por minuto
        REQUESTS_PER_MINUTE.put("paris.cl", 5);          // 5 requests por minuto
        REQUESTS_PER_MINUTE.put("abc.cl", 8);            // 8 requests por minuto

        REQUEST_DELAY_MS.put("mercadolibre.com", 6000);  // 6 segundos de espera
        REQUEST_DELAY_MS.put("paris.cl", 12000);         // 12 segundos de espera
        REQUEST_DELAY_MS.put("abc.cl", 7500);            // 7.5 segundos de espera
    }

    // Almacén de timestamps de últimas solicitudes por dominio
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> requestCounter = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    /**
     * Obtiene el dominio de una URL
     */
    private String extractDomain(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            // Elimina "www." si existe
            return host.replace("www.", "");
        } catch (Exception e) {
            logger.error("Error extrayendo dominio de URL: {}", url, e);
            return "unknown";
        }
    }

    /**
     * Espera según el rate limit del sitio
     * Implementa throttling automático basado en la velocidad de solicitud
     */
    public void waitBeforeRequest(String url) {
        String domain = extractDomain(url);
        long currentTime = System.currentTimeMillis();

        // Obtener tiempo de último request
        long lastRequest = lastRequestTime.getOrDefault(domain, 0L);
        long timeSinceLastRequest = currentTime - lastRequest;

        // Obtener delay configurado para este dominio
        int delayMs = REQUEST_DELAY_MS.getOrDefault(domain, 5000);

        // Si no ha pasado suficiente tiempo, esperar
        if (timeSinceLastRequest < delayMs) {
            long waitTime = delayMs - timeSinceLastRequest;
            logger.debug("Rate limiting: Esperando {} ms antes de conectar a {}",
                waitTime, domain);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.warn("Interrupción durante rate limiting", e);
                Thread.currentThread().interrupt();
            }
        }

        // Actualizar tiempo de última solicitud
        lastRequestTime.put(domain, System.currentTimeMillis());

        // Incrementar contador de requests
        incrementRequestCount(domain);
    }

    /**
     * Incrementa el contador de requests y valida límites
     */
    private void incrementRequestCount(String domain) {
        long currentTime = System.currentTimeMillis();
        long window = windowStart.getOrDefault(domain, currentTime);

        // Si pasó más de un minuto, reiniciar contador
        if (currentTime - window > 60000) {
            requestCounter.put(domain, new AtomicInteger(1));
            windowStart.put(domain, currentTime);
            return;
        }

        // Incrementar contador
        AtomicInteger counter = requestCounter.computeIfAbsent(domain, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        // Obtener límite para este dominio
        int limit = REQUESTS_PER_MINUTE.getOrDefault(domain, 10);

        // Loguear si se está acercando al límite
        if (count > limit) {
            logger.warn("ADVERTENCIA: Se ha excedido el límite de {} requests/minuto para {}. "
                + "Actual: {}", limit, domain, count);
        } else if (count > (limit * 0.8)) {
            logger.info("ALERTA: Acercándose al límite de rate limiting para {} ({}/{})",
                domain, count, limit);
        }
    }

    /**
     * Valida que la URL sea válida
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            logger.warn("URL vacía o nula");
            return false;
        }

        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            logger.warn("URL inválida: {}", url);
            return false;
        }
    }

    /**
     * Obtiene estadísticas de rate limiting
     */
    public Map<String, Object> getStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("domain", domain);
        stats.put("lastRequestTime", lastRequestTime.get(domain));
        stats.put("requestCount", requestCounter.get(domain) != null ?
            requestCounter.get(domain).get() : 0);
        stats.put("requestLimit", REQUESTS_PER_MINUTE.getOrDefault(domain, 10));
        stats.put("delayMs", REQUEST_DELAY_MS.getOrDefault(domain, 5000));
        return stats;
    }

    /**
     * Reinicia estadísticas de rate limiting
     */
    public void resetStatistics(String domain) {
        lastRequestTime.remove(domain);
        requestCounter.remove(domain);
        windowStart.remove(domain);
        logger.info("Estadísticas de rate limiting reiniciadas para {}", domain);
    }
}

