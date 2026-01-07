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

## Tecnologías

- Java 17
- Spring Boot 3.x
- Maven
- JSoup (Web Scraping)
- MySQL (Base de datos)
- JPA/Hibernate

## Instalación

1. Clona el repositorio
2. Configura MySQL en `application.properties`
3. Ejecuta `mvn clean install`
4. Ejecuta `mvn spring-boot:run`

## Uso

La aplicación se ejecuta en el puerto 8080.

### Endpoints disponibles:
- `GET /api/crawler/health` - Health check
- `POST /api/crawler/producto` - Extraer ficha de producto

## Contribución

1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request