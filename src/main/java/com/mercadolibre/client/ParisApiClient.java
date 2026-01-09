package com.mercadolibre.client;

import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.service.ParisCategoryMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para la API interna de Paris.cl
 *
 * IMPORTANTE: Los mapeos de categorías se leen desde la BD (ParisCategoryMapping)
 * No están hardcodeados en el código por seguridad y escalabilidad
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParisApiClient {

    private final RestTemplate restTemplate;
    private final ParisCategoryMappingService categoryMappingService;

    private static final String API_URL = "https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/";
    private static final String APPLICATION_ID = "34bb8686968a85a272a6c546ddcb9860db1ea14ee72f5207ef0c028280a6e7bc";

    /**
     * Obtiene una página de productos de una categoría específica
     *
     * @param categoryPath Ruta de la categoría (ej: "tecnologia/celulares/smartphone")
     * @param page Número de página (1-based)
     * @return Respuesta con productos
     */
    public ParisProductsResponse fetchCategory(String categoryPath, int page) {
        try {
            // Resolver groupId desde la BD o mapeo dinámico
            String groupId = categoryMappingService.resolveGroupId(categoryPath);
            log.info("Fetching Paris API - categoryPath: {}, groupId: {}, page: {}",
                categoryPath, groupId, page);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Origin", "https://www.paris.cl");
            headers.set("Referer", "https://www.paris.cl/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

            Map<String, Object> body = Map.of(
                    "applicationId", APPLICATION_ID,
                    "filters", List.of(
                            Map.of(
                                    "key", "group_id",
                                    "stringValues", List.of(groupId)
                            )
                    ),
                    "pagination", Map.of(
                            "page", page,
                            "pageSize", 30
                    ),
                    "sortBy", "relevance",
                    "sponsoredProducts", true,
                    "term", ""
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            log.debug("Request body: {}", body);

            ResponseEntity<ParisProductsResponse> response = restTemplate.postForEntity(
                    API_URL,
                    request,
                    ParisProductsResponse.class
            );

            if (response.getBody() == null) {
                log.error("Respuesta nula de la API de Paris");
                throw new IllegalStateException("Empty response from Paris API");
            }

            ParisProductsResponse responseBody = response.getBody();
            log.info("Paris API Response - total: {}, count: {}, limit: {}, page: {}",
                    responseBody.getTotal(),
                    responseBody.getCount(),
                    responseBody.getLimit(),
                    page);

            return responseBody;

        } catch (Exception e) {
            log.error("Error fetching Paris API (page={}): {}", page, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch category data from Paris API: " + e.getMessage(), e);
        }
    }
}


