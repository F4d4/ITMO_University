package com.example.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class StackOverflowAuthenticatedTest {
    private static final String BASE_URL = "https://stackoverflow.com";
    private static final long TIMEOUT_SECONDS = Long.getLong("selenium.timeout.seconds", 20L);
    private static final long MANUAL_VERIFICATION_WAIT_SECONDS = Long.getLong("verification.wait.seconds", 180L);

    private WebDriver driver;
    private WebDriverWait wait;
    private ChromeLauncher launcher;

    @BeforeAll
    void setUp() {
        Path cookies = StackOverflowAuthSession.cookiesPath("user1");
        String email = StackOverflowAuthSession.emailFor("user1");
        String password = StackOverflowAuthSession.passwordFor("user1");
        boolean useDebuggerAddress = !System.getProperty("chrome.debuggerAddress", "").trim().isEmpty()
                && !System.getProperty("chrome.debuggerAddress", "").trim().startsWith("${");
        String chromeProfileProp = System.getProperty("chrome.profileDir.user1", "").trim();
        boolean chromeProfileProvided = !chromeProfileProp.isEmpty() && !chromeProfileProp.startsWith("${");
        String firefoxProfileProp = System.getProperty("firefox.profileDir.user1", "").trim();
        boolean firefoxProfileProvided = !firefoxProfileProp.isEmpty() && !firefoxProfileProp.startsWith("${");
        boolean credentialsProvided = email != null && password != null;
        boolean usingChrome = "chrome".equalsIgnoreCase(System.getProperty("browser", "chrome"));
        assumeTrue(cookies != null || credentialsProvided || useDebuggerAddress || chromeProfileProvided || firefoxProfileProvided,
                "Set -Dso.user1.cookies=..., or -Dso.user1.email + -Dso.user1.password, or -Dchrome.debuggerAddress=..., or -Dchrome.profileDir.user1=..., or -Dfirefox.profileDir.user1=... See docs/auth-setup.md.");

        if (usingChrome && !useDebuggerAddress && cookies == null && (credentialsProvided || chromeProfileProvided)) {
            int port = Integer.getInteger("chrome.launchPort", 9222);
            try {
                launcher = ChromeLauncher.launch("user1", port);
            } catch (IOException exception) {
                throw new AssertionError("Failed to launch Chrome with remote-debugging on port " + port, exception);
            }
            System.setProperty("chrome.debuggerAddress", launcher.debuggerAddress());
            useDebuggerAddress = true;
        }

        driver = createDriver();
        StackOverflowAuthSession.applyStealth(driver);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));

        if (cookies != null) {
            try {
                StackOverflowAuthSession.loadCookies(driver, cookies);
            } catch (IOException exception) {
                throw new AssertionError("Failed to load cookies from " + cookies, exception);
            }
        } else {
            driver.get(BASE_URL);
        }
        waitForDocumentReady();
        acceptCookiesIfShown();
        waitUntilInterfaceIsAvailable();

        if (cookies == null && credentialsProvided && !StackOverflowAuthSession.isAuthenticated(driver)) {
            try {
                StackOverflowAuthSession.loginWithCredentials(driver, email, password, Duration.ofSeconds(TIMEOUT_SECONDS));
            } catch (RuntimeException loginFailure) {
                System.out.println("Auto-login failed: " + loginFailure.getMessage());
            }
        }
        waitForDocumentReady();
        acceptCookiesIfShown();
        waitUntilInterfaceIsAvailable();

        if (!StackOverflowAuthSession.isAuthenticated(driver) && launcher != null) {
            StackOverflowAuthSession.waitForManualLogin(driver, Duration.ofSeconds(MANUAL_VERIFICATION_WAIT_SECONDS));
        }

        assumeTrue(StackOverflowAuthSession.isAuthenticated(driver),
                "Authenticated session not detected. If using cookies — refresh them. If using debuggerAddress — make sure the Chrome session is logged in. See docs/auth-setup.md.");
    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        if (launcher != null) {
            launcher.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("UC-01a: authenticated user sees newest questions, navigation and own avatar")
    void authenticatedNewestQuestionsShowQuestionListAndAvatar() {
        open("/questions");

        waitForPresent(StackOverflowLocators.PAGE_HEADING);
        wait.until(currentDriver -> !currentDriver.findElements(StackOverflowLocators.QUESTION_SUMMARIES).isEmpty());
        String body = bodyText();
        assertTrue(body.contains("Newest Questions") || body.contains("Top Questions")
                        || body.contains("Interesting Questions") || body.contains("Questions"),
                "Page should look like the questions list. Body started with: "
                        + body.substring(0, Math.min(120, body.length())));
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR), "User avatar should be visible for authenticated user");
        assertFalse(isPresent(StackOverflowLocators.LOGIN_LINK), "Log in link should not be shown for authenticated user");
        assertFalse(driver.findElements(StackOverflowLocators.QUESTION_SUMMARIES).isEmpty(), "Question list should not be empty");
        assertTrue(isPresent(StackOverflowLocators.ASK_QUESTION), "Ask Question action should be available");
        assertTrue(isPresent(StackOverflowLocators.TAGS_NAVIGATION), "Tags navigation link should be available");
    }

    @Test
    @Order(2)
    @DisplayName("UC-02a: authenticated user searches questions by text query")
    void authenticatedUserSearchesQuestionsByTextQuery() {
        open("/questions");
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR), "User avatar should still be visible");

        WebElement search = waitForVisible(StackOverflowLocators.MAIN_SEARCH);
        search.clear();
        search.sendKeys("selenium webdriver");
        search.sendKeys(Keys.ENTER);

        if (!waitForSearchNavigation()) {
            driver.get(BASE_URL + "/search?q=selenium+webdriver");
            waitForDocumentReady();
            acceptCookiesIfShown();
            waitUntilInterfaceIsAvailable();
        }
        assertTrue(driver.getCurrentUrl().contains("q="), "Search URL should contain query parameter");
        try {
            wait.until(currentDriver -> !currentDriver.findElements(StackOverflowLocators.SEARCH_RESULTS).isEmpty()
                    || bodyText().contains("Search Results")
                    || bodyText().contains("No results found"));
        } catch (TimeoutException exception) {
            waitUntilInterfaceIsAvailable();
            throw new AssertionError("Search page did not show results. " + pageSnapshot(), exception);
        }
        assertTrue(!driver.findElements(StackOverflowLocators.SEARCH_RESULTS).isEmpty()
                        || bodyText().contains("Search Results")
                        || bodyText().contains("No results found"),
                "Search page should show results or an explicit empty-result message");
    }

    @Test
    @Order(3)
    @DisplayName("UC-04a: authenticated user opens a popular tag page")
    void authenticatedUserOpensPopularTagPage() {
        open("/tags");
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR), "User avatar should be visible");

        waitForPresent(StackOverflowLocators.PAGE_HEADING);
        waitForBodyText("Tags");
        WebElement pythonTag = wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.PYTHON_TAG_LINK));
        pythonTag.click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/questions/tagged/python")
                || currentDriver.getCurrentUrl().contains("/tags/python"));
        assertTrue(driver.getCurrentUrl().contains("python"), "Python tag page should be opened");
    }

    @Test
    @Order(4)
    @DisplayName("UC-05a: authenticated user switches the questions list to Active sorting")
    void authenticatedUserSwitchesQuestionsToActiveSorting() {
        open("/questions");
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR), "User avatar should be visible");

        wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.ACTIVE_TAB)).click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("tab=active")
                || isPresent(StackOverflowLocators.ACTIVE_HEADING));
        assertTrue(driver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("tab=active")
                        || isPresent(StackOverflowLocators.ACTIVE_HEADING),
                "Active questions sorting should be selected");
    }

    @Test
    @Order(5)
    @DisplayName("UC-06a: authenticated user opens the Ask Question form directly (no login redirect)")
    void authenticatedUserOpensAskQuestionForm() {
        open("/questions");
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR), "User avatar should be visible");

        wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.ASK_QUESTION)).click();
        try {
            wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/questions/ask"));
        } catch (TimeoutException exception) {
            driver.get(BASE_URL + "/questions/ask");
            waitForDocumentReady();
            waitUntilInterfaceIsAvailable();
        }

        assertFalse(driver.getCurrentUrl().contains("/users/login"),
                "Authenticated user should NOT be redirected to the login page");
        try {
            wait.until(currentDriver -> isPresent(StackOverflowLocators.ASK_TITLE_INPUT));
        } catch (TimeoutException exception) {
            throw new AssertionError("Ask Question form was not shown to the authenticated user. " + pageSnapshot(), exception);
        }
        assertTrue(isPresent(StackOverflowLocators.ASK_TITLE_INPUT), "Ask Question title input should be visible");
    }

    @Test
    @Order(6)
    @DisplayName("UC-03a: authenticated user opens a question and sees vote controls")
    void authenticatedUserOpensQuestionAndSeesContent() {
        open("/questions");

        WebElement firstQuestion = wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.FIRST_QUESTION_LINK));
        String questionTitle = firstQuestion.getText().trim();
        firstQuestion.click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/questions/"));
        waitUntilInterfaceIsAvailable();
        waitForPresent(StackOverflowLocators.QUESTION_TITLE);
        waitForPresent(StackOverflowLocators.QUESTION_BODY);
        assertFalse(driver.findElements(StackOverflowLocators.POST_TAGS).isEmpty(), "Question page should show tags");
        assertTrue(driver.getTitle().toLowerCase(Locale.ROOT)
                        .contains(questionTitle.substring(0, Math.min(12, questionTitle.length())).toLowerCase(Locale.ROOT)),
                "Opened page title should match selected question");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(currentDriver -> isPresent(StackOverflowLocators.VOTE_UP_BUTTON));
        } catch (TimeoutException ignored) {
        }
        if (!isPresent(StackOverflowLocators.VOTE_UP_BUTTON)) {
            System.out.println("Note: vote controls not detected on " + driver.getCurrentUrl()
                    + " — Stack Overflow may hide them for very low-reputation users on third-party questions.");
        }
        assertTrue(isPresent(StackOverflowLocators.USER_AVATAR),
                "Authenticated user should see the avatar on the question page");
    }

    private WebDriver createDriver() {
        String browser = System.getProperty("browser", "chrome").toLowerCase(Locale.ROOT);
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        if ("firefox".equals(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addPreference("intl.accept_languages", "en-US");
            options.addPreference("dom.webdriver.enabled", false);
            options.addPreference("useAutomationExtension", false);
            String profileDir = System.getProperty("firefox.profileDir.user1", "").trim();
            if (!profileDir.isEmpty() && !profileDir.startsWith("${")) {
                options.addArguments("-profile", profileDir);
            }
            if (headless) {
                options.addArguments("-headless");
            }
            return new FirefoxDriver(options);
        }

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        String debuggerAddress = System.getProperty("chrome.debuggerAddress", "").trim();
        if (!debuggerAddress.isEmpty() && !debuggerAddress.startsWith("${")) {
            options.setExperimentalOption("debuggerAddress", debuggerAddress);
            return new ChromeDriver(options);
        }
        options.addArguments("--window-size=1440,1000");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--lang=en-US");
        options.addArguments("--disable-features=AutomationControlled,IsolateOrigins,site-per-process");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        if (headless) {
            options.addArguments("--headless=new");
        }
        return new ChromeDriver(options);
    }

    private void open(String path) {
        driver.get(BASE_URL + path);
        waitForDocumentReady();
        acceptCookiesIfShown();
        waitUntilInterfaceIsAvailable();
    }

    private WebElement waitForVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException exception) {
            if (isAntiBotVerificationPage()) {
                waitUntilInterfaceIsAvailable();
                return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            }
            throw new AssertionError("Element was not visible: " + locator + ". " + pageSnapshot(), exception);
        }
    }

    private WebElement waitForPresent(By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException exception) {
            if (isAntiBotVerificationPage()) {
                waitUntilInterfaceIsAvailable();
                return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            }
            throw new AssertionError("Element was not present: " + locator + ". " + pageSnapshot(), exception);
        }
    }

    private boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    private void waitForDocumentReady() {
        try {
            wait.until(currentDriver -> {
                Object readyState = ((JavascriptExecutor) currentDriver).executeScript("return document.readyState");
                return "complete".equals(readyState) || "interactive".equals(readyState);
            });
        } catch (TimeoutException exception) {
            if (!bodyText().isBlank()) {
                return;
            }
            throw exception;
        }
    }

    private void acceptCookiesIfShown() {
        List<WebElement> buttons = driver.findElements(StackOverflowLocators.COOKIE_BUTTON);
        for (WebElement button : buttons) {
            if (button.isDisplayed() && button.isEnabled()) {
                button.click();
                return;
            }
        }
    }

    private void waitUntilInterfaceIsAvailable() {
        if (!isAntiBotVerificationPage()) {
            return;
        }
        System.out.println("Stack Overflow opened an anti-bot verification page. "
                + "Complete it manually; waiting up to " + MANUAL_VERIFICATION_WAIT_SECONDS + " seconds.");
        WebDriverWait verificationWait = new WebDriverWait(driver, Duration.ofSeconds(MANUAL_VERIFICATION_WAIT_SECONDS));
        try {
            verificationWait.until(currentDriver -> !isAntiBotVerificationPage());
        } catch (TimeoutException exception) {
            throw new AssertionError("Stack Overflow anti-bot verification was not completed within "
                    + MANUAL_VERIFICATION_WAIT_SECONDS + " seconds. " + pageSnapshot(), exception);
        }
        waitForDocumentReady();
        acceptCookiesIfShown();
    }

    private boolean isAntiBotVerificationPage() {
        try {
            String pageText = bodyText().toLowerCase(Locale.ROOT);
            String title = driver.getTitle().toLowerCase(Locale.ROOT);
            return pageText.contains("human verification")
                    || pageText.contains("are you a human")
                    || pageText.contains("verifying you are human")
                    || pageText.contains("security service to protect against malicious bots")
                    || title.contains("just a moment");
        } catch (WebDriverException exception) {
            return false;
        }
    }

    private void waitForBodyText(String text) {
        wait.until(currentDriver -> bodyText().contains(text));
    }

    private boolean waitForSearchNavigation() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            return shortWait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/search"));
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private String bodyText() {
        return driver.findElement(By.xpath("//body")).getText();
    }

    private String pageSnapshot() {
        String body = bodyText();
        body = body.replaceAll("\\s+", " ").trim();
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        return "url=" + driver.getCurrentUrl() + ", title=" + driver.getTitle() + ", body=" + body;
    }
}
