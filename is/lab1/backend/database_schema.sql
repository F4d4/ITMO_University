-- ===================================================================
-- SQL СКРИПТ ДЛЯ СОЗДАНИЯ ТАБЛИЦ VEHICLE MANAGEMENT SYSTEM
-- База данных: PostgreSQL
-- ===================================================================

-- Удаление существующих таблиц (опционально, раскомментировать при необходимости)
-- DROP TABLE IF EXISTS vehicle CASCADE;
-- DROP TABLE IF EXISTS coordinates CASCADE;

-- ===================================================================
-- ТАБЛИЦА: coordinates
-- Хранит координаты транспортных средств
-- ===================================================================
CREATE TABLE IF NOT EXISTS coordinates (
    id SERIAL PRIMARY KEY,
    x DOUBLE PRECISION NOT NULL,
    y BIGINT NOT NULL CHECK (y <= 621),
    CONSTRAINT coordinates_y_check CHECK (y <= 621)
);

-- Индексы для coordinates
CREATE INDEX IF NOT EXISTS idx_coordinates_x ON coordinates(x);
CREATE INDEX IF NOT EXISTS idx_coordinates_y ON coordinates(y);

-- Комментарии к таблице coordinates
COMMENT ON TABLE coordinates IS 'Координаты транспортных средств';
COMMENT ON COLUMN coordinates.id IS 'Уникальный идентификатор';
COMMENT ON COLUMN coordinates.x IS 'Координата X (double)';
COMMENT ON COLUMN coordinates.y IS 'Координата Y (long), максимальное значение 621';


-- ===================================================================
-- ТАБЛИЦА: vehicle
-- Основная таблица транспортных средств
-- ===================================================================
CREATE TABLE IF NOT EXISTS vehicle (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (name <> ''),
    coordinates_id BIGINT NOT NULL,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50),
    engine_power INTEGER NOT NULL CHECK (engine_power > 0),
    number_of_wheels INTEGER NOT NULL CHECK (number_of_wheels > 0),
    capacity DOUBLE PRECISION NOT NULL CHECK (capacity > 0),
    distance_travelled BIGINT NOT NULL CHECK (distance_travelled >= 0),
    fuel_consumption BIGINT NOT NULL CHECK (fuel_consumption > 0),
    fuel_type VARCHAR(50),
    CONSTRAINT fk_vehicle_coordinates 
        FOREIGN KEY (coordinates_id) 
        REFERENCES coordinates(id) 
        ON DELETE RESTRICT,
    CONSTRAINT vehicle_name_check CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT vehicle_type_check 
        CHECK (type IS NULL OR type IN ('CAR', 'HELICOPTER', 'BOAT', 'HOVERBOARD')),
    CONSTRAINT vehicle_fuel_type_check 
        CHECK (fuel_type IS NULL OR fuel_type IN ('KEROSENE', 'ELECTRICITY', 'DIESEL', 'ALCOHOL'))
);

-- Индексы для vehicle
CREATE INDEX IF NOT EXISTS idx_vehicle_name ON vehicle(name);
CREATE INDEX IF NOT EXISTS idx_vehicle_type ON vehicle(type);
CREATE INDEX IF NOT EXISTS idx_vehicle_capacity ON vehicle(capacity DESC);
CREATE INDEX IF NOT EXISTS idx_vehicle_fuel_consumption ON vehicle(fuel_consumption);
CREATE INDEX IF NOT EXISTS idx_vehicle_creation_date ON vehicle(creation_date DESC);
CREATE INDEX IF NOT EXISTS idx_vehicle_coordinates ON vehicle(coordinates_id);

-- Комментарии к таблице vehicle
COMMENT ON TABLE vehicle IS 'Транспортные средства';
COMMENT ON COLUMN vehicle.id IS 'Уникальный идентификатор, генерируется автоматически';
COMMENT ON COLUMN vehicle.name IS 'Название транспортного средства, не может быть пустым';
COMMENT ON COLUMN vehicle.coordinates_id IS 'Ссылка на координаты';
COMMENT ON COLUMN vehicle.creation_date IS 'Дата создания, генерируется автоматически';
COMMENT ON COLUMN vehicle.type IS 'Тип транспортного средства (CAR, HELICOPTER, BOAT, HOVERBOARD)';
COMMENT ON COLUMN vehicle.engine_power IS 'Мощность двигателя, должна быть больше 0';
COMMENT ON COLUMN vehicle.number_of_wheels IS 'Количество колес, должно быть больше 0';
COMMENT ON COLUMN vehicle.capacity IS 'Вместимость, должна быть больше 0';
COMMENT ON COLUMN vehicle.distance_travelled IS 'Пройденное расстояние, не может быть отрицательным';
COMMENT ON COLUMN vehicle.fuel_consumption IS 'Расход топлива, должен быть больше 0';
COMMENT ON COLUMN vehicle.fuel_type IS 'Тип топлива (KEROSENE, ELECTRICITY, DIESEL, ALCOHOL)';


-- ===================================================================
-- ТЕСТОВЫЕ ДАННЫЕ (опционально)
-- ===================================================================

-- Вставка координат
INSERT INTO coordinates (x, y) VALUES 
    (100.5, 200),
    (50.0, 300),
    (10.5, 100),
    (5.0, 50),
    (75.3, 450);

-- Вставка транспортных средств
INSERT INTO vehicle (
    name, coordinates_id, creation_date, type, 
    engine_power, number_of_wheels, capacity, 
    distance_travelled, fuel_consumption, fuel_type
) VALUES 
    (
        'Tesla Model S', 1, CURRENT_TIMESTAMP, 'CAR',
        500, 4, 5.0, 15000, 20, 'ELECTRICITY'
    ),
    (
        'AgustaWestland AW139', 2, CURRENT_TIMESTAMP, 'HELICOPTER',
        2000, 3, 15.0, 50000, 200, 'KEROSENE'
    ),
    (
        'Speedboat 3000', 3, CURRENT_TIMESTAMP, 'BOAT',
        350, 0, 8.0, 2000, 80, 'DIESEL'
    ),
    (
        'Future Hoverboard', 4, CURRENT_TIMESTAMP, 'HOVERBOARD',
        100, 0, 1.0, 100, 5, 'ELECTRICITY'
    ),
    (
        'BMW X5', 5, CURRENT_TIMESTAMP, 'CAR',
        300, 4, 7.0, 50000, 15, 'DIESEL'
    );


-- ===================================================================
-- ПОЛЕЗНЫЕ ЗАПРОСЫ ДЛЯ ПРОВЕРКИ
-- ===================================================================

-- Получить все транспортные средства с координатами
SELECT 
    v.id, v.name, v.type, v.capacity, v.fuel_consumption,
    c.x, c.y
FROM vehicle v
JOIN coordinates c ON v.coordinates_id = c.id
ORDER BY v.id;

-- Получить транспортное средство с максимальной capacity
SELECT * FROM vehicle 
WHERE capacity = (SELECT MAX(capacity) FROM vehicle);

-- Получить транспортные средства по префиксу имени
SELECT * FROM vehicle 
WHERE name LIKE 'Tesla%';

-- Получить транспортные средства с расходом топлива больше 50
SELECT * FROM vehicle 
WHERE fuel_consumption > 50;

-- Получить транспортные средства по типу
SELECT * FROM vehicle 
WHERE type = 'CAR';

-- Получить статистику по типам транспорта
SELECT 
    type, 
    COUNT(*) as count,
    AVG(capacity) as avg_capacity,
    AVG(fuel_consumption) as avg_fuel_consumption
FROM vehicle
GROUP BY type;

-- Получить транспортные средства с пагинацией
SELECT * FROM vehicle 
ORDER BY id 
LIMIT 10 OFFSET 0;


-- ===================================================================
-- ФУНКЦИИ ДЛЯ СПЕЦИАЛЬНЫХ ОПЕРАЦИЙ
-- ===================================================================

-- Функция для сброса пробега
CREATE OR REPLACE FUNCTION reset_vehicle_distance(vehicle_id INTEGER)
RETURNS VOID AS $$
BEGIN
    UPDATE vehicle 
    SET distance_travelled = 0 
    WHERE id = vehicle_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Vehicle with ID % not found', vehicle_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Пример использования функции
-- SELECT reset_vehicle_distance(1);


-- ===================================================================
-- ПРАВА ДОСТУПА (настройте под ваши нужды)
-- ===================================================================

-- GRANT SELECT, INSERT, UPDATE, DELETE ON vehicle TO your_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON coordinates TO your_user;
-- GRANT USAGE, SELECT ON SEQUENCE vehicle_id_seq TO your_user;
-- GRANT USAGE, SELECT ON SEQUENCE coordinates_id_seq TO your_user;


-- ===================================================================
-- ОЧИСТКА ДАННЫХ
-- ===================================================================

-- Удалить все данные (раскомментировать при необходимости)
-- TRUNCATE TABLE vehicle CASCADE;
-- TRUNCATE TABLE coordinates RESTART IDENTITY CASCADE;


-- ===================================================================
-- ПРОВЕРКА CONSTRAINTS
-- ===================================================================

-- Попытка вставить невалидные данные (должна завершиться ошибкой)

-- Y > 621 (должно вызвать ошибку)
-- INSERT INTO coordinates (x, y) VALUES (100, 700);

-- Пустое имя (должно вызвать ошибку)
-- INSERT INTO vehicle (name, coordinates_id, type, engine_power, number_of_wheels, capacity, distance_travelled, fuel_consumption)
-- VALUES ('', 1, 'CAR', 500, 4, 5.0, 0, 20);

-- Отрицательная capacity (должно вызвать ошибку)
-- INSERT INTO vehicle (name, coordinates_id, type, engine_power, number_of_wheels, capacity, distance_travelled, fuel_consumption)
-- VALUES ('Invalid', 1, 'CAR', 500, 4, -5.0, 0, 20);


-- ===================================================================
-- ИНФОРМАЦИЯ О СХЕМЕ
-- ===================================================================

-- Получить информацию о таблицах
SELECT 
    table_name, 
    table_type
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('vehicle', 'coordinates');

-- Получить информацию о столбцах
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name IN ('vehicle', 'coordinates')
ORDER BY table_name, ordinal_position;

-- Получить информацию о constraints
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_name IN ('vehicle', 'coordinates')
ORDER BY tc.table_name, tc.constraint_type;


-- ===================================================================








