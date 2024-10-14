package com.f4d4.server;

import com.fastcgi.FCGIInterface;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class App {

    private static final String RESPONSE_TEMPLATE = "Content-Type: application/json\nContent-Length: %d\n\n%s";

    public static void main(String[] args) throws IOException {
        FCGIInterface fcgi = new FCGIInterface();
        while (fcgi.FCGIaccept() >= 0) {
            long startedAt = System.nanoTime();
            try {
                String queryString = (String) FCGIInterface.request.params.get("QUERY_STRING");

                Map<String, String> queryParams = parseQueryString(queryString);

                String validationError = validateParameters(queryParams);
                if (validationError != null) {
                    sendJson(new JSONObject().put("error", validationError).toString());
                    continue;
                }

                double x = Double.parseDouble(queryParams.get("x"));
                double y = Double.parseDouble(queryParams.get("y"));
                double r = Double.parseDouble(queryParams.get("r"));

                boolean isInside = calculate(x, y, r);
                long endedAt = System.nanoTime();


                String jsonResponse = new JSONObject()
                        .put("result", isInside)
                        .put("x", x)
                        .put("y", y)
                        .put("r", r)
                        .put("currentTime",LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy ")) )
                        .put("executionTime",(endedAt-startedAt)/1000  + "micsec")
                        .toString();
                sendJson(jsonResponse);
            } catch (Exception e) {
                sendJson(new JSONObject().put("error", e.getMessage()).toString());
            }
        }
    }

    private static boolean calculate(double x, double y, double r) {
        if (x >= 0 && y >= 0 && x <= r/2 && y <= r) {
            return true;
        }
        if (x <= 0 && y >= 0 && x>=-r/2 && y<=r && y-2*x-r<=0) {
            return true;
        }
        if (x >= 0 && y <= 0 && y>-r/2 && x<=r/2 && Math.pow(x,2)+Math.pow(y,2)<=Math.pow(r/2,2) ) {
            return true;
        }
        return false;
    }


    private static void sendJson(String jsonDump) {
        System.out.printf(RESPONSE_TEMPLATE + "%n", jsonDump.getBytes(StandardCharsets.UTF_8).length, jsonDump);
    }


    private static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> queryParams = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                queryParams.put(keyValue[0], keyValue[1]);
            }
        }
        return queryParams;
    }

    private static String validateParameters(Map<String, String> queryParams) {
        String xParam = queryParams.get("x");
        String yParam = queryParams.get("y");
        String rParam = queryParams.get("r");

        try {
            double x = Double.parseDouble(xParam);
            // Массив допустимых значений
            double[] validXValues = {-2, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2};

            // Проверяем, содержится ли введенное значение в массиве допустимых значений
            boolean isValid = false;
            for (double validValue : validXValues) {
                if (x == validValue) {
                    isValid = true;
                    break;
                }
            }

            // Если значение не найдено в массиве, возвращаем сообщение об ошибке
            if (!isValid) {
                return "Значение X должно быть одним из следующих: -2, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2";
            }

        } catch (NumberFormatException e) {
            return "Значение X некорректное";
        }


        try {
            double y = Double.parseDouble(yParam);
            if (y < -5 || y > 3) {
                return "Значение Y должно быть в диапазоне от -5 до 3";
            }
        } catch (NumberFormatException e) {
            return "Значение Y некорректное";
        }

        try {
            double r = Double.parseDouble(rParam);
            if (r < 2 || r > 5) {
                return "Значение R должно быть в диапазоне от 2 до 5";
            }
        } catch (NumberFormatException e) {
            return "Значение R некорректное";
        }

        // Если все проверки пройдены, возвращаем null (ошибок нет)
        return null;
    }
}
