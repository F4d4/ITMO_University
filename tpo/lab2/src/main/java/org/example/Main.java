package org.example;

import functions.base.LnFunction;
import functions.base.SinFunction;
import functions.log.Log10Function;
import functions.log.Log2Function;
import functions.log.Log3Function;
import functions.trig.CosFunction;
import functions.trig.CotFunction;
import functions.trig.TanFunction;
import org.itmo.CsvWriter;
import org.itmo.FunctionSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final double EPSILON = 1e-9;
    private static final String SEPARATOR = ",";
    private static final Path OUTPUT_DIR = Path.of("data");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        SinFunction   sin   = new SinFunction();
        LnFunction    ln    = new LnFunction();
        CosFunction   cos   = new CosFunction(sin);
        TanFunction   tan   = new TanFunction(sin, cos);
        CotFunction   cot   = new CotFunction(sin, cos);
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);

        FunctionSystem system = new FunctionSystem(cot, tan, log2, log3, log10);
        CsvWriter csvWriter = new CsvWriter();

        // Система функций: полный диапазон, покрывающий обе ветви (x <= 0 и x > 0)
        csvWriter.write(OUTPUT_DIR.resolve("system.csv").toString(), -3.0, 10.0, 0.1,
                x -> system.compute(x, EPSILON), SEPARATOR);
        System.out.println("data/system.csv written (x from -3 to 10, step 0.1)");

        // Отдельные модули
        csvWriter.write(OUTPUT_DIR.resolve("sin.csv").toString(),   -6.3, 6.3,  0.1, x -> sin.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("ln.csv").toString(),     0.1, 10.0, 0.1, x -> ln.compute(x, EPSILON),    SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("cos.csv").toString(),   -6.3, 6.3,  0.1, x -> cos.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("tan.csv").toString(),   -1.5, 1.5,  0.1, x -> tan.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("cot.csv").toString(),    0.1, 3.0,  0.1, x -> cot.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("log2.csv").toString(),   0.1, 10.0, 0.1, x -> log2.compute(x, EPSILON),  SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("log3.csv").toString(),   0.1, 10.0, 0.1, x -> log3.compute(x, EPSILON),  SEPARATOR);
        csvWriter.write(OUTPUT_DIR.resolve("log10.csv").toString(),  0.1, 10.0, 0.1, x -> log10.compute(x, EPSILON), SEPARATOR);

        System.out.println("All module CSV files written to data/.");
    }
}
