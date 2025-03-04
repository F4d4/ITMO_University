package org.example;


import java.util.Scanner;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        try{
            //double[][] arguments = {{2, 2, 10, 14}, {10, 1, 1, 12}, {2, 10, 1, 13}};
            Scanner scanner = new Scanner(System.in);
            System.out.println("Введите размерность матрицы (n<=20) :");
            int size = Integer.valueOf(scanner.nextLine().trim());
            if(size>20){
                System.out.println("Некорректные данные");
                System.exit(0);
            }
            double[][] arguments = new double[size][size+1];
            System.out.println("Введите  строки матрицы:");

            for (int i = 0; i < size; i++) {
                arguments[i] = Arrays.stream(scanner.nextLine().trim().split(" "))
                        .mapToDouble(Double::parseDouble)
                        .limit(size+1)
                        .toArray();
            }
            // Вызываем метод для проверки и перестановки
            if (ensureDiagonalDominance(arguments, 3)) {
                System.out.println("Матрица приведена к диагональному преобладанию.");
                System.out.println("Обновленная матрица:");
                printMatrix(arguments);
            } else {
                System.out.println("Подходящей перестановки нет.");
            }

            System.out.println("Введите точность");
            double margin = Double.valueOf(scanner.nextLine().trim());
            GaussZeidel gaussZeidel = new GaussZeidel(arguments, size,margin);
            gaussZeidel.startAlgorithm();
        }catch (Exception e){
            System.out.println("Некорректные данные");
        }
    }


    public static boolean ensureDiagonalDominance(double[][] arguments, int n) {
        // Проверяем исходное состояние
        if (isDiagonallyDominant(arguments, n)) {
            return true;
        }

        // Генерируем массив порядка строк
        int[] rowOrder = new int[n];
        for (int i = 0; i < n; i++) {
            rowOrder[i] = i;
        }

        // Пробуем все перестановки строк
        return permuteAndCheck(arguments, rowOrder, 0, n);
    }


    private static boolean isDiagonallyDominant(double[][] arguments, int n) {
        for (int i = 0; i < n; i++) {
            double diagonal = Math.abs(arguments[i][i]);
            double offDiagonalSum = 0;
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    offDiagonalSum += Math.abs(arguments[i][j]);
                }
            }
            if (diagonal <= offDiagonalSum) {
                return false;
            }
        }
        return true;
    }


    private static boolean permuteAndCheck(double[][] arguments, int[] rowOrder, int start, int n) {
        if (start == n) {
            // Создаем временную матрицу с текущей перестановкой
            double[][] temp = new double[n][n + 1];
            for (int i = 0; i < n; i++) {
                temp[i] = arguments[rowOrder[i]].clone();
            }
            if (isDiagonallyDominant(temp, n)) {
                // Если диагональное преобладание достигнуто, обновляем исходную матрицу
                for (int i = 0; i < n; i++) {
                    arguments[i] = temp[i].clone();
                }
                return true;
            }
            return false;
        }

        // Перебираем все возможные перестановки
        for (int i = start; i < n; i++) {
            swap(rowOrder, start, i);
            if (permuteAndCheck(arguments, rowOrder, start + 1, n)) {
                return true;
            }
            swap(rowOrder, start, i); // Возвращаем обратно
        }
        return false;
    }


    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }


    private static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.print(val + " ");
            }
            System.out.println();
        }
    }
}