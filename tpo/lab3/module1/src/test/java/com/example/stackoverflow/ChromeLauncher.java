package com.example.stackoverflow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Starts a regular (not Selenium-managed) Chrome with --remote-debugging-port so
 * that Selenium can attach via CDP. The point is to keep the browser process
 * identical to a hand-launched Chrome — Cloudflare lets it through, unlike a
 * Selenium-launched Chrome whose fingerprint triggers the loop challenge.
 *
 * Each instance uses a persistent --user-data-dir per user so the cookies,
 * Cloudflare clearance and login state survive between test runs.
 */
final class ChromeLauncher {
    private final Process process;
    private final int port;
    private final Path userDataDir;

    private ChromeLauncher(Process process, int port, Path userDataDir) {
        this.process = process;
        this.port = port;
        this.userDataDir = userDataDir;
    }

    int port() {
        return port;
    }

    String debuggerAddress() {
        return "127.0.0.1:" + port;
    }

    void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static ChromeLauncher launch(String userKey, int port) throws IOException {
        String exe = chromeExecutable();
        Path userDataDir = persistentProfileDir(userKey);
        Files.createDirectories(userDataDir);

        List<String> command = new ArrayList<>();
        command.add(exe);
        command.add("--remote-debugging-port=" + port);
        command.add("--user-data-dir=" + userDataDir.toAbsolutePath());
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-features=Translate");
        command.add("--disable-popup-blocking");
        command.add("https://stackoverflow.com/");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();
        waitForPort("127.0.0.1", port, 20_000);
        return new ChromeLauncher(process, port, userDataDir);
    }

    private static Path persistentProfileDir(String userKey) {
        String override = System.getProperty("chrome.profileDir." + userKey, "").trim();
        if (!override.isEmpty() && !override.startsWith("${")) {
            return Paths.get(override);
        }
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, ".so-test-chrome", userKey);
    }

    private static String chromeExecutable() {
        String override = System.getProperty("chrome.binary", "").trim();
        if (!override.isEmpty() && !override.startsWith("${")) {
            return override;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
        }
        if (os.contains("win")) {
            return "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
        }
        return "google-chrome";
    }

    private static void waitForPort(String host, int port, long timeoutMillis) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 500);
                return;
            } catch (IOException exception) {
                last = exception;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for Chrome port " + port, interrupted);
                }
            }
        }
        throw new IOException("Chrome remote-debugging port " + port + " did not open in " + timeoutMillis + "ms", last);
    }
}
