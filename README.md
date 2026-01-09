# Crawler E-Commerce - MercadoLibre, Paris.cl y ABC.cl

Un crawler profesional para extraer información de productos desde sitios de e-commerce usando Java con Spring Boot, con almacenamiento en MySQL.

## 📋 Descripción

Aplicación Spring Boot que permite realizar web scraping de productos en **MercadoLibre**, **Paris.cl** y **ABC.cl** para obtener información relevante como precios, descripción, imágenes y disponibilidad. Los datos se almacenan automáticamente en una base de datos relacional (MySQL).

## ✨ Características Principales

### ⚠️ ESTADO ACTUAL DEL SOPORTE

| Sitio | Producto Individual | Listado por Categoría |
|-------|:-----------------:|:-----------------:|
| **MercadoLibre** | ✅ `/api/crawler/extract` | ❌ No soportado |
| **Paris.cl** | ❌ No implementado | ✅ `/api/crawler/listado-productos` |
| **ABC.cl** | ✅ `/api/crawler/extract` | ✅ `/api/crawler/listado-productos` |

### 1. **Extracción de Ficha Individual de Producto**

#### Datos a extraer requeridos
- ✅ **SKU**: Código identificador del producto
- ✅ **Nombre**: Nombre completo del producto
- ✅ **Precios**: Precio actual (y precio anterior si existe)
- ✅ **Imágenes**: URLs de las imágenes del producto
- ✅ **Disponibilidad**: Estado de disponibilidad para compra

#### MercadoLibre ✅
- **URL de Prueba**: `https://www.mercadolibre.com.ar/sierra-circular-7-14-185-190mm-1600w-hs7010-makita/p/MLA19813486`
- **Status**: ✅ **FUNCIONA**
- **Endpoint**: `POST /api/crawler/extract`
- **Datos extraídos**: SKU, nombre, precio actual, precio anterior, disponibilidad, imágenes

#### ABC.cl ✅
- **URL de Prueba**: `https://www.abc.cl/notebook-gamer-asus-tuf-gaming-a15-fa506nc-hn002w%C2%A0amd-ryzen-5-8gb-512gb-ssd-156-nvidia-rtx-3050/28518471.html`
- **Status**: ✅ **FUNCIONA**
- **Endpoint**: `POST /api/crawler/extract`
- **Datos extraídos**: SKU, nombre, precio actual, precio anterior, disponibilidad, imágenes, fecha de creación, fecha de actualización

#### Paris.cl ❌
- **Status**: ❌ **NO IMPLEMENTADO**
- **Nota**: Paris.cl no tiene implementación para ficha individual

### 2. **Extracción de Listados por Categoría**

#### Datos a extraer requeridos
- ✅ **Categoría**: Nombre o ruta de la categoría
- ✅ **Cantidad de páginas**: Número de páginas totales que se muestran en la categoría
- ✅ **Cantidad de productos por página**: Número de productos por página
- ✅ **Listado de productos** con:
  - ✅ SKU: Código identificador del producto
  - ✅ Nombre: Nombre completo del producto
  - ✅ Precios: Precio actual (y precio anterior si existe)
  - ✅ Imágenes: URLs de las imágenes del producto
  - ✅ Disponibilidad: Estado de disponibilidad para compra
- ✅ **Paginación automática**: Para recorrer el total de productos de la categoría

#### Paris.cl ✅
- **URL de Prueba**: `https://www.paris.cl/tecnologia/celulares/smartphone/`
- **Status**: ✅ **FUNCIONA**
- **Endpoint**: `POST /api/crawler/listado-productos`
- **Características**:
  - Paginación automática
  - Extracción de todas las páginas
  - Almacenamiento automático en BD

#### ABC.cl ✅
- **URL de Prueba**: `https://www.abc.cl/mochila-hidratacion-national-geographic-yakima-12-lts-azul-hng1121/21673269.html`
- **Status**: ✅ **FUNCIONA**
- **Endpoint**: `POST /api/crawler/listado-productos`
- **Características**:
  - Paginación automática
  - Extracción de todas las páginas
  - Almacenamiento automático en BD

#### MercadoLibre ❌
- **Status**: ❌ **NO SOPORTADO**
- **Nota**: MercadoLibre no soporta crawling de categorías completas en este proyecto

## 🚀 Endpoints API

### ✅ Health Check
```
GET /api/crawler/health
```
Verifica que el servicio está activo.

**Response:**
```
Crawler service is running
```

### ✅ Extracción de Ficha Individual
```
POST /api/crawler/extract
Content-Type: text/plain

URL_DEL_PRODUCTO
```

**Ejemplos de uso:**

**1. Extraer ficha MercadoLibre (✅ FUNCIONA)**
```bash
curl -X POST http://localhost:8080/api/crawler/extract \
  -H "Content-Type: text/plain" \
  -d "https://www.mercadolibre.com.ar/sierra-circular-7-14-185-190mm-1600w-hs7010-makita/p/MLA19813486"
```

**2. Extraer ficha ABC.cl (✅ FUNCIONA)**
```bash
curl -X POST http://localhost:8080/api/crawler/extract \
  -H "Content-Type: text/plain" \
  -d "https://www.abc.cl/notebook-gamer-asus-tuf-gaming-a15-fa506nc-hn002w%C2%A0amd-ryzen-5-8gb-512gb-ssd-156-nvidia-rtx-3050/28518471.html"
```

### ✅ Extracción de Listado por Categoría
```
POST /api/crawler/listado-productos
Content-Type: text/plain

URL_DE_CATEGORIA
```

**Ejemplos de uso:**

**1. Extraer listado Paris.cl (✅ FUNCIONA)**
```bash
curl -X POST http://localhost:8080/api/crawler/listado-productos \
  -H "Content-Type: text/plain" \
  -d "https://www.paris.cl/tecnologia/celulares/smartphone/"
```

**2. Extraer listado ABC.cl (✅ FUNCIONA)**
```bash
curl -X POST http://localhost:8080/api/crawler/listado-productos \
  -H "Content-Type: text/plain" \
  -d "https://www.abc.cl/mochila-hidratacion-national-geographic-yakima-12-lts-azul-hng1121/21673269.html"
```

**Response (Ficha exitosa):**
```json
{
  "success": true,
  "message": "Producto extraído exitosamente",
  "data": {
    "sku": "28518471",
    "nombre": "Notebook Gamer Asus TUF Gaming A15 FA506NC-HN002W",
    "precioActual": 689990,
    "precioAnterior": 1599990,
    "disponibilidad": "en_stock",
    "imagenes": [
      {
        "url": "https://www.abc.cl/dw/image/v2/...",
        "orden": 1
      }
    ],
    "fechaCreacion": "2026-01-09T16:31:58.502753Z",
    "fechaActualizacion": "2026-01-09T16:31:58.502753Z"
  }
}
```

**Response (Listado exitoso):**
```json
{
  "success": true,
  "message": "Listado extraído exitosamente",
  "data": [
    {
      "sku": "26832039",
      "nombre": "Cinturón Hombre Ypsd",
      "precioActual": 4950.0,
      "precioAnterior": 9990.0,
      "disponibilidad": "en_stock",
      "imagenes": ["https://www.abc.cl/..."]
    }
  ]
}
```

### ⚠️ ENDPOINTS DEPRECADOS

**`POST /api/crawler/producto` (DEPRECADO)**
- Redirige a `/api/crawler/extract`
- Genera warning en logs
- Usar `/api/crawler/extract` en su lugar

**`POST /api/crawler/listado-productos` (DEPRECADO como multipropósito)**
- Usar `/api/crawler/listado-productos` para listados solamente

## 🏗️ Arquitectura

### Stack Tecnológico
- **Framework**: Spring Boot 3.5.9
- **Lenguaje**: Java 21
- **Base de Datos**: MySQL 8.0
- **ORM**: Hibernate/JPA
- **Web Scraping**: JSoup
- **HTTP Client**: RestTemplate (Spring)
- **Build**: Maven
- **Testing**: JUnit 5 + Mockito
- **Logging**: SLF4J + Logback
- **Utilidades**: Lombok

### Estructura del Proyecto

```
src/main/java/com/mercadolibre/
├── controller/
│   ├── CrawlerController.java      # Endpoints REST
│   └── CategoryController.java
├── service/
│   ├── CrawlerService.java         # Interfaz del servicio
│   └── imp/
│       └── CrawlerServiceImpl.java  # Implementación (orquestación)
├── crawler/
│   ├── Crawler.java                # Interfaz común
│   ├── MercadoLibreCrawler.java   # Crawler específico
│   ├── ParisCrawler.java          # Crawler + API client
│   └── AbcCrawler.java            # Crawler específico
├── client/
│   └── ParisApiClient.java        # Cliente HTTP para API Paris
├── mapper/
│   └── ProductMapper.java         # Mapeo DTO -> Entity
├── model/
│   ├── Producto.java
│   ├── Categoria.java
│   ├── ImagenProducto.java
│   └── ProductoCategoria.java
├── repository/
│   ├── ProductoRepository.java
│   ├── CategoriaRepository.java
│   └── ImagenProductoRepository.java
├── dto/
│   ├── ApiResponse.java           # Respuesta estandarizada
│   └── ParisProductsResponse.java # DTO de respuesta Paris API
├── exception/
│   └── GlobalExceptionHandler.java # Manejo centralizado de errores
├── util/
│   └── RateLimitHandler.java      # Control de rate limiting
└── config/
    ├── RestTemplateConfig.java
    └── ParisCategoryMappingConfig.java
```

### Patrón de Persistencia

Los productos se guardan automáticamente en MySQL con relaciones:

- **Tabla `producto`**: Datos principales del producto
- **Tabla `imagen_producto`**: URLs de imágenes con orden secuencial
- **Tabla `categoria`**: Metadata de categorías
- **Tabla `producto_categoria`**: Relación many-to-many

Los datos se actualizan (upsert) si ya existen, evitando duplicados.

## 🔧 Instalación y Configuración

### Requisitos Previos
- Java 21 o superior
- Maven 3.8+
- MySQL 8.0+
- Git

### Pasos de Instalación

1. **Clonar el repositorio**
```bash
git clone <tu-repositorio>
cd crawler-mercado-libre
```

2. **Configurar MySQL**

Crear la base de datos:
```sql
CREATE DATABASE scraping_ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Configurar credenciales en `application.properties`**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/scraping_ecommerce?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

4. **Instalar dependencias y compilar**
```bash
mvn clean install
```

5. **Ejecutar la aplicación**
```bash
mvn spring-boot:run
```

La aplicación estará disponible en: **http://localhost:8080**

## 📝 Ejemplos de Uso

### ✅ Con cURL

#### Ficha de Producto

**1. Extraer ficha MercadoLibre ✅**
```bash
curl -X POST http://localhost:8080/api/crawler/extract \
  -H "Content-Type: text/plain" \
  -d "https://www.mercadolibre.com.ar/sierra-circular-7-14-185-190mm-1600w-hs7010-makita/p/MLA19813486"
```

**2. Extraer ficha ABC.cl ✅**
```bash
curl -X POST http://localhost:8080/api/crawler/extract \
  -H "Content-Type: text/plain" \
  -d "https://www.abc.cl/notebook-gamer-asus-tuf-gaming-a15-fa506nc-hn002w%C2%A0amd-ryzen-5-8gb-512gb-ssd-156-nvidia-rtx-3050/28518471.html"
```

#### Listado de Productos

**3. Extraer listado Paris.cl ✅**
```bash
curl -X POST http://localhost:8080/api/crawler/listado-productos \
  -H "Content-Type: text/plain" \
  -d "https://www.paris.cl/tecnologia/celulares/smartphone/"
```

**4. Extraer listado ABC.cl ✅**
```bash
curl -X POST http://localhost:8080/api/crawler/listado-productos \
  -H "Content-Type: text/plain" \
  -d "https://www.abc.cl/mochila-hidratacion-national-geographic-yakima-12-lts-azul-hng1121/21673269.html"
```

**5. Health check**
```bash
curl http://localhost:8080/api/crawler/health
```

### ⚠️ Con Postman

Importar la siguiente colección JSON en Postman:

**Collection JSON** (copiar y pegar en Postman → Import → Raw text):

```json
{
  "info": {
    "name": "Crawler E-Commerce",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/crawler/health"
      }
    },
    {
      "name": "MercadoLibre - Ficha ✅",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "text/plain"}],
        "body": {"mode": "raw", "raw": "https://www.mercadolibre.com.ar/sierra-circular-7-14-185-190mm-1600w-hs7010-makita/p/MLA19813486"},
        "url": "http://localhost:8080/api/crawler/extract"
      }
    },
    {
      "name": "ABC - Ficha Notebook ✅",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "text/plain"}],
        "body": {"mode": "raw", "raw": "https://www.abc.cl/notebook-gamer-asus-tuf-gaming-a15-fa506nc-hn002w%C2%A0amd-ryzen-5-8gb-512gb-ssd-156-nvidia-rtx-3050/28518471.html"},
        "url": "http://localhost:8080/api/crawler/extract"
      }
    },
    {
      "name": "Paris - Listado Celulares ✅",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "text/plain"}],
        "body": {"mode": "raw", "raw": "https://www.paris.cl/tecnologia/celulares/smartphone/"},
        "url": "http://localhost:8080/api/crawler/listado-productos"
      }
    },
    {
      "name": "ABC - Listado Mochilas ✅",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "text/plain"}],
        "body": {"mode": "raw", "raw": "https://www.abc.cl/mochila-hidratacion-national-geographic-yakima-12-lts-azul-hng1121/21673269.html"},
        "url": "http://localhost:8080/api/crawler/listado-productos"
      }
    }
  ]
}
```

## 🧪 Testing

Ejecutar los tests unitarios:
```bash
mvn test
```

**Tests disponibles:**
- `ParisApiClientTest`: Valida llamadas a la API interna de Paris.cl
- `ParisCategoryServiceTest`: Valida lógica de importación y actualización
- `ProductMapperTest`: Valida mapeo de datos desde API a entidades
- `AbcCrawlerTest`: Valida web scraping de ABC.cl
- `MercadoLibreCrawlerTest`: Valida web scraping de MercadoLibre

## ⚙️ Configuración Avanzada

### Rate Limiting

En `application.properties`:
```properties
# Rate Limiting
rate-limit.enabled=true
rate-limit.mercadolibre.requests-per-minute=10
rate-limit.mercadolibre.delay-ms=6000
rate-limit.paris.requests-per-minute=5
rate-limit.paris.delay-ms=12000
rate-limit.abc.requests-per-minute=8
rate-limit.abc.delay-ms=7500
```

### User Agent

```properties
crawler.user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
crawler.timeout-seconds=30
crawler.max-retries=3
```

### Logging

```properties
logging.level.com.mercadolibre=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 🎯 Requerimientos Cumplidos

### ✅ Obligatorios
- ✅ Framework: Spring Boot 3.5.9
- ✅ Lenguaje: Java 21
- ✅ Base de datos relacional: MySQL 8.0
- ✅ Arquitectura OOP
- ✅ Principios SOLID aplicados
- ✅ Repositorio en GitHub

### ⭐ Opcionales
- ✅ Tests unitarios (JUnit 5 + Mockito)
- ✅ Manejo de excepciones (GlobalExceptionHandler)
- ✅ Logging adecuado (SLF4J + Logback)
- ✅ Rate limiting implementado
- ✅ README completo con instrucciones

## 📚 Estructura de Respuestas de Error

Todas las respuestas de error siguen el mismo formato:

```json
{
  "success": false,
  "error": "Descripción del error",
  "timestamp": 1767973395443
}
```

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT.

## 📞 Soporte

Para reportar bugs o sugerencias, abre un issue en el repositorio.

---

**Última actualización**: Enero 2026


