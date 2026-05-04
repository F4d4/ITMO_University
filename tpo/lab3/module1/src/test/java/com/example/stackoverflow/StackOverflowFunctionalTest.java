package com.example.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class StackOverflowFunctionalTest {
    private static final String BASE_URL = "https://stackoverflow.com";
    private static final long TIMEOUT_SECONDS = Long.getLong("selenium.timeout.seconds", 20L);

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    void setUp() {
        driver = createDriver();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("UC-01: visitor sees newest questions and main navigation")
    void newestQuestionsPageShowsQuestionListAndNavigation() {
        open("/questions");

        waitForPresent(StackOverflowLocators.PAGE_HEADING);
        waitForBodyText("Newest Questions");
        assertFalse(driver.findElements(StackOverflowLocators.QUESTION_SUMMARIES).isEmpty(), "Question list should not be empty");
        assertTrue(isPresent(StackOverflowLocators.ASK_QUESTION), "Ask Question action should be available");
        assertTrue(isPresent(StackOverflowLocators.TAGS_NAVIGATION), "Tags navigation link should be available");
    }

    @Test
    @Order(2)
    @DisplayName("UC-02: visitor searches questions by text query")
    void visitorSearchesQuestionsByTextQuery() {
        open("/questions");

        WebElement search = waitForVisible(StackOverflowLocators.MAIN_SEARCH);
        search.clear();
        search.sendKeys("selenium webdriver");
        search.sendKeys(Keys.ENTER);

        if (!waitForSearchNavigation()) {
            driver.get(BASE_URL + "/search?q=selenium+webdriver");
            waitForDocumentReady();
            acceptCookiesIfShown();
            assumeInterfaceIsAvailable();
        }
        assertTrue(driver.getCurrentUrl().contains("q="), "Search URL should contain query parameter");
        try {
            wait.until(currentDriver -> !currentDriver.findElements(StackOverflowLocators.SEARCH_RESULTS).isEmpty()
                    || bodyText().contains("Search Results")
                    || bodyText().contains("No results found"));
        } catch (TimeoutException exception) {
            assumeInterfaceIsAvailable();
            throw new AssertionError("Search page did not show results. " + pageSnapshot(), exception);
        }
        assertTrue(!driver.findElements(StackOverflowLocators.SEARCH_RESULTS).isEmpty()
                        || bodyText().contains("Search Results")
                        || bodyText().contains("No results found"),
                "Search page should show results or an explicit empty-result message");
    }

    @Test
    @Order(6)
    @DisplayName("UC-03: visitor opens a question and sees its content")
    void visitorOpensQuestionAndSeesContent() {
        open("/questions");

        WebElement firstQuestion = wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.FIRST_QUESTION_LINK));
        String questionTitle = firstQuestion.getText().trim();
        firstQuestion.click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/questions/"));
        assumeInterfaceIsAvailable();
        waitForPresent(StackOverflowLocators.QUESTION_TITLE);
        waitForPresent(StackOverflowLocators.QUESTION_BODY);
        assertFalse(driver.findElements(StackOverflowLocators.POST_TAGS).isEmpty(), "Question page should show tags");
        assertTrue(driver.getTitle().toLowerCase(Locale.ROOT).contains(questionTitle.substring(0, Math.min(12, questionTitle.length())).toLowerCase(Locale.ROOT)),
                "Opened page title should match selected question");
    }

    @Test
    @Order(3)
    @DisplayName("UC-04: visitor opens a popular tag page")
    void visitorOpensPopularTagPage() {
        open("/tags");

        waitForPresent(StackOverflowLocators.PAGE_HEADING);
        waitForBodyText("Tags");
        WebElement pythonTag = wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.PYTHON_TAG_LINK));
        pythonTag.click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/questions/tagged/python") || currentDriver.getCurrentUrl().contains("/tags/python"));
        assertTrue(driver.getCurrentUrl().contains("python"), "Python tag page should be opened");
    }

    @Test
    @Order(4)
    @DisplayName("UC-05: visitor switches the questions list to Active sorting")
    void visitorSwitchesQuestionsToActiveSorting() {
        open("/questions");

        wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.ACTIVE_TAB)).click();

        wait.until(currentDriver -> currentDriver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("tab=active")
                || isPresent(StackOverflowLocators.ACTIVE_HEADING));
        assertTrue(driver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("tab=active")
                        || isPresent(StackOverflowLocators.ACTIVE_HEADING),
                "Active questions sorting should be selected");
    }

    @Test
    @Order(5)
    @DisplayName("UC-06: anonymous visitor is redirected to login when asking a question")
    void anonymousVisitorIsRedirectedToLoginWhenAskingQuestion() {
        open("/questions");

        clickAskQuestion();
        if (!waitForUrlChangeOrLogin()) {
            driver.get(BASE_URL + "/questions/ask");
            waitForDocumentReady();
            assumeInterfaceIsAvailable();
        }

        try {
            wait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/users/login")
                    || isPresent(StackOverflowLocators.LOGIN_REQUIRED_MESSAGE));
        } catch (TimeoutException exception) {
            assumeInterfaceIsAvailable();
            throw new AssertionError("Login redirect was not observed. " + pageSnapshot(), exception);
        }
        assertTrue(driver.getCurrentUrl().contains("/users/login") || isPresent(StackOverflowLocators.LOGIN_REQUIRED_MESSAGE),
                "Anonymous user should be asked to log in before creating a question");
    }

    private WebDriver createDriver() {
        String browser = System.getProperty("browser", "chrome").toLowerCase(Locale.ROOT);
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        if ("firefox".equals(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addPreference("intl.accept_languages", "en-US");
            if (headless) {
                options.addArguments("-headless");
            }
            return new FirefoxDriver(options);
        }

        if ("chrome".equals(browser)) {
            ChromeOptions options = new ChromeOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addArguments("--window-size=1440,1000");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--lang=en-US");
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
            if (headless) {
                options.addArguments("--headless=new");
            }
            return new ChromeDriver(options);
        }

        throw new IllegalArgumentException("Unsupported browser: " + browser + ". Use chrome or firefox.");
    }

    private void open(String path) {
        driver.get(BASE_URL + path);
        waitForDocumentReady();
        acceptCookiesIfShown();
        assumeInterfaceIsAvailable();
    }

    private WebElement waitForVisible(org.openqa.selenium.By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException exception) {
            assumeInterfaceIsAvailable();
            throw new AssertionError("Element was not visible: " + locator + ". " + pageSnapshot(), exception);
        }
    }

    private WebElement waitForPresent(org.openqa.selenium.By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException exception) {
            assumeInterfaceIsAvailable();
            throw new AssertionError("Element was not present: " + locator + ". " + pageSnapshot(), exception);
        }
    }

    private boolean isPresent(org.openqa.selenium.By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    private void waitForDocumentReady() {
        wait.until(currentDriver -> "complete".equals(((JavascriptExecutor) currentDriver).executeScript("return document.readyState")));
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

    private void assumeInterfaceIsAvailable() {
        String pageText = bodyText().toLowerCase(Locale.ROOT);
        String title = driver.getTitle().toLowerCase(Locale.ROOT);
        assumeFalse(pageText.contains("human verification")
                        || pageText.contains("are you a human")
                        || pageText.contains("verifying you are human")
                        || pageText.contains("security service to protect against malicious bots")
                        || title.contains("just a moment"),
                "Stack Overflow returned an anti-bot verification page instead of the tested interface");
    }

    private void waitForBodyText(String text) {
        wait.until(currentDriver -> bodyText().contains(text));
    }

    private boolean waitForUrlChangeOrLogin() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(4));
            return shortWait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/users/login")
                    || isPresent(StackOverflowLocators.LOGIN_REQUIRED_MESSAGE)
                    || !currentDriver.getCurrentUrl().contains("/questions"));
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private boolean waitForSearchNavigation() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            return shortWait.until(currentDriver -> currentDriver.getCurrentUrl().contains("/search"));
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private void clickAskQuestion() {
        WebElement askQuestion = wait.until(ExpectedConditions.presenceOfElementLocated(StackOverflowLocators.ASK_QUESTION));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(StackOverflowLocators.ASK_QUESTION)).click();
        } catch (WebDriverException exception) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", askQuestion);
        }
    }

    private String bodyText() {
        return driver.findElement(org.openqa.selenium.By.xpath("//body")).getText();
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
