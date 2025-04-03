package org.example;

import org.w3c.dom.ls.LSOutput;

import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class GaussZeidel {
    private double[][] arguments;

    private boolean isSolved = false;
    private int size;

    private double marginOfError;

    public GaussZeidel(double[][] arguments, int size, double marginOfError) {
        this.arguments = arguments;
        this.size = size;
        this.marginOfError = marginOfError;
    }

    public void startAlgorithm() {
        double[][] permuted = permute(arguments, size);
        norma(permuted,size);
        double[] x = new double[size];
        for(int i =0 ; i<size ; i++){
            x[i] = permuted[i][permuted.length-1];
        }
        double[] x_old = new double[size];
        int maxIterations = 100;
        boolean converged = false;
        int iterationCounter =0;

         while(!converged){
             iterationCounter+=1;
            System.arraycopy(x, 0, x_old, 0, size);
            for (int i = 0; i < size; i++) {
                double sum = 0;
                int index = 0;
                for (int j = 0; j < size; j++) {
                    if (j != i) {
                        if (j < i) {
                            sum += permuted[i][index] * x[j];
                        } else {
                            sum += permuted[i][index] * x_old[j];
                        }
                        index++;
                    }

                }
                x[i] = sum + permuted[i][size - 1]; // Добавляем константу
            }

            double maxDiff = 0;
            for (int i = 0; i < size; i++) {
                double diff = Math.abs(x[i] - x_old[i]);
                if (diff > maxDiff) {
                    maxDiff = diff;
                }
            }
             DecimalFormat df = new DecimalFormat("#.####" , new DecimalFormatSymbols(Locale.US));
             System.out.println("Вектор погрешностей для итерации N"+iterationCounter+" = " + df.format(maxDiff));

            if (maxDiff < marginOfError) {
                converged = true;
            }
        }
        System.out.println("Общее количество итераций = " + iterationCounter);

        if (converged) {
            isSolved = true;
            System.out.println("Решение найдено:");
            for (int i = 0; i < size; i++) {
                System.out.println("x" + (i + 1) + " = " + x[i]);
            }
            System.out.println("Векторы невязки: ");
            for ( int i =0 ; i< size ; i++){
                double sumFonev = 0;
                for(int j =0 ; j< size ; j++){
                    sumFonev+=arguments[i][j]*x[j];
                }
                System.out.println(sumFonev - arguments[i][size]);
            }
        } else {
            System.out.println("Не сошлось за " + maxIterations + " итераций.");
        }
    }

    public double[][] permute(double[][] originalArray, int sizeOfMatrix) {
        double[][] permuted = new double[sizeOfMatrix][sizeOfMatrix];

        double[] values = new double[sizeOfMatrix];

        double[][] temporary = new double[sizeOfMatrix][sizeOfMatrix];
        for(int i =0  ; i<sizeOfMatrix ; i++){
            int indexToRemove = i;
            for(int j=0,k=0; j<originalArray[i].length; j++){
                if(j!=indexToRemove){
                    if(j!=sizeOfMatrix){
                        temporary[i][k] = originalArray[i][j]*(-1);
                        k++;
                    }else{
                        temporary[i][k] = originalArray[i][j];
                        k++;
                    }
                }else{
                    values[i] = originalArray[i][j];
                }
            }
        }

        DecimalFormat df = new DecimalFormat("#.####" , new DecimalFormatSymbols(Locale.US));
        for(int i =0 ; i<sizeOfMatrix ; i++){
            for(int j = 0 ; j<sizeOfMatrix ; j++){
                permuted[i][j] = Double.parseDouble(df.format(temporary[i][j]/values[i]));
            }
        }


        return permuted;

    }

    public void norma(double [][] matrix , int size){
        double maximum = 0;
        for(int i = 0 ; i<size ; i++){
            double possibleMax =0;
            for(int j =0 ; j<size-1 ; j++){
                possibleMax+= Math.abs(matrix[i][j]);
                if(possibleMax>maximum){
                    maximum = possibleMax;
                }
            }
        }
        System.out.println("Норма матрицы = " + maximum);
    }

    public double[][] getArguments() {
        return arguments;
    }

    public int getSize() {
        return size;
    }

    public double getMarginOfError() {
        return marginOfError;
    }
}
