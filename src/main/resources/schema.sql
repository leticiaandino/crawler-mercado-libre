-- =====================================================
-- Schema SQL - Ejecutado automáticamente por Spring Boot
-- Archivo: src/main/resources/schema.sql
-- Nombre de la base de datos: scraping_ecommerce
-- =====================================================

-- Tabla de Categorías
CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ruta VARCHAR(500) NOT NULL UNIQUE,
    url_categoria TEXT NOT NULL,
    cantidad_paginas INT NOT NULL,
    productos_por_pagina INT NOT NULL,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de Productos
CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(500) NOT NULL,
    precio_actual DECIMAL(12, 2) NOT NULL,
    precio_anterior DECIMAL(12, 2) NULL,
    disponibilidad VARCHAR(50) NULL,
    url_ficha TEXT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de Imágenes de Productos
CREATE TABLE IF NOT EXISTS imagen_producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    url_imagen TEXT NOT NULL,
    orden INT,
    INDEX idx_producto_id (producto_id),
    CONSTRAINT fk_imagen_producto_producto
        FOREIGN KEY (producto_id)
        REFERENCES producto(id)
        ON DELETE CASCADE
);

-- Tabla de Relación Producto-Categoría
CREATE TABLE IF NOT EXISTS producto_categoria (
    producto_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    PRIMARY KEY (producto_id, categoria_id),
    CONSTRAINT fk_pc_producto
        FOREIGN KEY (producto_id)
        REFERENCES producto(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pc_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES categoria(id)
        ON DELETE CASCADE
);

-- =====================================================
-- Índices adicionales para optimizar queries
-- =====================================================

-- Índices en tabla producto
CREATE INDEX IF NOT EXISTS idx_producto_sku ON producto(sku);
CREATE INDEX IF NOT EXISTS idx_producto_nombre ON producto(nombre(100));
CREATE INDEX IF NOT EXISTS idx_producto_disponibilidad ON producto(disponibilidad);
CREATE INDEX IF NOT EXISTS idx_producto_fecha_creacion ON producto(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_producto_fecha_actualizacion ON producto(fecha_actualizacion);

-- Índices en tabla categoria
CREATE INDEX IF NOT EXISTS idx_categoria_ruta ON categoria(ruta(100));
CREATE INDEX IF NOT EXISTS idx_categoria_fecha_actualizacion ON categoria(fecha_actualizacion);

-- Índices en tabla imagen_producto
CREATE INDEX IF NOT EXISTS idx_imagen_orden ON imagen_producto(producto_id, orden);
