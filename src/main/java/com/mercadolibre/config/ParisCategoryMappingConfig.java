package com.mercadolibre.config;

import com.mercadolibre.service.ParisCategoryMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para inicializar mapeos de categorías de Paris
 * Se ejecuta automáticamente al iniciar la aplicación
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ParisCategoryMappingConfig {

    private final ParisCategoryMappingService mappingService;

    /**
     * Inicializa los mapeos de categorías por defecto en BD
     */
    @Bean
    public ApplicationRunner initializeParisMappings() {
        return args -> {
            log.info("Initializing Paris category mappings from database...");
            try {
                mappingService.initializeDefaultMappings();
                log.info("Paris category mappings initialized successfully");
            } catch (Exception e) {
                log.error("Error initializing Paris category mappings: {}", e.getMessage(), e);
            }
        };
    }
}

