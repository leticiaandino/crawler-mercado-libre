# Crawler Mercado Libre

Un crawler para extraer información de productos de MercadoLibre y Paris.cl.

## Descripción

Este proyecto es una aplicación Spring Boot que permite realizar web scraping de productos en MercadoLibre y Paris.cl para obtener información relevante como precios, descripciones y características.

## Funcionalidades

### 1. Extracción de Ficha de Producto
- **Endpoint**: `POST /api/crawler/producto`
- **Sitios soportados**: MercadoLibre, Paris.cl
- **Datos extraídos**: SKU, nombre, precios, disponibilidad, imágenes
- **Ejemplo**:
```json
{
  "success": true,
  "data": {
    "sku": "MLA123456",
    "nombre": "Samsung Galaxy S23",
    "precioActual": 649990,
    "disponibilidad": "en_stock",
    "imagenes": [...]
  }
}
```

### 2. Extracción de Productos por Categoría (Paris.cl)
- **Endpoint**: `POST /api/crawler/categoria`
- **URL de prueba**: `https://www.paris.cl/tecnologia/celulares/smartphone/`

#### Implementación Técnica
La extracción de productos por categoría consume la **API interna de Paris.cl** (`https://be-paris-backend-cl-ms-api.ccom.paris.cl/products/`) en lugar de parsear HTML, garantizando:
- ✅ Estabilidad ante cambios en el frontend
- ✅ Datos estructurados y consistentes
- ✅ Paginación automática y eficiente

#### Características
- **Paginación automática**: Itera todas las páginas hasta extraer todos los productos
- **Detección de duplicados**: Usa HashSet para evitar procesar el mismo SKU múltiples veces
- **Upsert inteligente**: Actualiza productos existentes en BD en lugar de fallar por constraint de SKU único
- **Mapeo de group_id**: Convierte rutas de categoría a identificadores internos de la API
  - `tecnologia/celulares/smartphone` → `tecCelSmartphones`
  - `tecnologia/impresoras/rotuladores` → `tecImpRotuladores`

#### Datos extraídos
**Por categoría:**
- Ruta de categoría (ej: `tecnologia/celulares/smartphone`)
- Cantidad total de páginas
- Productos por página (30)
- Total de productos

**Por producto:**
- SKU (incluye variante, ej: `625026999`)
- Nombre completo (desde campo `name["es-CL"]` con fallback a `brand`)
- Precio actual (desde `prices.offer` o `prices.regular`)
- Precio anterior (si existe oferta)
- Imágenes (URLs con orden secuencial)
- Disponibilidad (`stock_disponible` / `agotado`)

#### Request
```json
{
  "url": "https://www.paris.cl/tecnologia/celulares/smartphone/"
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "categoria": "tecnologia/celulares/smartphone",
    "totalPaginas": 36,
    "productosPorPagina": 30,
    "message": "Extracción completada exitosamente"
  }
}
```

#### Persistencia
Los productos se guardan automáticamente en MySQL con las siguientes relaciones:
- `producto`: Datos principales del producto
- `imagen_producto`: URLs de imágenes con orden
- `categoria`: Metadata de la categoría procesada
- `producto_categoria`: Relación many-to-many entre productos y categorías

## Tecnologías

- Java 21
- Spring Boot 3.5.9
- Maven
- JSoup (Web Scraping)
- RestTemplate (API Client)
- MySQL (Base de datos)
- JPA/Hibernate
- Lombok
- JUnit 5 + Mockito (Testing)

## Instalación

1. Clona el repositorio
2. Configura MySQL en `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/crawler_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
```
3. Ejecuta `mvn clean install`
4. Ejecuta `mvn spring-boot:run`

## Testing

Ejecuta los tests unitarios:
```bash
mvn test
```

Tests disponibles:
- `ParisApiClientTest`: Valida llamadas a la API de Paris.cl
- `ParisCategoryServiceTest`: Valida lógica de importación y actualización
- `ProductMapperTest`: Valida mapeo de datos desde API a entidades

## Uso

La aplicación se ejecuta en el puerto 8080.

### Endpoints disponibles:
- `GET /api/crawler/health` - Health check
- `POST /api/crawler/producto` - Extraer ficha de producto
- `POST /api/crawler/categoria` - Extraer productos por categoría (Paris.cl)

## Contribución

1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request