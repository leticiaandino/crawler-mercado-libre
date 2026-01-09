package com.mercadolibre.client;

import com.mercadolibre.dto.ParisProductsResponse;
import com.mercadolibre.service.ParisCategoryMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParisApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ParisCategoryMappingService categoryMappingService;

    @InjectMocks
    private ParisApiClient parisApiClient;

    private ParisProductsResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new ParisProductsResponse();
        mockResponse.setTotal(100);
        mockResponse.setCount(30);
        mockResponse.setLimit(30);
        mockResponse.setOffset(0);

        // Mock del servicio de mapeo de categorías
        when(categoryMappingService.resolveGroupId(anyString()))
            .thenReturn("tecCelSmartphones");
    }

    @Test
    void fetchCategory_Success() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ParisProductsResponse.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        ParisProductsResponse result = parisApiClient.fetchCategory("tecnologia/celulares/smartphone", 1);

        assertNotNull(result);
        assertEquals(100, result.getTotal());
        assertEquals(30, result.getCount());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(ParisProductsResponse.class));
    }

    @Test
    void fetchCategory_ThrowsException_WhenResponseIsNull() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ParisProductsResponse.class)))
            .thenReturn(ResponseEntity.ok(null));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            parisApiClient.fetchCategory("tecnologia/celulares/smartphone", 1)
        );
        
        assertTrue(exception.getMessage().contains("Empty response from Paris API"));
    }
}
