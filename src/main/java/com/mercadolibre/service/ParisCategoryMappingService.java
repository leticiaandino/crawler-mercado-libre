package com.mercadolibre.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar mapeos de categorías de Paris
 * Lee desde la base de datos en lugar de hardcodeado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParisCategoryMappingService {

    private final ParisCategoryMappingRepository repository;

    /**
     * Obtiene el groupId para una categoryPath
     * Primero busca en BD, si no encuentra, usa mapeo dinámico
     */
    public String resolveGroupId(String categoryPath) {
        try {
            // Intentar obtener de la base de datos
            Optional<ParisCategoryMapping> mapping = repository.findByCategoryPath(categoryPath);

            if (mapping.isPresent() && mapping.get().getActive()) {
                String groupId = mapping.get().getGroupId();
                log.debug("Found mapping in DB for '{}': {}", categoryPath, groupId);
                return groupId;
            }

            // Si no existe, crear mapeo dinámico
            log.warn("No mapping found in DB for '{}'. Using dynamic mapping.", categoryPath);
            return createDynamicGroupId(categoryPath);

        } catch (Exception e) {
            log.error("Error resolving groupId for '{}': {}", categoryPath, e.getMessage(), e);
            return createDynamicGroupId(categoryPath);
        }
    }

    /**
     * Crea un groupId dinámico si no existe en BD
     */
    private String createDynamicGroupId(String categoryPath) {
        try {
            String[] parts = categoryPath.split("/");
            StringBuilder groupId = new StringBuilder();

            for (String part : parts) {
                if (part.isEmpty()) continue;

                String[] subParts = part.split("-");
                for (String subPart : subParts) {
                    if (!subPart.isEmpty()) {
                        groupId.append(subPart.substring(0, 1).toUpperCase())
                               .append(subPart.substring(1).toLowerCase());
                    }
                }
            }

            String result = groupId.toString();
            log.info("Created dynamic groupId for '{}': {}", categoryPath, result);
            return result;

        } catch (Exception e) {
            log.error("Error creating dynamic groupId for '{}': {}", categoryPath, e.getMessage());
            return categoryPath.replaceAll("/", "");
        }
    }

    /**
     * Guarda o actualiza un mapeo de categoría
     */
    @Transactional
    public ParisCategoryMapping saveMapping(String categoryPath, String groupId, String description) {
        try {
            Optional<ParisCategoryMapping> existing = repository.findByCategoryPath(categoryPath);

            ParisCategoryMapping mapping;
            if (existing.isPresent()) {
                mapping = existing.get();
                log.info("Updating existing mapping for '{}': {}", categoryPath, groupId);
            } else {
                mapping = new ParisCategoryMapping();
                log.info("Creating new mapping for '{}': {}", categoryPath, groupId);
            }

            mapping.setCategoryPath(categoryPath);
            mapping.setGroupId(groupId);
            mapping.setDescription(description);
            mapping.setActive(true);

            return repository.save(mapping);

        } catch (Exception e) {
            log.error("Error saving mapping for '{}': {}", categoryPath, e.getMessage(), e);
            throw new RuntimeException("Failed to save category mapping: " + e.getMessage(), e);
        }
    }

    /**
     * Desactiva un mapeo
     */
    @Transactional
    public void deactivateMapping(String categoryPath) {
        try {
            Optional<ParisCategoryMapping> mapping = repository.findByCategoryPath(categoryPath);
            if (mapping.isPresent()) {
                mapping.get().setActive(false);
                repository.save(mapping.get());
                log.info("Mapping deactivated for '{}'", categoryPath);
            }
        } catch (Exception e) {
            log.error("Error deactivating mapping for '{}': {}", categoryPath, e.getMessage());
        }
    }

    /**
     * Obtiene todos los mapeos activos
     */
    public List<ParisCategoryMapping> getAllActiveMappings() {
        return repository.findAllActive();
    }

    /**
     * Busca mapeos por patrón
     */
    public List<ParisCategoryMapping> searchMappings(String pattern) {
        return repository.findByPattern(pattern);
    }

    /**
     * Verifica si existe un mapeo
     */
    public boolean mappingExists(String categoryPath) {
        return repository.existsByCategoryPath(categoryPath);
    }

    /**
     * Inicializa mapeos por defecto en BD si no existen
     * Se ejecuta una sola vez al iniciar la aplicación
     */
    @Transactional
    public void initializeDefaultMappings() {
        log.info("Initializing default Paris category mappings...");

        if (repository.count() > 0) {
            log.debug("Mappings already exist. Skipping initialization.");
            return;
        }

        String[][] defaultMappings = {
            // Tecnología - Celulares
            {"tecnologia/celulares/smartphone", "tecCelSmartphones", "Smartphones"},
            {"tecnologia/celulares/accesorios", "tecCelAccesorios", "Accesorios Celulares"},

            // Tecnología - Computación
            {"tecnologia/computacion/notebooks", "tecComNotebooks", "Notebooks"},
            {"tecnologia/computacion/desktops", "tecComDesktops", "Desktops"},
            {"tecnologia/computacion/tablets", "tecComTablets", "Tablets"},

            // Tecnología - Impresoras
            {"tecnologia/impresoras/rotuladores", "tecImpRotuladores", "Impresoras Rotuladores"},
            {"tecnologia/impresoras/tinta", "tecImpTinta", "Tinta para Impresoras"},

            // Electrohogar
            {"electrohogar/refrigeracion/refrigeradores", "eleRefRefrigeradores", "Refrigeradores"},
            {"electrohogar/refrigeracion/congeladores", "eleRefCongeladores", "Congeladores"},
            {"electrohogar/cocina/estufas", "eleCoEstufas", "Estufas"},
            {"electrohogar/lavado/lavadoras", "eleLavLavadoras", "Lavadoras"},

            // Moda - Ropa Interior
            {"mujer/ropa-interior/pantuflas", "mujIntPantuflas", "Pantuflas Mujer"},
            {"mujer/ropa-interior/sostenes", "mujIntSostenes", "Sostenes"},
            {"mujer/ropa-interior/calzones", "mujIntCalzones", "Calzones"},

            // Moda - Ropa
            {"mujer/ropa/blusas", "mujRopBlusas", "Blusas Mujer"},
            {"mujer/ropa/jeans", "mujRopJeans", "Jeans Mujer"},
            {"mujer/ropa/vestidos", "mujRopVestidos", "Vestidos Mujer"},
            {"hombre/ropa/camisas", "homRopCamisas", "Camisas Hombre"},
            {"hombre/ropa/pantalones", "homRopPantalones", "Pantalones Hombre"},

            // Moda - Calzado
            {"mujer/calzado/zapatos", "mujCalZapatos", "Zapatos Mujer"},
            {"mujer/calzado/zapatillas", "mujCalZapatillas", "Zapatillas Mujer"},
            {"hombre/calzado/zapatos", "homCalZapatos", "Zapatos Hombre"},
            {"hombre/calzado/zapatillas", "homCalZapatillas", "Zapatillas Hombre"},

            // Deportes
            {"deportes/ropa-deportiva/mujer", "depRopMujer", "Ropa Deportiva Mujer"},
            {"deportes/ropa-deportiva/hombre", "depRopHombre", "Ropa Deportiva Hombre"},
            {"deportes/accesorios/mochilas", "depAccMochilas", "Mochilas Deportivas"},

            // Hogar
            {"hogar/dormitorio/sabanas", "hogDorSabanas", "Sábanas"},
            {"hogar/dormitorio/almohadas", "hogDorAlmohadas", "Almohadas"},
            {"hogar/cocina/utensilios", "hogCocUtensilios", "Utensilios de Cocina"},
            {"hogar/decoracion/cuadros", "hogDecCuadros", "Cuadros Decorativos"}
        };

        for (String[] mapping : defaultMappings) {
            try {
                ParisCategoryMapping categoryMapping = new ParisCategoryMapping();
                categoryMapping.setCategoryPath(mapping[0]);
                categoryMapping.setGroupId(mapping[1]);
                categoryMapping.setDescription(mapping[2]);
                categoryMapping.setActive(true);
                repository.save(categoryMapping);
                log.debug("Initialized mapping: {} → {}", mapping[0], mapping[1]);
            } catch (Exception e) {
                log.error("Error initializing mapping for '{}': {}", mapping[0], e.getMessage());
            }
        }

        log.info("Default mappings initialization completed. Total: {} mappings", defaultMappings.length);
    }
}

