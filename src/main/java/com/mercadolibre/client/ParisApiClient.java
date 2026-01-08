package com.mercadolibre.client;

import com.mercadolibre.dto.ParisProductsResponse;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ParisApiClient {

    private final RestTemplate restTemplate;
    private static final String API_URL = "https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/";

    public ParisProductsResponse fetchCategory(String categoryPath, int page) {
        String groupId = resolveGroupId(categoryPath);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Origin", "https://www.paris.cl");
        headers.set("Referer", "https://www.paris.cl/");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        Map<String, Object> body = Map.of(
                "applicationId", "34bb8686968a85a272a6c546ddcb9860db1ea14ee72f5207ef0c028280a6e7bc",
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
        log.info("Fetching Paris API - groupId: {}, page: {}", groupId, page);

        try {
            ResponseEntity<ParisProductsResponse> response = restTemplate.postForEntity(API_URL, request, ParisProductsResponse.class);
            
            if (response.getBody() == null) {
                throw new IllegalStateException("Empty response from Paris API");
            }
            
            log.info("Response - total: {}, count: {}, limit: {}", 
                response.getBody().getTotal(),
                response.getBody().getCount(),
                response.getBody().getLimit());
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching Paris API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch category data: " + e.getMessage(), e);
        }
    }

    private String resolveGroupId(String categoryPath) {
        Map<String, String> categoryMap = Map.of(
            "tecnologia/celulares/smartphone", "tecCelSmartphones",
            "tecnologia/impresoras/rotuladores", "tecImpRotuladores",
            "tecnologia/computacion/notebooks", "tecComNotebooks",
            "electrohogar/refrigeracion/refrigeradores", "eleRefRefrigeradores"
        );
        
        String groupId = categoryMap.get(categoryPath);
        if (groupId == null) {
            log.warn("Unknown category path: {}, using default mapping", categoryPath);
            groupId = categoryPath.replaceAll("/", "");
        }
        
        return groupId;
    }
}
