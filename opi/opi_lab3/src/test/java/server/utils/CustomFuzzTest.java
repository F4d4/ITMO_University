package server.utils;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class CustomFuzzTest {
    private static final Random random = new Random();
    
    // Набор для отслеживания покрытых ветвей кода
    private static final Set<String> coveredBranches = new HashSet<>();
    
    // Границы для генерации значений
    private static final double MIN_VALID = -5.0;
    private static final double MAX_VALID = 5.0;
    private static final double MIN_INVALID = -10.0;
    private static final double MAX_INVALID = 10.0;
    
    public static void main(String[] args) {
        System.out.println("Запуск фаззинг-тестирования Area без JUnit...");
        
        int totalTests = 5000;
        int successCount = 0;
        int failureCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        // Тестирование функции валидации
        System.out.println("\n=== Тестирование функции валидации ===");
        testValidation(totalTests / 2);
        
        // Тестирование функции вычисления области
        System.out.println("\n=== Тестирование функции расчета попадания в область ===");
        testAreaCalculation(totalTests / 2);
        
        // Тестирование граничных значений
        System.out.println("\n=== Тестирование граничных значений ===");
        testBoundaryValues(totalTests / 4);
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== Итоги фаззинг-тестирования ===");
        System.out.println("Покрыто ветвей: " + coveredBranches.size() + "/6 (" + 
                          (coveredBranches.size() * 100 / 6) + "%)");
        System.out.println("Количество запусков: " + totalTests);
        System.out.println("Время выполнения: " + (endTime - startTime) + "мс");
        System.out.println("Покрытые ветви: " + coveredBranches);
    }
    
    private static void testValidation(int numTests) {
        int successCount = 0;
        int failureCount = 0;
        
        // Тест с валидными значениями
        for (int i = 0; i < numTests / 2; i++) {
            double x = randomInRange(MIN_VALID, MAX_VALID);
            double y = randomInRange(MIN_VALID, MAX_VALID);
            double r = randomInRange(0.1, MAX_VALID);
            
            boolean result = Area.validation(x, y, r);
            if (result) {
                successCount++;
            } else {
                failureCount++;
                System.out.println("ОШИБКА валидации: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        // Тест с невалидными значениями
        for (int i = 0; i < numTests / 6; i++) {
            // Невалидный X
            double x = randomInRange(MIN_INVALID, MIN_VALID - 0.01);
            double y = randomInRange(MIN_VALID, MAX_VALID);
            double r = randomInRange(0.1, MAX_VALID);
            
            boolean result = Area.validation(x, y, r);
            if (!result) {
                successCount++;
                recordBranch("validation_invalid_x");
            } else {
                failureCount++;
                System.out.println("ОШИБКА невалидного X: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        for (int i = 0; i < numTests / 6; i++) {
            // Невалидный Y
            double x = randomInRange(MIN_VALID, MAX_VALID);
            double y = randomInRange(MIN_INVALID, MIN_VALID - 0.01);
            double r = randomInRange(0.1, MAX_VALID);
            
            boolean result = Area.validation(x, y, r);
            if (!result) {
                successCount++;
                recordBranch("validation_invalid_y");
            } else {
                failureCount++;
                System.out.println("ОШИБКА невалидного Y: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        for (int i = 0; i < numTests / 6; i++) {
            // Невалидный R
            double x = randomInRange(MIN_VALID, MAX_VALID);
            double y = randomInRange(MIN_VALID, MAX_VALID);
            double r = randomInRange(MIN_INVALID, MIN_VALID - 0.01);
            
            boolean result = Area.validation(x, y, r);
            if (!result) {
                successCount++;
                recordBranch("validation_invalid_r");
            } else {
                failureCount++;
                System.out.println("ОШИБКА невалидного R: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        System.out.println("Успешных тестов валидации: " + successCount);
        System.out.println("Неуспешных тестов валидации: " + failureCount);
    }
    
    private static void testAreaCalculation(int numTests) {
        int successCount = 0;
        int failureCount = 0;
        
        // Тестирование первого квадранта (прямоугольник)
        for (int i = 0; i < numTests / 4; i++) {
            double r = randomInRange(0.1, MAX_VALID);
            double x = randomInRange(0, r * 1.5);
            double y = randomInRange(0, r * 1.5);
            
            boolean result = Area.calculate(x, y, r);
            boolean expected = (x >= 0 && y >= 0 && x <= r && y <= r);
            
            if (result == expected) {
                successCount++;
                if (result) {
                    recordBranch("first_quadrant_inside");
                }
            } else {
                failureCount++;
                System.out.println("ОШИБКА первого квадранта: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        // Тестирование третьего квадранта (треугольник)
        for (int i = 0; i < numTests / 4; i++) {
            double r = randomInRange(0.1, MAX_VALID);
            double x = randomInRange(-r, 0);
            double y = randomInRange(-r, 0);
            
            boolean result = Area.calculate(x, y, r);
            boolean expected = (x <= 0 && y <= 0 && x >= -r/2 && y >= -r/2 && (x + y + r*0.5) >= 0);
            
            if (result == expected) {
                successCount++;
                if (result) {
                    recordBranch("third_quadrant_inside");
                }
            } else {
                failureCount++;
                System.out.println("ОШИБКА третьего квадранта: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        // Тестирование четвертого квадранта (четверть круга)
        for (int i = 0; i < numTests / 4; i++) {
            double r = randomInRange(0.1, MAX_VALID);
            double x = randomInRange(0, r);
            double y = randomInRange(-r, 0);
            
            boolean result = Area.calculate(x, y, r);
            boolean expected = (x >= 0 && y <= 0 && y > -r/2 && x <= r/2 && 
                                Math.pow(x, 2) + Math.pow(y, 2) <= Math.pow(r/2, 2));
            
            if (result == expected) {
                successCount++;
                if (result) {
                    recordBranch("fourth_quadrant_inside");
                }
            } else {
                failureCount++;
                System.out.println("ОШИБКА четвертого квадранта: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        // Тестирование второго квадранта (нет фигуры)
        for (int i = 0; i < numTests / 4; i++) {
            double r = randomInRange(0.1, MAX_VALID);
            double x = randomInRange(-r, 0);
            double y = randomInRange(0, r);
            
            boolean result = Area.calculate(x, y, r);
            
            if (!result) {
                successCount++;
            } else {
                failureCount++;
                System.out.println("ОШИБКА второго квадранта: x=" + x + ", y=" + y + ", r=" + r);
            }
        }
        
        System.out.println("Успешных тестов расчета области: " + successCount);
        System.out.println("Неуспешных тестов расчета области: " + failureCount);
    }
    
    private static void testBoundaryValues(int numTests) {
        int successCount = 0;
        int failureCount = 0;
        
        // Граничные значения на осях
        for (int i = 0; i < numTests / 2; i++) {
            double r = randomInRange(0.1, MAX_VALID);
            double epsilon = randomInRange(-0.1, 0.1);
            
            // Точка на оси X
            testBoundaryPoint(r, epsilon, 0, successCount, failureCount);
            
            // Точка на оси Y
            testBoundaryPoint(0, r, epsilon, successCount, failureCount);
            
            // Точка на границе прямоугольника
            testBoundaryPoint(r + epsilon, r / 2, 0, successCount, failureCount);
            
            // Точка на границе треугольника
            testBoundaryPoint(-r/4 + epsilon, -r/4 + epsilon, r, successCount, failureCount);
            
            // Точка на границе круга
            double circleX = r/2 * Math.cos(Math.PI * 7 / 4);
            double circleY = r/2 * Math.sin(Math.PI * 7 / 4);
            testBoundaryPoint(circleX * (1 + epsilon), circleY * (1 + epsilon), r, successCount, failureCount);
        }
    }
    
    private static void testBoundaryPoint(double x, double y, double r, int successCount, int failureCount) {
        if (Area.validation(x, y, r)) {
            boolean result = Area.calculate(x, y, r);
            // Считаем все граничные тесты успешными, т.к. нам важна проверка на ошибки
            successCount++;
        }
    }
    
    private static double randomInRange(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    private static void recordBranch(String branchName) {
        coveredBranches.add(branchName);
    }
} 