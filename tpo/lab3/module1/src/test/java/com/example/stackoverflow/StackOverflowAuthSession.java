package com.example.stackoverflow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

final class StackOverflowAuthSession {
    private static final String BASE_URL = "https://stackoverflow.com";

    private StackOverflowAuthSession() {
    }

    static Path cookiesPath(String userKey) {
        return propertyAsPath("so." + userKey + ".cookies");
    }

    static String emailFor(String userKey) {
        return propertyAsString("so." + userKey + ".email");
    }

    static String passwordFor(String userKey) {
        return propertyAsString("so." + userKey + ".password");
    }

    private static Path propertyAsPath(String key) {
        String raw = propertyAsString(key);
        return raw == null ? null : Paths.get(raw);
    }

    private static String propertyAsString(String key) {
        String raw = System.getProperty(key, "").trim();
        if (raw.isEmpty() || raw.startsWith("${")) {
            return null;
        }
        return raw;
    }

    /**
     * Wait for a human to log in manually in the browser window. Useful when
     * Cloudflare loops the CAPTCHA against Selenium-driven sessions and the
     * persistent Chrome profile hasn't been "cleared" yet. Once the user signs in
     * (we detect USER_AVATAR), the profile keeps the session for subsequent runs.
     */
    static void waitForManualLogin(WebDriver driver, Duration timeout) {
        long seconds = Math.max(timeout.toSeconds(), 60L);
        System.out.println();
        System.out.println("====================================================================");
        System.out.println(" ACTION REQUIRED: log in manually in the opened Chrome window.");
        System.out.println(" Tests will resume automatically once a user avatar is detected.");
        System.out.println(" Waiting up to " + seconds + " seconds.");
        System.out.println(" This is a one-time step per Chrome profile — next runs are silent.");
        System.out.println("====================================================================");
        System.out.println();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        try {
            wait.until(d -> {
                try {
                    return isAuthenticated(d);
                } catch (RuntimeException ignored) {
                    return false;
                }
            });
        } catch (TimeoutException exception) {
            throw new AssertionError("Manual login was not completed within " + seconds + " seconds.", exception);
        }
    }

    /**
     * Login via the visible /users/login form. Stack Overflow has no reCAPTCHA on the
     * first login attempt, so this works for fresh sessions; subsequent suspicious
     * activity may still trigger one. Use cookies-based loading whenever possible.
     */
    static void loginWithCredentials(WebDriver driver, String email, String password, Duration timeout) {
        driver.get(BASE_URL + "/users/login");
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(StackOverflowLocators.LOGIN_FORM_EMAIL));
        emailInput.clear();
        emailInput.sendKeys(email);
        WebElement passwordInput = driver.findElement(StackOverflowLocators.LOGIN_FORM_PASSWORD);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        driver.findElement(StackOverflowLocators.LOGIN_FORM_SUBMIT).click();
        try {
            wait.until(d -> !d.getCurrentUrl().contains("/users/login")
                    || !d.findElements(StackOverflowLocators.USER_AVATAR).isEmpty());
        } catch (TimeoutException ignored) {
        }
    }

    static void loadCookies(WebDriver driver, Path cookiesFile) throws IOException {
        if (cookiesFile == null) {
            throw new IllegalArgumentException("cookiesFile must not be null");
        }
        if (!Files.isRegularFile(cookiesFile)) {
            throw new IOException("Cookies file not found: " + cookiesFile.toAbsolutePath());
        }

        driver.get(BASE_URL);
        driver.manage().deleteAllCookies();

        String json = Files.readString(cookiesFile);
        JsonElement root = JsonParser.parseString(json);
        JsonArray entries = root.isJsonArray() ? root.getAsJsonArray()
                : root.getAsJsonObject().getAsJsonArray("cookies");

        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            Cookie cookie = buildCookie(entry);
            if (cookie == null) {
                continue;
            }
            try {
                driver.manage().addCookie(cookie);
            } catch (RuntimeException ignored) {
            }
        }

        driver.navigate().refresh();
    }

    static boolean isAuthenticated(WebDriver driver) {
        boolean avatar = !driver.findElements(StackOverflowLocators.USER_AVATAR).isEmpty();
        boolean loginLink = !driver.findElements(StackOverflowLocators.LOGIN_LINK).isEmpty();
        return avatar && !loginLink;
    }

    /**
     * Best-effort stealth: hide common Selenium / CDP fingerprints from the page
     * (navigator.webdriver, automation flags). Cloudflare's bot detection is
     * fingerprint-based, and overriding these via CDP's
     * Page.addScriptToEvaluateOnNewDocument silences the loudest signals.
     * Does NOT guarantee bypass — Cloudflare has many other signals.
     */
    static void applyStealth(WebDriver driver) {
        if (!(driver instanceof org.openqa.selenium.chromium.ChromiumDriver)) {
            return;
        }
        String script = "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });"
                + "window.chrome = window.chrome || { runtime: {} };"
                + "Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });"
                + "Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("source", script);
        try {
            ((org.openqa.selenium.chromium.ChromiumDriver) driver)
                    .executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);
        } catch (RuntimeException ignored) {
        }
    }

    private static Cookie buildCookie(JsonObject entry) {
        String name = optString(entry, "name");
        String value = optString(entry, "value");
        if (name == null || value == null) {
            return null;
        }
        String domain = normalizeDomain(optString(entry, "domain"));
        String path = optString(entry, "path");
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        Date expiry = readExpiry(entry);
        boolean secure = optBoolean(entry, "secure", false);
        boolean httpOnly = optBoolean(entry, "httpOnly", false);
        String sameSite = optString(entry, "sameSite");

        Cookie.Builder builder = new Cookie.Builder(name, value)
                .path(path)
                .isSecure(secure)
                .isHttpOnly(httpOnly);
        if (domain != null) {
            builder.domain(domain);
        }
        if (expiry != null) {
            builder.expiresOn(expiry);
        }
        if (sameSite != null && !sameSite.isEmpty() && !"unspecified".equalsIgnoreCase(sameSite)) {
            try {
                builder.sameSite(capitalize(sameSite));
            } catch (RuntimeException ignored) {
            }
        }
        return builder.build();
    }

    private static String normalizeDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        if (domain.equalsIgnoreCase("stackoverflow.com")
                || domain.equalsIgnoreCase(".stackoverflow.com")
                || domain.endsWith(".stackoverflow.com")
                || domain.endsWith(".stackexchange.com")
                || domain.endsWith(".stackauth.com")) {
            return domain;
        }
        return domain;
    }

    private static Date readExpiry(JsonObject entry) {
        if (entry.has("expirationDate") && entry.get("expirationDate").isJsonPrimitive()) {
            double seconds = entry.get("expirationDate").getAsDouble();
            return new Date((long) (seconds * 1000L));
        }
        if (entry.has("expiry") && entry.get("expiry").isJsonPrimitive()) {
            long seconds = entry.get("expiry").getAsLong();
            return new Date(seconds * 1000L);
        }
        if (entry.has("expires") && entry.get("expires").isJsonPrimitive()) {
            long seconds = entry.get("expires").getAsLong();
            if (seconds > 0L) {
                return new Date(seconds * 1000L);
            }
        }
        return null;
    }

    private static String optString(JsonObject entry, String key) {
        if (!entry.has(key) || entry.get(key).isJsonNull()) {
            return null;
        }
        return entry.get(key).getAsString();
    }

    private static boolean optBoolean(JsonObject entry, String key, boolean fallback) {
        if (!entry.has(key) || entry.get(key).isJsonNull()) {
            return fallback;
        }
        return entry.get(key).getAsBoolean();
    }

    private static String capitalize(String value) {
        if (value.length() <= 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
