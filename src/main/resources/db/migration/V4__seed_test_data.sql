-- Seed data: ~70 garments + ~90 week_plans for visual testing
-- Simulates a real wardrobe with varied outfits across 3 weeks

DELETE FROM week_plans;
DELETE FROM garments;

ALTER SEQUENCE garments_id_seq RESTART WITH 1;
ALTER SEQUENCE week_plans_id_seq RESTART WITH 1;

-- ============================================================
-- GARMENTS (70)
-- ============================================================

-- TOPS (1-14)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(1,  'Remera Blanca',           'Top',       'Blanco',       '#FFFFFF', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/FFFFFF/000000?text=Remera+Blanca', true, true, 'Top', 'Blanco', '#FFFFFF', 0.95, 'qwen3.6', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(2,  'Remera Negra',            'Top',       'Negro',        '#1A1A1A', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Remera+Negra', true, false, 'Top', 'Negro', '#1A1A1A', 0.97, 'qwen3.6', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(3,  'Remera Gris',             'Top',       'Gris',         '#808080', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/808080/FFFFFF?text=Remera+Gris', true, true, 'Top', 'Gris', '#808080', 0.94, 'qwen3.6', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(4,  'Remera Azul Marino',      'Top',       'Azul Marino',  '#000080', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/000080/FFFFFF?text=Remera+Azul+Marino', true, false, 'Top', 'Azul Marino', '#000080', 0.93, 'qwen3.6', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(5,  'Remera Roja',             'Top',       'Rojo',         '#CC0000', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/CC0000/FFFFFF?text=Remera+Roja', true, false, 'Top', 'Rojo', '#CC0000', 0.96, 'qwen3.6', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(6,  'Remera Verde Militar',    'Top',       'Verde Militar','#4A5D23', 'Algodón',       'Otoño/Invierno',   'https://placehold.co/600x800/4A5D23/FFFFFF?text=Remera+Verde+Militar', true, false, 'Top', 'Verde Militar', '#4A5D23', 0.91, 'qwen3.6', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(7,  'Remera Mostaza',          'Top',       'Mostaza',      '#E1AD01', 'Algodón',       'Otoño',            'https://placehold.co/600x800/E1AD01/000000?text=Remera+Mostaza', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
(8,  'Top Lino Beige',          'Top',       'Beige',        '#F5F5DC', 'Lino',          'Primavera/Verano', 'https://placehold.co/600x800/F5F5DC/000000?text=Top+Lino+Beige', false, false, 'Top', 'Beige', '#F5F5DC', 0.88, 'qwen3.6', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(9,  'Musculosa Negra',         'Top',       'Negro',        '#2C2C2C', 'Algodón',       'Verano',           'https://placehold.co/600x800/2C2C2C/FFFFFF?text=Musculosa+Negra', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(10, 'Top Rayado Blanco/Negro', 'Top',       'Rayado',       '#E8E8E8', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/E8E8E8/000000?text=Top+Rayado', true, true, 'Top', 'Rayado', '#E8E8E8', 0.89, 'qwen3.6', NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
(11, 'Top Coral',               'Top',       'Coral',        '#FF7F50', 'Viscosa',       'Primavera/Verano', 'https://placehold.co/600x800/FF7F50/000000?text=Top+Coral', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
(12, 'Top Lila',                'Top',       'Lila',         '#C8A2C8', 'Seda',          'Primavera/Verano', 'https://placehold.co/600x800/C8A2C8/000000?text=Top+Lila', true, false, 'Top', 'Lila', '#C8A2C8', 0.92, 'qwen3.6', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(13, 'Remera Manga Larga Gris', 'Top',       'Gris',         '#A9A9A9', 'Algodón',       'Invierno',         'https://placehold.co/600x800/A9A9A9/FFFFFF?text=Manga+Larga+Gris', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
(14, 'Top Blanco Escote V',     'Top',       'Blanco',       '#FFFFF0', 'Seda',          'Primavera/Verano', 'https://placehold.co/600x800/FFFFF0/000000?text=Top+Blanco+Escote', true, false, 'Top', 'Blanco', '#FFFFF0', 0.97, 'qwen3.6', NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days');

-- PANTALONES (15-22)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(15, 'Pantalón Vestir Gris',    'Pantalón',  'Gris',         '#696969', 'Lana',          'Invierno',         'https://placehold.co/600x800/696969/FFFFFF?text=Pantalon+Vestir+Gris', true, true, 'Pantalón', 'Gris', '#696969', 0.93, 'qwen3.6', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(16, 'Pantalón Vestir Beige',   'Pantalón',  'Beige',        '#D2B48C', 'Gabardina',     'Primavera/Verano', 'https://placehold.co/600x800/D2B48C/000000?text=Pantalon+Vestir+Beige', true, false, 'Pantalón', 'Beige', '#D2B48C', 0.90, 'qwen3.6', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(17, 'Jean Azul Claro',         'Pantalón',  'Azul',         '#4A90D9', 'Jeans',         'Todo el año',      'https://placehold.co/600x800/4A90D9/FFFFFF?text=Jean+Azul+Claro', true, true, 'Pantalón', 'Azul', '#4A90D9', 0.95, 'qwen3.6', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(18, 'Jean Azul Oscuro',        'Pantalón',  'Azul',         '#1E3A5F', 'Jeans',         'Todo el año',      'https://placehold.co/600x800/1E3A5F/FFFFFF?text=Jean+Azul+Oscuro', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
(19, 'Jogging Gris',            'Pantalón',  'Gris',         '#C0C0C0', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/C0C0C0/000000?text=Jogging+Gris', true, false, 'Pantalón', 'Gris', '#C0C0C0', 0.88, 'qwen3.6', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(20, 'Short Blanco',            'Pantalón',  'Blanco',       '#F0F0F0', 'Algodón',       'Verano',           'https://placehold.co/600x800/F0F0F0/000000?text=Short+Blanco', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
(21, 'Jean Negro',              'Pantalón',  'Negro',        '#1A1A1A', 'Jeans',         'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Jean+Negro', true, true, 'Pantalón', 'Negro', '#1A1A1A', 0.96, 'qwen3.6', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
(22, 'Jogging Negro',           'Pantalón',  'Negro',        '#2C2C2C', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/2C2C2C/FFFFFF?text=Jogging+Negro', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days');

-- CHAQUETAS (23-28)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(23, 'Jean Jacket Azul',        'Chaqueta',  'Azul',         '#4A7FB5', 'Jeans',         'Primavera/Verano', 'https://placehold.co/600x800/4A7FB5/FFFFFF?text=Jean+Jacket+Azul', true, false, 'Chaqueta', 'Azul', '#4A7FB5', 0.92, 'qwen3.6', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(24, 'Blazer Negro',            'Chaqueta',  'Negro',        '#1A1A1A', 'Tweed',         'Invierno',         'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Blazer+Negro', true, true, 'Chaqueta', 'Negro', '#1A1A1A', 0.97, 'qwen3.6', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(25, 'Blazer Azul Marino',      'Chaqueta',  'Azul Marino',  '#1B1B3A', 'Lana',          'Otoño/Invierno',   'https://placehold.co/600x800/1B1B3A/FFFFFF?text=Blazer+Azul+Marino', true, false, 'Chaqueta', 'Azul Marino', '#1B1B3A', 0.94, 'qwen3.6', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(26, 'Campera Cuero Marrón',    'Chaqueta',  'Marrón',       '#5C3A21', 'Cuero',         'Otoño/Invierno',   'https://placehold.co/600x800/5C3A21/FFFFFF?text=Campera+Cuero+Marrón', true, false, 'Chaqueta', 'Marrón', '#5C3A21', 0.89, 'qwen3.6', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(27, 'Bomber Verde Oliva',      'Chaqueta',  'Verde Oliva',  '#556B2F', 'Nailon',        'Otoño/Invierno',   'https://placehold.co/600x800/556B2F/FFFFFF?text=Bomber+Verde+Oliva', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(28, 'Cazadora Beige',          'Chaqueta',  'Beige',        '#D4C5A9', 'Gabardina',     'Primavera',        'https://placehold.co/600x800/D4C5A9/000000?text=Cazadora+Beige', true, false, 'Chaqueta', 'Beige', '#D4C5A9', 0.86, 'qwen3.6', NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days');

-- CAMISAS (29-33)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(29, 'Camisa Blanca',           'Camisa',    'Blanco',       '#FFFFFF', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/FFFFFF/000000?text=Camisa+Blanca', true, true, 'Camisa', 'Blanco', '#FFFFFF', 0.98, 'qwen3.6', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(30, 'Camisa Celeste',          'Camisa',    'Celeste',      '#87CEEB', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/87CEEB/000000?text=Camisa+Celeste', true, false, 'Camisa', 'Celeste', '#87CEEB', 0.95, 'qwen3.6', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(31, 'Camisa Rosa',             'Camisa',    'Rosa',         '#FFB6C1', 'Algodón',       'Primavera/Verano', 'https://placehold.co/600x800/FFB6C1/000000?text=Camisa+Rosa', false, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
(32, 'Camisa Cuadros Rojo/Negro','Camisa',   'Rojo',         '#8B0000', 'Lana',          'Invierno',         'https://placehold.co/600x800/8B0000/FFFFFF?text=Camisa+Cuadros', true, false, 'Camisa', 'Rojo', '#8B0000', 0.87, 'qwen3.6', NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(33, 'Camisa Lino Beige',       'Camisa',    'Beige',        '#E8D5B7', 'Lino',           'Verano',           'https://placehold.co/600x800/E8D5B7/000000?text=Camisa+Lino+Beige', true, false, 'Camisa', 'Beige', '#E8D5B7', 0.91, 'qwen3.6', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days');

-- VESTIDOS (34-37)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(34, 'Vestido Negro Tubo',      'Vestido',   'Negro',        '#1A1A1A', 'Viscosa',       'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Vestido+Negro+Tubo', true, true, 'Vestido', 'Negro', '#1A1A1A', 0.96, 'qwen3.6', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(35, 'Vestido Floral Primavera', 'Vestido',  'Multicolor',   '#D4567A', 'Viscosa',       'Primavera/Verano', 'https://placehold.co/600x800/D4567A/FFFFFF?text=Vestido+Floral', true, false, 'Vestido', 'Multicolor', '#D4567A', 0.85, 'qwen3.6', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
(36, 'Vestido Rojo Noche',      'Vestido',   'Rojo',         '#8B0000', 'Seda',          'Invierno',         'https://placehold.co/600x800/8B0000/FFFFFF?text=Vestido+Rojo+Noche', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
(37, 'Vestido Verde Esmeralda', 'Vestido',   'Verde',        '#2E8B57', 'Seda',          'Primavera/Verano', 'https://placehold.co/600x800/2E8B57/FFFFFF?text=Vestido+Verde+Esmeralda', true, false, 'Vestido', 'Verde', '#2E8B57', 0.93, 'qwen3.6', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days');

-- FALDAS (38-41)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(38, 'Falda Lápiz Negra',       'Falda',     'Negro',        '#1A1A1A', 'Poliéster',     'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Falda+Lapiz+Negra', true, false, 'Falda', 'Negro', '#1A1A1A', 0.94, 'qwen3.6', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(39, 'Falda Plisada Azul',      'Falda',     'Azul',         '#4682B4', 'Poliéster',     'Primavera/Verano', 'https://placehold.co/600x800/4682B4/FFFFFF?text=Falda+Plisada+Azul', true, false, 'Falda', 'Azul', '#4682B4', 0.90, 'qwen3.6', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(40, 'Mini Falda Vaquera',      'Falda',     'Azul',         '#5B8DB8', 'Jeans',         'Verano',           'https://placehold.co/600x800/5B8DB8/FFFFFF?text=Mini+Falda+Vaquera', false, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
(41, 'Falda Larga Estampada',   'Falda',     'Multicolor',   '#B8860B', 'Viscosa',       'Primavera/Verano', 'https://placehold.co/600x800/B8860B/FFFFFF?text=Falda+Larga+Estampada', true, false, 'Falda', 'Multicolor', '#B8860B', 0.82, 'qwen3.6', NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days');

-- SUDADERAS (42-45)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(42, 'Hoodie Gris',             'Sudadera',  'Gris',         '#808080', 'Algodón',       'Invierno',         'https://placehold.co/600x800/808080/FFFFFF?text=Hoodie+Gris', true, true, 'Sudadera', 'Gris', '#808080', 0.93, 'qwen3.6', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(43, 'Buzo Azul Marino',        'Sudadera',  'Azul Marino',  '#191970', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/191970/FFFFFF?text=Buzo+Azul+Marino', true, false, 'Sudadera', 'Azul Marino', '#191970', 0.95, 'qwen3.6', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(44, 'Crewneck Verde',          'Sudadera',  'Verde',        '#2F4F2F', 'Algodón',       'Otoño/Invierno',   'https://placehold.co/600x800/2F4F2F/FFFFFF?text=Crewneck+Verde', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(45, 'Buzo Oversize Negro',     'Sudadera',  'Negro',        '#2C2C2C', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/2C2C2C/FFFFFF?text=Buzo+Oversize+Negro', true, false, 'Sudadera', 'Negro', '#2C2C2C', 0.91, 'qwen3.6', NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days');

-- ABRIGOS (46-48)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(46, 'Abrigo Largo Beige',      'Abrigo',    'Beige',        '#C4A882', 'Lana',          'Invierno',         'https://placehold.co/600x800/C4A882/000000?text=Abrigo+Largo+Beige', true, true, 'Abrigo', 'Beige', '#C4A882', 0.96, 'qwen3.6', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(47, 'Parka Verde',             'Abrigo',    'Verde',        '#3B5323', 'Nailon',        'Invierno',         'https://placehold.co/600x800/3B5323/FFFFFF?text=Parka+Verde', true, false, 'Abrigo', 'Verde', '#3B5323', 0.92, 'qwen3.6', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
(48, 'Tapado Negro Lana',       'Abrigo',    'Negro',        '#1A1A1A', 'Lana',          'Invierno',         'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Tapado+Negro+Lana', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days');

-- ZAPATOS (49-55)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(49, 'Zapatillas Blancas',      'Zapatos',   'Blanco',       '#F5F5F5', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/F5F5F5/000000?text=Zapatillas+Blancas', true, true, 'Zapatos', 'Blanco', '#F5F5F5', 0.97, 'qwen3.6', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(50, 'Zapatillas Negras',       'Zapatos',   'Negro',        '#333333', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/333333/FFFFFF?text=Zapatillas+Negras', true, false, 'Zapatos', 'Negro', '#333333', 0.96, 'qwen3.6', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(51, 'Botines Cuero Negros',    'Zapatos',   'Negro',        '#1A1A1A', 'Cuero',         'Invierno',         'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Botines+Cuero+Negros', true, false, 'Zapatos', 'Negro', '#1A1A1A', 0.94, 'qwen3.6', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(52, 'Botas de Lluvia',         'Zapatos',   'Verde',        '#4A6741', 'Goma',          'Invierno',         'https://placehold.co/600x800/4A6741/FFFFFF?text=Botas+de+Lluvia', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(53, 'Zapatos Cuero Marrón',    'Zapatos',   'Marrón',       '#5C3A21', 'Cuero',         'Otoño/Invierno',   'https://placehold.co/600x800/5C3A21/FFFFFF?text=Zapatos+Cuero+Marrón', true, true, 'Zapatos', 'Marrón', '#5C3A21', 0.93, 'qwen3.6', NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
(54, 'Zapatos Cuero Negro',     'Zapatos',   'Negro',        '#1A1A1A', 'Cuero',         'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Zapatos+Cuero+Negro', true, false, 'Zapatos', 'Negro', '#1A1A1A', 0.95, 'qwen3.6', NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(55, 'Sandalias Marrones',      'Zapatos',   'Marrón',       '#8B7355', 'Cuero',         'Verano',           'https://placehold.co/600x800/8B7355/FFFFFF?text=Sandalias+Marrones', true, false, 'Zapatos', 'Marrón', '#8B7355', 0.88, 'qwen3.6', NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days');

-- ACCESORIOS (56-65)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(56, 'Cartera Negra',           'Accesorio', 'Negro',        '#1A1A1A', 'Cuero',         'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Cartera+Negra', true, true, 'Accesorio', 'Negro', '#1A1A1A', 0.97, 'qwen3.6', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(57, 'Mochila Azul',            'Accesorio', 'Azul',         '#4169E1', 'Nailon',        'Todo el año',      'https://placehold.co/600x800/4169E1/FFFFFF?text=Mochila+Azul', true, false, 'Accesorio', 'Azul', '#4169E1', 0.94, 'qwen3.6', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(58, 'Cinturón Marrón',         'Accesorio', 'Marrón',       '#6B4226', 'Cuero',         'Todo el año',      'https://placehold.co/600x800/6B4226/FFFFFF?text=Cinturón+Marrón', true, false, 'Accesorio', 'Marrón', '#6B4226', 0.92, 'qwen3.6', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
(59, 'Cinturón Negro',          'Accesorio', 'Negro',        '#1A1A1A', 'Cuero',         'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Cinturón+Negro', true, false, 'Accesorio', 'Negro', '#1A1A1A', 0.95, 'qwen3.6', NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(60, 'Gafas de Sol',            'Accesorio', 'Negro',        '#1A1A1A', NULL,            NULL,               'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Gafas+de+Sol', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
(61, 'Reloj Plateado',          'Accesorio', 'Plateado',     '#C0C0C0', NULL,            'Todo el año',      'https://placehold.co/600x800/C0C0C0/000000?text=Reloj+Plateado', true, false, 'Accesorio', 'Plateado', '#C0C0C0', 0.89, 'qwen3.6', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(62, 'Reloj Dorado',            'Accesorio', 'Dorado',       '#D4AF37', NULL,            'Todo el año',      'https://placehold.co/600x800/D4AF37/000000?text=Reloj+Dorado', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days'),
(63, 'Bufanda Gris',            'Accesorio', 'Gris',         '#808080', 'Lana',          'Invierno',         'https://placehold.co/600x800/808080/FFFFFF?text=Bufanda+Gris', true, false, 'Accesorio', 'Gris', '#808080', 0.91, 'qwen3.6', NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(64, 'Gorra Negra',             'Accesorio', 'Negro',        '#1A1A1A', 'Algodón',       'Verano',           'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Gorra+Negra', true, false, 'Accesorio', 'Negro', '#1A1A1A', 0.93, 'qwen3.6', NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),
(65, 'Pañuelo Seda Rojo',       'Accesorio', 'Rojo',         '#DC143C', 'Seda',          'Primavera/Verano', 'https://placehold.co/600x800/DC143C/FFFFFF?text=Pañuelo+Seda+Rojo', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days');

-- OTROS (66-70)
INSERT INTO garments (id, name, category, color_name, color_hex, material, season, image_url, user_confirmed, favorite, ai_type, ai_color_name, ai_color_hex, ai_confidence, ai_model, created_at, updated_at)
VALUES
(66, 'Sombrero de Paja',        'Otro',      'Beige',        '#D4AC0D', NULL,            'Verano',           'https://placehold.co/600x800/D4AC0D/000000?text=Sombrero+de+Paja', false, false, 'Otro', 'Beige', '#D4AC0D', 0.84, 'qwen3.6', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(67, 'Paraguas Negro',          'Otro',      'Negro',        '#1A1A1A', NULL,            'Todo el año',      'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Paraguas+Negro', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days'),
(68, 'Guantes Cuero Negros',     'Otro',     'Negro',        '#1A1A1A', 'Cuero',         'Invierno',         'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Guantes+Cuero+Negros', true, false, 'Otro', 'Negro', '#1A1A1A', 0.90, 'qwen3.6', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
(69, 'Medias Largas Negras',    'Otro',      'Negro',        '#1A1A1A', 'Algodón',       'Invierno',         'https://placehold.co/600x800/1A1A1A/FFFFFF?text=Medias+Largas+Negras', true, false, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '32 days', NOW() - INTERVAL '32 days'),
(70, 'Cinturón Elástico Beige', 'Otro',      'Beige',        '#F5DEB3', 'Algodón',       'Todo el año',      'https://placehold.co/600x800/F5DEB3/000000?text=Cinturón+Elástico+Beige', false, false, 'Otro', 'Beige', '#F5DEB3', 0.86, 'qwen3.6', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days');

-- ============================================================
-- WEEK PLANS (90 entries — 3 semanas × 7 días)
-- ============================================================

-- SEMANA 1 — Oficina (looks formales)
-- Lunes: Camisa Blanca + Pantalón Vestir Gris + Blazer Negro + Zapatos Cuero Negro
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(29, 'Lunes', 0, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(15, 'Lunes', 1, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(24, 'Lunes', 2, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(54, 'Lunes', 3, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days');

-- Martes: Remera Negra + Falda Lápiz Negra + Blazer Azul Marino + Zapatos Cuero Negro + Cartera Negra
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(2,  'Martes', 0, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(38, 'Martes', 1, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(25, 'Martes', 2, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(54, 'Martes', 3, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
(56, 'Martes', 4, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days');

-- Miércoles: Camisa Celeste + Pantalón Vestir Beige + Cinturón Marrón + Zapatos Cuero Marrón
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(30, 'Miercoles', 0, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(16, 'Miercoles', 1, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(58, 'Miercoles', 2, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(53, 'Miercoles', 3, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days');

-- Jueves: Vestido Negro Tubo + Blazer Negro + Botines Cuero Negros + Reloj Plateado
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(34, 'Jueves', 0, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(24, 'Jueves', 1, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(51, 'Jueves', 2, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
(61, 'Jueves', 3, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days');

-- Viernes: Top Lila + Falda Plisada Azul + Blazer Azul Marino + Zapatos Cuero Negro + Pañuelo Rojo
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(12, 'Viernes', 0, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(39, 'Viernes', 1, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(25, 'Viernes', 2, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(54, 'Viernes', 3, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(65, 'Viernes', 4, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days');

-- Sábado: Musculosa Negra + Short Blanco + Sandalias Marrones + Gafas de Sol
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(9,  'Sabado', 0, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(20, 'Sabado', 1, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(55, 'Sabado', 2, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days'),
(60, 'Sabado', 3, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days');

-- Domingo: Buzo Azul Marino + Jogging Gris + Zapatillas Blancas + Gorra Negra
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(43, 'Domingo', 0, NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days'),
(19, 'Domingo', 1, NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days'),
(49, 'Domingo', 2, NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days'),
(64, 'Domingo', 3, NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days');

-- SEMANA 2 — Casual
-- Lunes: Top Rayado + Jean Azul Claro + Zapatillas Blancas + Mochila Azul
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(10, 'Lunes', 0, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(17, 'Lunes', 1, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(49, 'Lunes', 2, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(57, 'Lunes', 3, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days');

-- Martes: Crewneck Verde + Jean Negro + Zapatillas Negras + Reloj Dorado
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(44, 'Martes', 0, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(21, 'Martes', 1, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(50, 'Martes', 2, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
(62, 'Martes', 3, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days');

-- Miércoles: Remera Roja + Pantalón Vestir Beige + Cinturón Marrón + Botines Cuero Negros
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(5,  'Miercoles', 0, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(16, 'Miercoles', 1, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(58, 'Miercoles', 2, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(51, 'Miercoles', 3, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days');

-- Jueves: Remera Mostaza + Jean Azul Oscuro + Cinturón Negro + Zapatillas Blancas
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(7,  'Jueves', 0, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(18, 'Jueves', 1, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(59, 'Jueves', 2, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
(49, 'Jueves', 3, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days');

-- Viernes: Camisa Cuadros + Jean Azul Claro + Botas de Lluvia + Mochila Azul
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(32, 'Viernes', 0, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(17, 'Viernes', 1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(52, 'Viernes', 2, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(57, 'Viernes', 3, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days');

-- Sábado: Hoodie Gris + Jogging Negro + Zapatillas Negras
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(42, 'Sabado', 0, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(22, 'Sabado', 1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
(50, 'Sabado', 2, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days');

-- Domingo: Top Coral + Short Blanco + Sandalias Marrones + Gafas de Sol
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(11, 'Domingo', 0, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
(20, 'Domingo', 1, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
(55, 'Domingo', 2, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
(60, 'Domingo', 3, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days');

-- SEMANA 3 — Capas e Invierno
-- Lunes: Remera Verde Militar + Campera Cuero Marrón + Jogging Negro + Botines Cuero Negros
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(6,  'Lunes', 0, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(26, 'Lunes', 1, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(22, 'Lunes', 2, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(51, 'Lunes', 3, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days');

-- Martes: Buzo Azul Marino + Parka Verde + Jean Azul Oscuro + Zapatillas Blancas + Bufanda Gris
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(43, 'Martes', 0, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(47, 'Martes', 1, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(18, 'Martes', 2, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(49, 'Martes', 3, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
(63, 'Martes', 4, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days');

-- Miércoles: Camisa Blanca + Hoodie Gris + Pantalón Vestir Gris + Abrigo Largo Beige + Zapatos Cuero Marrón
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(29, 'Miercoles', 0, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(42, 'Miercoles', 1, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(15, 'Miercoles', 2, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(46, 'Miercoles', 3, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(53, 'Miercoles', 4, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days');

-- Jueves: Vestido Verde Esmeralda + Abrigo Largo Beige + Botines Cuero Negros + Cartera Negra
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(37, 'Jueves', 0, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(46, 'Jueves', 1, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(51, 'Jueves', 2, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'),
(56, 'Jueves', 3, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days');

-- Viernes: Remera Negra + Jean Negro + Bomber Verde Oliva + Zapatillas Blancas + Reloj Plateado
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(2,  'Viernes', 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(21, 'Viernes', 1, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(27, 'Viernes', 2, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(49, 'Viernes', 3, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(61, 'Viernes', 4, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days');

-- Sábado: Buzo Oversize Negro + Jogging Gris + Zapatillas Blancas
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(45, 'Sabado', 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(19, 'Sabado', 1, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
(49, 'Sabado', 2, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days');

-- Domingo: Remera Manga Larga Gris + Falda Larga Estampada + Tapado Negro Lana + Botas de Lluvia + Guantes Cuero
INSERT INTO week_plans (garment_id, day_of_week, position, created_at, updated_at) VALUES
(13, 'Domingo', 0, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(41, 'Domingo', 1, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(48, 'Domingo', 2, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(52, 'Domingo', 3, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
(68, 'Domingo', 4, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days');
