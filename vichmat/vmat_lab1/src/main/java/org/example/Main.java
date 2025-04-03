package org.example;


import java.io.File;
import java.util.Scanner;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите как вы хотите вводить данные. 1- клавиатура ; 2 - файл :");
        try{
            int userChoice = Integer.valueOf(scanner.nextLine().trim());
            if(userChoice == 2){
                try {
                    System.out.println("Введите название файла:");
                    String filename = scanner.nextLine().trim();
                    Scanner scannerFile = new Scanner(new File(filename));
                    int sizeFromFile = Integer.valueOf(scannerFile.nextLine().trim());
                    System.out.println(sizeFromFile);
                    double[][] argumentsFromFile = new double[sizeFromFile][sizeFromFile+1];
                    for (int i = 0; i < sizeFromFile; i++) {
                        argumentsFromFile[i] = Arrays.stream(scannerFile.nextLine().trim().split(" "))
                                .mapToDouble(Double::parseDouble)
                                .limit(sizeFromFile+1)
                                .toArray();
                    }
                    if (ensureDiagonalDominance(argumentsFromFile, sizeFromFile)) {
                        System.out.println("Матрица приведена к диагональному преобладанию.");
                        System.out.println("Обновленная матрица:");
                        printMatrix(argumentsFromFile);
                    } else {
                        System.out.println("Подходящей перестановки нет.");
                        printMatrix(argumentsFromFile);
                        //System.exit(0);
                    }
                    double marginFromFile = Double.parseDouble(scannerFile.nextLine().trim());
                    GaussZeidel gaussZeidel = new GaussZeidel(argumentsFromFile, sizeFromFile,marginFromFile);
                    gaussZeidel.startAlgorithm();

                    scannerFile.close();
                    System.exit(0);
                } catch (Exception e) {
                    System.err.println("Ошибка чтения файла :  " + e.getMessage());
                    System.exit(0);
                }
            }
            if(userChoice !=1){
                System.out.println("Некорректные данные");
                System.exit(0);
            }
        }catch (Exception e ){
            System.out.println("Некрректные данные");
            System.exit(0);
        }
        try{
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
            if (ensureDiagonalDominance(arguments, size)) {
                System.out.println("Матрица приведена к диагональному преобладанию.");
                System.out.println("Обновленная матрица:");
                printMatrix(arguments);
            } else {
                System.out.println("Подходящей перестановки нет.");
                printMatrix(arguments);
                //System.exit(0);
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
        if (isDiagonallyDominant(arguments, n)) {
            return true;
        }

        // Генерируем массив порядка строк
        int[] rowOrder = new int[n];
        for (int i = 0; i < n; i++) {
            rowOrder[i] = i;
        }

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
            if (diagonal < offDiagonalSum) {
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