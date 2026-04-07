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

    private static final double EPSILON   = 1e-9;
    private static final String SEPARATOR = ",";
    private static final Path   OUTPUT_DIR = Path.of("data");

    private static final double FROM = -3.0;
    private static final double TO   = 10.0;
    private static final double STEP = 0.1;

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

        csvWriter.write(out("system.csv"), FROM, TO, STEP, x -> system.compute(x, EPSILON), SEPARATOR);
        csvWriter.write(out("sin.csv"),    FROM, TO, STEP, x -> sin.compute(x, EPSILON),    SEPARATOR);
        csvWriter.write(out("cos.csv"),    FROM, TO, STEP, x -> cos.compute(x, EPSILON),    SEPARATOR);
        csvWriter.write(out("tan.csv"),    FROM, TO, STEP, x -> tan.compute(x, EPSILON),    SEPARATOR);
        csvWriter.write(out("cot.csv"),    FROM, TO, STEP, x -> cot.compute(x, EPSILON),    SEPARATOR);
        csvWriter.write(out("ln.csv"),     FROM, TO, STEP, x -> ln.compute(x, EPSILON),     SEPARATOR);
        csvWriter.write(out("log2.csv"),   FROM, TO, STEP, x -> log2.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(out("log3.csv"),   FROM, TO, STEP, x -> log3.compute(x, EPSILON),   SEPARATOR);
        csvWriter.write(out("log10.csv"),  FROM, TO, STEP, x -> log10.compute(x, EPSILON),  SEPARATOR);

        System.out.printf("All CSV files written to data/ (x from %.1f to %.1f, step %.1f)%n", FROM, TO, STEP);
    }

    private static String out(String filename) {
        return OUTPUT_DIR.resolve(filename).toString();
    }
}
