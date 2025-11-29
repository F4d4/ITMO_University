-- ===================================================================
-- СКРИПТ МИГРАЦИИ ДЛЯ ИСПРАВЛЕНИЯ ТИПОВ ENUM КОЛОНОК
-- Изменяет type и fueltype с bytea на VARCHAR
-- ===================================================================

-- Проверка текущих типов колонок
SELECT 
    column_name, 
    data_type, 
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'vehicle' 
AND column_name IN ('type', 'fueltype');

-- ===================================================================
-- ИСПРАВЛЕНИЕ КОЛОНКИ type
-- ===================================================================

-- Если колонка имеет тип bytea, преобразуем её в VARCHAR
ALTER TABLE vehicle 
ALTER COLUMN type TYPE VARCHAR(50) 
USING CASE 
    WHEN type IS NULL THEN NULL
    ELSE encode(type, 'escape')::VARCHAR
END;

-- ===================================================================
-- ИСПРАВЛЕНИЕ КОЛОНКИ fueltype
-- ===================================================================

-- Если колонка имеет тип bytea, преобразуем её в VARCHAR
ALTER TABLE vehicle 
ALTER COLUMN fueltype TYPE VARCHAR(50) 
USING CASE 
    WHEN fueltype IS NULL THEN NULL
    ELSE encode(fueltype, 'escape')::VARCHAR
END;

-- ===================================================================
-- ПРОВЕРКА РЕЗУЛЬТАТА
-- ===================================================================

-- Проверяем новые типы колонок
SELECT 
    column_name, 
    data_type, 
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'vehicle' 
AND column_name IN ('type', 'fueltype');

-- Проверяем данные
SELECT id, name, type, fueltype 
FROM vehicle 
LIMIT 10;

-- ===================================================================
-- ВОССТАНОВЛЕНИЕ CONSTRAINTS (если они были удалены)
-- ===================================================================

-- Удаляем старые constraints, если они существуют
ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS vehicle_type_check;
ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS vehicle_fuel_type_check;

-- Создаем новые constraints
ALTER TABLE vehicle 
ADD CONSTRAINT vehicle_type_check 
CHECK (type IS NULL OR type IN ('CAR', 'HELICOPTER', 'BOAT', 'HOVERBOARD'));

ALTER TABLE vehicle 
ADD CONSTRAINT vehicle_fuel_type_check 
CHECK (fueltype IS NULL OR fueltype IN ('KEROSENE', 'ELECTRICITY', 'DIESEL', 'ALCOHOL'));

-- ===================================================================
-- КОНЕЦ СКРИПТА
-- ===================================================================

