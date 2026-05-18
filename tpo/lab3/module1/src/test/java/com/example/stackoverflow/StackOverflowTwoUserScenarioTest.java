package com.example.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
class StackOverflowTwoUserScenarioTest {
    private static final String BASE_URL = "https://stackoverflow.com";
    private static final long TIMEOUT_SECONDS = Long.getLong("selenium.timeout.seconds", 30L);
    private static final long FORM_SUBMIT_WAIT_SECONDS = 45L;

    private WebDriver user1Driver;
    private WebDriver user2Driver;
    private WebDriverWait user1Wait;
    private WebDriverWait user2Wait;
    private ChromeLauncher user1Launcher;
    private ChromeLauncher user2Launcher;

    private static final QuestionTemplate[] TEMPLATES = new QuestionTemplate[] {
            new QuestionTemplate(
                    "How to safely iterate over dict items in Python 3 while mutating?",
                    String.join("\n",
                            "I have a Python 3 dict and need to remove keys during iteration. The naive approach raises",
                            "`RuntimeError: dictionary changed size during iteration`:",
                            "",
                            "```python",
                            "data = {'a': 1, 'b': 2, 'c': 3}",
                            "for key, value in data.items():",
                            "    if value % 2 == 0:",
                            "        del data[key]",
                            "```",
                            "",
                            "What is the idiomatic way to filter a dict in place without copying the whole structure?"),
                    String.join("\n",
                            "Iterate over a snapshot of the keys using `list(data)`:",
                            "",
                            "```python",
                            "for key in list(data):",
                            "    if data[key] % 2 == 0:",
                            "        del data[key]",
                            "```",
                            "",
                            "This creates a temporary list of keys which is cheap compared to copying values.")),
            new QuestionTemplate(
                    "How to handle KeyError when accessing missing dict keys in Python?",
                    String.join("\n",
                            "I need to read values from a dict but missing keys raise `KeyError`:",
                            "",
                            "```python",
                            "stats = {'visits': 5}",
                            "n = stats['unique_visitors']  # KeyError",
                            "```",
                            "",
                            "What is the preferred Pythonic way to return a default when the key is absent?"),
                    String.join("\n",
                            "Use `dict.get` with a default value:",
                            "",
                            "```python",
                            "n = stats.get('unique_visitors', 0)",
                            "```",
                            "",
                            "Or `collections.defaultdict(int)` if you need the same default for many keys.")),
            new QuestionTemplate(
                    "How to merge two lists element-wise in Python 3 without numpy?",
                    String.join("\n",
                            "I have two lists of equal length and want to pair their elements into tuples:",
                            "",
                            "```python",
                            "a = [1, 2, 3]",
                            "b = ['x', 'y', 'z']",
                            "# desired: [(1, 'x'), (2, 'y'), (3, 'z')]",
                            "```",
                            "",
                            "I would rather avoid bringing numpy as a dependency for such a tiny task."),
                    String.join("\n",
                            "Use the built-in `zip`:",
                            "",
                            "```python",
                            "pairs = list(zip(a, b))",
                            "```",
                            "",
                            "`zip` is lazy in Python 3, so wrap it in `list` if you need a concrete sequence.")),
            new QuestionTemplate(
                    "How to count occurrences of substring in a Python string efficiently?",
                    String.join("\n",
                            "I want to count how many times a substring appears in a long string. I tried:",
                            "",
                            "```python",
                            "n = sum(1 for _ in re.finditer('foo', text))",
                            "```",
                            "",
                            "Is there a more idiomatic non-regex approach for plain substring counting?"),
                    String.join("\n",
                            "Use the built-in `str.count`:",
                            "",
                            "```python",
                            "n = text.count('foo')",
                            "```",
                            "",
                            "It scans the string once in C and avoids regex overhead.")),
    };

    private static final QuestionTemplate TEMPLATE = TEMPLATES[new java.util.Random().nextInt(TEMPLATES.length)];

    private final String uniqueMarker = "tpo-2422-" + UUID.randomUUID().toString().substring(0, 8);
    private final String questionTitle = uniqueMarker + ": " + TEMPLATE.title;
    private final String questionBody = TEMPLATE.body + "\n\nMarker for automated test cleanup: " + uniqueMarker;
    private final String answerBody = TEMPLATE.answer + "\n\nMarker: " + uniqueMarker;

    private record QuestionTemplate(String title, String body, String answer) {
    }

    private String questionUrl;

    @BeforeAll
    void setUp() {
        Path user1Cookies = StackOverflowAuthSession.cookiesPath("user1");
        Path user2Cookies = StackOverflowAuthSession.cookiesPath("user2");
        boolean user1HasCreds = StackOverflowAuthSession.emailFor("user1") != null
                && StackOverflowAuthSession.passwordFor("user1") != null;
        boolean user2HasCreds = StackOverflowAuthSession.emailFor("user2") != null
                && StackOverflowAuthSession.passwordFor("user2") != null;
        boolean user1HasProfile = hasProperty("chrome.profileDir.user1") || hasProperty("chrome.debuggerAddress.user1")
                || hasProperty("firefox.profileDir.user1");
        boolean user2HasProfile = hasProperty("chrome.profileDir.user2") || hasProperty("chrome.debuggerAddress.user2")
                || hasProperty("firefox.profileDir.user2");
        assumeTrue((user1Cookies != null || user1HasCreds || user1HasProfile)
                        && (user2Cookies != null || user2HasCreds || user2HasProfile),
                "Provide so.userN.cookies OR so.userN.email + so.userN.password OR chrome.profileDir.userN OR firefox.profileDir.userN for both. See docs/auth-setup.md.");

        boolean usingChrome = "chrome".equalsIgnoreCase(System.getProperty("browser", "chrome"));
        user1Launcher = usingChrome ? ensureChromeFor("user1", 9222, user1Cookies, user1HasCreds) : null;
        user2Launcher = usingChrome ? ensureChromeFor("user2", 9223, user2Cookies, user2HasCreds) : null;

        user1Driver = createDriver("user1");
        user2Driver = createDriver("user2");
        user1Wait = configureDriver(user1Driver);
        user2Wait = configureDriver(user2Driver);

        loadSession(user1Driver, user1Cookies, "user1");
        loadSession(user2Driver, user2Cookies, "user2");
    }

    @AfterAll
    void tearDown() {
        quitQuietly(user1Driver);
        quitQuietly(user2Driver);
        if (user1Launcher != null) {
            user1Launcher.close();
        }
        if (user2Launcher != null) {
            user2Launcher.close();
        }
    }

    private static boolean hasProperty(String key) {
        String raw = System.getProperty(key, "").trim();
        return !raw.isEmpty() && !raw.startsWith("${");
    }

    private ChromeLauncher ensureChromeFor(String userKey, int port, Path cookies, boolean hasCreds) {
        String externalAddress = System.getProperty("chrome.debuggerAddress." + userKey, "").trim();
        if (!externalAddress.isEmpty() && !externalAddress.startsWith("${")) {
            return null;
        }
        String profileDir = System.getProperty("chrome.profileDir." + userKey, "").trim();
        boolean profileProvided = !profileDir.isEmpty() && !profileDir.startsWith("${");
        if (cookies != null) {
            return null;
        }
        if (!hasCreds && !profileProvided) {
            return null;
        }
        try {
            ChromeLauncher launcher = ChromeLauncher.launch(userKey, port);
            System.setProperty("chrome.debuggerAddress." + userKey, launcher.debuggerAddress());
            return launcher;
        } catch (IOException exception) {
            throw new AssertionError("Failed to launch Chrome for " + userKey + " on port " + port, exception);
        }
    }

    @Test
    @DisplayName("UC-X: User1 asks a python question, User2 answers, both posts are deleted")
    void twoUserQuestionAnswerDeleteScenario() {
        try {
            askQuestionAsUser1();
            answerQuestionAsUser2();
            verifyUser1SeesAnswer();
        } finally {
            cleanup();
        }
    }

    private void askQuestionAsUser1() {
        navigate(user1Driver, "/questions/ask");

        WebElement titleInput = waitForVisible(user1Driver, user1Wait, StackOverflowLocators.ASK_TITLE_INPUT);
        titleInput.clear();
        titleInput.sendKeys(questionTitle);

        fillStacksEditor(user1Driver, user1Wait,
                StackOverflowLocators.ASK_BODY_BACKING_TEXTAREA,
                StackOverflowLocators.ASK_BODY_EDITOR_TEXTBOX,
                questionBody);

        WebElement tagsInput;
        try {
            tagsInput = waitForVisible(user1Driver, user1Wait, StackOverflowLocators.ASK_TAGS_INPUT);
            tagsInput.click();
            tagsInput.sendKeys("python");
            tagsInput.sendKeys("\n");
        } catch (AssertionError ignored) {
        }

        List<WebElement> deployOptions = user1Driver.findElements(StackOverflowLocators.DEPLOY_TO_STACKOVERFLOW_RADIO);
        if (!deployOptions.isEmpty()) {
            WebElement option = deployOptions.get(0);
            scrollIntoView(user1Driver, option);
            try {
                option.click();
            } catch (WebDriverException exception) {
                ((JavascriptExecutor) user1Driver).executeScript("arguments[0].click();", option);
            }
        }

        WebElement post = waitForClickable(user1Driver, user1Wait, StackOverflowLocators.POST_QUESTION_BUTTON);
        scrollIntoView(user1Driver, post);
        try {
            post.click();
        } catch (WebDriverException exception) {
            ((JavascriptExecutor) user1Driver).executeScript("arguments[0].click();", post);
        }

        WebDriverWait submitWait = new WebDriverWait(user1Driver, Duration.ofSeconds(FORM_SUBMIT_WAIT_SECONDS));
        try {
            submitWait.until(d -> d.getCurrentUrl().matches(".*/questions/\\d+/.*")
                    || hasRateLimitMessage(d) || hasDuplicateMessage(d));
        } catch (TimeoutException exception) {
            throw new AssertionError("Question was not posted (URL did not change to /questions/<id>/...). "
                    + snapshot(user1Driver), exception);
        }
        if (hasRateLimitMessage(user1Driver)) {
            assumeTrue(false, "Stack Overflow rate limit: new accounts can only post one question per 90 minutes. "
                    + "Wait and rerun, or use an account with reputation >= 10.");
        }
        if (hasDuplicateMessage(user1Driver)) {
            assumeTrue(false, "Stack Overflow flagged the question as a duplicate of a previous test post. "
                    + "Modify the question templates in StackOverflowTwoUserScenarioTest or wait for indexing to expire.");
        }
        questionUrl = user1Driver.getCurrentUrl();
        assertNotNull(questionUrl, "Question URL must be captured after Post Your Question");
        assertTrue(questionUrl.contains("/questions/"), "Captured URL must be a question page: " + questionUrl);
    }

    private void answerQuestionAsUser2() {
        assertNotNull(questionUrl, "Question must be created by user1 before user2 can answer");
        navigate(user2Driver, questionUrl.replace(BASE_URL, ""));

        WebElement formContainer = waitForPresent(user2Driver, user2Wait, StackOverflowLocators.ANSWER_FORM_CONTAINER);
        scrollIntoView(user2Driver, formContainer);

        fillStacksEditor(user2Driver, user2Wait,
                StackOverflowLocators.ANSWER_BACKING_TEXTAREA,
                StackOverflowLocators.ANSWER_EDITOR_TEXTBOX,
                answerBody);

        WebElement postAnswer = waitForClickable(user2Driver, user2Wait, StackOverflowLocators.POST_ANSWER_BUTTON);
        scrollIntoView(user2Driver, postAnswer);
        try {
            postAnswer.click();
        } catch (WebDriverException exception) {
            ((JavascriptExecutor) user2Driver).executeScript("arguments[0].click();", postAnswer);
        }

        WebDriverWait submitWait = new WebDriverWait(user2Driver, Duration.ofSeconds(FORM_SUBMIT_WAIT_SECONDS));
        try {
            submitWait.until(d -> bodyOf(d).contains(uniqueMarker));
        } catch (TimeoutException exception) {
            throw new AssertionError("Answer body marker did not appear on the question page after submit. "
                    + snapshot(user2Driver), exception);
        }
        assertTrue(!user2Driver.findElements(StackOverflowLocators.ANSWER_BLOCK).isEmpty(),
                "Answer block must be present after user2 posted an answer");
    }

    private void verifyUser1SeesAnswer() {
        user1Driver.get(questionUrl);
        waitForDocumentReady(user1Driver, user1Wait);
        try {
            user1Wait.until(d -> bodyOf(d).contains(uniqueMarker)
                    && !d.findElements(StackOverflowLocators.ANSWER_BLOCK).isEmpty());
        } catch (TimeoutException exception) {
            throw new AssertionError("User1 does not see user2 answer on the question page. "
                    + snapshot(user1Driver), exception);
        }
    }

    private void cleanup() {
        if (questionUrl == null) {
            return;
        }
        if (user2Driver != null) {
            tryDelete(user2Driver, user2Wait, StackOverflowLocators.ANSWER_DELETE_LINK, "user2 answer");
        }
        if (user1Driver != null) {
            tryDelete(user1Driver, user1Wait, StackOverflowLocators.QUESTION_DELETE_LINK, "user1 question");
        }
        if (user1Driver != null) {
            try {
                user1Driver.get(questionUrl);
                WebDriverWait shortWait = new WebDriverWait(user1Driver, Duration.ofSeconds(10));
                shortWait.until(d -> !d.findElements(StackOverflowLocators.DELETED_BADGE).isEmpty()
                        || d.getCurrentUrl().contains("/questions") && !d.getCurrentUrl().equals(questionUrl));
            } catch (WebDriverException ignored) {
                System.out.println("MANUAL CLEANUP NEEDED: " + questionUrl);
            }
        }
    }

    private void tryDelete(WebDriver driver, WebDriverWait wait, By deleteLink, String label) {
        try {
            driver.get(questionUrl);
            waitForDocumentReady(driver, wait);
            WebDriverWait deleteWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            WebElement link;
            try {
                link = deleteWait.until(ExpectedConditions.presenceOfElementLocated(deleteLink));
            } catch (TimeoutException exception) {
                System.out.println("Delete link not found for " + label + " — MANUAL CLEANUP NEEDED: " + questionUrl);
                return;
            }
            scrollIntoView(driver, link);
            try {
                link.click();
            } catch (WebDriverException exception) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
            }
            WebDriverWait confirmWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                WebElement confirm = confirmWait.until(
                        ExpectedConditions.elementToBeClickable(StackOverflowLocators.DELETE_CONFIRM_BUTTON));
                confirm.click();
                System.out.println("Deleted " + label + " on " + questionUrl);
            } catch (TimeoutException ignored) {
                System.out.println("Delete confirmation dialog did not appear for " + label
                        + " (assuming inline delete) — verify manually: " + questionUrl);
            }
        } catch (RuntimeException exception) {
            System.out.println("Cleanup of " + label + " failed: " + exception.getMessage()
                    + " — MANUAL CLEANUP NEEDED: " + questionUrl);
        }
    }

    private WebDriver createDriver(String userKey) {
        String browser = System.getProperty("browser", "chrome").toLowerCase(Locale.ROOT);
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        if ("firefox".equals(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addPreference("intl.accept_languages", "en-US");
            options.addPreference("dom.webdriver.enabled", false);
            options.addPreference("useAutomationExtension", false);
            String profileDir = System.getProperty("firefox.profileDir." + userKey, "").trim();
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
        String perUserDebugger = System.getProperty("chrome.debuggerAddress." + userKey, "").trim();
        if (!perUserDebugger.isEmpty() && !perUserDebugger.startsWith("${")) {
            options.setExperimentalOption("debuggerAddress", perUserDebugger);
            return new ChromeDriver(options);
        }
        options.addArguments("user1".equals(userKey) ? "--window-size=1440,1000" : "--window-size=1280,900");
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

    private WebDriverWait configureDriver(WebDriver driver) {
        StackOverflowAuthSession.applyStealth(driver);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    private void loadSession(WebDriver driver, Path cookies, String label) {
        if (cookies != null) {
            try {
                StackOverflowAuthSession.loadCookies(driver, cookies);
            } catch (IOException exception) {
                throw new AssertionError("Failed to load cookies for " + label + " from " + cookies, exception);
            }
        } else {
            driver.get(BASE_URL);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
        waitForDocumentReady(driver, wait);
        acceptCookiesIfShown(driver);

        if (cookies == null && !StackOverflowAuthSession.isAuthenticated(driver)) {
            String email = StackOverflowAuthSession.emailFor(label);
            String password = StackOverflowAuthSession.passwordFor(label);
            if (email != null && password != null) {
                try {
                    StackOverflowAuthSession.loginWithCredentials(driver, email, password, Duration.ofSeconds(TIMEOUT_SECONDS));
                } catch (RuntimeException loginFailure) {
                    System.out.println("Auto-login failed for " + label + ": " + loginFailure.getMessage());
                }
                waitForDocumentReady(driver, wait);
                acceptCookiesIfShown(driver);
            }
        }

        boolean launchedByUs = "user1".equals(label) ? user1Launcher != null : user2Launcher != null;
        if (!StackOverflowAuthSession.isAuthenticated(driver) && launchedByUs) {
            System.out.println("--- Manual login required for " + label + " ---");
            StackOverflowAuthSession.waitForManualLogin(driver, Duration.ofSeconds(300));
        }

        assumeTrue(StackOverflowAuthSession.isAuthenticated(driver),
                "Session for " + label + " not detected as authenticated. Check cookies or credentials — see docs/auth-setup.md.");
    }

    private void navigate(WebDriver driver, String path) {
        driver.get(BASE_URL + path);
        WebDriverWait wait = driver == user1Driver ? user1Wait : user2Wait;
        waitForDocumentReady(driver, wait);
        acceptCookiesIfShown(driver);
    }

    private WebElement waitForVisible(WebDriver driver, WebDriverWait wait, By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException exception) {
            throw new AssertionError("Element not visible: " + locator + ". " + snapshot(driver), exception);
        }
    }

    private WebElement waitForClickable(WebDriver driver, WebDriverWait wait, By locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException exception) {
            throw new AssertionError("Element not clickable: " + locator + ". " + snapshot(driver), exception);
        }
    }

    private WebElement waitForPresent(WebDriver driver, WebDriverWait wait, By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException exception) {
            throw new AssertionError("Element not present: " + locator + ". " + snapshot(driver), exception);
        }
    }

    /**
     * Fills a Stacks Editor (ProseMirror-based contenteditable). Two flavours seen in
     * the wild: (a) Ask Wizard for fresh accounts — only the [role=textbox] div, no
     * backing textarea; (b) Answer form on a question page — a hidden backing
     * textarea[name=post-text] plus a [role=textbox] mirror.
     *
     * Strategy: prefer typing into the visible textbox. If we can't see it, fall back
     * to JS-writing the backing textarea (with input/change events so Svelte/Stimulus
     * controllers pick it up).
     */
    private void fillStacksEditor(WebDriver driver, WebDriverWait wait, By backingTextarea, By editorTextbox, String text) {
        try {
            WebElement textbox = wait.until(ExpectedConditions.visibilityOfElementLocated(editorTextbox));
            scrollIntoView(driver, textbox);
            try {
                textbox.click();
            } catch (WebDriverException ignored) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", textbox);
            }
            textbox.sendKeys(text);
            return;
        } catch (TimeoutException ignored) {
        }
        List<WebElement> backing = driver.findElements(backingTextarea);
        if (backing.isEmpty()) {
            throw new AssertionError("Neither editor textbox nor backing textarea found for body. "
                    + snapshot(driver));
        }
        ((JavascriptExecutor) driver).executeScript(
                "const ta = arguments[0]; ta.value = arguments[1];"
                        + " ta.dispatchEvent(new Event('input', { bubbles: true }));"
                        + " ta.dispatchEvent(new Event('change', { bubbles: true }));",
                backing.get(0), text);
    }

    private void scrollIntoView(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        } catch (WebDriverException ignored) {
        }
    }

    private void waitForDocumentReady(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(d -> {
                Object readyState = ((JavascriptExecutor) d).executeScript("return document.readyState");
                return "complete".equals(readyState) || "interactive".equals(readyState);
            });
        } catch (TimeoutException ignored) {
        }
    }

    private void acceptCookiesIfShown(WebDriver driver) {
        for (WebElement button : driver.findElements(StackOverflowLocators.COOKIE_BUTTON)) {
            if (button.isDisplayed() && button.isEnabled()) {
                try {
                    button.click();
                    return;
                } catch (WebDriverException ignored) {
                }
            }
        }
    }

    private boolean hasRateLimitMessage(WebDriver driver) {
        String text = bodyOf(driver);
        return text.contains("can only post once every") || text.contains("every 90 minutes")
                || text.contains("can only ask one question every");
    }

    private boolean hasDuplicateMessage(WebDriver driver) {
        String text = bodyOf(driver);
        return text.contains("appears to be a duplicate") || text.contains("This post appears to be a duplicate");
    }

    private String bodyOf(WebDriver driver) {
        try {
            return driver.findElement(By.xpath("//body")).getText();
        } catch (WebDriverException exception) {
            return "";
        }
    }

    private String snapshot(WebDriver driver) {
        String body = bodyOf(driver).replaceAll("\\s+", " ").trim();
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        return "url=" + driver.getCurrentUrl() + ", title=" + driver.getTitle() + ", body=" + body;
    }

    private void quitQuietly(WebDriver driver) {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (WebDriverException ignored) {
        }
    }
}
