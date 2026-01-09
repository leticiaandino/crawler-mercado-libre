-- Tabla para almacenar mapeos de categorías de Paris
-- Se ejecuta automáticamente con Hibernate si usa ddl-auto=update

CREATE TABLE IF NOT EXISTS paris_category_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_path VARCHAR(255) NOT NULL UNIQUE COMMENT 'Ruta de categoría en Paris (ej: mujer/ropa-interior/pantuflas)',
    group_id VARCHAR(100) NOT NULL COMMENT 'GroupId en API de Paris (ej: mujIntPantuflas)',
    description VARCHAR(500) COMMENT 'Descripción de la categoría',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Si está activo para extracción',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    INDEX idx_category_path (category_path),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar mapeos por defecto
INSERT IGNORE INTO paris_category_mapping (category_path, group_id, description, active, created_at, updated_at) VALUES

-- Tecnología - Celulares
('tecnologia/celulares/smartphone', 'tecCelSmartphones', 'Smartphones', TRUE, NOW(), NOW()),
('tecnologia/celulares/accesorios', 'tecCelAccesorios', 'Accesorios Celulares', TRUE, NOW(), NOW()),

-- Tecnología - Computación
('tecnologia/computacion/notebooks', 'tecComNotebooks', 'Notebooks', TRUE, NOW(), NOW()),
('tecnologia/computacion/desktops', 'tecComDesktops', 'Desktops', TRUE, NOW(), NOW()),
('tecnologia/computacion/tablets', 'tecComTablets', 'Tablets', TRUE, NOW(), NOW()),

-- Tecnología - Impresoras
('tecnologia/impresoras/rotuladores', 'tecImpRotuladores', 'Impresoras Rotuladores', TRUE, NOW(), NOW()),
('tecnologia/impresoras/tinta', 'tecImpTinta', 'Tinta para Impresoras', TRUE, NOW(), NOW()),

-- Electrohogar
('electrohogar/refrigeracion/refrigeradores', 'eleRefRefrigeradores', 'Refrigeradores', TRUE, NOW(), NOW()),
('electrohogar/refrigeracion/congeladores', 'eleRefCongeladores', 'Congeladores', TRUE, NOW(), NOW()),
('electrohogar/cocina/estufas', 'eleCoEstufas', 'Estufas', TRUE, NOW(), NOW()),
('electrohogar/lavado/lavadoras', 'eleLavLavadoras', 'Lavadoras', TRUE, NOW(), NOW()),

-- Moda - Ropa Interior
('mujer/ropa-interior/pantuflas', 'mujIntPantuflas', 'Pantuflas Mujer', TRUE, NOW(), NOW()),
('mujer/ropa-interior/sostenes', 'mujIntSostenes', 'Sostenes', TRUE, NOW(), NOW()),
('mujer/ropa-interior/calzones', 'mujIntCalzones', 'Calzones', TRUE, NOW(), NOW()),

-- Moda - Ropa
('mujer/ropa/blusas', 'mujRopBlusas', 'Blusas Mujer', TRUE, NOW(), NOW()),
('mujer/ropa/jeans', 'mujRopJeans', 'Jeans Mujer', TRUE, NOW(), NOW()),
('mujer/ropa/vestidos', 'mujRopVestidos', 'Vestidos Mujer', TRUE, NOW(), NOW()),
('hombre/ropa/camisas', 'homRopCamisas', 'Camisas Hombre', TRUE, NOW(), NOW()),
('hombre/ropa/pantalones', 'homRopPantalones', 'Pantalones Hombre', TRUE, NOW(), NOW()),

-- Moda - Calzado
('mujer/calzado/zapatos', 'mujCalZapatos', 'Zapatos Mujer', TRUE, NOW(), NOW()),
('mujer/calzado/zapatillas', 'mujCalZapatillas', 'Zapatillas Mujer', TRUE, NOW(), NOW()),
('hombre/calzado/zapatos', 'homCalZapatos', 'Zapatos Hombre', TRUE, NOW(), NOW()),
('hombre/calzado/zapatillas', 'homCalZapatillas', 'Zapatillas Hombre', TRUE, NOW(), NOW()),

-- Deportes
('deportes/ropa-deportiva/mujer', 'depRopMujer', 'Ropa Deportiva Mujer', TRUE, NOW(), NOW()),
('deportes/ropa-deportiva/hombre', 'depRopHombre', 'Ropa Deportiva Hombre', TRUE, NOW(), NOW()),
('deportes/accesorios/mochilas', 'depAccMochilas', 'Mochilas Deportivas', TRUE, NOW(), NOW()),

-- Hogar
('hogar/dormitorio/sabanas', 'hogDorSabanas', 'Sábanas', TRUE, NOW(), NOW()),
('hogar/dormitorio/almohadas', 'hogDorAlmohadas', 'Almohadas', TRUE, NOW(), NOW()),
('hogar/cocina/utensilios', 'hogCocUtensilios', 'Utensilios de Cocina', TRUE, NOW(), NOW()),
('hogar/decoracion/cuadros', 'hogDecCuadros', 'Cuadros Decorativos', TRUE, NOW(), NOW());

