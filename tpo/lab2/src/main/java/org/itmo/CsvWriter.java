package org.itmo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.DoubleUnaryOperator;

/**
 * Writes function values to a CSV file in the format: X,f(X).
 * Undefined values (ArithmeticException) are recorded as "undefined".
 */
public class CsvWriter {

    public void write(String filename, double from, double to, double step,
                      DoubleUnaryOperator function, String separator) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("x" + separator + "f(x)");
            for (double x = from; x <= to + step / 2; x += step) {
                try {
                    double y = function.applyAsDouble(x);
                    writer.printf("%.6f%s%.6f%n", x, separator, y);
                } catch (ArithmeticException e) {
                    writer.printf("%.6f%sundefined%n", x, separator);
                }
            }
        }
    }
}
